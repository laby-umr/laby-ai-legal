package com.laby.module.legal.enums;

/**
 * Playbook 黄金集评测常量
 */
public final class LegalEvalConstants {

    /** 默认黄金集 classpath 路径 */
    public static final String DEFAULT_PLAYBOOK_DATASET = "eval/playbook-cases.json";

    /** CI 门禁最低通过率 JVM 参数名 */
    public static final String MIN_PASS_RATE_PROPERTY = "legal.eval.minPassRate";

    /** 默认 CI 门禁通过率（100%） */
    public static final double DEFAULT_MIN_PASS_RATE = 1.0D;

    private LegalEvalConstants() {
    }

}
