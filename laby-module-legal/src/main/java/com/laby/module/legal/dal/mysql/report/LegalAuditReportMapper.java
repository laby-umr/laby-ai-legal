package com.laby.module.legal.dal.mysql.report;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegalAuditReportMapper extends BaseMapperX<LegalAuditReportDO> {

    default void deleteByContractIdAndRound(Long contractId, Integer auditRound) {
        delete(new LambdaQueryWrapperX<LegalAuditReportDO>()
                .eq(LegalAuditReportDO::getContractId, contractId)
                .eq(LegalAuditReportDO::getAuditRound, auditRound));
    }

    default LegalAuditReportDO selectByContractIdAndRound(Long contractId, Integer auditRound) {
        return selectOne(new LambdaQueryWrapperX<LegalAuditReportDO>()
                .eq(LegalAuditReportDO::getContractId, contractId)
                .eq(LegalAuditReportDO::getAuditRound, auditRound)
                .orderByDesc(LegalAuditReportDO::getId)
                .last("LIMIT 1"));
    }

    default LegalAuditReportDO selectLatestByContractId(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<LegalAuditReportDO>()
                .eq(LegalAuditReportDO::getContractId, contractId)
                .orderByDesc(LegalAuditReportDO::getAuditRound)
                .orderByDesc(LegalAuditReportDO::getId)
                .last("LIMIT 1"));
    }

}
