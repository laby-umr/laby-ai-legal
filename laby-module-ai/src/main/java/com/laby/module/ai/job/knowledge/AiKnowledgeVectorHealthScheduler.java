package com.laby.module.ai.job.knowledge;

import cn.hutool.core.collection.CollUtil;
import com.laby.framework.tenant.core.service.TenantFrameworkService;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.ai.framework.agentscope.rag.AiVectorStoreHealthProperties;
import com.laby.module.ai.service.knowledge.AiVectorStoreHealthService;
import com.laby.module.ai.service.knowledge.bo.AiVectorHealthReportBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时对账知识库 segment 与 Qdrant 向量，必要时自动 re-embed。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "laby.ai.vector-store.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiKnowledgeVectorHealthScheduler {

    @Resource
    private AiVectorStoreHealthService vectorStoreHealthService;
    @Resource
    private AiVectorStoreHealthProperties healthProperties;
    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Scheduled(cron = "${laby.ai.vector-store.health.cron:0 30 3 * * ?}")
    public void checkVectorsDaily() {
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (CollUtil.isEmpty(tenantIds)) {
            return;
        }
        boolean dryRun = !healthProperties.isAutoRepair();
        for (Long tenantId : tenantIds) {
            TenantUtils.execute(tenantId, () -> runForCurrentTenant(dryRun, tenantId));
        }
    }

    private void runForCurrentTenant(boolean dryRun, Long tenantId) {
        try {
            AiVectorHealthReportBO report = vectorStoreHealthService.runHealthCheck(null, dryRun);
            if (report.getMissingInQdrant() > 0 || report.getMissingVectorId() > 0
                    || report.getModelMismatch() > 0 || report.getRepaired() > 0) {
                log.info("[checkVectorsDaily][tenantId={} dryRun={}] knowledgeCount={} scanned={} missingVectorId={} "
                                + "missingInQdrant={} modelMismatch={} repaired={} repairFailed={}",
                        tenantId, dryRun, report.getKnowledgeCount(), report.getSegmentScanned(),
                        report.getMissingVectorId(), report.getMissingInQdrant(), report.getModelMismatch(),
                        report.getRepaired(), report.getRepairFailed());
            }
        } catch (Exception ex) {
            log.warn("[checkVectorsDaily][tenantId={}] failed: {}", tenantId, ex.getMessage(), ex);
        }
    }

}
