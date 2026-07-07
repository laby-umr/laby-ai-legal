package com.laby.module.legal.service.contract;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.infra.dal.dataobject.file.FileDO;
import com.laby.module.infra.service.file.FileService;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractExportPrecheckRespVO;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractPublishLogDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractVersionDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractFileMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractPublishLogMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractVersionMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.dal.mysql.report.LegalAuditReportMapper;
import com.laby.module.legal.enums.LegalContractConstants;
import com.laby.module.legal.enums.contract.LegalContractDeliverableEnum;
import com.laby.module.legal.enums.contract.LegalContractExportModeEnum;
import com.laby.module.legal.enums.contract.LegalContractExportVisibilityEnum;
import com.laby.module.legal.enums.contract.LegalContractVersionTypeEnum;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.service.contract.util.LegalContractDocxRenderUtil;
import com.laby.module.legal.service.opinion.LegalAuditOpinionRewriteSupport;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;
import com.laby.module.legal.enums.contract.LegalContractFileRoleEnum;
import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;
import com.laby.module.legal.service.contract.util.LegalContractArchiveZipUtil;
import com.laby.module.legal.service.contract.util.LegalExceptionUtils;
import com.laby.module.legal.service.contract.util.LegalMarkdownDocxConverter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_EXPORT_REPORT_FAILED;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

/**
 * 法务合同导出 Service 实现类
 */
@Slf4j
@Service
public class LegalContractExportServiceImpl implements LegalContractExportService {

    private static final String EXPORT_DIRECTORY = LegalContractConstants.EXPORT_FILE_DIRECTORY;
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String DELIV_SPEC = "Laby-Legal-DELIV-001-v1.0";

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalAuditReportMapper reportMapper;
    @Resource
    private LegalContractFileMapper contractFileMapper;
    @Resource
    private LegalContractVersionMapper contractVersionMapper;
    @Resource
    private LegalContractPublishLogMapper publishLogMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private FileService fileService;
    @Resource
    private LegalContractVersionService contractVersionService;
    @Resource
    private LegalContractDeliverableService deliverableService;

    @Override
    public Long exportReportDocx(Long contractId) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        int round = contract.getAuditRound() != null ? contract.getAuditRound() : 1;
        LegalAuditReportDO report = reportMapper.selectByContractIdAndRound(contractId, round);
        String markdown = report != null && StrUtil.isNotBlank(report.getContent())
                ? report.getContent()
                : "# 合同审核报告\n\n暂无审核报告内容。\n";

        byte[] bytes;
        try {
            bytes = LegalMarkdownDocxConverter.toDocxBytes(markdown);
        } catch (Exception e) {
            LegalExceptionUtils.rethrowServiceException(e);
            log.error("[exportReportDocx][contractId={}] docx 生成失败", contractId, e);
            throw exception(CONTRACT_EXPORT_REPORT_FAILED);
        }

        String safeTitle = StrUtil.blankToDefault(contract.getTitle(), "合同");
        String fileName = safeTitle + "-审核报告-第" + round + "轮.docx";
        Long fileId = fileService.createFileReturnId(bytes, fileName, EXPORT_DIRECTORY, DOCX_CONTENT_TYPE);
        FileDO fileDO = fileService.getFile(fileId);

        contractFileMapper.insert(LegalContractFileDO.builder()
                .contractId(contractId)
                .fileId(fileId)
                .fileName(fileDO.getName())
                .mainFlag(false)
                .build());
        log.info("[exportReportDocx] contractId={} fileId={} fileName={}", contractId, fileId, fileName);
        return fileId;
    }

    @Override
    public Long exportAnnotatedContractDocx(Long contractId, LegalContractExportVisibilityEnum visibility) {
        LegalContractDO contract = getContract(contractId);
        int auditRound = resolveAuditRound(contract);
        byte[] rendered;
        if (visibility == LegalContractExportVisibilityEnum.EXTERNAL) {
            rendered = contractVersionService.readOriginalBytes(contractId);
            try {
                rendered = LegalContractDocxRenderUtil.stripComments(rendered);
            } catch (Exception e) {
                LegalExceptionUtils.rethrowServiceException(e);
            }
        } else {
            LegalContractFileDownloadBO bo = deliverableService.generate(
                    contractId, LegalContractDeliverableEnum.ANNOTATED, auditRound);
            rendered = bo.getContent();
        }
        String suffix = visibility == LegalContractExportVisibilityEnum.EXTERNAL ? "外发版" : "内部版";
        String fileName = StrUtil.blankToDefault(contract.getTitle(), "合同") + "-标注版-" + suffix + ".docx";
        return saveEphemeralExportFile(contractId, fileName, rendered, DOCX_CONTENT_TYPE);
    }

    @Override
    public Long exportAdoptedContractDocx(Long contractId, LegalContractExportModeEnum mode,
                                          LegalContractExportVisibilityEnum visibility) {
        LegalContractDO contract = getContract(contractId);
        int auditRound = resolveAuditRound(contract);
        LegalContractDeliverableEnum deliverable = mode == LegalContractExportModeEnum.TRACKED
                ? LegalContractDeliverableEnum.REVISION
                : LegalContractDeliverableEnum.ADOPTED;
        LegalContractFileDownloadBO bo = deliverableService.generate(contractId, deliverable, auditRound);
        byte[] rendered = bo.getContent();
        if (visibility == LegalContractExportVisibilityEnum.EXTERNAL) {
            try {
                rendered = LegalContractDocxRenderUtil.stripComments(rendered);
            } catch (Exception e) {
                LegalExceptionUtils.rethrowServiceException(e);
            }
        }
        String modeName = mode == LegalContractExportModeEnum.TRACKED ? "修订版" : "采纳版";
        String suffix = visibility == LegalContractExportVisibilityEnum.EXTERNAL ? "外发版" : "内部版";
        String fileName = StrUtil.blankToDefault(contract.getTitle(), "合同") + "-" + modeName + "-" + suffix + ".docx";
        return saveEphemeralExportFile(contractId, fileName, rendered, DOCX_CONTENT_TYPE);
    }

    @Override
    public LegalContractExportPrecheckRespVO precheckAdoptedExport(Long contractId) {
        LegalContractDO contract = getContract(contractId);
        List<LegalAuditOpinionDO> adoptedOpinions = opinionMapper
                .selectListByContractIdAndRound(contract.getId(), contract.getAuditRound() == null ? 1 : contract.getAuditRound())
                .stream()
                .filter(item -> LegalOpinionStatusEnum.ADOPTED.getStatus().equals(item.getStatus()))
                .toList();
        Map<String, LegalContractParagraphDO> paragraphMap = paragraphMapper.selectListByContractId(contractId)
                .stream()
                .collect(Collectors.toMap(LegalContractParagraphDO::getParagraphId, Function.identity(), (a, b) -> a));
        int conflictCount = 0;
        int autoWritableCount = 0;
        int manualConfirmCount = 0;
        for (LegalAuditOpinionDO opinion : adoptedOpinions) {
            boolean writable = LegalAuditOpinionRewriteSupport.isAdoptApplicableToDocument(opinion);
            if (writable) {
                autoWritableCount++;
            }
            if (isConflict(opinion, paragraphMap)) {
                conflictCount++;
            }
            if (needManualConfirm(opinion)) {
                manualConfirmCount++;
            }
        }
        LegalContractExportPrecheckRespVO respVO = new LegalContractExportPrecheckRespVO();
        respVO.setAdoptedCount(adoptedOpinions.size());
        respVO.setAutoWritableCount(autoWritableCount);
        respVO.setConflictCount(conflictCount);
        respVO.setManualConfirmCount(manualConfirmCount);
        int auditRound = contract.getAuditRound() == null ? 1 : contract.getAuditRound();
        var anchorPrecheck = contractVersionService.precheckBookmarkAnchors(contractId, auditRound);
        respVO.setAnchorMissingCount(anchorPrecheck.getMissingCount());
        respVO.setAnchorOrphanCount(anchorPrecheck.getOrphanCount());
        respVO.setMissingParagraphIds(anchorPrecheck.getMissingParagraphIds());
        respVO.setOrphanBookmarkNames(anchorPrecheck.getOrphanBookmarkNames());
        return respVO;
    }

    @Override
    public Long exportDeliveryBundle(Long contractId) {
        LegalContractDO contract = getContract(contractId);
        int auditRound = resolveAuditRound(contract);
        try {
            Deliv001Bundle bundle = collectDeliv001Bundle(contractId, contract, auditRound);
            Map<String, byte[]> entries = buildDeliv001ZipEntries(bundle);
            String zipName = StrUtil.blankToDefault(contract.getTitle(), "合同")
                    + "-交付包-第" + auditRound + "轮.zip";
            return storeZipFile(contract, zipName, entries, false, auditRound, null);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[exportDeliveryBundle][contractId={}] 打包失败，降级返回报告", contractId, ex);
            return exportReportDocx(contractId);
        }
    }

    @Override
    public Long exportArchivePackage(Long contractId) {
        LegalContractDO contract = getContract(contractId);
        int auditRound = resolveAuditRound(contract);
        LegalContractVersionDO existing = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.PUBLISHED.getCode(), auditRound);
        if (existing != null && existing.getFileId() != null) {
            log.info("[exportArchivePackage][contractId={} round={}] 已存在 PUBLISHED 发布包 fileId={}",
                    contractId, auditRound, existing.getFileId());
            return existing.getFileId();
        }
        LegalContractPublishLogDO publishLog = publishLogMapper.selectByContractIdAndRound(contractId, auditRound);
        if (publishLog != null && publishLog.getBundleFileId() != null) {
            log.info("[exportArchivePackage][contractId={} round={}] 已存在 publish_log fileId={}",
                    contractId, auditRound, publishLog.getBundleFileId());
            return publishLog.getBundleFileId();
        }
        try {
            Deliv001Bundle bundle = collectDeliv001Bundle(contractId, contract, auditRound);
            Map<String, byte[]> entries = buildDeliv001ZipEntries(bundle);
            String zipName = StrUtil.blankToDefault(contract.getTitle(), "合同")
                    + "-归档包-第" + auditRound + "轮.zip";
            return storeZipFile(contract, zipName, entries, true, auditRound, bundle);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[exportArchivePackage][contractId={}] 归档 zip 打包失败，降级返回报告", contractId, ex);
            return exportReportDocx(contractId);
        }
    }

    /**
     * 收集四件套 + 报告字节（按需生成，不写 AI_ANNOTATED/ADOPTED 缓存）。
     */
    private Deliv001Bundle collectDeliv001Bundle(Long contractId, LegalContractDO contract, int auditRound)
            throws Exception {
        Long reportFileId = exportReportDocx(contractId);
        byte[] reportBytes = readFileBytesById(reportFileId);
        LegalContractFileDownloadBO original = deliverableService.generate(
                contractId, LegalContractDeliverableEnum.ORIGINAL, auditRound);
        LegalContractFileDownloadBO annotated = deliverableService.generate(
                contractId, LegalContractDeliverableEnum.ANNOTATED, auditRound);
        LegalContractFileDownloadBO revision = tryGenerateDeliverable(contractId, auditRound,
                LegalContractDeliverableEnum.REVISION);
        LegalContractFileDownloadBO adopted = tryGenerateDeliverable(contractId, auditRound,
                LegalContractDeliverableEnum.ADOPTED);
        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractIdAndRound(contractId, auditRound);
        byte[] workingBytes = contractVersionService.readWorkingBytes(contractId, auditRound);
        return new Deliv001Bundle(contract, auditRound, reportBytes, original, annotated, revision, adopted,
                opinions, workingBytes);
    }

    /**
     * 按 DELIV-001 §17 命名规则构建 ZIP 条目。
     */
    private Map<String, byte[]> buildDeliv001ZipEntries(Deliv001Bundle bundle) {
        Map<String, String> entryNames = buildDeliv001EntryNames(bundle);

        Map<String, byte[]> entries = new LinkedHashMap<>();
        String manifestJson = JsonUtils.toJsonPrettyString(buildDeliv001Manifest(bundle, entryNames));
        entries.put("manifest.json", manifestJson.getBytes(StandardCharsets.UTF_8));
        addBytesToZip(entries, entryNames.get("original"), bundle.original().getContent());
        addBytesToZip(entries, entryNames.get("annotated"), bundle.annotated().getContent());
        if (bundle.revision() != null) {
            addBytesToZip(entries, entryNames.get("revision"), bundle.revision().getContent());
        }
        if (bundle.adopted() != null) {
            addBytesToZip(entries, entryNames.get("adopted"), bundle.adopted().getContent());
        }
        addBytesToZip(entries, entryNames.get("report"), bundle.reportBytes());
        return entries;
    }

    private Map<String, Object> buildDeliv001Manifest(Deliv001Bundle bundle, Map<String, String> entryNames) {
        LegalContractDO contract = bundle.contract();
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("contractId", contract.getId());
        manifest.put("title", contract.getTitle());
        manifest.put("auditRound", bundle.auditRound());
        manifest.put("sourceFormat", contract.getSourceFormat());
        manifest.put("generatedAt", OffsetDateTime.now().toString());
        manifest.put("originalSha256", resolveOriginalSha256(contract, bundle.original().getContent()));
        manifest.put("workingSha256", bundle.workingBytes() == null
                ? null : DigestUtil.sha256Hex(bundle.workingBytes()));
        manifest.put("opinionStats", buildOpinionStats(bundle.opinions()));
        manifest.put("files", entryNames);
        manifest.put("deliverableSpec", DELIV_SPEC);
        return manifest;
    }

    private static Map<String, Object> buildOpinionStats(List<LegalAuditOpinionDO> opinions) {
        Map<String, Object> stats = new LinkedHashMap<>();
        int pending = 0;
        int adopted = 0;
        int ignored = 0;
        int adoptApplicable = 0;
        for (LegalAuditOpinionDO opinion : opinions) {
            if (LegalOpinionStatusEnum.PENDING.getStatus().equals(opinion.getStatus())) {
                pending++;
            } else if (LegalOpinionStatusEnum.ADOPTED.getStatus().equals(opinion.getStatus())) {
                adopted++;
                if (LegalAuditOpinionRewriteSupport.isAdoptApplicableToDocument(opinion)) {
                    adoptApplicable++;
                }
            } else if (LegalOpinionStatusEnum.IGNORED.getStatus().equals(opinion.getStatus())) {
                ignored++;
            }
        }
        stats.put("total", opinions.size());
        stats.put("pending", pending);
        stats.put("adopted", adopted);
        stats.put("ignored", ignored);
        stats.put("adoptApplicable", adoptApplicable);
        return stats;
    }

    private Long storeZipFile(LegalContractDO contract, String zipName, Map<String, byte[]> entries,
                              boolean recordPublished, int auditRound, Deliv001Bundle bundle) throws Exception {
        byte[] zipBytes = LegalContractArchiveZipUtil.buildZip(entries);
        if (zipBytes.length == 0) {
            return exportReportDocx(contract.getId());
        }
        Long zipFileId = fileService.createFileReturnId(zipBytes, zipName, EXPORT_DIRECTORY, "application/zip");
        FileDO zipFile = fileService.getFile(zipFileId);
        contractFileMapper.insert(LegalContractFileDO.builder()
                .contractId(contract.getId())
                .fileId(zipFileId)
                .fileName(zipFile.getName())
                .mainFlag(false)
                .role(recordPublished ? LegalContractFileRoleEnum.PUBLISHED_BUNDLE.getRole() : null)
                .build());
        if (recordPublished && bundle != null) {
            Map<String, String> entryNames = buildDeliv001EntryNames(bundle);
            String manifestJson = JsonUtils.toJsonPrettyString(buildDeliv001Manifest(bundle, entryNames));
            recordPublishedBundle(contract, auditRound, zipFileId, zipBytes, bundle, manifestJson);
        }
        log.info("[storeZipFile][contractId={} published={}] zipFileId={} entries={}",
                contract.getId(), recordPublished, zipFileId, entries.size());
        return zipFileId;
    }

    private Map<String, String> buildDeliv001EntryNames(Deliv001Bundle bundle) {
        LegalContractDO contract = bundle.contract();
        String originalExt = resolveOriginalExtension(contract);
        Map<String, String> entryNames = new LinkedHashMap<>();
        entryNames.put("original", "01-源文件." + originalExt);
        entryNames.put("annotated", "02-标注版.docx");
        if (bundle.revision() != null) {
            entryNames.put("revision", "03-修订版.docx");
        }
        if (bundle.adopted() != null) {
            entryNames.put("adopted", "04-采纳版.docx");
        }
        entryNames.put("report", "05-审核报告.docx");
        return entryNames;
    }

    private LegalContractFileDownloadBO tryGenerateDeliverable(Long contractId, int auditRound,
                                                               LegalContractDeliverableEnum deliverable) {
        try {
            return deliverableService.generate(contractId, deliverable, auditRound);
        } catch (Exception ex) {
            log.warn("[tryGenerateDeliverable][contractId={} deliverable={}] 跳过：{}",
                    contractId, deliverable.getCode(), ex.getMessage());
            return null;
        }
    }

    private void recordPublishedBundle(LegalContractDO contract, int auditRound, Long zipFileId, byte[] zipBytes,
                                       Deliv001Bundle bundle, String manifestJson) {
        LegalContractVersionDO latest = contractVersionMapper.selectLatestByContractId(contract.getId());
        int nextVersionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        LegalContractVersionDO workingVersion = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contract.getId(), LegalContractVersionTypeEnum.WORKING.getCode(), auditRound);
        contractVersionMapper.insert(LegalContractVersionDO.builder()
                .contractId(contract.getId())
                .auditRound(auditRound)
                .versionNo(nextVersionNo)
                .type(LegalContractVersionTypeEnum.PUBLISHED.getCode())
                .sourceVersionId(workingVersion == null ? null : workingVersion.getId())
                .fileId(zipFileId)
                .visibility(LegalContractExportVisibilityEnum.INTERNAL.getCode())
                .immutableHash(DigestUtil.sha256Hex(zipBytes))
                .build());

        Map<String, Object> stats = buildOpinionStats(bundle.opinions());
        int adoptedCount = stats.get("adopted") instanceof Number n ? n.intValue() : 0;
        int annotatedCount = adoptedCount + (stats.get("pending") instanceof Number p ? p.intValue() : 0);
        if (publishLogMapper.selectByContractIdAndRound(contract.getId(), auditRound) == null) {
            publishLogMapper.insert(LegalContractPublishLogDO.builder()
                    .contractId(contract.getId())
                    .auditRound(auditRound)
                    .bundleFileId(zipFileId)
                    .manifestJson(manifestJson)
                    .adoptedCount(adoptedCount)
                    .annotatedCount(annotatedCount)
                    .workingHash(bundle.workingBytes() == null ? null : DigestUtil.sha256Hex(bundle.workingBytes()))
                    .originalHash(resolveOriginalSha256(contract, bundle.original().getContent()))
                    .build());
        }
    }

    private Long saveEphemeralExportFile(Long contractId, String fileName, byte[] content, String contentType) {
        Long fileId = fileService.createFileReturnId(content, fileName, EXPORT_DIRECTORY, contentType);
        FileDO fileDO = fileService.getFile(fileId);
        contractFileMapper.insert(LegalContractFileDO.builder()
                .contractId(contractId)
                .fileId(fileId)
                .fileName(fileDO.getName())
                .mainFlag(false)
                .build());
        return fileId;
    }

    private byte[] readFileBytesById(Long fileId) throws Exception {
        FileDO file = fileService.getFile(fileId);
        if (file == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
        if (content == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        return content;
    }

    private static String resolveOriginalSha256(LegalContractDO contract, byte[] originalBytes) {
        if (StrUtil.isNotBlank(contract.getOriginalHash())) {
            return contract.getOriginalHash();
        }
        return originalBytes == null ? null : DigestUtil.sha256Hex(originalBytes);
    }

    private static String resolveOriginalExtension(LegalContractDO contract) {
        if (StrUtil.isNotBlank(contract.getOriginalFileName()) && contract.getOriginalFileName().contains(".")) {
            return contract.getOriginalFileName()
                    .substring(contract.getOriginalFileName().lastIndexOf('.') + 1)
                    .toLowerCase();
        }
        LegalContractSourceFormatEnum format = LegalContractSourceFormatEnum.of(contract.getSourceFormat());
        if (format == LegalContractSourceFormatEnum.DOC) {
            return "doc";
        }
        if (format == LegalContractSourceFormatEnum.PDF) {
            return "pdf";
        }
        return "docx";
    }

    private static int resolveAuditRound(LegalContractDO contract) {
        return contract.getAuditRound() == null ? 1 : contract.getAuditRound();
    }

    private void addBytesToZip(Map<String, byte[]> entries, String entryName, byte[] content) {
        if (content == null || content.length == 0 || StrUtil.isBlank(entryName)) {
            return;
        }
        entries.put(entryName, content);
    }

    /**
     * DELIV-001 发布包上下文（四件套 + 报告 + 意见统计）。
     */
    private record Deliv001Bundle(
            LegalContractDO contract,
            int auditRound,
            byte[] reportBytes,
            LegalContractFileDownloadBO original,
            LegalContractFileDownloadBO annotated,
            LegalContractFileDownloadBO revision,
            LegalContractFileDownloadBO adopted,
            List<LegalAuditOpinionDO> opinions,
            byte[] workingBytes) {
    }

    private void addFileToZip(Map<String, byte[]> entries, Long fileId, String entryName) {
        if (fileId == null) {
            return;
        }
        try {
            FileDO file = fileService.getFile(fileId);
            if (file == null) {
                return;
            }
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (content != null && content.length > 0) {
                entries.put(entryName, content);
            }
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[addFileToZip][fileId={} name={}] 跳过", fileId, entryName, ex);
        }
    }

    private LegalContractDO getContract(Long contractId) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private static boolean needManualConfirm(LegalAuditOpinionDO opinion) {
        if (LegalRiskLevelEnum.HIGH.getCode().equalsIgnoreCase(opinion.getRiskLevel())) {
            return true;
        }
        return LegalOpinionSourceTypeEnum.RULE.getCode().equalsIgnoreCase(opinion.getSourceType())
                || LegalOpinionSourceTypeEnum.STANDARD_CLAUSE.getCode().equalsIgnoreCase(opinion.getSourceType());
    }

    private static boolean isConflict(LegalAuditOpinionDO opinion, Map<String, LegalContractParagraphDO> paragraphMap) {
        if (StrUtil.isBlank(opinion.getOldText()) || StrUtil.isBlank(opinion.getParagraphId())) {
            return false;
        }
        LegalContractParagraphDO paragraph = paragraphMap.get(opinion.getParagraphId());
        if (paragraph == null || StrUtil.isBlank(paragraph.getText())) {
            return true;
        }
        return !paragraph.getText().contains(opinion.getOldText());
    }

}
