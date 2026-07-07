package com.laby.module.legal.dal.mysql.trace;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.trace.vo.LegalAiTracePageReqVO;
import com.laby.module.legal.dal.dataobject.trace.LegalAiTraceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegalAiTraceMapper extends BaseMapperX<LegalAiTraceDO> {

    default LegalAiTraceDO selectByTraceId(String traceId) {
        return selectOne(LegalAiTraceDO::getTraceId, traceId);
    }

    default PageResult<LegalAiTraceDO> selectPage(LegalAiTracePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalAiTraceDO>()
                .eqIfPresent(LegalAiTraceDO::getContractId, reqVO.getContractId())
                .eqIfPresent(LegalAiTraceDO::getScene, reqVO.getScene())
                .eqIfPresent(LegalAiTraceDO::getStatus, reqVO.getStatus())
                .orderByDesc(LegalAiTraceDO::getId));
    }

}
