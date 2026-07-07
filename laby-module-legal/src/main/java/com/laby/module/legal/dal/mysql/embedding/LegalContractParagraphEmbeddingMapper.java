package com.laby.module.legal.dal.mysql.embedding;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.embedding.LegalContractParagraphEmbeddingDO;
import com.laby.module.legal.enums.contract.LegalParagraphEmbeddingStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractParagraphEmbeddingMapper extends BaseMapperX<LegalContractParagraphEmbeddingDO> {

    default List<LegalContractParagraphEmbeddingDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<LegalContractParagraphEmbeddingDO>()
                .eq(LegalContractParagraphEmbeddingDO::getContractId, contractId));
    }

    default long countSuccessByContractId(Long contractId) {
        return selectCount(new LambdaQueryWrapperX<LegalContractParagraphEmbeddingDO>()
                .eq(LegalContractParagraphEmbeddingDO::getContractId, contractId)
                .eq(LegalContractParagraphEmbeddingDO::getStatus,
                        LegalParagraphEmbeddingStatusEnum.SUCCESS.getStatus()));
    }

    default void deleteByContractId(Long contractId) {
        delete(new LambdaQueryWrapperX<LegalContractParagraphEmbeddingDO>()
                .eq(LegalContractParagraphEmbeddingDO::getContractId, contractId));
    }

}
