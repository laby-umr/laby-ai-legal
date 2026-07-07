package com.laby.module.legal.service.agent;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.framework.security.core.util.SecurityFrameworkUtils;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphSkipReqVO;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.agent.LegalAgentProposalMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.agent.LegalAgentProposalActionEnum;
import com.laby.module.legal.enums.agent.LegalAgentProposalStatusEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.opinion.LegalAuditOpinionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationContractCreateExecutor;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationTypePackageService;
import com.laby.module.legal.tool.agent.LegalAgentSseEventHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.AGENT_PROPOSAL_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.OPINION_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_SESSION_NOT_EXISTS;

/**
 * Agent 写操作提案 Service
 */
@Slf4j
@Service
public class LegalAgentProposalService {

    private static final int PROPOSAL_TTL_MINUTES = 5;

    @Resource
    private LegalAgentProposalMapper proposalMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalAuditOpinionService opinionService;
    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAgentStepLogService agentStepLogService;
    @Resource
    private LegalOrchestrationContractCreateExecutor orchestrationContractCreateExecutor;
    @Resource
    private LegalOrchestrationTypePackageService orchestrationTypePackageService;
    @Resource
    private LegalOrchestrationSessionService orchestrationSessionService;

    /**
     * 创建「采纳意见」提案（不写库变更意见状态）
     */
    public String createAdoptOpinionProposal(Long contractId, Long userId, String sessionId,
                                             Long opinionId, String reason) {
        LegalAuditOpinionDO opinion = opinionMapper.selectById(opinionId);
        if (opinion == null || !contractId.equals(opinion.getContractId())) {
            throw exception(OPINION_NOT_EXISTS);
        }
        if (!LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
            throw exception(OPINION_NOT_EXISTS);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("opinionId", opinionId);
        payload.put("reason", StrUtil.sub(StrUtil.blankToDefault(reason, ""), 0, 200));
        String title = "采纳意见：" + StrUtil.blankToDefault(opinion.getTitle(), "#" + opinionId);
        return createProposal(contractId, userId, sessionId,
                LegalAgentProposalActionEnum.ADOPT_OPINION, title, payload);
    }

    /**
     * 创建「跳过段落审核」提案
     */
    public String createSkipParagraphProposal(Long contractId, Long userId, String sessionId,
                                              String paragraphId, boolean skipAudit, String reason) {
        contractService.validateContractExists(contractId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("paragraphId", paragraphId);
        payload.put("skipAudit", skipAudit);
        payload.put("reason", StrUtil.sub(StrUtil.blankToDefault(reason, ""), 0, 200));
        String title = (skipAudit ? "跳过段落审核：" : "恢复段落审核：") + paragraphId;
        return createProposal(contractId, userId, sessionId,
                LegalAgentProposalActionEnum.SKIP_PARAGRAPH, title, payload);
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeProposal(String proposalNo) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        LegalAgentProposalDO proposal = proposalMapper.selectByProposalNo(proposalNo);
        if (proposal == null) {
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
        if (!userId.equals(proposal.getUserId())) {
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
        validateProposalConversationAccess(proposal, userId);
        if (!LegalAgentProposalStatusEnum.PENDING.getStatus().equals(proposal.getStatus())) {
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
        if (proposal.getExpireTime() != null && proposal.getExpireTime().isBefore(LocalDateTime.now())) {
            proposalMapper.updateById(new LegalAgentProposalDO()
                    .setId(proposal.getId())
                    .setStatus(LegalAgentProposalStatusEnum.EXPIRED.getStatus()));
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }

        LegalAgentProposalActionEnum action = LegalAgentProposalActionEnum.of(proposal.getAction());
        if (action == null) {
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
        switch (action) {
            case ADOPT_OPINION -> executeAdopt(proposal);
            case SKIP_PARAGRAPH -> executeSkipParagraph(proposal);
            case CLASSIFY_CONFIRM -> orchestrationContractCreateExecutor.executeClassifyConfirm(proposal);
            case CREATE_CONTRACTS_BATCH -> orchestrationContractCreateExecutor.executeCreateContractsBatch(proposal);
            case CREATE_TYPE_PACKAGE -> orchestrationTypePackageService.publishFromProposalPayload(
                    JsonUtils.parseObject(proposal.getPayloadJson(), Map.class));
            default -> throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }

        proposalMapper.updateById(new LegalAgentProposalDO()
                .setId(proposal.getId())
                .setStatus(LegalAgentProposalStatusEnum.EXECUTED.getStatus()));
        if (proposal.getContractId() != null) {
            agentStepLogService.logProposal(proposal.getContractId(), proposal.getUserId(), proposal.getSessionId(),
                    "提案已执行: " + proposal.getProposalNo() + " " + proposal.getAction());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelProposal(String proposalNo) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        LegalAgentProposalDO proposal = proposalMapper.selectByProposalNo(proposalNo);
        if (proposal == null || !userId.equals(proposal.getUserId())) {
            return;
        }
        validateProposalConversationAccess(proposal, userId);
        if (LegalAgentProposalStatusEnum.PENDING.getStatus().equals(proposal.getStatus())) {
            proposalMapper.updateById(new LegalAgentProposalDO()
                    .setId(proposal.getId())
                    .setStatus(LegalAgentProposalStatusEnum.CANCELLED.getStatus()));
            agentStepLogService.logProposal(proposal.getContractId(), proposal.getUserId(), proposal.getSessionId(),
                    "提案已取消: " + proposal.getProposalNo());
        }
    }

    /**
     * 将已过期的待处理提案标记为 EXPIRED
     *
     * @return 更新条数
     */
    public int expirePendingProposals() {
        return TenantUtils.executeIgnore(() -> proposalMapper.updateExpiredPending(LocalDateTime.now()));
    }

    private void validateProposalConversationAccess(LegalAgentProposalDO proposal, Long userId) {
        if (proposal.getConversationId() == null) {
            return;
        }
        LegalOrchestrationSessionDO session =
                orchestrationSessionService.getByConversationId(proposal.getConversationId());
        if (session == null || !userId.equals(session.getUserId())) {
            throw exception(ORCHESTRATION_SESSION_NOT_EXISTS);
        }
    }

    private void executeAdopt(LegalAgentProposalDO proposal) {
        Map<?, ?> payload = JsonUtils.parseObject(proposal.getPayloadJson(), Map.class);
        Object opinionIdObj = payload != null ? payload.get("opinionId") : null;
        if (opinionIdObj == null) {
            throw exception(OPINION_NOT_EXISTS);
        }
        Long opinionId = Long.parseLong(String.valueOf(opinionIdObj));
        opinionService.adopt(opinionId);
    }

    private void executeSkipParagraph(LegalAgentProposalDO proposal) {
        Map<?, ?> payload = JsonUtils.parseObject(proposal.getPayloadJson(), Map.class);
        if (payload == null) {
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
        LegalContractParagraphSkipReqVO reqVO = new LegalContractParagraphSkipReqVO();
        reqVO.setContractId(proposal.getContractId());
        reqVO.setParagraphId(String.valueOf(payload.get("paragraphId")));
        Object skip = payload.get("skipAudit");
        reqVO.setSkipAudit(skip instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(skip)));
        contractService.updateParagraphSkipAudit(reqVO);
    }

    private String createProposal(Long contractId, Long userId, String sessionId,
                                  LegalAgentProposalActionEnum action, String title,
                                  Map<String, Object> payload) {
        String proposalNo = IdUtil.fastSimpleUUID();
        proposalMapper.insert(LegalAgentProposalDO.builder()
                .proposalNo(proposalNo)
                .contractId(contractId)
                .userId(userId)
                .sessionId(sessionId)
                .action(action.getAction())
                .status(LegalAgentProposalStatusEnum.PENDING.getStatus())
                .title(title)
                .payloadJson(JsonUtils.toJsonString(payload))
                .expireTime(LocalDateTime.now().plusMinutes(PROPOSAL_TTL_MINUTES))
                .build());

        LegalAgentSseEventHolder.pushProposal(sessionId, proposalNo, action.getAction(), title, payload);
        agentStepLogService.logProposal(contractId, userId, sessionId,
                "提案已创建: " + proposalNo + " " + action.getAction());
        return proposalNo;
    }

}
