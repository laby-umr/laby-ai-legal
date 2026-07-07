package com.laby.module.legal.framework.bpm;

import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.service.contract.LegalContractExportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("legalExportDelegate")
public class LegalExportDelegate implements JavaDelegate {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractExportService exportService;

    @Override
    public void execute(DelegateExecution execution) {
        Long contractId = LegalContractParseDelegate.getContractId(execution);
        log.info("[LegalExportDelegate] contractId={} 开始归档导出", contractId);
        try {
            Long zipFileId = exportService.exportArchivePackage(contractId);
            log.info("[LegalExportDelegate] contractId={} 归档 zip fileId={}", contractId, zipFileId);
        } catch (Exception e) {
            log.warn("[LegalExportDelegate] contractId={} 归档导出失败，仍标记归档", contractId, e);
            try {
                exportService.exportReportDocx(contractId);
            } catch (Exception reportEx) {
                log.warn("[LegalExportDelegate] contractId={} 报告导出也失败", contractId, reportEx);
            }
        }
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setStatus(LegalContractStatusEnum.ARCHIVED.getStatus()));
    }

}
