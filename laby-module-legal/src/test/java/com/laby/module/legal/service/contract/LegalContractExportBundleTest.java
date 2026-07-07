package com.laby.module.legal.service.contract;

import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DELIV-001 发布包 manifest 单测。
 */
class LegalContractExportBundleTest {

    @Test
    void buildOpinionStats_shouldCountAllStatuses() throws Exception {
        List<LegalAuditOpinionDO> opinions = List.of(
                LegalAuditOpinionDO.builder().status(LegalOpinionStatusEnum.PENDING.getStatus()).build(),
                LegalAuditOpinionDO.builder().status(LegalOpinionStatusEnum.ADOPTED.getStatus())
                        .changeType("REPLACE").newText("x").build(),
                LegalAuditOpinionDO.builder().status(LegalOpinionStatusEnum.IGNORED.getStatus()).build());
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) invokeBuildOpinionStats(opinions);
        assertEquals(3, stats.get("total"));
        assertEquals(1, stats.get("pending"));
        assertEquals(1, stats.get("adopted"));
        assertEquals(1, stats.get("ignored"));
        assertEquals(1, stats.get("adoptApplicable"));
    }

    private static Object invokeBuildOpinionStats(List<LegalAuditOpinionDO> opinions) throws Exception {
        Method method = LegalContractExportServiceImpl.class.getDeclaredMethod("buildOpinionStats", List.class);
        method.setAccessible(true);
        return method.invoke(null, opinions);
    }

}
