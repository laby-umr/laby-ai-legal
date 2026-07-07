package com.laby.module.legal.service.orchestration;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;
import com.laby.module.legal.dal.mysql.agent.LegalAgentProposalMapper;
import com.laby.module.legal.enums.LegalOrchestrationConstants;
import com.laby.module.legal.enums.agent.LegalAgentProposalActionEnum;
import com.laby.module.legal.enums.agent.LegalAgentProposalStatusEnum;
import com.laby.module.legal.enums.orchestration.LegalOrchestrationPhaseEnum;
import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationClassificationItemBO;
import com.laby.module.legal.tool.agent.LegalAgentSseEventHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 法务编排提案 Service
 */
@Service
public class LegalOrchestrationProposalService {

    @Resource
    private LegalAgentProposalMapper proposalMapper;
    @Resource
    private LegalOrchestrationSessionService sessionService;
    @Resource
    private LegalAiPolicyResolver policyResolver;

    public String createClassifyConfirmProposal(Long conversationId, Long userId, Long sessionId,
                                                List<LegalOrchestrationClassificationItemBO> items) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("mappings", items.stream().map(item -> {
            Map<String, Object> mapping = new HashMap<>();
            mapping.put("fileItemId", item.getFileItemId());
            mapping.put("fileName", item.getFileName());
            mapping.put("typeId", item.getSuggestedTypeId());
            mapping.put("typeName", item.getSuggestedTypeName());
            mapping.put("reason", item.getReason());
            mapping.put("needNewType", item.isNeedNewType());
            return mapping;
        }).collect(Collectors.toList()));

        String title = "确认合同文件分类（" + items.size() + " 个文件）";
        sessionService.updatePhase(sessionId, LegalOrchestrationPhaseEnum.CLASSIFY_PENDING.getPhase());
        return insertProposal(conversationId, userId, sessionId, LegalAgentProposalActionEnum.CLASSIFY_CONFIRM,
                title, payload);
    }

    public String createCreateContractsProposal(Long conversationId, Long userId, Long sessionId,
                                                LegalAiPolicyBO policy, Boolean editable) {
        Map<String, Object> payload = policyResolver.toPayloadMap(policy);
        payload.put("sessionId", sessionId);
        payload.put("editable", editable);

        String title = "确认创建合同并启动审核";
        sessionService.updatePhase(sessionId, LegalOrchestrationPhaseEnum.CREATE_PENDING.getPhase());
        return insertProposal(conversationId, userId, sessionId, LegalAgentProposalActionEnum.CREATE_CONTRACTS_BATCH,
                title, payload);
    }

    public String createTypePackageProposal(Long conversationId, Long userId, Long sessionId,
                                            Long draftId, String typeName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("draftId", draftId);
        payload.put("typeName", typeName);
        String title = "确认发布合同类型：" + typeName;
        sessionService.updatePhase(sessionId, LegalOrchestrationPhaseEnum.TYPE_PACKAGE_PENDING.getPhase());
        return insertProposal(conversationId, userId, sessionId, LegalAgentProposalActionEnum.CREATE_TYPE_PACKAGE,
                title, payload);
    }

    public List<LegalAgentProposalDO> listPendingByConversationId(Long conversationId) {
        return proposalMapper.selectPendingListByConversationId(conversationId);
    }

    private String insertProposal(Long conversationId, Long userId, Long sessionId,
                                  LegalAgentProposalActionEnum action, String title,
                                  Map<String, Object> payload) {
        String proposalNo = IdUtil.fastSimpleUUID();
        proposalMapper.insert(LegalAgentProposalDO.builder()
                .proposalNo(proposalNo)
                .conversationId(conversationId)
                .contractId(null)
                .userId(userId)
                .sessionId(String.valueOf(conversationId))
                .action(action.getAction())
                .status(LegalAgentProposalStatusEnum.PENDING.getStatus())
                .title(title)
                .payloadJson(JsonUtils.toJsonString(payload))
                .expireTime(LocalDateTime.now().plusMinutes(LegalOrchestrationConstants.PROPOSAL_TTL_MINUTES))
                .build());
        LegalAgentSseEventHolder.pushProposal(String.valueOf(conversationId), proposalNo,
                action.getAction(), title, payload);
        return proposalNo;
    }

}
