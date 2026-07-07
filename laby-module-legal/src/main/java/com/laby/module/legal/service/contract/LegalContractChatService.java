package com.laby.module.legal.service.contract;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatRespVO;
import reactor.core.publisher.Flux;

/**
 * 法务合同问答 Service 接口
 */
public interface LegalContractChatService {

    /**
     * 同步问答
     *
     * @param reqVO 问答请求
     * @return 模型回复
     */
    LegalContractChatRespVO chat(LegalContractChatReqVO reqVO);

    /**
     * 流式问答（SSE）
     *
     * @param reqVO 问答请求
     * @return 流式 chunk
     */
    Flux<CommonResult<LegalContractChatRespVO>> chatStream(LegalContractChatReqVO reqVO);

}
