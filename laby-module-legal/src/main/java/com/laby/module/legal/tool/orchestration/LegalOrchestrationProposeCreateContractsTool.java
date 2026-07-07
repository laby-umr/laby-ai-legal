package com.laby.module.legal.tool.orchestration;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.ai.policy.LegalAiPolicyResolver;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationProposalService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_TYPE_NOT_RESOLVED;

@Component("legal_orchestration_propose_create_contracts")
public class LegalOrchestrationProposeCreateContractsTool {

    @Resource
    private LegalOrchestrationSessionService sessionService;
    @Resource
    private LegalOrchestrationProposalService proposalService;
    @Resource
    private LegalAiPolicyResolver policyResolver;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;

    @Data
    public static class Response {
        private String proposalNo;
        private Long sessionId;
        private String message;
    }

    @Tool(name = "legal_orchestration_propose_create_contracts",
            description = "生成批量创建合同提案（需用户在前端确认执行）。文件须已完成分类确认（confirmedTypeId）。sessionId 可省略。")
    public Response proposeCreateContracts(
            @ToolParam(name = "sessionId", description = "编排会话编号（可省略）", required = false) Long sessionId,
            @ToolParam(name = "partyRole", description = "我方立场 A/B/OTHER", required = false) String partyRole,
            @ToolParam(name = "auditLevel", description = "审核强度", required = false) String auditLevel,
            @ToolParam(name = "editable", description = "是否可编辑", required = false) Boolean editable,
            LegalOrchestrationToolRuntimeContext toolContext) {
        LegalOrchestrationToolRuntimeContext ctx = LegalOrchestrationToolSupport.resolve(toolContext);
        Long conversationId = LegalOrchestrationToolSupport.requireConversationId(ctx);
        Long userId = LegalOrchestrationToolSupport.requireUserId(ctx);
        LegalOrchestrationSessionDO session = LegalOrchestrationToolSupport.resolveOrchestrationSession(
                sessionService, sessionId, ctx);

        List<LegalOrchestrationFileItemDO> files = sessionService.listFileItems(session.getId());
        if (CollUtil.isEmpty(files) || files.stream().anyMatch(file -> file.getConfirmedTypeId() == null)) {
            throw exception(ORCHESTRATION_TYPE_NOT_RESOLVED);
        }

        LegalAiPolicyBO policy = policyResolver.resolveForSession(session, ctx.getModelId(),
                partyRole, auditLevel);
        Long primaryTypeId = files.get(0).getConfirmedTypeId();
        skillPackSnapshotService.freezeSnapshotOnPolicy(policy, primaryTypeId);
        sessionService.syncPolicy(session.getId(), policy);

        String proposalNo = proposalService.createCreateContractsProposal(
                conversationId,
                userId,
                session.getId(),
                policy,
                editable != null ? editable : Boolean.TRUE);

        Response response = new Response();
        response.setProposalNo(proposalNo);
        response.setSessionId(session.getId());
        response.setMessage("已生成创建合同提案 proposalNo=" + proposalNo + "，请用户确认后执行");
        return response;
    }

}
