package com.laby.module.legal.dal.mysql.agent;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentProposalDO;
import com.laby.module.legal.enums.agent.LegalAgentProposalStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LegalAgentProposalMapper extends BaseMapperX<LegalAgentProposalDO> {

    default LegalAgentProposalDO selectByProposalNo(String proposalNo) {
        return selectOne(LegalAgentProposalDO::getProposalNo, proposalNo);
    }

    default LegalAgentProposalDO selectPendingByProposalNo(String proposalNo) {
        return selectOne(new LambdaQueryWrapperX<LegalAgentProposalDO>()
                .eq(LegalAgentProposalDO::getProposalNo, proposalNo)
                .eq(LegalAgentProposalDO::getStatus, "PENDING"));
    }

    default int updateExpiredPending(LocalDateTime now) {
        return update(new LegalAgentProposalDO()
                        .setStatus(LegalAgentProposalStatusEnum.EXPIRED.getStatus()),
                new LambdaQueryWrapperX<LegalAgentProposalDO>()
                        .eq(LegalAgentProposalDO::getStatus, LegalAgentProposalStatusEnum.PENDING.getStatus())
                        .lt(LegalAgentProposalDO::getExpireTime, now));
    }

    default List<LegalAgentProposalDO> selectPendingListByConversationId(Long conversationId) {
        return selectList(new LambdaQueryWrapperX<LegalAgentProposalDO>()
                .eq(LegalAgentProposalDO::getConversationId, conversationId)
                .eq(LegalAgentProposalDO::getStatus, LegalAgentProposalStatusEnum.PENDING.getStatus())
                .orderByDesc(LegalAgentProposalDO::getCreateTime));
    }

}
