package com.laby.module.legal.controller.admin.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.ratelimiter.core.annotation.RateLimiter;
import com.laby.module.legal.framework.ratelimiter.TenantRateLimiterKeyResolver;
import com.laby.module.legal.controller.admin.contract.vo.*;
import com.laby.module.legal.controller.admin.opinion.vo.LegalOpinionDocumentApplyResultVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.enums.contract.LegalContractDeliverableEnum;
import com.laby.module.legal.enums.contract.LegalContractExportModeEnum;
import com.laby.module.legal.enums.contract.LegalContractExportVisibilityEnum;
import com.laby.module.legal.service.contract.LegalAiAuditProgressService;
import com.laby.module.legal.service.contract.LegalContractDeliverableService;
import com.laby.module.legal.service.contract.LegalContractExportParamHelper;
import com.laby.module.legal.service.contract.LegalContractExportService;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.contract.LegalContractVersionService;
import com.laby.module.legal.service.contract.LegalContractWorkbenchService;
import com.laby.module.legal.service.contract.LegalContractVersionDiffService;
import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.laby.module.infra.framework.file.core.utils.FileTypeUtils.writeAttachment;

/**
 * 管理后台 - 法务合同审核 Controller
 * <p>
 * 仅负责参数校验、权限控制与 HTTP 适配；业务逻辑下沉至 Service 层。
 */
@Tag(name = "管理后台 - 法务合同审核")
@RestController
@RequestMapping("/legal/contract")
@Validated
public class LegalContractController {

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalContractExportService exportService;
    @Resource
    private LegalContractDeliverableService deliverableService;
    @Resource
    private LegalContractVersionService contractVersionService;
    @Resource
    private LegalContractWorkbenchService workbenchService;
    @Resource
    private LegalContractVersionDiffService versionDiffService;
    @Resource
    private LegalAiAuditProgressService auditProgressService;

    @PostMapping("/create")
    @Operation(summary = "创建合同审核并发起流程")
    @RateLimiter(time = 60, count = 20, keyResolver = TenantRateLimiterKeyResolver.class)
    @PreAuthorize("@ss.hasPermission('legal:contract:create')")
    public CommonResult<Long> createContract(@Valid @RequestBody LegalContractCreateReqVO createReqVO) {
        return success(contractService.createContract(getLoginUserId(), createReqVO));
    }

    @PostMapping("/retry-pipeline")
    @Operation(summary = "处理失败后重试（解析+AI+流程）")
    @RateLimiter(time = 60, count = 20, keyResolver = TenantRateLimiterKeyResolver.class)
    @PreAuthorize("@ss.hasPermission('legal:contract:create')")
    public CommonResult<Boolean> retryPipeline(@RequestParam("id") Long id) {
        contractService.retryPipeline(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/start-first-audit")
    @Operation(summary = "发起首轮 AI 审核（AI 对话创建且已解析后）")
    @RateLimiter(time = 60, count = 20, keyResolver = TenantRateLimiterKeyResolver.class)
    @PreAuthorize("@ss.hasPermission('legal:contract:create')")
    public CommonResult<Boolean> startFirstAudit(@RequestParam("id") Long id) {
        contractService.startFirstAudit(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传合同文件")
    @PreAuthorize("@ss.hasPermission('legal:contract:create')")
    public CommonResult<LegalContractUploadRespVO> uploadContractFile(
            @RequestParam("file") MultipartFile file) throws Exception {
        return success(contractService.uploadContractFile(file));
    }

    @GetMapping("/get")
    @Operation(summary = "获得合同审核详情")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalContractRespVO> getContract(@RequestParam("id") Long id) {
        return success(contractService.getContractResp(id));
    }

    /**
     * 下载合同附件；走应用鉴权，勿直接打开存储 domain 外链。
     */
    @GetMapping("/download-file")
    @Operation(summary = "下载合同附件（走应用鉴权，勿直接打开存储 domain 外链）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public void downloadContractFile(@RequestParam("fileId") Long fileId,
                                     HttpServletResponse response) throws Exception {
        LegalContractFileDownloadBO download = contractService.downloadContractFile(fileId);
        writeAttachment(response, download.getFileName(), download.getContent());
    }

    @GetMapping("/page")
    @Operation(summary = "获得合同审核分页")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<PageResult<LegalContractRespVO>> getContractPage(@Valid LegalContractPageReqVO pageVO) {
        return success(contractService.getContractRespPage(getLoginUserId(), pageVO));
    }

    @PutMapping("/complete-opinion")
    @Operation(summary = "完成意见处置（业务侧，流程待办需另行审批）")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> completeOpinionReview(@Valid @RequestBody LegalContractOpinionCompleteReqVO reqVO) {
        contractService.completeOpinionReview(getLoginUserId(), reqVO);
        return success(true);
    }

    /**
     * 获得合同段落列表，供前端定位与高亮。
     */
    @GetMapping("/list-paragraph")
    @Operation(summary = "获得合同段落列表（用于定位高亮）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<List<LegalContractParagraphRespVO>> listParagraphs(
            @RequestParam("contractId") Long contractId) {
        return success(contractService.listParagraphRespList(contractId));
    }

    @GetMapping("/get-workbench")
    @Operation(summary = "获得合同审阅工作台聚合数据")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalContractWorkbenchRespVO> getWorkbench(@RequestParam("contractId") Long contractId) {
        return success(workbenchService.getWorkbench(contractId));
    }

    @GetMapping("/download-deliverable")
    @Operation(summary = "下载合同四件套交付物（按需生成，DELIV-001）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public void downloadDeliverable(@RequestParam("contractId") Long contractId,
                                    @RequestParam("deliverable") String deliverable,
                                    @RequestParam(value = "auditRound", required = false) Integer auditRound,
                                    HttpServletResponse response) throws Exception {
        contractService.validateContractExists(contractId);
        LegalContractDeliverableEnum type = LegalContractExportParamHelper.normalizeDeliverable(deliverable);
        LegalContractFileDownloadBO download = deliverableService.generate(contractId, type, auditRound);
        writeAttachment(response, download.getFileName(), download.getContent());
    }

    @PostMapping("/document/sync-working")
    @Operation(summary = "同步 WORKING 状态（OnlyOffice forceSave 后调用，DELIV-001 §15.2）")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<LegalContractSyncWorkingRespVO> syncWorkingDocument(
            @RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        String revision = contractVersionService.readWorkingDocumentRevision(contractId);
        Long workingFileId = contractVersionService.resolveWorkingFileId(contractId);
        return success(new LegalContractSyncWorkingRespVO(revision, workingFileId));
    }

    @PostMapping("/export-report")
    @Operation(summary = "手动导出审核报告 Word")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Long> exportReport(@RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        return success(exportService.exportReportDocx(contractId));
    }

    @PostMapping("/export-annotated-docx")
    @Operation(summary = "导出合同标注版 Word")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Long> exportAnnotatedDocx(@RequestParam("contractId") Long contractId,
                                                  @RequestParam(value = "visibility", required = false,
                                                          defaultValue = "INTERNAL") String visibility) {
        contractService.validateContractExists(contractId);
        LegalContractExportVisibilityEnum exportVisibility =
                LegalContractExportParamHelper.normalizeVisibility(visibility);
        return success(exportService.exportAnnotatedContractDocx(contractId, exportVisibility));
    }

    @PostMapping("/export-adopted-docx")
    @Operation(summary = "导出合同采纳版（MVP）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Long> exportAdoptedDocx(@RequestParam("contractId") Long contractId,
                                                @RequestParam(value = "mode", required = false,
                                                        defaultValue = "CLEAN") String mode,
                                                @RequestParam(value = "visibility", required = false,
                                                        defaultValue = "INTERNAL") String visibility) {
        contractService.validateContractExists(contractId);
        LegalContractExportVisibilityEnum exportVisibility =
                LegalContractExportParamHelper.normalizeVisibility(visibility);
        LegalContractExportModeEnum exportMode = LegalContractExportParamHelper
                .normalizeAdoptedMode(mode, exportVisibility);
        return success(exportService.exportAdoptedContractDocx(contractId, exportMode, exportVisibility));
    }

    @PostMapping("/precheck-adopted-export")
    @Operation(summary = "采纳导出前预检")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalContractExportPrecheckRespVO> precheckAdoptedExport(
            @RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        return success(exportService.precheckAdoptedExport(contractId));
    }

    @PostMapping("/apply-risk-annotations")
    @Operation(summary = "将全部审核意见标注写入 WORKING 合同（OnlyOffice 预览）")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<LegalOpinionDocumentApplyResultVO> applyRiskAnnotations(
            @RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        String beforeRevision = contractVersionService.readWorkingDocumentRevision(contractId);
        String revision = contractVersionService.applyRiskAnnotationsToWorking(contractId);
        boolean updated = StrUtil.isNotBlank(revision) && !StrUtil.equals(revision, beforeRevision);
        return success(new LegalOpinionDocumentApplyResultVO(updated, revision));
    }

    @PostMapping("/repair-working-version")
    @Operation(summary = "修复 WORKING 版段落 Bookmark 锚点")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> repairWorkingVersion(@RequestParam("contractId") Long contractId) {
        LegalContractDO contract = contractService.validateContractExists(contractId);
        int auditRound = contract.getAuditRound() == null ? 1 : contract.getAuditRound();
        contractVersionService.repairWorkingVersion(contractId, auditRound);
        return success(true);
    }

    @PostMapping("/export-archive-zip")
    @Operation(summary = "打包归档发布包 zip（四件套+报告+manifest，immutable）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Long> exportArchiveZip(@RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        return success(exportService.exportArchivePackage(contractId));
    }

    @PostMapping("/export-bundle")
    @Operation(summary = "导出法务交付包 zip（四件套+报告+manifest，按需生成）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Long> exportBundle(@RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        return success(exportService.exportDeliveryBundle(contractId));
    }

    @GetMapping("/version-diff")
    @Operation(summary = "合同版本条款级 Diff")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalContractVersionDiffRespVO> getVersionDiff(
            @RequestParam("contractId") Long contractId,
            @RequestParam("fromVersionId") Long fromVersionId,
            @RequestParam("toVersionId") Long toVersionId) {
        return success(versionDiffService.getVersionDiff(contractId, fromVersionId, toVersionId));
    }

    @GetMapping("/audit-progress")
    @Operation(summary = "获得 AI 审核进度（含推理过程，供轮询）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalAiAuditProgressRespVO> getAuditProgress(@RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        return success(auditProgressService.get(contractId));
    }

    @GetMapping("/version-list")
    @Operation(summary = "获得合同版本列表")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<List<LegalContractVersionRespVO>> getVersionList(@RequestParam("contractId") Long contractId) {
        return success(contractService.getContractVersionRespList(contractId));
    }

    @GetMapping("/audit-report")
    @Operation(summary = "获得审核报告（Markdown）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalAuditReportRespVO> getAuditReport(
            @RequestParam("contractId") Long contractId,
            @RequestParam(value = "auditRound", required = false) Integer auditRound) {
        return success(contractService.getAuditReportResp(contractId, auditRound));
    }

}
