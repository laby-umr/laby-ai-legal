package com.laby.module.legal.tool.agent;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.service.agent.LegalAgentProposalService;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 提议跳过/恢复段落审核（需用户 Confirm 后执行，contractId 来自 RuntimeContext）。
 */
@Component("legal_propose_skip_paragraph")
public class LegalProposeSkipParagraphTool {

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAgentProposalService proposalService;

    @Data
    @JsonClassDescription("提议标记段落是否跳过 AI 审核，需用户在界面确认后才会执行")
    public static class Request {

        @JsonProperty(required = true)
        @JsonPropertyDescription("段落编号，如 p-12")
        private String paragraphId;

        @JsonPropertyDescription("true 跳过审核，false 恢复审核，默认 true")
        private Boolean skipAudit;

        @JsonPropertyDescription("操作理由，可选，最多 200 字")
        private String reason;
    }

    @Data
    public static class Response {
        private String proposalNo;
        private String message;
    }

    @Tool(name = "legal_propose_skip_paragraph",
            description = "提议标记段落是否跳过 AI 审核，需用户在界面确认后才会执行")
    public Response proposeSkipParagraph(
            @ToolParam(name = "paragraphId", description = "段落编号，如 p-12") String paragraphId,
            @ToolParam(name = "skipAudit", description = "true 跳过审核，false 恢复审核，默认 true", required = false) Boolean skipAudit,
            @ToolParam(name = "reason", description = "操作理由，可选，最多 200 字", required = false) String reason,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setParagraphId(paragraphId);
        request.setSkipAudit(skipAudit);
        request.setReason(reason);
        return doPropose(request, toolContext);
    }

    private Response doPropose(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolSupport.assertProposalMode(toolContext);
        Long contractId = LegalAgentToolSupport.requireContractId(toolContext);
        contractService.validateContractExists(contractId);
        Long userId = LegalAgentToolSupport.requireUserId(toolContext);
        String sessionId = LegalAgentToolSupport.requireSessionId(toolContext);

        boolean skipAudit = request.getSkipAudit() == null || request.getSkipAudit();
        String proposalNo = proposalService.createSkipParagraphProposal(
                contractId, userId, sessionId, request.getParagraphId(), skipAudit, request.getReason());

        Response response = new Response();
        response.setProposalNo(proposalNo);
        response.setMessage("已生成段落审核提案，请用户在界面确认后执行");
        return response;
    }

}
