package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatReqVO;
import com.laby.module.legal.service.agent.LegalAgentSessionGuard;
import com.laby.module.legal.service.agent.LegalAgentStepLogContext;
import com.laby.module.legal.service.agent.LegalAgentStepLogService;
import com.laby.module.legal.service.agent.LegalAgentToolProvider;
import com.laby.module.legal.tool.agent.LegalAgentSseEventHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.SignalType;

/**
 * 合同问答 Agent 模式：Tool 可用性判断与会话清理。
 */
@Slf4j
@Component
public class LegalContractChatAgentSupport {

    @Resource
    private LegalAgentToolProvider legalAgentToolProvider;
    @Resource
    private LegalAgentStepLogService agentStepLogService;
    @Resource
    private LegalAgentSessionGuard agentSessionGuard;

    public static boolean isAgentMode(LegalContractChatReqVO reqVO) {
        return Boolean.TRUE.equals(reqVO.getAgentMode());
    }

    /**
     * 解析是否启用 Agent：请求开启且至少注册一个只读 Tool，否则降级普通模式。
     */
    public boolean resolveAgentMode(LegalContractChatReqVO reqVO) {
        if (!isAgentMode(reqVO)) {
            return false;
        }
        if (legalAgentToolProvider.hasReadOnlyTools()) {
            return true;
        }
        log.warn("[resolveAgentMode][contractId={}] Agent 已开启但未解析到 Tool，降级普通模式",
                reqVO.getContractId());
        return false;
    }

    public void cleanupAgentStream(String sessionId, SignalType signal) {
        if (StrUtil.isNotBlank(sessionId)) {
            if (LegalAgentStepLogContext.isActive()) {
                agentStepLogService.logLlm("流式结束: " + signal);
            }
            LegalAgentSseEventHolder.unbindSession(sessionId);
            LegalAgentStepLogContext.removeSession(sessionId);
            agentSessionGuard.release(sessionId);
        } else {
            LegalAgentStepLogContext.unbindThread();
        }
    }

}
