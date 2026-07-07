package com.laby.module.legal.service.trace;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.trace.vo.LegalAiTracePageReqVO;
import com.laby.module.legal.controller.admin.trace.vo.LegalAiTraceRespVO;

/**
 * 法务 AI 链路追踪 Service 接口
 */
public interface LegalAiTraceService {

    /**
     * 获得 AI 追踪分页
     *
     * @param pageReqVO 分页查询
     * @return 追踪分页
     */
    PageResult<LegalAiTraceRespVO> getTracePage(LegalAiTracePageReqVO pageReqVO);

}
