package com.laby.module.legal.service.orchestration;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;

import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;

import java.util.List;

/**
 * 法务 AI 编排会话 Service
 */
public interface LegalOrchestrationSessionService {

    LegalOrchestrationSessionDO getOrCreateSession(Long conversationId, Long userId, Long modelId);

    LegalOrchestrationSessionDO getOrCreateSession(Long conversationId, Long userId, LegalAiPolicyBO policy);

    void syncPolicy(Long sessionId, LegalAiPolicyBO policy);

    LegalOrchestrationSessionDO validateSessionExists(Long sessionId);

    LegalOrchestrationSessionDO getByConversationId(Long conversationId);

    void updatePhase(Long sessionId, String phase);

    List<LegalOrchestrationFileItemDO> registerFiles(Long sessionId, List<Long> infraFileIds, List<String> fileNames);

    List<LegalOrchestrationFileItemDO> listFileItems(Long sessionId);

}
