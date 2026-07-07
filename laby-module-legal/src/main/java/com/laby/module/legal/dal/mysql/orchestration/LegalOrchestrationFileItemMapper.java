package com.laby.module.legal.dal.mysql.orchestration;

import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalOrchestrationFileItemMapper extends BaseMapperX<LegalOrchestrationFileItemDO> {

    default List<LegalOrchestrationFileItemDO> selectListBySessionId(Long sessionId) {
        return selectList(new LambdaQueryWrapperX<LegalOrchestrationFileItemDO>()
                .eq(LegalOrchestrationFileItemDO::getSessionId, sessionId)
                .orderByAsc(LegalOrchestrationFileItemDO::getSort)
                .orderByAsc(LegalOrchestrationFileItemDO::getId));
    }

    default LegalOrchestrationFileItemDO selectByContractId(Long contractId) {
        return selectOne(LegalOrchestrationFileItemDO::getContractId, contractId);
    }

}
