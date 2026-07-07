package com.laby.module.legal.service.trace;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.trace.vo.LegalAiTracePageReqVO;
import com.laby.module.legal.controller.admin.trace.vo.LegalAiTraceRespVO;
import com.laby.module.legal.dal.dataobject.trace.LegalAiTraceDO;
import com.laby.module.legal.dal.mysql.trace.LegalAiTraceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 法务 AI 链路追踪 Service 实现类
 */
@Service
public class LegalAiTraceServiceImpl implements LegalAiTraceService {

    @Resource
    private LegalAiTraceMapper traceMapper;

    @Override
    public PageResult<LegalAiTraceRespVO> getTracePage(LegalAiTracePageReqVO pageReqVO) {
        PageResult<LegalAiTraceDO> page = traceMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(page, LegalAiTraceRespVO.class);
    }

}
