package com.laby.module.legal.tool.orchestration;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationClassificationService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationProposalService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationClassificationItemBO;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("legal_orchestration_propose_file_classification")
public class LegalOrchestrationProposeFileClassificationTool {

    @Resource
    private LegalOrchestrationSessionService sessionService;
    @Resource
    private LegalOrchestrationClassificationService classificationService;
    @Resource
    private LegalOrchestrationProposalService proposalService;

    @Data
    public static class Response {
        private String proposalNo;
        private Long sessionId;
        private List<LegalOrchestrationClassificationItemBO> classifications;
        private String message;
    }

    @Tool(name = "legal_orchestration_propose_file_classification",
            description = "对已登记文件进行语义分类并生成分类确认提案，需用户确认后才可落库")
    public Response proposeFileClassification(
            @ToolParam(name = "sessionId", description = "编排会话编号", required = false) Long sessionId,
            LegalOrchestrationToolRuntimeContext toolContext) {
        Long conversationId = LegalOrchestrationToolSupport.requireConversationId(toolContext);
        Long userId = LegalOrchestrationToolSupport.requireUserId(toolContext);

        LegalOrchestrationSessionDO session;
        if (sessionId != null) {
            session = sessionService.validateSessionExists(sessionId);
        } else {
            session = sessionService.getOrCreateSession(conversationId, userId,
                    toolContext != null ? toolContext.getModelId() : null);
        }

        List<LegalOrchestrationClassificationItemBO> items =
                classificationService.classifyFiles(session.getId(), session.getModelId());
        String proposalNo = proposalService.createClassifyConfirmProposal(
                conversationId, userId, session.getId(), items);

        Response response = new Response();
        response.setProposalNo(proposalNo);
        response.setSessionId(session.getId());
        response.setClassifications(items);
        response.setMessage("已生成分类确认提案 proposalNo=" + proposalNo + "，请用户在前端确认后执行");
        return response;
    }

}
