package com.laby.module.legal.dal.mysql.memory;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemoryPageReqVO;
import com.laby.module.legal.dal.dataobject.memory.LegalContractMemoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractMemoryMapper extends BaseMapperX<LegalContractMemoryDO> {

    default List<LegalContractMemoryDO> selectListByContractId(Long contractId, String sessionId, int limit) {
        LambdaQueryWrapperX<LegalContractMemoryDO> wrapper = new LambdaQueryWrapperX<LegalContractMemoryDO>()
                .eq(LegalContractMemoryDO::getContractId, contractId)
                .orderByDesc(LegalContractMemoryDO::getId)
                .last("LIMIT " + Math.max(limit, 1));
        if (StrUtil.isNotBlank(sessionId)) {
            wrapper.eq(LegalContractMemoryDO::getSessionId, sessionId);
        }
        return selectList(wrapper);
    }

    default LegalContractMemoryDO selectByContractIdAndHash(Long contractId, String contentHash) {
        return selectOne(new LambdaQueryWrapperX<LegalContractMemoryDO>()
                .eq(LegalContractMemoryDO::getContractId, contractId)
                .eq(LegalContractMemoryDO::getContentHash, contentHash)
                .last("LIMIT 1"));
    }

    default boolean existsBySourceMessageId(Long sourceMessageId) {
        if (sourceMessageId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<LegalContractMemoryDO>()
                .eq(LegalContractMemoryDO::getSourceMessageId, sourceMessageId)) > 0;
    }

    default PageResult<LegalContractMemoryDO> selectPage(LegalContractMemoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalContractMemoryDO>()
                .eqIfPresent(LegalContractMemoryDO::getContractId, reqVO.getContractId())
                .likeIfPresent(LegalContractMemoryDO::getSessionId, reqVO.getSessionId())
                .eqIfPresent(LegalContractMemoryDO::getMemoryType, reqVO.getMemoryType())
                .likeIfPresent(LegalContractMemoryDO::getContent, reqVO.getContent())
                .orderByDesc(LegalContractMemoryDO::getId));
    }

}
