package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import com.laby.module.bpm.enums.task.BpmTaskStatusEnum;
import com.laby.module.infra.dal.dataobject.file.FileDO;
import com.laby.module.infra.service.file.FileService;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractUploadRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractFileMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.LegalContractConstants;
import com.laby.module.legal.enums.contract.LegalContractCreateSourceEnum;
import com.laby.module.legal.enums.contract.LegalContractFileRoleEnum;
import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalContractTaskKeyEnum;
import com.laby.module.legal.enums.contract.LegalParseStatusEnum;
import com.laby.module.legal.service.contract.util.LegalContractFormatUtils;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_AUDIT_NOT_READY;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_FORMAT_NOT_SUPPORTED;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_REQUIRED;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_SIZE_EXCEEDED;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_MAIN_FILE_EMPTY;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_RETRY_NOT_FAILED;

/**
 * 合同创建与上传：落库、附件登记、事务提交后启动解析流水线。
 */
@Service
public class LegalContractCreateService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractFileMapper contractFileMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private FileService fileService;
    @Resource
    private LegalContractAuditRoleService auditRoleService;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;
    @Resource
    private LegalContractVersionService contractVersionService;
    @Resource
    private LegalContractProcessStarter processStarter;
    @Resource
    private LegalContractQueryService queryService;

    public LegalContractUploadRespVO uploadContractFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        validateContractUploadFileName(fileName);
        validateContractUploadFileSize(file.getSize());
        byte[] content = IoUtil.readBytes(file.getInputStream());
        Long fileId = fileService.createFileReturnId(content, fileName,
                LegalContractConstants.CONTRACT_FILE_DIRECTORY, file.getContentType());
        FileDO fileDO = fileService.getFile(fileId);
        return LegalContractUploadRespVO.builder()
                .fileId(fileId)
                .fileName(fileDO.getName())
                .url(fileDO.getUrl())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createContract(Long userId, LegalContractCreateReqVO createReqVO) {
        return doCreateContract(userId, createReqVO, LegalContractCreateSourceEnum.MANUAL.getSource(), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createContractFromOrchestration(Long userId, LegalContractCreateReqVO createReqVO,
                                                String createSource, Long createConversationId) {
        return doCreateContract(userId, createReqVO, createSource, createConversationId);
    }

    public void retryPipeline(Long userId, Long contractId) {
        LegalContractDO contract = queryService.validateContractExists(contractId);
        if (!LegalContractStatusEnum.FAILED.getStatus().equals(contract.getStatus())) {
            throw exception(CONTRACT_RETRY_NOT_FAILED);
        }
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setFeedbackSummary(null)
                .setProcessInstanceId(null));
        LegalContractCreateReqVO req = new LegalContractCreateReqVO();
        req.setModelId(contract.getModelId());
        processStarter.startProcessAsync(userId, req, contractId, TenantContextHolder.getTenantId());
    }

    public void startFirstAudit(Long userId, Long contractId) {
        LegalContractDO contract = queryService.validateContractExists(contractId);
        if (!LegalContractPermissionHelper.canStartFirstAudit(contract)) {
            throw exception(CONTRACT_AUDIT_NOT_READY);
        }
        if (opinionMapper.selectCountByContractId(contractId) > 0) {
            throw exception(CONTRACT_AUDIT_NOT_READY);
        }
        LegalContractCreateReqVO req = new LegalContractCreateReqVO();
        req.setModelId(contract.getModelId());
        processStarter.startFirstAuditAsync(userId, req, contractId, TenantContextHolder.getTenantId());
    }

    private Long doCreateContract(Long userId, LegalContractCreateReqVO createReqVO,
                                  String createSource, Long createConversationId) {
        if (CollUtil.isEmpty(createReqVO.getFiles())) {
            throw exception(CONTRACT_FILE_REQUIRED);
        }
        Long mainFileId = null;
        for (LegalContractCreateReqVO.LegalContractFileItemVO file : createReqVO.getFiles()) {
            if (Boolean.TRUE.equals(file.getMainFlag())) {
                mainFileId = file.getFileId();
            }
        }
        if (mainFileId == null) {
            mainFileId = createReqVO.getFiles().get(0).getFileId();
        }
        final Long resolvedMainFileId = mainFileId;
        String mainFileName = createReqVO.getFiles().stream()
                .filter(f -> resolvedMainFileId.equals(f.getFileId()))
                .map(LegalContractCreateReqVO.LegalContractFileItemVO::getFileName)
                .findFirst()
                .orElse(createReqVO.getFiles().get(0).getFileName());
        LegalContractSourceFormatEnum sourceFormat =
                LegalContractFormatUtils.detectSourceFormat(mainFileName);

        LegalContractDO contract = LegalContractDO.builder()
                .title(createReqVO.getTitle())
                .contractTypeId(createReqVO.getContractTypeId())
                .skillPackSnapshot(StrUtil.isNotBlank(createReqVO.getSkillPackSnapshotJson())
                        ? createReqVO.getSkillPackSnapshotJson()
                        : skillPackSnapshotService.buildSnapshotJson(createReqVO.getContractTypeId()))
                .partyRole(createReqVO.getPartyRole())
                .auditLevel(createReqVO.getAuditLevel())
                .modelId(createReqVO.getModelId())
                .auditRoleId(resolveAuditRoleId(createReqVO.getAuditRoleId()))
                .reauditRoleId(createReqVO.getReauditRoleId())
                .editable(createReqVO.getEditable())
                .status(LegalContractStatusEnum.DRAFT.getStatus())
                .bpmStatus(BpmTaskStatusEnum.RUNNING.getStatus())
                .auditRound(1)
                .needSecondRound(false)
                .riskHighCount(0)
                .mainFileId(mainFileId)
                .sourceFormat(sourceFormat.getFormat())
                .parseStatus(LegalParseStatusEnum.WAITING.getStatus())
                .userId(userId)
                .createSource(StrUtil.blankToDefault(createSource, LegalContractCreateSourceEnum.MANUAL.getSource()))
                .createConversationId(createConversationId)
                .build();
        contractMapper.insert(contract);

        for (LegalContractCreateReqVO.LegalContractFileItemVO file : createReqVO.getFiles()) {
            LegalContractSourceFormatEnum fileFormat =
                    LegalContractFormatUtils.detectSourceFormat(file.getFileName());
            contractFileMapper.insert(LegalContractFileDO.builder()
                    .contractId(contract.getId())
                    .fileId(file.getFileId())
                    .fileName(file.getFileName())
                    .mainFlag(Boolean.TRUE.equals(file.getMainFlag())
                            || file.getFileId().equals(mainFileId))
                    .role(LegalContractFileRoleEnum.ORIGINAL.getRole())
                    .format(fileFormat.getFormat())
                    .build());
        }

        contractVersionService.ensureOriginalVersion(contract.getId());

        Long contractId = contract.getId();
        Long tenantId = TenantContextHolder.getTenantId();
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setStatus(LegalContractStatusEnum.PARSING.getStatus())
                .setCurrentTaskKey(LegalContractTaskKeyEnum.PARSE_CONTRACT.getKey()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processStarter.startProcessAsync(userId, createReqVO, contractId, tenantId);
            }
        });
        return contractId;
    }

    private void validateContractUploadFileName(String fileName) {
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase();
        if (LegalContractFormatUtils.isPdfFileName(fileName)) {
            throw exception(CONTRACT_FILE_FORMAT_NOT_SUPPORTED);
        }
        LegalContractSourceFormatEnum format = LegalContractFormatUtils.detectSourceFormat(fileName);
        boolean ok = format == LegalContractSourceFormatEnum.DOCX && lower.endsWith(".docx")
                || format == LegalContractSourceFormatEnum.DOC && lower.endsWith(".doc");
        if (!ok) {
            throw exception(CONTRACT_FILE_FORMAT_NOT_SUPPORTED);
        }
    }

    private void validateContractUploadFileSize(long sizeBytes) {
        long maxBytes = (long) LegalContractConstants.MAX_CONTRACT_FILE_SIZE_MB * 1024 * 1024;
        if (sizeBytes <= 0) {
            throw exception(CONTRACT_MAIN_FILE_EMPTY);
        }
        if (sizeBytes > maxBytes) {
            throw exception(CONTRACT_FILE_SIZE_EXCEEDED, LegalContractConstants.MAX_CONTRACT_FILE_SIZE_MB);
        }
    }

    private Long resolveAuditRoleId(Long auditRoleId) {
        if (auditRoleId != null) {
            return auditRoleId;
        }
        return auditRoleService.resolveDefaultRound1RoleId();
    }

}
