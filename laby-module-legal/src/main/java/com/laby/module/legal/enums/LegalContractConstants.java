package com.laby.module.legal.enums;

/**
 * 法务合同模块常量（流程 Key、业务阈值、存储路径等）
 */
public final class LegalContractConstants {

    /** BPM 流程定义 Key，与 legal_contract_review.bpmn20.xml 一致 */
    public static final String PROCESS_KEY = "legal_contract_review";

    /** 合同附件 infra 存储目录 */
    public static final String CONTRACT_FILE_DIRECTORY = "legal/contract";

    /** 二轮 AI 反馈说明最少字数 */
    public static final int FEEDBACK_SUMMARY_MIN_LENGTH = 20;

    /** 单份合同主文件大小上限（MB） */
    public static final int MAX_CONTRACT_FILE_SIZE_MB = 30;

    /** 报告导出 infra 存储目录 */
    public static final String EXPORT_FILE_DIRECTORY = CONTRACT_FILE_DIRECTORY + "/export";

    private LegalContractConstants() {
    }

}
