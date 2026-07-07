package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.infra.dal.dataobject.file.FileDO;
import com.laby.module.infra.service.file.FileService;
import com.laby.module.legal.controller.admin.contract.vo.LegalAuditReportRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphSkipReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractVersionRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractVersionDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractFileMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractVersionMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.dal.mysql.report.LegalAuditReportMapper;
import com.laby.module.legal.service.bpm.LegalContractBpmSyncService;
import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_OPINION_NOT_EDITABLE;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_PARAGRAPH_NOT_EXISTS;

/**
 * 合同查询、详情组装与只读校验。
 */
@Slf4j
@Service
public class LegalContractQueryService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalContractFileMapper contractFileMapper;
    @Resource
    private LegalContractVersionMapper contractVersionMapper;
    @Resource
    private LegalAuditReportMapper auditReportMapper;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private FileService fileService;
    @Resource
    private LegalContractBpmSyncService bpmSyncService;
    @Resource
    private LegalContractRespAssembler contractRespAssembler;
    @Resource
    private LegalAiAuditService aiAuditService;

    public LegalContractDO getContract(Long id) {
        return contractMapper.selectById(id);
    }

    public LegalContractDO refreshAndGetContract(Long id) {
        LegalContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            return null;
        }
        if (StrUtil.isNotBlank(contract.getProcessInstanceId())) {
            bpmSyncService.refreshFromProcessInstance(contract.getId(), contract.getProcessInstanceId());
            contract = contractMapper.selectById(id);
        }
        return contract;
    }

    public LegalContractRespVO getContractResp(Long id) {
        validateContractExists(id);
        LegalContractDO contract = refreshAndGetContract(id);
        return contractRespAssembler.buildDetail(contract);
    }

    public PageResult<LegalContractRespVO> getContractRespPage(Long userId, LegalContractPageReqVO pageReqVO) {
        PageResult<LegalContractDO> page = getContractPage(userId, pageReqVO);
        PageResult<LegalContractRespVO> result = BeanUtils.toBean(page, LegalContractRespVO.class);
        if (result.getList() == null || page.getList() == null) {
            return result;
        }
        for (int i = 0; i < result.getList().size(); i++) {
            contractRespAssembler.enrichListRow(result.getList().get(i), page.getList().get(i));
        }
        return result;
    }

    public PageResult<LegalContractDO> getContractPage(Long userId, LegalContractPageReqVO pageReqVO) {
        PageResult<LegalContractDO> page = contractMapper.selectPage(userId, pageReqVO);
        if (page.getList() != null) {
            for (LegalContractDO row : page.getList()) {
                if (StrUtil.isNotBlank(row.getProcessInstanceId())) {
                    bpmSyncService.refreshFromProcessInstance(row.getId(), row.getProcessInstanceId());
                }
            }
            page = contractMapper.selectPage(userId, pageReqVO);
        }
        return page;
    }

    public LegalAuditReportRespVO getAuditReportResp(Long contractId, Integer auditRound) {
        LegalContractDO contract = validateContractExists(contractId);
        int round = auditRound != null ? auditRound
                : (contract.getAuditRound() != null ? contract.getAuditRound() : 1);
        LegalAuditReportDO report = auditReportMapper.selectByContractIdAndRound(contractId, round);
        if (report == null) {
            report = auditReportMapper.selectLatestByContractId(contractId);
        }
        if (report == null || StrUtil.isBlank(report.getContent())) {
            report = aiAuditService.rebuildAuditReportIfMissing(contractId, round);
        }
        if (report == null) {
            report = auditReportMapper.selectLatestByContractId(contractId);
        }
        if (report == null) {
            return new LegalAuditReportRespVO();
        }
        return BeanUtils.toBean(report, LegalAuditReportRespVO.class);
    }

    public List<LegalContractVersionRespVO> getContractVersionRespList(Long contractId) {
        validateContractExists(contractId);
        try {
            List<LegalContractVersionDO> list = contractVersionMapper.selectListByContractId(contractId);
            return BeanUtils.toBean(list, LegalContractVersionRespVO.class);
        } catch (RuntimeException ex) {
            if (isMissingTable(ex, "legal_contract_version")) {
                return new ArrayList<>();
            }
            throw ex;
        }
    }

    public LegalContractDO validateContractExists(Long id) {
        LegalContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    public void validateOpinionManageable(Long contractId) {
        LegalContractDO contract = validateContractExists(contractId);
        long pending = opinionMapper.selectPendingCount(contractId);
        if (!LegalContractPermissionHelper.canManageOpinions(contract, pending)) {
            throw exception(CONTRACT_OPINION_NOT_EDITABLE);
        }
    }

    public void updateParagraphSkipAudit(LegalContractParagraphSkipReqVO reqVO) {
        validateOpinionManageable(reqVO.getContractId());
        LegalContractParagraphDO paragraph = paragraphMapper.selectListByContractId(reqVO.getContractId()).stream()
                .filter(p -> reqVO.getParagraphId().equals(p.getParagraphId()))
                .findFirst()
                .orElse(null);
        if (paragraph == null) {
            throw exception(CONTRACT_PARAGRAPH_NOT_EXISTS);
        }
        paragraphMapper.updateById(new LegalContractParagraphDO()
                .setId(paragraph.getId())
                .setSkipAudit(Boolean.TRUE.equals(reqVO.getSkipAudit())));
    }

    public List<LegalContractParagraphRespVO> listParagraphRespList(Long contractId) {
        validateContractExists(contractId);
        List<LegalContractParagraphDO> list = paragraphMapper.selectListByContractId(contractId);
        return BeanUtils.toBean(list, LegalContractParagraphRespVO.class);
    }

    public LegalContractFileDownloadBO downloadContractFile(Long fileId) throws Exception {
        LegalContractFileDO contractFile = contractFileMapper.selectByFileId(fileId);
        if (contractFile == null) {
            contractFile = resolveOrRegisterContractFileByVersion(fileId);
        }
        if (contractFile == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        validateContractExists(contractFile.getContractId());
        FileDO fileDO = fileService.getFile(fileId);
        if (fileDO == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        byte[] content = fileService.getFileContent(fileDO.getConfigId(), fileDO.getPath());
        if (content == null) {
            throw exception(CONTRACT_FILE_NOT_EXISTS);
        }
        String fileName = StrUtil.blankToDefault(contractFile.getFileName(), fileDO.getName());
        return new LegalContractFileDownloadBO(fileName, content);
    }

    private LegalContractFileDO resolveOrRegisterContractFileByVersion(Long fileId) {
        LegalContractVersionDO version = contractVersionMapper.selectByFileId(fileId);
        if (version == null || version.getContractId() == null) {
            return null;
        }
        FileDO fileDO = fileService.getFile(fileId);
        if (fileDO == null) {
            return null;
        }
        LegalContractFileDO row = LegalContractFileDO.builder()
                .contractId(version.getContractId())
                .fileId(fileId)
                .fileName(fileDO.getName())
                .mainFlag(false)
                .build();
        contractFileMapper.insert(row);
        log.info("[resolveOrRegisterContractFileByVersion][contractId={} fileId={}] 已补登记合同附件",
                version.getContractId(), fileId);
        return row;
    }

    private static boolean isMissingTable(Throwable ex, String tableName) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg == null) {
                continue;
            }
            if (msg.contains(tableName) && (msg.contains("doesn't exist") || msg.contains("does not exist"))) {
                return true;
            }
            if (msg.contains("Table") && msg.contains(tableName) && msg.contains("exist")) {
                return true;
            }
        }
        return false;
    }

}
