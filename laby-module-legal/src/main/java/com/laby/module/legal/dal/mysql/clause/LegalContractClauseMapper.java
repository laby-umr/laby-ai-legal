package com.laby.module.legal.dal.mysql.clause;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractClauseMapper extends BaseMapperX<LegalContractClauseDO> {

    default List<LegalContractClauseDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<LegalContractClauseDO>()
                .eq(LegalContractClauseDO::getContractId, contractId)
                .orderByAsc(LegalContractClauseDO::getSort));
    }

    default long countByContractId(Long contractId) {
        return selectCount(new LambdaQueryWrapperX<LegalContractClauseDO>()
                .eq(LegalContractClauseDO::getContractId, contractId));
    }

    default LegalContractClauseDO selectByContractIdAndClauseId(Long contractId, String clauseId) {
        return selectOne(new LambdaQueryWrapperX<LegalContractClauseDO>()
                .eq(LegalContractClauseDO::getContractId, contractId)
                .eq(LegalContractClauseDO::getClauseId, clauseId));
    }

    default void deleteByContractId(Long contractId) {
        delete(new LambdaQueryWrapperX<LegalContractClauseDO>()
                .eq(LegalContractClauseDO::getContractId, contractId));
    }

}
