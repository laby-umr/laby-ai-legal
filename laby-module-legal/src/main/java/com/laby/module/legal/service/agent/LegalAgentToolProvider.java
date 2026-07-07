package com.laby.module.legal.service.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.security.core.LoginUser;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.ai.util.AiUtils;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.framework.agentscope.chat.LegalContractCompactionSummaryContext;
import com.laby.module.legal.tool.agent.LegalAgentToolContext;
import com.laby.module.legal.tool.agent.LegalAgentToolRuntimeContext;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 法务合同 Agent Tool 注册与 RuntimeContext 构建。
 */
@Slf4j
@Service
public class LegalAgentToolProvider {

    /** 按 sessionId 缓存 Tool 上下文，供异步 Tool 线程 fallback（AgentScope 需 ToolExecutionContext 注入） */
    private static final ConcurrentHashMap<String, LegalAgentToolRuntimeContext> SESSION_TOOL_CONTEXTS =
            new ConcurrentHashMap<>();
    /** 按 contractId 缓存，sessionId 丢失时的兜底 */
    private static final ConcurrentHashMap<Long, LegalAgentToolRuntimeContext> CONTRACT_TOOL_CONTEXTS =
            new ConcurrentHashMap<>();

    public static LegalAgentToolRuntimeContext getSessionToolContext(String sessionId) {
        return StrUtil.isBlank(sessionId) ? null : SESSION_TOOL_CONTEXTS.get(sessionId);
    }

    public static LegalAgentToolRuntimeContext getContractToolContext(Long contractId) {
        return contractId == null ? null : CONTRACT_TOOL_CONTEXTS.get(contractId);
    }

    public static void removeSessionToolContext(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            LegalAgentToolRuntimeContext removed = SESSION_TOOL_CONTEXTS.remove(sessionId);
            if (removed != null && removed.getContractId() != null) {
                CONTRACT_TOOL_CONTEXTS.remove(removed.getContractId(), removed);
            }
        }
    }

    private static final List<String> READONLY_TOOL_NAMES = List.of(
            "legal_get_contract_meta",
            "legal_search_paragraphs",
            "legal_get_audit_opinions",
            "legal_search_knowledge",
            "legal_get_audit_report",
            "legal_compare_audit_rounds"
    );

    private static final List<String> WRITE_TOOL_NAMES = List.of(
            "legal_adopt_opinion",
            "legal_batch_adopt_pending_opinions",
            "legal_propose_adopt_opinion",
            "legal_propose_skip_paragraph"
    );

    /** @deprecated 使用 {@link #WRITE_TOOL_NAMES} */
    @Deprecated
    private static final List<String> PROPOSAL_TOOL_NAMES = WRITE_TOOL_NAMES;

    public static List<String> getReadonlyToolNames() {
        return READONLY_TOOL_NAMES;
    }

    public static List<String> getProposalToolNames() {
        return WRITE_TOOL_NAMES;
    }

    public static List<String> getWriteToolNames() {
        return WRITE_TOOL_NAMES;
    }

    @Resource
    private ApplicationContext applicationContext;

    public Toolkit buildToolkit(boolean allowProposal) {
        return buildToolkit(allowProposal, null);
    }

    public Toolkit buildToolkit(boolean allowProposal, List<String> skillPackToolNames) {
        Toolkit toolkit = new Toolkit();
        for (String name : buildEffectiveToolNames(allowProposal, skillPackToolNames)) {
            if (!applicationContext.containsBean(name)) {
                log.warn("[buildToolkit] 未解析到 Tool Bean: {}", name);
                continue;
            }
            toolkit.registerTool(applicationContext.getBean(name));
        }
        return toolkit;
    }

    public RuntimeContext buildRuntimeContext(Long contractId, String sessionId, boolean allowProposal) {
        return buildRuntimeContext(contractId, sessionId, allowProposal, null);
    }

    public RuntimeContext buildRuntimeContext(Long contractId, String sessionId, boolean allowProposal,
                                              Long contractTenantId) {
        return buildRuntimeContext(contractId, sessionId, allowProposal, contractTenantId, null);
    }

    public RuntimeContext buildRuntimeContext(Long contractId, String sessionId, boolean allowProposal,
                                              Long contractTenantId,
                                              LegalContractCompactionSummaryContext compactionContext) {
        Map<String, Object> context = buildToolContext(contractId, sessionId, allowProposal, contractTenantId);
        LegalAgentToolRuntimeContext toolContext = LegalAgentToolRuntimeContext.from(context);
        ToolExecutionContext toolExecutionContext = ToolExecutionContext.builder()
                .register(toolContext)
                .build();
        if (StrUtil.isNotBlank(sessionId)) {
            SESSION_TOOL_CONTEXTS.put(sessionId, toolContext);
        }
        if (contractId != null) {
            CONTRACT_TOOL_CONTEXTS.put(contractId, toolContext);
        }
        RuntimeContext.Builder builder = RuntimeContext.builder()
                .sessionId(sessionId)
                .put(LegalAgentToolRuntimeContext.class, toolContext)
                .toolExecutionContext(toolExecutionContext);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId != null) {
            builder.userId(String.valueOf(userId));
        }
        if (compactionContext != null) {
            builder.put(LegalContractCompactionSummaryContext.class, compactionContext);
        }
        return builder.build();
    }

    public RuntimeContext buildAgentRuntimeContext(LegalContractDO contract, String sessionId,
                                                   boolean allowProposal, boolean persistCompactionSummary) {
        LegalContractCompactionSummaryContext compactionContext = null;
        if (persistCompactionSummary && contract != null && contract.getId() != null) {
            compactionContext = LegalContractCompactionSummaryContext.of(
                    contract.getId(), SecurityFrameworkUtils.getLoginUserId(), sessionId);
        }
        return buildRuntimeContext(contract.getId(), sessionId, allowProposal,
                contract.getTenantId(), compactionContext);
    }

    /**
     * 获取 Agent 可用 Tool 名称列表；开启提案模式时追加 propose Tool。
     */
    public List<String> getAgentToolNames(boolean allowProposal) {
        return getAgentToolNames(allowProposal, null);
    }

    public List<String> getAgentToolNames(boolean allowProposal, List<String> skillPackToolNames) {
        return buildEffectiveToolNames(allowProposal, skillPackToolNames);
    }

    public Map<String, Object> buildToolContext(Long contractId, String sessionId, boolean allowProposal) {
        return buildToolContext(contractId, sessionId, allowProposal, null);
    }

    public Map<String, Object> buildToolContext(Long contractId, String sessionId, boolean allowProposal,
                                                Long contractTenantId) {
        Map<String, Object> context = AiUtils.buildCommonToolContext();
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
            if (loginUser != null && loginUser.getTenantId() != null) {
                tenantId = loginUser.getTenantId();
            }
        }
        if (tenantId == null && contractTenantId != null) {
            tenantId = contractTenantId;
        }
        if (tenantId != null) {
            context.put(AiUtils.TOOL_CONTEXT_TENANT_ID, tenantId);
        }
        context.put(LegalAgentToolContext.CONTRACT_ID, contractId);
        context.put(LegalAgentToolContext.READONLY, !allowProposal);
        if (StrUtil.isNotBlank(sessionId)) {
            context.put(LegalAgentToolContext.SESSION_ID, sessionId);
        }
        return context;
    }

    public Map<String, Object> buildToolContext(Long contractId, String sessionId) {
        return buildToolContext(contractId, sessionId, false);
    }

    public boolean hasReadOnlyTools() {
        return READONLY_TOOL_NAMES.stream().anyMatch(applicationContext::containsBean);
    }

    private List<String> buildEffectiveToolNames(boolean allowProposal, List<String> skillPackToolNames) {
        if (CollUtil.isEmpty(skillPackToolNames)) {
            List<String> names = new ArrayList<>(READONLY_TOOL_NAMES);
            if (allowProposal) {
                names.addAll(WRITE_TOOL_NAMES);
            }
            return names;
        }
        List<String> effective = new ArrayList<>();
        for (String name : skillPackToolNames) {
            if (StrUtil.isBlank(name)) {
                continue;
            }
            String trimmed = name.trim();
            if (READONLY_TOOL_NAMES.contains(trimmed)) {
                effective.add(trimmed);
            } else if (allowProposal && WRITE_TOOL_NAMES.contains(trimmed)) {
                effective.add(trimmed);
            } else if (allowProposal && "legal_propose_adopt_opinion".equals(trimmed)) {
                effective.add("legal_adopt_opinion");
            }
        }
        if (CollUtil.isEmpty(effective)) {
            log.warn("[buildEffectiveToolNames] SkillPack tool 配置无效，降级为只读 Tool 集");
            return new ArrayList<>(READONLY_TOOL_NAMES);
        }
        return effective;
    }

}
