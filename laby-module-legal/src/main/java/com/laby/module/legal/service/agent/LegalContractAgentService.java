package com.laby.module.legal.service.agent;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import reactor.core.publisher.Flux;

/**
 * 法务合同 Agent 编排 Service
 */
public interface LegalContractAgentService {

    /** Agent 流式问答最大 Tool 步数 */
    int MAX_AGENT_STEPS = 5;

    /** Agent 流式超时秒数 */
    int AGENT_TIMEOUT_SECONDS = 90;

    /**
     * Agent 模式流式问答
     *
     * @param contract 已校验合同
     * @param reqVO    问答请求（含 agentMode / allowProposal / sessionId）
     * @return SSE 流
     */
    Flux<CommonResult<LegalContractChatRespVO>> runStream(LegalContractDO contract,
                                                          LegalContractChatReqVO reqVO);

    /**
     * Agent 模式同步问答（走 HarnessAgent Tool 循环）
     */
    LegalContractChatRespVO runSync(LegalContractDO contract,
                                    LegalContractChatReqVO reqVO);

    /**
     * 执行用户确认的提案
     */
    void executeProposal(String proposalNo);

    /**
     * 取消待处理提案
     */
    void cancelProposal(String proposalNo);

}
