package com.laby.module.legal.tool.agent;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.report.LegalAuditReportMapper;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 获取审核报告 Markdown（contractId 来自 RuntimeContext）。
 */
@Component("legal_get_audit_report")
public class LegalGetAuditReportTool {

    private static final int SUMMARY_MAX_CHARS = 2_000;
    private static final int FULL_MAX_CHARS = 8_000;

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAuditReportMapper reportMapper;

    @Data
    @JsonClassDescription("获取合同审核报告 Markdown")
    public static class Request {

        @JsonPropertyDescription("审核轮次，空则使用合同当前轮次")
        private Integer auditRound;

        @JsonPropertyDescription("true 时仅返回摘要（前 2000 字）")
        private Boolean summaryOnly;
    }

    @Data
    public static class Response {
        private Integer auditRound;
        private Boolean found;
        private String content;
        private Integer contentLength;
    }

    @Tool(name = "legal_get_audit_report",
            description = "获取合同审核报告 Markdown",
            readOnly = true, concurrencySafe = true)
    public Response getAuditReport(
            @ToolParam(name = "auditRound", description = "审核轮次，空则使用合同当前轮次", required = false) Integer auditRound,
            @ToolParam(name = "summaryOnly", description = "true 时仅返回摘要（前 2000 字）", required = false) Boolean summaryOnly,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setAuditRound(auditRound);
        request.setSummaryOnly(summaryOnly);
        return doGet(request, toolContext);
    }

    private Response doGet(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalContractDO contract = LegalAgentToolSupport.requireContract(contractService, toolContext);
        int round = request.getAuditRound() != null ? request.getAuditRound()
                : (contract.getAuditRound() != null ? contract.getAuditRound() : 1);

        LegalAuditReportDO report = reportMapper.selectByContractIdAndRound(contract.getId(), round);
        if (report == null) {
            report = reportMapper.selectLatestByContractId(contract.getId());
        }

        Response response = new Response();
        response.setAuditRound(round);
        if (report == null || StrUtil.isBlank(report.getContent())) {
            response.setFound(false);
            response.setContent("");
            response.setContentLength(0);
            return response;
        }
        response.setFound(true);
        response.setContentLength(report.getContent().length());
        boolean summaryOnly = Boolean.TRUE.equals(request.getSummaryOnly());
        int maxChars = summaryOnly ? SUMMARY_MAX_CHARS : FULL_MAX_CHARS;
        String content = StrUtil.sub(report.getContent(), 0, maxChars);
        if (report.getContent().length() > maxChars) {
            content = content + "\n…（报告已截断）";
        }
        response.setContent(content);
        return response;
    }

}
