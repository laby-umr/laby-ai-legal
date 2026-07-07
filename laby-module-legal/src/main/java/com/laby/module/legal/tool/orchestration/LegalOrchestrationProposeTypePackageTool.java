package com.laby.module.legal.tool.orchestration;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationProposalService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationTypePackageService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component("legal_orchestration_propose_type_package")
public class LegalOrchestrationProposeTypePackageTool {

    @Resource
    private LegalOrchestrationSessionService sessionService;
    @Resource
    private LegalOrchestrationTypePackageService typePackageService;
    @Resource
    private LegalOrchestrationProposalService proposalService;

    @Data
    public static class Response {
        private String proposalNo;
        private Long draftId;
        private Long sessionId;
        private String message;
    }

    @Tool(name = "legal_orchestration_propose_type_package",
            description = "为缺省合同类型起草类型包草稿并生成确认提案（确认后发布为租户合同类型）")
    public Response proposeTypePackage(
            @ToolParam(name = "sessionId", description = "编排会话编号", required = true) Long sessionId,
            @ToolParam(name = "typeName", description = "建议类型名称", required = true) String typeName,
            @ToolParam(name = "typeCode", description = "建议类型编码（英文）", required = false) String typeCode,
            @ToolParam(name = "description", description = "类型说明", required = false) String description,
            @ToolParam(name = "contentJson", description = "条款/规则草稿 JSON", required = false) String contentJson,
            LegalOrchestrationToolRuntimeContext toolContext) {
        Long conversationId = LegalOrchestrationToolSupport.requireConversationId(toolContext);
        Long userId = LegalOrchestrationToolSupport.requireUserId(toolContext);
        sessionService.validateSessionExists(sessionId);

        String code = StrUtil.blankToDefault(typeCode, "ai_" + System.currentTimeMillis());
        String content = StrUtil.blankToDefault(contentJson, "{\"clauses\":[],\"auditRules\":[]}");
        Long draftId = typePackageService.saveDraft(sessionId, userId, typeName, code, description, content);
        String proposalNo = proposalService.createTypePackageProposal(
                conversationId, userId, sessionId, draftId, typeName);

        Response response = new Response();
        response.setProposalNo(proposalNo);
        response.setDraftId(draftId);
        response.setSessionId(sessionId);
        response.setMessage("已生成类型包确认提案 proposalNo=" + proposalNo);
        return response;
    }

}
