package com.laby.module.legal.dal.mysql.agent;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.agent.vo.LegalAgentStepLogPageReqVO;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentStepLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalAgentStepLogMapper extends BaseMapperX<LegalAgentStepLogDO> {

    default PageResult<LegalAgentStepLogDO> selectPage(LegalAgentStepLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalAgentStepLogDO>()
                .eqIfPresent(LegalAgentStepLogDO::getContractId, reqVO.getContractId())
                .eqIfPresent(LegalAgentStepLogDO::getUserId, reqVO.getUserId())
                .eqIfPresent(LegalAgentStepLogDO::getSessionId, reqVO.getSessionId())
                .eqIfPresent(LegalAgentStepLogDO::getStepType, reqVO.getStepType())
                .likeIfPresent(LegalAgentStepLogDO::getToolName, reqVO.getToolName())
                .betweenIfPresent(LegalAgentStepLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(LegalAgentStepLogDO::getId));
    }

    default List<LegalAgentStepLogDO> selectListBySessionId(String sessionId) {
        return selectList(new LambdaQueryWrapperX<LegalAgentStepLogDO>()
                .eq(LegalAgentStepLogDO::getSessionId, sessionId)
                .orderByAsc(LegalAgentStepLogDO::getStepIndex)
                .orderByAsc(LegalAgentStepLogDO::getId));
    }

    default int selectMaxStepIndexBySessionId(String sessionId) {
        LegalAgentStepLogDO last = selectOne(new LambdaQueryWrapperX<LegalAgentStepLogDO>()
                .eq(LegalAgentStepLogDO::getSessionId, sessionId)
                .orderByDesc(LegalAgentStepLogDO::getStepIndex)
                .last("LIMIT 1"));
        return last == null || last.getStepIndex() == null ? 0 : last.getStepIndex();
    }

}
