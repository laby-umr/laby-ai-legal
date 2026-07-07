package com.laby.module.legal.tool.agent;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.service.agent.LegalAgentStepLogContext;
import com.laby.module.legal.service.agent.LegalAgentToolProvider;
import com.laby.module.legal.service.contract.LegalContractService;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

/**
 * Agent Tool 公共校验与上下文读取。
 */
public final class LegalAgentToolSupport {

    private LegalAgentToolSupport() {
    }

    /**
     * 解析 Tool 运行时上下文。AgentScope 需通过 {@code ToolExecutionContext} 注入；
     * 异步 Tool 线程缺失时从 session 缓存 / StepLog 上下文回填 contractId。
     */
    public static LegalAgentToolRuntimeContext resolve(LegalAgentToolRuntimeContext toolContext) {
        if (toolContext != null && toolContext.getContractId() != null) {
            return toolContext;
        }
        String sessionId = resolveSessionId(toolContext);
        if (StrUtil.isNotBlank(sessionId)) {
            LegalAgentToolRuntimeContext cached = LegalAgentToolProvider.getSessionToolContext(sessionId);
            if (cached != null && cached.getContractId() != null) {
                return cached;
            }
            LegalAgentStepLogContext.State sessionState = LegalAgentStepLogContext.getSessionState(sessionId);
            if (sessionState != null && sessionState.getContractId() != null) {
                return fromStepLogState(sessionState);
            }
        }
        LegalAgentStepLogContext.State state = LegalAgentStepLogContext.getState();
        if (state != null && state.getContractId() != null) {
            return fromStepLogState(state);
        }
        Long contractId = toolContext != null ? toolContext.getContractId() : null;
        if (contractId == null && state != null) {
            contractId = state.getContractId();
        }
        if (contractId != null) {
            LegalAgentToolRuntimeContext byContract = LegalAgentToolProvider.getContractToolContext(contractId);
            if (byContract != null) {
                return byContract;
            }
        }
        return toolContext;
    }

    public static Long requireContractId(LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolRuntimeContext resolved = resolve(toolContext);
        if (resolved == null || resolved.getContractId() == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return resolved.getContractId();
    }

    public static void assertReadonly(LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolRuntimeContext resolved = resolve(toolContext);
        if (resolved == null || !resolved.isReadonly()) {
            throw new IllegalStateException("当前 Agent 会话为只读模式，禁止写操作");
        }
    }

    public static void assertProposalMode(LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolRuntimeContext resolved = resolve(toolContext);
        if (resolved == null || resolved.isReadonly()) {
            throw new IllegalStateException("当前 Agent 会话未开启写操作提案");
        }
    }

    public static String requireSessionId(LegalAgentToolRuntimeContext toolContext) {
        String sessionId = resolveSessionId(resolve(toolContext));
        if (StrUtil.isNotBlank(sessionId)) {
            return sessionId;
        }
        throw new IllegalStateException("无法获取 Agent 会话 ID");
    }

    public static String resolveSessionId(LegalAgentToolRuntimeContext toolContext) {
        if (toolContext != null && StrUtil.isNotBlank(toolContext.getSessionId())) {
            return toolContext.getSessionId();
        }
        LegalAgentStepLogContext.State state = LegalAgentStepLogContext.getState();
        if (state != null && StrUtil.isNotBlank(state.getSessionId())) {
            return state.getSessionId();
        }
        return null;
    }

    public static Long requireUserId(LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolRuntimeContext resolved = resolve(toolContext);
        if (resolved != null && resolved.getLoginUser() != null) {
            return resolved.getLoginUser().getId();
        }
        LegalAgentStepLogContext.State state = LegalAgentStepLogContext.getState();
        if (state != null && state.getUserId() != null) {
            return state.getUserId();
        }
        throw new IllegalStateException("无法获取当前用户");
    }

    public static LegalContractDO requireContract(LegalContractService contractService,
                                                  LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolRuntimeContext resolved = resolve(toolContext);
        Long contractId = requireContractId(resolved);
        Long tenantId = resolveTenantId(resolved);
        if (tenantId != null) {
            return TenantUtils.execute(tenantId, () -> contractService.validateContractExists(contractId));
        }
        // Reactor 异步 Tool 线程上 TenantContextHolder 常为空，先忽略租户查合同再恢复租户
        LegalContractDO contract = TenantUtils.executeIgnore(
                () -> contractService.validateContractExists(contractId));
        if (contract.getTenantId() != null) {
            TenantContextHolder.setTenantId(contract.getTenantId());
        }
        return contract;
    }

    public static Long resolveTenantId(LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolRuntimeContext resolved = resolve(toolContext);
        if (resolved != null && resolved.getTenantId() != null) {
            return resolved.getTenantId();
        }
        if (resolved != null && resolved.getLoginUser() != null
                && resolved.getLoginUser().getTenantId() != null) {
            return resolved.getLoginUser().getTenantId();
        }
        LegalAgentStepLogContext.State state = LegalAgentStepLogContext.getState();
        if (state != null && state.getTenantId() != null) {
            return state.getTenantId();
        }
        return TenantContextHolder.getTenantId();
    }

    private static LegalAgentToolRuntimeContext fromStepLogState(LegalAgentStepLogContext.State state) {
        return LegalAgentToolRuntimeContext.builder()
                .contractId(state.getContractId())
                .sessionId(state.getSessionId())
                .readonly(!state.isAllowProposal())
                .tenantId(state.getTenantId())
                .loginUser(state.getLoginUser())
                .build();
    }

}
