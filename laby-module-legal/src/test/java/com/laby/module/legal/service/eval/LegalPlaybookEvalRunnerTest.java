package com.laby.module.legal.service.eval;

import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.service.eval.bo.LegalAuditEvalReportBO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalPlaybookEvalRunnerTest {

    private final LegalPlaybookEvalRunner runner = new LegalPlaybookEvalRunner();

    @Test
    void runFromClasspath_shouldPassAllGoldenCases() {
        LegalAuditEvalReportBO report = runner.runDefaultDataset();

        assertEquals(10, report.getTotalCases());
        assertEquals(10, report.getPassedCases());
        assertTrue(report.getFailedCaseIds().isEmpty());
        assertEquals(1.0D, report.passRate(), 0.001);
    }

    @Test
    void ciGate_shouldMeetDefaultThreshold() throws Exception {
        LegalAuditEvalReportBO report = runner.runDefaultDataset();
        LegalPlaybookEvalGate.assertGate(report);

        Path reportDir = Path.of("target", "eval-reports");
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("playbook-eval-report.json");
        Files.writeString(reportFile, JsonUtils.toJsonPrettyString(report));
    }

}
