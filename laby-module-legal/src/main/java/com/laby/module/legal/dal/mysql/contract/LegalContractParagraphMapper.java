package com.laby.module.legal.dal.mysql.contract;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractParagraphMapper extends BaseMapperX<LegalContractParagraphDO> {

    default List<LegalContractParagraphDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<LegalContractParagraphDO>()
                .eq(LegalContractParagraphDO::getContractId, contractId)
                .orderByAsc(LegalContractParagraphDO::getSort));
    }

    default void deleteByContractId(Long contractId) {
        delete(new LambdaQueryWrapperX<LegalContractParagraphDO>()
                .eq(LegalContractParagraphDO::getContractId, contractId));
    }

}
