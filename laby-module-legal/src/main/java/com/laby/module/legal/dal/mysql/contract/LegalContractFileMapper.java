package com.laby.module.legal.dal.mysql.contract;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractFileMapper extends BaseMapperX<LegalContractFileDO> {

    default List<LegalContractFileDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<LegalContractFileDO>()
                .eq(LegalContractFileDO::getContractId, contractId));
    }

    default LegalContractFileDO selectByFileId(Long fileId) {
        return selectOne(LegalContractFileDO::getFileId, fileId);
    }

    default LegalContractFileDO selectMainByContractId(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<LegalContractFileDO>()
                .eq(LegalContractFileDO::getContractId, contractId)
                .eq(LegalContractFileDO::getMainFlag, true)
                .orderByDesc(LegalContractFileDO::getId)
                .last("LIMIT 1"));
    }

}
