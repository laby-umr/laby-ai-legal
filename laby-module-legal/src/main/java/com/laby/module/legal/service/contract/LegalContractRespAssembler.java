package com.laby.module.legal.service.contract;

import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractFileMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.dal.mysql.report.LegalAuditReportMapper;
import com.laby.module.legal.enums.contract.LegalContractFileRoleEnum;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 合同审核 Response VO 组装器
 * <p>
 * 将 DO 与列表/详情展示字段（按钮权限、审核摘要等）转换为 {@link LegalContractRespVO}，
 * 避免 Controller 直接访问 Mapper，符合 ruoyi-pro 分层规范。
 */
@Component
public class LegalContractRespAssembler {

    @Resource
    private LegalContractFileMapper contractFileMapper;
    @Resource
    private LegalAuditOpinionMapper auditOpinionMapper;
    @Resource
    private LegalAuditReportMapper auditReportMapper;

    /**
     * 组装合同详情 Response VO（含附件与操作按钮）
     *
     * @param contract 合同 DO
     * @return 详情 VO
     */
    public LegalContractRespVO buildDetail(LegalContractDO contract) {
        LegalContractRespVO resp = BeanUtils.toBean(contract, LegalContractRespVO.class);
        List<LegalContractFileDO> files = contractFileMapper.selectListByContractId(contract.getId());
        // 详情「附件」仅展示用户上传（ORIGINAL）；导出/工作版等系统文件见版本链与下载面板
        List<LegalContractFileDO> uploadFiles = files.stream()
                .filter(file -> Objects.equals(LegalContractFileRoleEnum.ORIGINAL.getRole(), file.getRole()))
                .toList();
        resp.setFiles(BeanUtils.toBean(uploadFiles, LegalContractRespVO.LegalContractFileRespVO.class));
        fillActionFlags(resp, contract);
        fillAuditSummary(resp, contract.getId());
        return resp;
    }

    /**
     * 填充分页/列表行上的操作按钮与审核摘要
     *
     * @param resp     列表行 VO
     * @param contract 合同 DO
     */
    public void enrichListRow(LegalContractRespVO resp, LegalContractDO contract) {
        fillActionFlags(resp, contract);
        fillAuditSummary(resp, contract.getId());
    }

    /**
     * 填充前端按钮可见性与业务状态名称
     */
    public void fillActionFlags(LegalContractRespVO resp, LegalContractDO contract) {
        resp.setStatusName(resolveStatusName(contract.getStatus()));
        long pendingOpinionCount = auditOpinionMapper.selectPendingCount(contract.getId());
        resp.setOpinionEditable(LegalContractPermissionHelper.canManageOpinions(contract, pendingOpinionCount));
        resp.setSecondRoundApplicable(LegalContractPermissionHelper.canApplySecondRound(contract));
        resp.setOpinionCompletable(LegalContractPermissionHelper.canCompleteOpinionReview(contract));
        resp.setReviewActionVisible(LegalContractPermissionHelper.canOpenReviewWorkbench(contract));
        resp.setStartAuditVisible(LegalContractPermissionHelper.canStartFirstAudit(contract));
        resp.setRetryVisible(LegalContractPermissionHelper.canRetryPipeline(contract));
        if (LegalContractStatusEnum.FAILED.getStatus().equals(contract.getStatus())) {
            resp.setFailReason(contract.getFeedbackSummary());
        }
    }

    /**
     * 填充审核报告与意见数量摘要
     */
    private void fillAuditSummary(LegalContractRespVO resp, Long contractId) {
        long opinionCount = auditOpinionMapper.selectCountByContractId(contractId);
        resp.setAuditOpinionCount((int) opinionCount);
        LegalAuditReportDO latestReport = auditReportMapper.selectLatestByContractId(contractId);
        resp.setHasAuditReport(latestReport != null);
        if (latestReport != null) {
            resp.setLatestAuditReportRound(latestReport.getAuditRound());
        }
    }

    private static String resolveStatusName(Integer status) {
        if (status == null) {
            return "-";
        }
        for (LegalContractStatusEnum item : LegalContractStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item.getName();
            }
        }
        return String.valueOf(status);
    }

}
