package com.laby.module.legal.service.contract;

import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.service.contract.bo.LegalContractAnchorPrecheckResult;

import java.util.List;

/**
 * 合同版本链：ORIGINAL → WORKING → AI_ANNOTATED → ADOPTED_*。
 */
public interface LegalContractVersionService {

    /**
     * 上传建合同时记录 V0 原始版（幂等）。
     */
    void ensureOriginalVersion(Long contractId);

    /**
     * 解析成功后生成 V0′ 工作版（段落 Bookmark 锚点，幂等）。
     *
     * @return 工作版 infra 文件编号
     */
    Long ensureWorkingVersion(Long contractId);

    /**
     * AI 审核完成后自动生成标注版 V1（幂等）。
     */
    void ensureAnnotatedVersion(Long contractId, int auditRound);

    /**
     * 意见处置完成后自动生成采纳版 V2（TRACKED + CLEAN，幂等）。
     */
    void ensureAdoptedVersions(Long contractId, int auditRound);

    /**
     * 读取导出/渲染用的源 docx 字节（优先 WORKING，二轮起优先上轮 CLEAN）。
     */
    byte[] readExportSourceBytes(Long contractId, Integer auditRound);

    /**
     * 获取当前轮次工作版版本记录 ID（供意见 fromVersionId 绑定）。
     */
    Long resolveWorkingVersionId(Long contractId, int auditRound);

    /**
     * 二轮审核前：生成一轮采纳版、按 CLEAN 重解析段落、生成二轮 WORKING。
     */
    void prepareSecondAuditRound(Long contractId);

    /**
     * 意见落库时绑定的合同版本（二轮绑定上轮 ADOPTED_CLEAN）。
     */
    Long resolveFromVersionIdForOpinions(Long contractId, int auditRound);

    /**
     * 校验 WORKING 版 Bookmark 与段落表是否一致。
     */
    LegalContractAnchorPrecheckResult precheckBookmarkAnchors(Long contractId, int auditRound);

    /**
     * 按当前导出源重新写入 Bookmark，修复 WORKING 锚点漂移。
     */
    void repairWorkingVersion(Long contractId, int auditRound);

    /**
     * 确保存在 WORKING 并返回其 infra 文件编号。
     */
    Long resolveWorkingFileId(Long contractId);

    /**
     * 将当前轮次全部审核意见以 Word 批注写入 WORKING，返回新 revision。
     */
    String applyRiskAnnotationsToWorking(Long contractId);

    /**
     * 将采纳意见增量应用到 WORKING（干净替换），返回新 revision。
     */
    String applyAdoptedOpinionsToWorking(Long contractId, List<LegalAuditOpinionDO> opinions);

    /**
     * 按当前仍「已采纳」的意见全量重建 WORKING（撤销采纳时使用）。
     */
    String rebuildWorkingFromAdoptedOpinions(Long contractId);

    /**
     * OnlyOffice 回调：保存编辑后的 WORKING 内容。
     */
    String saveWorkingFromOnlyOffice(Long contractId, Long expectedFileId, byte[] content);

    /**
     * 读取 ORIGINAL 源文件字节（审计原件，不改写）。
     */
    byte[] readOriginalBytes(Long contractId);

    /**
     * 读取指定轮次 WORKING 字节；不存在时返回 {@code null}。
     */
    byte[] readWorkingBytes(Long contractId, int auditRound);

    /**
     * 读取当前 WORKING 文档 revision（内容 hash 前缀）。
     */
    String readWorkingDocumentRevision(Long contractId);

    /**
     * PDF 合同 WORKING 文件编号（无则 null，预览回退原件）。
     */
    Long resolvePdfWorkingFileId(Long contractId);

    /**
     * PDF 文档 revision（按文件内容 hash）。
     */
    String readPdfDocumentRevision(Long contractId, Long fileId);

}
