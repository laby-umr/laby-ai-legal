package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.laby.module.infra.dal.dataobject.file.FileDO;
import com.laby.module.infra.service.file.FileService;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractVersionDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractFileMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractVersionMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.LegalContractConstants;
import com.laby.module.legal.enums.contract.LegalContractExportModeEnum;
import com.laby.module.legal.enums.contract.LegalContractExportVisibilityEnum;
import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;
import com.laby.module.legal.enums.contract.LegalContractVersionTypeEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.service.contract.bo.LegalContractAnchorPrecheckResult;
import com.laby.module.legal.service.contract.util.LegalContractDocxRenderUtil;
import com.laby.module.legal.service.contract.util.LegalExceptionUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

/**
 * 法务合同版本 Service 实现类
 */
@Slf4j
@Service
public class LegalContractVersionServiceImpl implements LegalContractVersionService {

    private static final String EXPORT_DIRECTORY = LegalContractConstants.EXPORT_FILE_DIRECTORY;
    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractFileMapper contractFileMapper;
    @Resource
    private LegalContractVersionMapper contractVersionMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private FileService fileService;
    @Resource
    @Lazy
    private LegalContractParseService parseService;
    @Resource
    private LegalContractAnchorSnapshotService anchorSnapshotService;

    @Override
    public void ensureOriginalVersion(Long contractId) {
        LegalContractDO contract = requireContract(contractId);
        int auditRound = resolveAuditRound(contract);
        if (existsVersion(contractId, LegalContractVersionTypeEnum.ORIGINAL, auditRound)) {
            return;
        }
        byte[] content = readMainFileBytes(contractId);
        if (StrUtil.isBlank(contract.getOriginalHash())) {
            LegalContractFileDO mainFile = contractFileMapper.selectMainByContractId(contractId);
            contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setOriginalHash(DigestUtil.sha256Hex(content))
                    .setOriginalFileName(mainFile != null ? mainFile.getFileName() : null));
        }
        String fileName = buildFileName(contract, "原始版");
        saveVersion(contract, resolveAuditRound(contract), fileName, LegalContractExportVisibilityEnum.INTERNAL,
                LegalContractVersionTypeEnum.ORIGINAL, content, null);
        log.info("[ensureOriginalVersion][contractId={}] 已记录 ORIGINAL", contractId);
    }

    @Override
    public Long ensureWorkingVersion(Long contractId) {
        return ensureWorkingVersion(contractId, null, null);
    }

    public Long ensureWorkingVersion(Long contractId, Integer auditRound, byte[] explicitSource) {
        LegalContractDO contract = requireContract(contractId);
        int round = auditRound != null ? auditRound : resolveAuditRound(contract);
        LegalContractVersionDO existing = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), round);
        if (existing != null) {
            return existing.getFileId();
        }
        byte[] source = explicitSource;
        if (source == null) {
            source = resolveWorkingSourceBytes(contractId, round);
        }
        byte[] working;
        try {
            working = LegalContractDocxRenderUtil.insertParagraphBookmarks(source);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[ensureWorkingVersion][contractId={} round={}] Bookmark 写入失败，降级复制源文件",
                    contractId, round, ex);
            working = source;
        }
        Long sourceVersionId = round > 1
                ? resolveVersionId(contractId, LegalContractVersionTypeEnum.ADOPTED_CLEAN, round - 1)
                : resolveVersionId(contractId, LegalContractVersionTypeEnum.ORIGINAL, 1);
        if (sourceVersionId == null) {
            sourceVersionId = resolveVersionId(contractId, LegalContractVersionTypeEnum.ORIGINAL, 1);
        }
        String fileName = buildFileName(contract, round > 1 ? "工作版-第" + round + "轮" : "工作版");
        return saveVersion(contract, round, fileName, LegalContractExportVisibilityEnum.INTERNAL,
                LegalContractVersionTypeEnum.WORKING, working, sourceVersionId);
    }

    @Override
    public void prepareSecondAuditRound(Long contractId) {
        int previousRound = 1;
        ensureAdoptedVersions(contractId, previousRound);
        byte[] clean = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.ADOPTED_CLEAN, previousRound);
        if (clean == null) {
            clean = readExportSourceBytes(contractId, 2);
            log.warn("[prepareSecondAuditRound][contractId={}] 未找到一轮 ADOPTED_CLEAN，降级使用导出源", contractId);
        }
        parseService.reparseFromDocxBytes(contractId, clean);
        int targetRound = 2;
        LegalContractVersionDO existingWorking = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), targetRound);
        if (existingWorking == null) {
            ensureWorkingVersion(contractId, targetRound, clean);
        }
        log.info("[prepareSecondAuditRound][contractId={}] 二轮审核准备完成", contractId);
    }

    @Override
    public Long resolveFromVersionIdForOpinions(Long contractId, int auditRound) {
        if (auditRound > 1) {
            Long cleanVersionId = resolveVersionId(contractId, LegalContractVersionTypeEnum.ADOPTED_CLEAN, auditRound - 1);
            if (cleanVersionId != null) {
                return cleanVersionId;
            }
        }
        return resolveWorkingVersionId(contractId, auditRound);
    }

    @Override
    public LegalContractAnchorPrecheckResult precheckBookmarkAnchors(Long contractId, int auditRound) {
        LegalContractAnchorPrecheckResult result = LegalContractAnchorPrecheckResult.empty();
        List<String> paragraphIds = paragraphMapper.selectListByContractId(contractId).stream()
                .map(LegalContractParagraphDO::getParagraphId)
                .filter(StrUtil::isNotBlank)
                .toList();
        if (CollUtil.isEmpty(paragraphIds)) {
            return result;
        }
        byte[] workingBytes = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.WORKING, auditRound);
        if (workingBytes == null) {
            result.setMissingCount(paragraphIds.size());
            result.setMissingParagraphIds(paragraphIds.stream().limit(20).collect(Collectors.toList()));
            return result;
        }
        try {
            LinkedHashSet<String> bookmarkNames = LegalContractDocxRenderUtil.listParagraphBookmarkNames(workingBytes);
            Set<String> expected = paragraphIds.stream()
                    .map(LegalContractDocxRenderUtil::toBookmarkName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> missing = new ArrayList<>();
            for (String paragraphId : paragraphIds) {
                if (!bookmarkNames.contains(LegalContractDocxRenderUtil.toBookmarkName(paragraphId))) {
                    missing.add(paragraphId);
                }
            }
            List<String> orphan = new ArrayList<>();
            for (String bookmarkName : bookmarkNames) {
                if (!expected.contains(bookmarkName)) {
                    orphan.add(bookmarkName);
                }
            }
            result.setMissingCount(missing.size());
            result.setOrphanCount(orphan.size());
            result.setMissingParagraphIds(missing.stream().limit(20).collect(Collectors.toList()));
            result.setOrphanBookmarkNames(orphan.stream().limit(20).collect(Collectors.toList()));
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[precheckBookmarkAnchors][contractId={} round={}] 读取 Bookmark 失败", contractId, auditRound, ex);
            result.setMissingCount(paragraphIds.size());
            result.setMissingParagraphIds(paragraphIds.stream().limit(20).collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public void repairWorkingVersion(Long contractId, int auditRound) {
        byte[] source = readExportSourceBytes(contractId, auditRound);
        byte[] working;
        try {
            working = LegalContractDocxRenderUtil.insertParagraphBookmarks(source);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[repairWorkingVersion][contractId={} round={}] Bookmark 写入失败，降级复制源文件",
                    contractId, auditRound, ex);
            working = source;
        }
        LegalContractVersionDO existing = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), auditRound);
        LegalContractDO contract = requireContract(contractId);
        String fileName = buildFileName(contract, "工作版-修复锚点-第" + auditRound + "轮");
        Long fileId = fileService.createFileReturnId(working, fileName, EXPORT_DIRECTORY, DOCX_CONTENT_TYPE);
        FileDO fileDO = fileService.getFile(fileId);
        if (existing != null) {
            contractVersionMapper.updateById(new LegalContractVersionDO()
                    .setId(existing.getId())
                    .setFileId(fileId)
                    .setImmutableHash(DigestUtil.sha256Hex(working)));
            anchorSnapshotService.createOrRefreshSnapshot(contractId, existing.getId());
            log.info("[repairWorkingVersion][contractId={} round={}] 已更新 WORKING versionId={}",
                    contractId, auditRound, existing.getId());
        } else {
            Long sourceVersionId = auditRound > 1
                    ? resolveVersionId(contractId, LegalContractVersionTypeEnum.ADOPTED_CLEAN, auditRound - 1)
                    : resolveVersionId(contractId, LegalContractVersionTypeEnum.ORIGINAL, 1);
            saveVersion(contract, auditRound, fileName, LegalContractExportVisibilityEnum.INTERNAL,
                    LegalContractVersionTypeEnum.WORKING, working, sourceVersionId);
            log.info("[repairWorkingVersion][contractId={} round={}] 已新建 WORKING", contractId, auditRound);
        }
        contractFileMapper.insert(LegalContractFileDO.builder()
                .contractId(contractId)
                .fileId(fileId)
                .fileName(fileDO.getName())
                .mainFlag(false)
                .build());
    }

    private byte[] resolveWorkingSourceBytes(Long contractId, int round) {
        if (round > 1) {
            byte[] previousClean = readVersionFileBytes(contractId,
                    LegalContractVersionTypeEnum.ADOPTED_CLEAN, round - 1);
            if (previousClean != null) {
                return previousClean;
            }
        }
        byte[] original = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.ORIGINAL, 1);
        if (original != null) {
            return original;
        }
        return readMainFileBytes(contractId);
    }

    @Override
    public void ensureAnnotatedVersion(Long contractId, int auditRound) {
        LegalContractDO contract = requireContract(contractId);
        if (existsVersion(contractId, LegalContractVersionTypeEnum.AI_ANNOTATED, auditRound)) {
            return;
        }
        byte[] source = readExportSourceBytes(contractId, auditRound);
        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractIdAndRound(contractId, auditRound);
        byte[] rendered;
        try {
            rendered = LegalContractDocxRenderUtil.renderAnnotated(source, opinions);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[ensureAnnotatedVersion][contractId={} round={}] 标注渲染失败，降级源文件", contractId, auditRound, ex);
            rendered = source;
        }
        Long sourceVersionId = resolveWorkingVersionId(contractId, auditRound);
        String fileName = buildFileName(contract, "标注版-内部版-第" + auditRound + "轮");
        saveVersion(contract, auditRound, fileName, LegalContractExportVisibilityEnum.INTERNAL,
                LegalContractVersionTypeEnum.AI_ANNOTATED, rendered, sourceVersionId);
        log.info("[ensureAnnotatedVersion][contractId={} round={}] 已自动生成 AI_ANNOTATED", contractId, auditRound);
    }

    @Override
    public void ensureAdoptedVersions(Long contractId, int auditRound) {
        LegalContractDO contract = requireContract(contractId);
        List<LegalAuditOpinionDO> adoptedOpinions = opinionMapper
                .selectListByContractIdAndRound(contractId, auditRound)
                .stream()
                .filter(item -> LegalOpinionStatusEnum.ADOPTED.getStatus().equals(item.getStatus()))
                .toList();
        if (CollUtil.isEmpty(adoptedOpinions)) {
            log.info("[ensureAdoptedVersions][contractId={} round={}] 无采纳意见，跳过", contractId, auditRound);
            return;
        }
        byte[] source = readExportSourceBytes(contractId, auditRound);
        Long sourceVersionId = resolveWorkingVersionId(contractId, auditRound);

        if (!existsVersion(contractId, LegalContractVersionTypeEnum.ADOPTED_TRACKED, auditRound)) {
            byte[] tracked = renderAdopted(contractId, auditRound, source, adoptedOpinions,
                    LegalContractExportModeEnum.TRACKED);
            String fileName = buildFileName(contract, "采纳版-带修订-内部版-第" + auditRound + "轮");
            saveVersion(contract, auditRound, fileName, LegalContractExportVisibilityEnum.INTERNAL,
                    LegalContractVersionTypeEnum.ADOPTED_TRACKED, tracked, sourceVersionId);
        }
        if (!existsVersion(contractId, LegalContractVersionTypeEnum.ADOPTED_CLEAN, auditRound)) {
            byte[] clean = renderAdopted(contractId, auditRound, source, adoptedOpinions,
                    LegalContractExportModeEnum.CLEAN);
            String fileName = buildFileName(contract, "采纳版-干净-内部版-第" + auditRound + "轮");
            saveVersion(contract, auditRound, fileName, LegalContractExportVisibilityEnum.INTERNAL,
                    LegalContractVersionTypeEnum.ADOPTED_CLEAN, clean, sourceVersionId);
        }
        log.info("[ensureAdoptedVersions][contractId={} round={}] 已自动生成 ADOPTED 版本", contractId, auditRound);
    }

    @Override
    public byte[] readExportSourceBytes(Long contractId, Integer auditRound) {
        int round = auditRound == null ? 1 : auditRound;
        if (round > 1) {
            byte[] previousClean = readVersionFileBytes(contractId,
                    LegalContractVersionTypeEnum.ADOPTED_CLEAN, round - 1);
            if (previousClean != null) {
                return previousClean;
            }
        }
        byte[] working = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.WORKING, round);
        if (working != null) {
            return working;
        }
        byte[] original = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.ORIGINAL, round);
        if (original != null) {
            return original;
        }
        return readMainFileBytes(contractId);
    }

    @Override
    public Long resolveWorkingVersionId(Long contractId, int auditRound) {
        LegalContractVersionDO working = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), auditRound);
        if (working != null) {
            return working.getId();
        }
        return resolveVersionId(contractId, LegalContractVersionTypeEnum.ORIGINAL, auditRound);
    }

    private byte[] renderAdopted(Long contractId, int auditRound, byte[] source,
                                 List<LegalAuditOpinionDO> adoptedOpinions,
                                 LegalContractExportModeEnum mode) {
        try {
            return mode == LegalContractExportModeEnum.TRACKED
                    ? LegalContractDocxRenderUtil.renderAdoptedTracked(source, adoptedOpinions)
                    : LegalContractDocxRenderUtil.renderAdoptedClean(source, adoptedOpinions);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[renderAdopted][contractId={} round={} mode={}] 采纳渲染失败，降级源文件",
                    contractId, auditRound, mode, ex);
            return source;
        }
    }

    private Long saveVersion(LegalContractDO contract, int auditRound, String targetName,
                             LegalContractExportVisibilityEnum visibility,
                             LegalContractVersionTypeEnum versionType,
                             byte[] content, Long explicitSourceVersionId) {
        Long contractId = contract.getId();
        Long fileId = fileService.createFileReturnId(content, targetName, EXPORT_DIRECTORY, DOCX_CONTENT_TYPE);
        FileDO fileDO = fileService.getFile(fileId);
        contractFileMapper.insert(LegalContractFileDO.builder()
                .contractId(contractId)
                .fileId(fileId)
                .fileName(fileDO.getName())
                .mainFlag(false)
                .build());

        LegalContractVersionDO latest = contractVersionMapper.selectLatestByContractId(contractId);
        int nextVersionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        Long sourceVersionId = explicitSourceVersionId != null
                ? explicitSourceVersionId
                : (latest == null ? null : latest.getId());
        LegalContractVersionDO versionRow = LegalContractVersionDO.builder()
                .contractId(contractId)
                .auditRound(auditRound)
                .versionNo(nextVersionNo)
                .type(versionType.getCode())
                .sourceVersionId(sourceVersionId)
                .fileId(fileId)
                .visibility(visibility.getCode())
                .immutableHash(DigestUtil.sha256Hex(content))
                .build();
        contractVersionMapper.insert(versionRow);
        if (versionType == LegalContractVersionTypeEnum.WORKING && versionRow.getId() != null) {
            anchorSnapshotService.createOrRefreshSnapshot(contractId, versionRow.getId());
        }
        return fileId;
    }

    private boolean existsVersion(Long contractId, LegalContractVersionTypeEnum type, int auditRound) {
        if (type == LegalContractVersionTypeEnum.ORIGINAL) {
            return contractVersionMapper.selectByContractIdAndType(contractId, type.getCode()) != null;
        }
        return contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, type.getCode(), auditRound) != null;
    }

    private Long resolveVersionId(Long contractId, LegalContractVersionTypeEnum type, int auditRound) {
        LegalContractVersionDO version = type == LegalContractVersionTypeEnum.ORIGINAL
                ? contractVersionMapper.selectByContractIdAndType(contractId, type.getCode())
                : contractVersionMapper.selectByContractIdAndTypeAndRound(contractId, type.getCode(), auditRound);
        return version == null ? null : version.getId();
    }

    private byte[] readVersionFileBytes(Long contractId, LegalContractVersionTypeEnum type, int auditRound) {
        LegalContractVersionDO version = type == LegalContractVersionTypeEnum.ORIGINAL
                ? contractVersionMapper.selectByContractIdAndType(contractId, type.getCode())
                : contractVersionMapper.selectByContractIdAndTypeAndRound(contractId, type.getCode(), auditRound);
        if (version == null || version.getFileId() == null) {
            return null;
        }
        return readFileBytes(version.getFileId());
    }

    private byte[] readMainFileBytes(Long contractId) {
        LegalContractFileDO mainFile = contractFileMapper.selectMainByContractId(contractId);
        if (mainFile == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        return readFileBytes(mainFile.getFileId());
    }

    private byte[] readFileBytes(Long fileId) {
        FileDO file = fileService.getFile(fileId);
        if (file == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        try {
            byte[] content = fileService.getFileContent(file.getConfigId(), file.getPath());
            if (content == null) {
                throw exception(CONTRACT_FILE_NOT_EXISTS);
            }
            return content;
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.error("[readFileBytes][fileId={}] 读取失败", fileId, ex);
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
    }

    private LegalContractDO requireContract(Long contractId) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    private static int resolveAuditRound(LegalContractDO contract) {
        return contract.getAuditRound() == null ? 1 : contract.getAuditRound();
    }

    private static String buildFileName(LegalContractDO contract, String suffix) {
        return StrUtil.blankToDefault(contract.getTitle(), "合同") + "-" + suffix + ".docx";
    }

    @Override
    public Long resolveWorkingFileId(Long contractId) {
        return ensureWorkingVersion(contractId);
    }

    @Override
    public byte[] readOriginalBytes(Long contractId) {
        byte[] original = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.ORIGINAL, 1);
        if (original != null) {
            return original;
        }
        return readMainFileBytes(contractId);
    }

    @Override
    public byte[] readWorkingBytes(Long contractId, int auditRound) {
        return readVersionFileBytes(contractId, LegalContractVersionTypeEnum.WORKING, auditRound);
    }

    @Override
    public String readWorkingDocumentRevision(Long contractId) {
        LegalContractDO contract = requireContract(contractId);
        byte[] working = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.WORKING,
                resolveAuditRound(contract));
        if (working == null) {
            return "0";
        }
        return contentRevision(working);
    }

    @Override
    public Long resolvePdfWorkingFileId(Long contractId) {
        LegalContractDO contract = requireContract(contractId);
        int round = resolveAuditRound(contract);
        LegalContractVersionDO working = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), round);
        return working == null ? null : working.getFileId();
    }

    @Override
    public String readPdfDocumentRevision(Long contractId, Long fileId) {
        if (fileId == null) {
            return "0";
        }
        try {
            return contentRevision(readFileBytes(fileId));
        } catch (Exception ex) {
            log.warn("[readPdfDocumentRevision][contractId={} fileId={}] 读取失败", contractId, fileId, ex);
            LegalContractDO contract = contractMapper.selectById(contractId);
            String hash = contract != null ? contract.getOriginalHash() : null;
            if (StrUtil.isNotBlank(hash)) {
                return hash.substring(0, Math.min(16, hash.length()));
            }
            return "0";
        }
    }

    @Override
    public String applyRiskAnnotationsToWorking(Long contractId) {
        LegalContractDO contract = requireContract(contractId);
        if (isPdfContract(contract)) {
            Long fileId = resolvePdfWorkingFileId(contractId);
            if (fileId == null) {
                fileId = contract.getMainFileId();
            }
            return readPdfDocumentRevision(contractId, fileId);
        }
        int round = resolveAuditRound(contract);
        ensureWorkingVersion(contractId);
        List<LegalAuditOpinionDO> opinions = opinionMapper.selectListByContractIdAndRound(contractId, round);
        if (CollUtil.isEmpty(opinions)) {
            return readWorkingDocumentRevision(contractId);
        }
        byte[] source = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.WORKING, round);
        if (source == null) {
            source = readExportSourceBytes(contractId, round);
        }
        byte[] base;
        try {
            base = LegalContractDocxRenderUtil.stripComments(source);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[applyRiskAnnotationsToWorking][contractId={} round={}] 去批注重建失败，使用当前 WORKING",
                    contractId, round, ex);
            base = source;
        }
        byte[] annotated;
        try {
            annotated = LegalContractDocxRenderUtil.renderAnnotated(base, opinions);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[applyRiskAnnotationsToWorking][contractId={} round={}] 标注渲染失败", contractId, round, ex);
            return readWorkingDocumentRevision(contractId);
        }
        String revision = updateWorkingVersionBytes(contractId, annotated);
        log.info("[applyRiskAnnotationsToWorking][contractId={} round={} opinionCount={}] WORKING 已写入风险标注",
                contractId, round, opinions.size());
        return revision;
    }

    @Override
    public String applyAdoptedOpinionsToWorking(Long contractId, List<LegalAuditOpinionDO> opinions) {
        LegalContractDO contract = requireContract(contractId);
        if (LegalContractSourceFormatEnum.PDF == LegalContractSourceFormatEnum.of(contract.getSourceFormat())) {
            log.info("[applyAdoptedOpinionsToWorking][contractId={}] PDF 合同采纳不自动改正文，请在工作台 OnlyOffice 中处理",
                    contractId);
            Long fileId = resolvePdfWorkingFileId(contractId);
            if (fileId == null) {
                fileId = contract.getMainFileId();
            }
            return readPdfDocumentRevision(contractId, fileId);
        }
        if (CollUtil.isEmpty(opinions)) {
            return readWorkingDocumentRevision(contractId);
        }
        List<LegalAuditOpinionDO> applicable = opinions.stream()
                .filter(item -> LegalOpinionStatusEnum.ADOPTED.getStatus().equals(item.getStatus()))
                .filter(LegalContractDocxRenderUtil::isAdoptApplicableToDocument)
                .toList();
        if (CollUtil.isEmpty(applicable)) {
            return readWorkingDocumentRevision(contractId);
        }
        ensureWorkingVersion(contractId);
        int round = resolveAuditRound(contract);
        byte[] source = readVersionFileBytes(contractId, LegalContractVersionTypeEnum.WORKING, round);
        if (source == null) {
            source = readExportSourceBytes(contractId, round);
        }
        byte[] patched;
        try {
            patched = LegalContractDocxRenderUtil.renderAdoptedClean(source, applicable);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[applyAdoptedOpinionsToWorking][contractId={}] 采纳 patch 失败", contractId, ex);
            return readWorkingDocumentRevision(contractId);
        }
        return updateWorkingVersionBytes(contractId, patched);
    }

    @Override
    public String rebuildWorkingFromAdoptedOpinions(Long contractId) {
        LegalContractDO contract = requireContract(contractId);
        if (isPdfContract(contract)) {
            Long fileId = resolvePdfWorkingFileId(contractId);
            if (fileId == null) {
                fileId = contract.getMainFileId();
            }
            return readPdfDocumentRevision(contractId, fileId);
        }
        int round = resolveAuditRound(contract);
        ensureWorkingVersion(contractId);
        byte[] source = resolveWorkingSourceBytes(contractId, round);
        byte[] base;
        try {
            base = LegalContractDocxRenderUtil.insertParagraphBookmarks(source);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[rebuildWorkingFromAdoptedOpinions][contractId={} round={}] Bookmark 写入失败，降级源文件",
                    contractId, round, ex);
            base = source;
        }
        List<LegalAuditOpinionDO> adopted = opinionMapper.selectListByContractIdAndRound(contractId, round)
                .stream()
                .filter(item -> LegalOpinionStatusEnum.ADOPTED.getStatus().equals(item.getStatus()))
                .filter(LegalContractDocxRenderUtil::isAdoptApplicableToDocument)
                .toList();
        byte[] patched;
        try {
            patched = CollUtil.isEmpty(adopted)
                    ? base
                    : LegalContractDocxRenderUtil.renderAdoptedClean(base, adopted);
        } catch (Exception ex) {
            LegalExceptionUtils.rethrowServiceException(ex);
            log.warn("[rebuildWorkingFromAdoptedOpinions][contractId={}] 重建 WORKING 失败", contractId, ex);
            return readWorkingDocumentRevision(contractId);
        }
        return updateWorkingVersionBytes(contractId, patched);
    }

    @Override
    public String saveWorkingFromOnlyOffice(Long contractId, Long expectedFileId, byte[] content) {
        LegalContractDO contract = requireContract(contractId);
        int round = resolveAuditRound(contract);
        LegalContractVersionDO working = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), round);
        if (working == null) {
            if (isPdfContract(contract)) {
                savePdfWorkingVersion(contract, round, content);
            } else {
                ensureWorkingVersion(contractId, round, content);
            }
            return contentRevision(content);
        }
        if (expectedFileId != null && !expectedFileId.equals(working.getFileId())) {
            log.info("[saveWorkingFromOnlyOffice][contractId={}] fileId 已变更 expected={} current={}，仍保存",
                    contractId, expectedFileId, working.getFileId());
        }
        return updateWorkingVersionBytes(contractId, content);
    }

    private void savePdfWorkingVersion(LegalContractDO contract, int round, byte[] content) {
        Long contractId = contract.getId();
        String fileName = buildFileName(contract, round > 1 ? "工作版-PDF-第" + round + "轮" : "工作版-PDF");
        Long fileId = fileService.createFileReturnId(content, fileName, EXPORT_DIRECTORY, PDF_CONTENT_TYPE);
        Long sourceVersionId = resolveVersionId(contractId, LegalContractVersionTypeEnum.ORIGINAL, 1);
        LegalContractVersionDO existing = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), round);
        if (existing != null) {
            contractVersionMapper.updateById(new LegalContractVersionDO()
                    .setId(existing.getId())
                    .setFileId(fileId)
                    .setImmutableHash(DigestUtil.sha256Hex(content)));
            ensureContractFileRegistered(contractId, fileId, fileName);
            return;
        }
        saveVersion(contract, round, fileName, LegalContractExportVisibilityEnum.INTERNAL,
                LegalContractVersionTypeEnum.WORKING, content, sourceVersionId);
        log.info("[savePdfWorkingVersion][contractId={} round={}] PDF WORKING 已创建", contractId, round);
    }

    private void ensureContractFileRegistered(Long contractId, Long fileId, String fileName) {
        if (contractFileMapper.selectByFileId(fileId) != null) {
            return;
        }
        contractFileMapper.insert(LegalContractFileDO.builder()
                .contractId(contractId)
                .fileId(fileId)
                .fileName(fileName)
                .mainFlag(false)
                .build());
    }

    private static boolean isPdfContract(LegalContractDO contract) {
        return LegalContractSourceFormatEnum.PDF == LegalContractSourceFormatEnum.of(contract.getSourceFormat());
    }

    private String updateWorkingVersionBytes(Long contractId, byte[] content) {
        LegalContractDO contract = requireContract(contractId);
        int round = resolveAuditRound(contract);
        LegalContractVersionDO existing = contractVersionMapper.selectByContractIdAndTypeAndRound(
                contractId, LegalContractVersionTypeEnum.WORKING.getCode(), round);
        if (existing == null) {
            if (isPdfContract(contract)) {
                savePdfWorkingVersion(contract, round, content);
            } else {
                ensureWorkingVersion(contractId, round, content);
            }
            return contentRevision(content);
        }
        String fileName = buildFileName(contract, round > 1 ? "工作版-第" + round + "轮" : "工作版");
        if (isPdfContract(contract)) {
            fileName = buildFileName(contract, round > 1 ? "工作版-PDF-第" + round + "轮" : "工作版-PDF");
        }
        if (existing.getFileId() != null) {
            FileDO previous = fileService.getFile(existing.getFileId());
            if (previous != null && StrUtil.isNotBlank(previous.getName())) {
                fileName = previous.getName();
            }
        }
        Long newFileId = fileService.createFileReturnId(content, fileName, EXPORT_DIRECTORY,
                isPdfContract(contract) ? PDF_CONTENT_TYPE : DOCX_CONTENT_TYPE);
        contractVersionMapper.updateById(new LegalContractVersionDO()
                .setId(existing.getId())
                .setFileId(newFileId)
                .setImmutableHash(DigestUtil.sha256Hex(content)));
        ensureContractFileRegistered(contractId, newFileId, fileName);
        anchorSnapshotService.createOrRefreshSnapshot(contractId, existing.getId());
        log.info("[updateWorkingVersionBytes][contractId={} round={}] WORKING 已更新 fileId={}",
                contractId, round, newFileId);
        return contentRevision(content);
    }

    private static String contentRevision(byte[] content) {
        return DigestUtil.sha256Hex(content).substring(0, 16);
    }

}
