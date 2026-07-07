package com.laby.module.legal.dal.mysql.contract;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.contract.LegalContractPublishLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegalContractPublishLogMapper extends BaseMapperX<LegalContractPublishLogDO> {

    default LegalContractPublishLogDO selectByContractIdAndRound(Long contractId, Integer auditRound) {
        return selectOne(new LambdaQueryWrapperX<LegalContractPublishLogDO>()
                .eq(LegalContractPublishLogDO::getContractId, contractId)
                .eq(LegalContractPublishLogDO::getAuditRound, auditRound)
                .orderByDesc(LegalContractPublishLogDO::getId)
                .last("LIMIT 1"));
    }

}
