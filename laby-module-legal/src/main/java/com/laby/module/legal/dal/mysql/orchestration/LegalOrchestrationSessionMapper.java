package com.laby.module.legal.dal.mysql.orchestration;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegalOrchestrationSessionMapper extends BaseMapperX<LegalOrchestrationSessionDO> {

    default LegalOrchestrationSessionDO selectByConversationId(Long conversationId) {
        return selectOne(LegalOrchestrationSessionDO::getConversationId, conversationId);
    }

}
