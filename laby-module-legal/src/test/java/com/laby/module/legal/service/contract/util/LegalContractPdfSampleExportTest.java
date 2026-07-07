package com.laby.module.legal.service.contract.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * 生成测试资源 PDF（本地执行一次即可，勿在 CI 强依赖中文字体）。
 */
class LegalContractPdfSampleExportTest {

    @Test
    void exportSampleContractPdfToResources() throws Exception {
        Path target = Path.of("src/test/resources/contracts/sample-contract.pdf");
        LegalContractPdfTestFixtures.writeSampleContractPdf(target);
    }

}
