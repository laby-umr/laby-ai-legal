package com.laby.module.legal.tool.orchestration;

import com.laby.module.legal.controller.admin.contract.vo.LegalContractRespVO;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

@Component("legal_orchestration_get_contract_summary")
public class LegalOrchestrationGetContractSummaryTool {

    @Resource
    private LegalContractService contractService;

    @Data
    public static class Response {
        private Long contractId;
        private String title;
        private Integer status;
        private String statusName;
        private Integer riskHighCount;
        private Integer auditOpinionCount;
        private Boolean hasAuditReport;
        private String reviewPath;
        private String createSource;
    }

    @Tool(name = "legal_orchestration_get_contract_summary",
            description = "查询单份合同摘要：业务状态、高风险数、意见数、工作台路径")
    public Response getContractSummary(
            @ToolParam(name = "contractId", description = "合同编号", required = true) Long contractId,
            LegalOrchestrationToolRuntimeContext toolContext) {
        Long userId = LegalOrchestrationToolSupport.requireUserId(toolContext);
        LegalContractRespVO contract = contractService.getContractResp(contractId);
        if (contract == null || !userId.equals(contract.getUserId())) {
            throw exception(CONTRACT_NOT_EXISTS);
        }

        Response response = new Response();
        response.setContractId(contract.getId());
        response.setTitle(contract.getTitle());
        response.setStatus(contract.getStatus());
        response.setStatusName(resolveStatusName(contract.getStatus()));
        response.setRiskHighCount(contract.getRiskHighCount());
        response.setAuditOpinionCount(contract.getAuditOpinionCount());
        response.setHasAuditReport(contract.getHasAuditReport());
        response.setReviewPath("/legal/contract/review?id=" + contract.getId());
        response.setCreateSource(contract.getCreateSource());
        return response;
    }

    private static String resolveStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        for (LegalContractStatusEnum item : LegalContractStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item.getName();
            }
        }
        return String.valueOf(status);
    }

}
