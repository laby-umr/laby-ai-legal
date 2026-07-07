package com.laby.module.legal.dal.mysql.memory;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactPageReqVO;
import com.laby.module.legal.dal.dataobject.memory.LegalUserFactDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalUserFactMapper extends BaseMapperX<LegalUserFactDO> {

    default PageResult<LegalUserFactDO> selectPage(LegalUserFactPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalUserFactDO>()
                .eqIfPresent(LegalUserFactDO::getUserId, reqVO.getUserId())
                .eqIfPresent(LegalUserFactDO::getContractId, reqVO.getContractId())
                .likeIfPresent(LegalUserFactDO::getSessionId, reqVO.getSessionId())
                .likeIfPresent(LegalUserFactDO::getContent, reqVO.getContent())
                .orderByDesc(LegalUserFactDO::getId));
    }

    default List<LegalUserFactDO> selectListByContractAndUser(Long contractId, Long userId, int limit) {
        return selectList(new LambdaQueryWrapperX<LegalUserFactDO>()
                .eqIfPresent(LegalUserFactDO::getContractId, contractId)
                .eqIfPresent(LegalUserFactDO::getUserId, userId)
                .orderByDesc(LegalUserFactDO::getId)
                .last("LIMIT " + limit));
    }

    default LegalUserFactDO selectByUserAndHash(Long userId, String contentHash) {
        return selectOne(new LambdaQueryWrapperX<LegalUserFactDO>()
                .eq(LegalUserFactDO::getUserId, userId)
                .eq(LegalUserFactDO::getContentHash, contentHash)
                .last("LIMIT 1"));
    }

    default boolean existsBySourceMessageId(Long sourceMessageId) {
        if (sourceMessageId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<LegalUserFactDO>()
                .eq(LegalUserFactDO::getSourceMessageId, sourceMessageId)) > 0;
    }

}
