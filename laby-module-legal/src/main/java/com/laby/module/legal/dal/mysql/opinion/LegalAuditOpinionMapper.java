package com.laby.module.legal.dal.mysql.opinion;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalAuditOpinionMapper extends BaseMapperX<LegalAuditOpinionDO> {

    default List<LegalAuditOpinionDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<LegalAuditOpinionDO>()
                .eq(LegalAuditOpinionDO::getContractId, contractId)
                .orderByAsc(LegalAuditOpinionDO::getId));
    }

    default List<LegalAuditOpinionDO> selectListByContractIdAndRound(Long contractId, Integer auditRound) {
        return selectList(new LambdaQueryWrapperX<LegalAuditOpinionDO>()
                .eq(LegalAuditOpinionDO::getContractId, contractId)
                .eq(LegalAuditOpinionDO::getAuditRound, auditRound)
                .orderByAsc(LegalAuditOpinionDO::getId));
    }

    default long selectCountByContractId(Long contractId) {
        return selectCount(new LambdaQueryWrapperX<LegalAuditOpinionDO>()
                .eq(LegalAuditOpinionDO::getContractId, contractId));
    }

    default long selectHighRiskPendingCount(Long contractId) {
        return selectCount(new LambdaQueryWrapperX<LegalAuditOpinionDO>()
                .eq(LegalAuditOpinionDO::getContractId, contractId)
                .eq(LegalAuditOpinionDO::getRiskLevel, LegalRiskLevelEnum.HIGH.getCode())
                .eq(LegalAuditOpinionDO::getStatus, LegalOpinionStatusEnum.PENDING.getStatus()));
    }

    default long selectPendingCount(Long contractId) {
        return selectCount(new LambdaQueryWrapperX<LegalAuditOpinionDO>()
                .eq(LegalAuditOpinionDO::getContractId, contractId)
                .eq(LegalAuditOpinionDO::getStatus, LegalOpinionStatusEnum.PENDING.getStatus()));
    }

    default void deleteByContractIdAndRound(Long contractId, Integer auditRound) {
        delete(new LambdaQueryWrapperX<LegalAuditOpinionDO>()
                .eq(LegalAuditOpinionDO::getContractId, contractId)
                .eq(LegalAuditOpinionDO::getAuditRound, auditRound));
    }

}
