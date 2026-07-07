package com.laby.module.legal.service.contract;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.framework.config.LegalFormatConvertProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 合同格式转换（LibreOffice headless：DOC→DOCX）。
 */
@Slf4j
@Service
public class LegalFormatConvertService {

    @Resource
    private LegalFormatConvertProperties convertProperties;

    /**
     * DOC → DOCX
     *
     * @return docx 字节；不可用或失败时返回 null
     */
    public byte[] tryConvertDocToDocx(byte[] docBytes, String baseFileName) {
        return tryConvertToDocx(docBytes, "doc", baseFileName);
    }

    private byte[] tryConvertToDocx(byte[] sourceBytes, String sourceExtension, String baseFileName) {
        if (!Boolean.TRUE.equals(convertProperties.getLibreOfficeEnabled())) {
            log.debug("[tryConvertToDocx] LibreOffice 未启用");
            return null;
        }
        if (sourceBytes == null || sourceBytes.length == 0 || StrUtil.isBlank(sourceExtension)) {
            return null;
        }
        String ext = sourceExtension.toLowerCase();
        if ("docx".equals(ext)) {
            return sourceBytes;
        }
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("laby-format-convert-");
            String safeName = StrUtil.blankToDefault(baseFileName, "contract." + ext);
            if (!safeName.toLowerCase().endsWith("." + ext)) {
                safeName = FileUtil.mainName(safeName) + "." + ext;
            }
            Path input = workDir.resolve(safeName);
            Files.write(input, sourceBytes);

            ProcessBuilder builder = new ProcessBuilder(
                    convertProperties.getLibreOfficePath(),
                    "--headless",
                    "--convert-to", "docx",
                    "--outdir", workDir.toAbsolutePath().toString(),
                    input.toAbsolutePath().toString());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean finished = process.waitFor(
                    convertProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("[tryConvertToDocx][ext={}] 转换超时", ext);
                return null;
            }
            if (process.exitValue() != 0) {
                log.warn("[tryConvertToDocx][ext={}] 转换失败 exit={}", ext, process.exitValue());
                return null;
            }
            Path docxPath = workDir.resolve(FileUtil.mainName(safeName) + ".docx");
            if (!Files.exists(docxPath)) {
                log.warn("[tryConvertToDocx][ext={}] 未找到输出 docx", ext);
                return null;
            }
            return Files.readAllBytes(docxPath);
        } catch (Exception ex) {
            log.warn("[tryConvertToDocx][ext={}] 转换异常: {}", ext, ex.getMessage());
            return null;
        } finally {
            if (workDir != null) {
                try {
                    FileUtil.del(workDir.toFile());
                } catch (Exception ignored) {
                    // ignore cleanup
                }
            }
        }
    }

}
