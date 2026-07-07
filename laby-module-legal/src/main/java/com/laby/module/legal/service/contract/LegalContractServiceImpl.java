package com.laby.module.legal.service.contract;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractOpinionCompleteReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphSkipReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractUploadRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalAuditReportRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractVersionRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 法务合同审核 Service 实现类（薄 Facade，委托 Create / BPM / Query）。
 */
@Service
@Validated
public class LegalContractServiceImpl implements LegalContractService {

    @Resource
    private LegalContractCreateService createService;
    @Resource
    private LegalContractBpmService bpmService;
    @Resource
    private LegalContractQueryService queryService;

    @Override
    public LegalContractUploadRespVO uploadContractFile(MultipartFile file) throws Exception {
        return createService.uploadContractFile(file);
    }

    @Override
    public List<LegalContractParagraphRespVO> listParagraphRespList(Long contractId) {
        return queryService.listParagraphRespList(contractId);
    }

    @Override
    public LegalContractFileDownloadBO downloadContractFile(Long fileId) throws Exception {
        return queryService.downloadContractFile(fileId);
    }

    @Override
    public Long createContract(Long userId, LegalContractCreateReqVO createReqVO) {
        return createService.createContract(userId, createReqVO);
    }

    @Override
    public Long createContractFromOrchestration(Long userId, LegalContractCreateReqVO createReqVO,
                                                String createSource, Long createConversationId) {
        return createService.createContractFromOrchestration(userId, createReqVO, createSource, createConversationId);
    }

    @Override
    public void retryPipeline(Long userId, Long contractId) {
        createService.retryPipeline(userId, contractId);
    }

    @Override
    public void startFirstAudit(Long userId, Long contractId) {
        createService.startFirstAudit(userId, contractId);
    }

    @Override
    public void validateOpinionManageable(Long contractId) {
        queryService.validateOpinionManageable(contractId);
    }

    @Override
    public void updateParagraphSkipAudit(LegalContractParagraphSkipReqVO reqVO) {
        queryService.updateParagraphSkipAudit(reqVO);
    }

    @Override
    public void completeOpinionReview(Long userId, LegalContractOpinionCompleteReqVO reqVO) {
        bpmService.completeOpinionReview(userId, reqVO);
    }

    @Override
    public void updateContractStatus(Long id, Integer status) {
        bpmService.updateContractStatus(id, status);
    }

    @Override
    public void updateBpmStatus(Long id, Integer bpmStatus) {
        bpmService.updateBpmStatus(id, bpmStatus);
    }

    @Override
    public LegalContractDO getContract(Long id) {
        return queryService.getContract(id);
    }

    @Override
    public LegalContractDO refreshAndGetContract(Long id) {
        return queryService.refreshAndGetContract(id);
    }

    @Override
    public LegalContractRespVO getContractResp(Long id) {
        return queryService.getContractResp(id);
    }

    @Override
    public PageResult<LegalContractRespVO> getContractRespPage(Long userId, LegalContractPageReqVO pageReqVO) {
        return queryService.getContractRespPage(userId, pageReqVO);
    }

    @Override
    public LegalAuditReportRespVO getAuditReportResp(Long contractId, Integer auditRound) {
        return queryService.getAuditReportResp(contractId, auditRound);
    }

    @Override
    public List<LegalContractVersionRespVO> getContractVersionRespList(Long contractId) {
        return queryService.getContractVersionRespList(contractId);
    }

    @Override
    public PageResult<LegalContractDO> getContractPage(Long userId, LegalContractPageReqVO pageReqVO) {
        return queryService.getContractPage(userId, pageReqVO);
    }

    @Override
    public LegalContractDO validateContractExists(Long id) {
        return queryService.validateContractExists(id);
    }

}
