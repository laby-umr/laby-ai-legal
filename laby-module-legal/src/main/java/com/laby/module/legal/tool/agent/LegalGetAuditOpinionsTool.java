package com.laby.module.legal.tool.agent;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询审核意见（contractId 来自 RuntimeContext）。
 */
@Component("legal_get_audit_opinions")
public class LegalGetAuditOpinionsTool {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final int MAX_FIELD_CHARS = 200;

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;

    @Data
    @JsonClassDescription("查询当前合同的 AI/人工审核意见")
    public static class Request {

        @JsonPropertyDescription("审核轮次，空则使用合同当前轮次")
        private Integer auditRound;

        @JsonPropertyDescription("风险等级：HIGH / MEDIUM / LOW")
        private String riskLevel;

        @JsonPropertyDescription("关联段落编号，如 p-12")
        private String paragraphId;

        @JsonPropertyDescription("意见状态：0 待处理 1 已采纳 2 已忽略")
        private Integer status;

        @JsonPropertyDescription("最多返回条数，默认 10，最大 20")
        private Integer limit;
    }

    @Data
    public static class Response {
        private Integer auditRound;
        private Integer totalMatched;
        private Integer pendingCount;
        private Integer adoptedCount;
        private Integer ignoredCount;
        private List<OpinionItem> items = new ArrayList<>();
    }

    @Data
    public static class OpinionItem {
        private Long id;
        private String title;
        private String riskLevel;
        private String content;
        private String suggestion;
        private String paragraphId;
        private Integer status;
        private Integer auditRound;
    }

    @Tool(name = "legal_get_audit_opinions",
            description = "查询当前合同的审核意见；统计高风险条数请传 riskLevel=HIGH，统计待处置请传 status=0",
            readOnly = true, concurrencySafe = true)
    public Response getAuditOpinions(
            @ToolParam(name = "auditRound", description = "审核轮次，空则使用合同当前轮次", required = false) Integer auditRound,
            @ToolParam(name = "riskLevel", description = "风险等级：HIGH / MEDIUM / LOW", required = false) String riskLevel,
            @ToolParam(name = "paragraphId", description = "关联段落编号，如 p-12，可选", required = false) String paragraphId,
            @ToolParam(name = "status", description = "意见状态：0 待处理 1 已采纳 2 已忽略", required = false) Integer status,
            @ToolParam(name = "limit", description = "最多返回条数，默认 10，最大 20", required = false) Integer limit,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setAuditRound(auditRound);
        request.setRiskLevel(riskLevel);
        request.setParagraphId(paragraphId);
        request.setStatus(status);
        request.setLimit(limit);
        return doQuery(request, toolContext);
    }

    private Response doQuery(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalContractDO contract = LegalAgentToolSupport.requireContract(contractService, toolContext);
        int round = request.getAuditRound() != null ? request.getAuditRound()
                : (contract.getAuditRound() != null ? contract.getAuditRound() : 1);
        int limit = request.getLimit() == null ? DEFAULT_LIMIT : Math.min(request.getLimit(), MAX_LIMIT);

        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractIdAndRound(contract.getId(), round);
        Response response = new Response();
        response.setAuditRound(round);
        int pendingCount = 0;
        int adoptedCount = 0;
        int ignoredCount = 0;
        for (LegalAuditOpinionDO opinion : opinions) {
            if (LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                pendingCount++;
            } else if (LegalOpinionStatusEnum.ADOPTED.getStatus().equals(opinion.getStatus())) {
                adoptedCount++;
            } else if (LegalOpinionStatusEnum.IGNORED.getStatus().equals(opinion.getStatus())) {
                ignoredCount++;
            }
        }
        for (LegalAuditOpinionDO opinion : opinions) {
            if (response.getItems().size() >= limit) {
                break;
            }
            if (StrUtil.isNotBlank(request.getRiskLevel())
                    && !StrUtil.equalsIgnoreCase(request.getRiskLevel(), opinion.getRiskLevel())) {
                continue;
            }
            if (StrUtil.isNotBlank(request.getParagraphId())
                    && !StrUtil.equals(request.getParagraphId(), opinion.getParagraphId())) {
                continue;
            }
            if (request.getStatus() != null && !request.getStatus().equals(opinion.getStatus())) {
                continue;
            }
            OpinionItem item = new OpinionItem();
            item.setId(opinion.getId());
            item.setTitle(opinion.getTitle());
            item.setRiskLevel(opinion.getRiskLevel());
            item.setContent(StrUtil.sub(StrUtil.blankToDefault(opinion.getContent(), ""), 0, MAX_FIELD_CHARS));
            item.setSuggestion(StrUtil.sub(StrUtil.blankToDefault(opinion.getSuggestion(), ""), 0, MAX_FIELD_CHARS));
            item.setParagraphId(opinion.getParagraphId());
            item.setStatus(opinion.getStatus());
            item.setAuditRound(opinion.getAuditRound());
            response.getItems().add(item);
        }
        return response;
    }

}
