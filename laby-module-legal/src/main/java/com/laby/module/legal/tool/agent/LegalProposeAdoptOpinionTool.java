package com.laby.module.legal.tool.agent;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.opinion.LegalAuditOpinionService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 兼容旧 Tool 名：行为与 {@link LegalAdoptOpinionTool} 一致，直接采纳，不再生成待确认提案。
 */
@Component("legal_propose_adopt_opinion")
public class LegalProposeAdoptOpinionTool {

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalAuditOpinionService opinionService;

    @Data
    @JsonClassDescription("直接采纳一条待处理的审核意见")
    public static class Request {

        @JsonProperty(required = true)
        @JsonPropertyDescription("审核意见编号")
        private Long opinionId;

        @JsonPropertyDescription("采纳理由，可选，最多 200 字")
        private String reason;
    }

    @Data
    public static class Response {
        private Long opinionId;
        private String message;
    }

    @Tool(name = "legal_propose_adopt_opinion",
            description = "直接采纳一条待处理的审核意见（用户对话指令即授权）")
    public Response proposeAdoptOpinion(
            @ToolParam(name = "opinionId", description = "审核意见编号") Long opinionId,
            @ToolParam(name = "reason", description = "采纳理由，可选", required = false) String reason,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setOpinionId(opinionId);
        request.setReason(reason);
        return doAdopt(request, toolContext);
    }

    private Response doAdopt(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolSupport.assertProposalMode(toolContext);
        Long contractId = LegalAgentToolSupport.requireContractId(toolContext);
        contractService.validateContractExists(contractId);

        LegalAuditOpinionDO opinion = opinionMapper.selectById(request.getOpinionId());
        if (opinion == null || !contractId.equals(opinion.getContractId())) {
            throw new IllegalArgumentException("审核意见不存在或不属于当前合同");
        }
        if (!LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
            throw new IllegalStateException("该意见已处置，无法重复采纳");
        }

        opinionService.adopt(request.getOpinionId());

        Response response = new Response();
        response.setOpinionId(request.getOpinionId());
        response.setMessage("已采纳意见 #" + request.getOpinionId());
        return response;
    }

}
