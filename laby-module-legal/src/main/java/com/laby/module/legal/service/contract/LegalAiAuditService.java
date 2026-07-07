package com.laby.module.legal.service.contract;

import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;

/**
 * AI 审核编排（V1 占位，后续对接 laby-module-ai）
 */
public interface LegalAiAuditService {

    /**
     * 异步入队 AI 审核（Redis Stream，替代裸 @Async）
     */
    void auditAsync(Long contractId, int auditRound);

    /**
     * BPM ServiceTask：仅异步入队 AI 审核（等待由 ReceiveTask + signal 完成）
     */
    void enqueueAuditForBpm(Long contractId, int auditRound);

    /**
     * @deprecated 使用 {@link #enqueueAuditForBpm}；保留兼容旧 delegate 名称
     */
    @Deprecated
    void waitForAuditRoundComplete(Long contractId, int auditRound);

    /**
     * BPM 服务任务内同步执行（兼容旧调用，内部转 {@link #enqueueAuditForBpm}）
     */
    void auditForBpm(Long contractId, int auditRound);

    /**
     * 应用层流水线同步审核（失败抛异常，不进入 BPM）
     */
    void auditForPipeline(Long contractId, int auditRound);

    /**
     * 执行 AI 审核（MQ 消费者或同步流水线调用）
     */
    void executeAudit(Long contractId, int auditRound, boolean failFast);

    /**
     * 若报告缺失但已有意见，按意见重新生成报告（修复历史租户数据不一致）
     */
    LegalAuditReportDO rebuildAuditReportIfMissing(Long contractId, int auditRound);

}
