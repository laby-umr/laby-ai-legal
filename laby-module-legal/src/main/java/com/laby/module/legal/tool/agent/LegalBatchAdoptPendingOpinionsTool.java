package com.laby.module.legal.tool.agent;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 批量直接采纳待处理审核意见。
 */
@Component("legal_batch_adopt_pending_opinions")
public class LegalBatchAdoptPendingOpinionsTool {

    private static final int MAX_BATCH = 50;

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalAuditOpinionService opinionService;

    @Data
    @JsonClassDescription("批量采纳当前合同下待处理的审核意见")
    public static class Request {

        @JsonPropertyDescription("审核轮次，空则使用合同当前轮次")
        private Integer auditRound;

        @JsonPropertyDescription("仅采纳指定风险等级：HIGH / MEDIUM / LOW，空表示全部")
        private String riskLevel;

        @JsonPropertyDescription("最多采纳条数，默认 20，最大 50")
        private Integer limit;
    }

    @Data
    public static class Response {
        private int adoptedCount;
        private List<Long> adoptedIds = new ArrayList<>();
        private String message;
    }

    @Tool(name = "legal_batch_adopt_pending_opinions",
            description = "批量直接采纳待处理的审核意见，适用于用户要求采纳全部/高风险等待处置意见")
    public Response batchAdoptPendingOpinions(
            @ToolParam(name = "auditRound", description = "审核轮次，空则使用合同当前轮次", required = false) Integer auditRound,
            @ToolParam(name = "riskLevel", description = "仅采纳 HIGH/MEDIUM/LOW，空表示全部", required = false) String riskLevel,
            @ToolParam(name = "limit", description = "最多采纳条数，默认 20", required = false) Integer limit,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setAuditRound(auditRound);
        request.setRiskLevel(riskLevel);
        request.setLimit(limit);
        return doBatchAdopt(request, toolContext);
    }

    private Response doBatchAdopt(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalAgentToolSupport.assertProposalMode(toolContext);
        LegalContractDO contract = LegalAgentToolSupport.requireContract(contractService, toolContext);
        int limit = request.getLimit() == null ? 20 : Math.min(request.getLimit(), MAX_BATCH);

        // 跨轮次扫描待处置意见（撤销采纳后重回待处置的也算）
        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractId(contract.getId());
        List<Long> toAdopt = new ArrayList<>();
        for (LegalAuditOpinionDO opinion : opinions) {
            if (toAdopt.size() >= limit) {
                break;
            }
            if (request.getAuditRound() != null && !request.getAuditRound().equals(opinion.getAuditRound())) {
                continue;
            }
            if (!LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                continue;
            }
            if (StrUtil.isNotBlank(request.getRiskLevel())
                    && !StrUtil.equalsIgnoreCase(request.getRiskLevel(), opinion.getRiskLevel())) {
                continue;
            }
            toAdopt.add(opinion.getId());
        }
        if (toAdopt.isEmpty()) {
            Response empty = new Response();
            empty.setAdoptedCount(0);
            empty.setMessage("没有符合条件的待处理意见");
            return empty;
        }

        opinionService.batchAdopt(toAdopt);

        Response response = new Response();
        response.setAdoptedCount(toAdopt.size());
        response.setAdoptedIds(toAdopt);
        response.setMessage("已批量采纳 " + toAdopt.size() + " 条待处理意见");
        return response;
    }

}
