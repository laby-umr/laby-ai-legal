package com.laby.module.legal.tool.agent;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.service.agent.LegalAgentContractSnapshotHelper;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 获取当前合同元数据（contractId 来自 RuntimeContext）。
 */
@Component("legal_get_contract_meta")
public class LegalGetContractMetaTool {

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;

    @Data
    @JsonClassDescription("获取当前绑定合同的元数据概览，无需参数")
    public static class Request {
    }

    @Data
    public static class Response {
        private Long contractId;
        private String title;
        private Integer status;
        private Integer auditRound;
        private Long contractTypeId;
        private Integer riskHighCount;
        private String partyRole;
        private String auditLevel;
        private Integer parseStatus;
        /** 业务状态中文名，如「意见处置」 */
        private String statusName;
        /** 当前轮次意见总数 */
        private Integer totalOpinionCount;
        /** 待处置（status=0，含撤销采纳后重回待处置） */
        private Integer pendingOpinionCount;
        private Integer adoptedOpinionCount;
        private Integer ignoredOpinionCount;
        /** 全合同待处置条数（跨轮次） */
        private Integer pendingOpinionCountAllRounds;
        /** 是否已有审核意见（true=AI 审核已产出意见） */
        private Boolean auditCompleted;
        /** 高风险意见总数（含已采纳/已忽略） */
        private Integer highRiskTotalCount;
        /** 高风险且待处置 */
        private Integer highRiskPendingCount;
    }

    @Tool(name = "legal_get_contract_meta",
            description = "获取当前绑定合同的元数据概览（含待处置/已采纳意见数量），无需参数",
            readOnly = true, concurrencySafe = true)
    public Response getContractMeta(LegalAgentToolRuntimeContext toolContext) {
        LegalContractDO contract = LegalAgentToolSupport.requireContract(contractService, toolContext);
        Response response = new Response();
        response.setContractId(contract.getId());
        response.setTitle(contract.getTitle());
        response.setStatus(contract.getStatus());
        response.setAuditRound(contract.getAuditRound() != null ? contract.getAuditRound() : 1);
        response.setContractTypeId(contract.getContractTypeId());
        response.setRiskHighCount(contract.getRiskHighCount());
        response.setPartyRole(contract.getPartyRole());
        response.setAuditLevel(contract.getAuditLevel());
        response.setParseStatus(contract.getParseStatus());
        response.setStatusName(resolveStatusName(contract.getStatus()));

        int round = contract.getAuditRound() != null ? contract.getAuditRound() : 1;
        List<LegalAuditOpinionDO> roundOpinions =
                opinionMapper.selectListByContractIdAndRound(contract.getId(), round);
        int pending = 0;
        int adopted = 0;
        int ignored = 0;
        for (LegalAuditOpinionDO opinion : roundOpinions) {
            if (LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                pending++;
            } else if (LegalOpinionStatusEnum.ADOPTED.getStatus().equals(opinion.getStatus())) {
                adopted++;
            } else if (LegalOpinionStatusEnum.IGNORED.getStatus().equals(opinion.getStatus())) {
                ignored++;
            }
        }
        response.setTotalOpinionCount(roundOpinions.size());
        response.setPendingOpinionCount(pending);
        response.setAdoptedOpinionCount(adopted);
        response.setIgnoredOpinionCount(ignored);

        int pendingAll = 0;
        for (LegalAuditOpinionDO opinion : opinionMapper.selectListByContractId(contract.getId())) {
            if (LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                pendingAll++;
            }
        }
        response.setPendingOpinionCountAllRounds(pendingAll);

        LegalAgentContractSnapshotHelper.Snapshot snapshot =
                LegalAgentContractSnapshotHelper.build(contract, opinionMapper);
        response.setAuditCompleted(snapshot.isHasAuditOpinions());
        response.setHighRiskTotalCount(snapshot.getHighRiskTotalCount());
        response.setHighRiskPendingCount(snapshot.getHighRiskPendingCount());
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
