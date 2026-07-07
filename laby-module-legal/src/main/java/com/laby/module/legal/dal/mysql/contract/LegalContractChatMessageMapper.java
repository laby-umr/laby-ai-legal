package com.laby.module.legal.dal.mysql.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractChatMessageMapper extends BaseMapperX<LegalContractChatMessageDO> {

    default List<LegalContractChatMessageDO> selectListByContractIdAndUserId(Long contractId, Long userId) {
        return selectList(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getContractId, contractId)
                .eq(LegalContractChatMessageDO::getUserId, userId)
                .orderByAsc(LegalContractChatMessageDO::getId));
    }

    default List<LegalContractChatMessageDO> selectListByContractIdAndUserIdAndSessionId(
            Long contractId, Long userId, String sessionId) {
        return selectList(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getContractId, contractId)
                .eq(LegalContractChatMessageDO::getUserId, userId)
                .eqIfPresent(LegalContractChatMessageDO::getSessionId,
                        StrUtil.isBlank(sessionId) ? null : sessionId)
                .orderByAsc(LegalContractChatMessageDO::getId));
    }

    default List<LegalContractChatMessageDO> selectListBeforeId(Long contractId, Long userId, Long beforeId) {
        return selectListBeforeId(contractId, userId, null, beforeId);
    }

    default List<LegalContractChatMessageDO> selectListBeforeId(Long contractId, Long userId,
                                                                String sessionId, Long beforeId) {
        return selectList(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getContractId, contractId)
                .eq(LegalContractChatMessageDO::getUserId, userId)
                .eqIfPresent(LegalContractChatMessageDO::getSessionId,
                        StrUtil.isBlank(sessionId) ? null : sessionId)
                .lt(LegalContractChatMessageDO::getId, beforeId)
                .orderByAsc(LegalContractChatMessageDO::getId));
    }

    default LegalContractChatMessageDO selectByReplyId(Long replyId) {
        return selectOne(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getReplyId, replyId)
                .last("LIMIT 1"));
    }

    default int deleteByContractIdAndUserId(Long contractId, Long userId) {
        return delete(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getContractId, contractId)
                .eq(LegalContractChatMessageDO::getUserId, userId));
    }

    default int deleteFromId(Long contractId, Long userId, Long fromId) {
        return delete(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getContractId, contractId)
                .eq(LegalContractChatMessageDO::getUserId, userId)
                .ge(LegalContractChatMessageDO::getId, fromId));
    }

    default List<LegalContractChatMessageDO> selectListUnprocessedForMemoryExtraction(int limit) {
        return selectList(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getType, "assistant")
                .isNotNull(LegalContractChatMessageDO::getReplyId)
                .ne(LegalContractChatMessageDO::getContent, "")
                .orderByDesc(LegalContractChatMessageDO::getId)
                .last("LIMIT " + limit));
    }

    default boolean existsSummaryContent(Long contractId, String sessionId, String content) {
        return selectCount(new LambdaQueryWrapperX<LegalContractChatMessageDO>()
                .eq(LegalContractChatMessageDO::getContractId, contractId)
                .eq(LegalContractChatMessageDO::getType, "summary")
                .eqIfPresent(LegalContractChatMessageDO::getSessionId,
                        StrUtil.isBlank(sessionId) ? null : sessionId)
                .eq(LegalContractChatMessageDO::getContent, content)) > 0;
    }

}
