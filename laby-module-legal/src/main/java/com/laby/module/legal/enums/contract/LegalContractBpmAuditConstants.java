package com.laby.module.legal.enums.contract;

/**
 * 法务合同 BPM 与 AI 审核衔接常量
 */
public final class LegalContractBpmAuditConstants {

    /** 二轮 AI 审核入队 ServiceTask */
    public static final String SERVICE_AI_ROUND2_ENQUEUE = "aiRound2Enqueue";

    /** 等待二轮 AI 审核完成的 ReceiveTask */
    public static final String RECEIVE_AWAIT_AI_ROUND2 = "awaitAiRound2";

    private LegalContractBpmAuditConstants() {
    }

}
