package com.laby.module.ai.service.eval;

import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.ai.service.eval.bo.AiRagEvalReportBO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiRagEvalRunnerTest {

    private final AiRagEvalRunner runner = new AiRagEvalRunner();

    @Test
    void runFixtureDataset_shouldPassGoldenCases() {
        AiRagEvalReportBO report = runner.runFixtureDataset();

        assertEquals(10, report.getTotalCases());
        assertEquals(10, report.getPassedCases(), () -> "失败用例: " + report.getFailedCaseIds());
        assertTrue(report.getFailedCaseIds().isEmpty());
        assertEquals(1.0D, report.passRate(), 0.001);
        assertTrue(report.getHitAtKRate() >= 0.8D, () -> "Hit@K 过低: " + report.getHitAtKRate());
    }

    @Test
    void runPdfStructuredDataset_shouldPassGoldenCases() {
        AiRagEvalReportBO report = runner.runPdfStructuredDataset();

        assertEquals(12, report.getTotalCases());
        assertEquals(12, report.getPassedCases(), () -> "失败用例: " + report.getFailedCaseIds());
        assertTrue(report.getFailedCaseIds().isEmpty());
        assertTrue(report.getHitAtKRate() >= 0.8D, () -> "Hit@K 过低: " + report.getHitAtKRate());
    }

    @Test
    void runFuzzyQueryDataset_shouldPassGoldenCases() {
        AiRagEvalReportBO report = runner.runFuzzyQueryDataset();

        assertEquals(5, report.getTotalCases());
        assertEquals(5, report.getPassedCases(), () -> "失败用例: " + report.getFailedCaseIds());
        assertTrue(report.getFailedCaseIds().isEmpty());
    }

    @Test
    void runExcelDataset_shouldPassGoldenCases() {
        AiRagEvalReportBO report = runner.runExcelDataset();

        assertEquals(3, report.getTotalCases());
        assertEquals(3, report.getPassedCases(), () -> "失败用例: " + report.getFailedCaseIds());
        assertTrue(report.getFailedCaseIds().isEmpty());
    }

    @Test
    void ciGate_shouldMeetDefaultThreshold() throws Exception {
        AiRagEvalReportBO report = runner.runFixtureDataset();
        AiRagEvalGate.assertGate(report);

        Path reportDir = Path.of("target", "eval-reports");
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("rag-eval-report.json");
        Files.writeString(reportFile, JsonUtils.toJsonPrettyString(report));
    }

}
