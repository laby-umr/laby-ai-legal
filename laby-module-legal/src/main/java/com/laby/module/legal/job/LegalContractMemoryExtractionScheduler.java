package com.laby.module.legal.job;

import cn.hutool.core.collection.CollUtil;
import com.laby.framework.tenant.core.service.TenantFrameworkService;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.legal.service.memory.LegalContractMemoryExtractionBackfillService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时补抽合同问答记忆（Mem0 风格事实 + 情节记忆）。
 */
@Slf4j
@Component
public class LegalContractMemoryExtractionScheduler {

    private static final int BATCH_SIZE = 30;

    @Resource
    private LegalContractMemoryExtractionBackfillService backfillService;
    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Scheduled(cron = "0 15 * * * ?")
    public void backfillMemoriesHourly() {
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (CollUtil.isEmpty(tenantIds)) {
            return;
        }
        for (Long tenantId : tenantIds) {
            TenantUtils.execute(tenantId, () -> runForTenant(tenantId));
        }
    }

    private void runForTenant(Long tenantId) {
        try {
            int processed = backfillService.backfillRecentUnprocessed(BATCH_SIZE);
            if (processed > 0) {
                log.info("[backfillMemoriesHourly][tenantId={}] processed {} chat turns", tenantId, processed);
            }
        } catch (Exception ex) {
            log.warn("[backfillMemoriesHourly][tenantId={}] failed: {}", tenantId, ex.getMessage(), ex);
        }
    }

}
