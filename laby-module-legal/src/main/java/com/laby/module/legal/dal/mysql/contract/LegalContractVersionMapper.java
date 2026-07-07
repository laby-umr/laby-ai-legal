package com.laby.module.legal.dal.mysql.contract;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.contract.LegalContractVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractVersionMapper extends BaseMapperX<LegalContractVersionDO> {

    default LegalContractVersionDO selectLatestByContractId(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<LegalContractVersionDO>()
                .eq(LegalContractVersionDO::getContractId, contractId)
                .orderByDesc(LegalContractVersionDO::getVersionNo)
                .orderByDesc(LegalContractVersionDO::getId)
                .last("LIMIT 1"));
    }

    default List<LegalContractVersionDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<LegalContractVersionDO>()
                .eq(LegalContractVersionDO::getContractId, contractId)
                .orderByDesc(LegalContractVersionDO::getVersionNo)
                .orderByDesc(LegalContractVersionDO::getId));
    }

    default LegalContractVersionDO selectByContractIdAndType(Long contractId, String type) {
        return selectOne(new LambdaQueryWrapperX<LegalContractVersionDO>()
                .eq(LegalContractVersionDO::getContractId, contractId)
                .eq(LegalContractVersionDO::getType, type)
                .orderByDesc(LegalContractVersionDO::getVersionNo)
                .orderByDesc(LegalContractVersionDO::getId)
                .last("LIMIT 1"));
    }

    default LegalContractVersionDO selectByContractIdAndTypeAndRound(Long contractId, String type,
                                                                     Integer auditRound) {
        return selectOne(new LambdaQueryWrapperX<LegalContractVersionDO>()
                .eq(LegalContractVersionDO::getContractId, contractId)
                .eq(LegalContractVersionDO::getType, type)
                .eq(LegalContractVersionDO::getAuditRound, auditRound)
                .orderByDesc(LegalContractVersionDO::getVersionNo)
                .orderByDesc(LegalContractVersionDO::getId)
                .last("LIMIT 1"));
    }

    default LegalContractVersionDO selectByFileId(Long fileId) {
        return selectOne(new LambdaQueryWrapperX<LegalContractVersionDO>()
                .eq(LegalContractVersionDO::getFileId, fileId)
                .orderByDesc(LegalContractVersionDO::getId)
                .last("LIMIT 1"));
    }

}
