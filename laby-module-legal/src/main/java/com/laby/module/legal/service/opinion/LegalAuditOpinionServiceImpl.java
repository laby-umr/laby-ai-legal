package com.laby.module.legal.service.opinion;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.legal.controller.admin.opinion.vo.LegalAuditOpinionSaveReqVO;
import com.laby.module.legal.controller.admin.opinion.vo.LegalOpinionDocumentApplyResultVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.contract.LegalContractVersionService;
import com.laby.module.legal.service.contract.util.LegalContractDocxRenderUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.OPINION_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.OPINION_NOT_MANUAL;

/**
 * 法务审核意见 Service 实现类
 */
@Service
@Validated
public class LegalAuditOpinionServiceImpl implements LegalAuditOpinionService {

    /** 手工意见条款类型标识 */
    public static final String MANUAL_CLAUSE_TYPE = "MANUAL";

    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalContractVersionService contractVersionService;

    @Override
    public List<LegalAuditOpinionDO> getOpinionListByContractId(Long contractId) {
        contractService.validateContractExists(contractId);
        return opinionMapper.selectListByContractId(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LegalOpinionDocumentApplyResultVO adopt(Long id) {
        LegalAuditOpinionDO opinion = validateOpinionExists(id);
        contractService.validateOpinionManageable(opinion.getContractId());
        opinionMapper.updateById(new LegalAuditOpinionDO()
                .setId(id)
                .setStatus(LegalOpinionStatusEnum.ADOPTED.getStatus()));
        refreshContractRiskHighCount(opinion.getContractId());
        LegalAuditOpinionDO updated = opinionMapper.selectById(id);
        String revision = contractVersionService.rebuildWorkingFromAdoptedOpinions(opinion.getContractId());
        return buildApplyResult(revision, updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ignore(Long id) {
        updateOpinionStatus(id, LegalOpinionStatusEnum.IGNORED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LegalOpinionDocumentApplyResultVO revoke(Long id) {
        LegalAuditOpinionDO opinion = validateOpinionExists(id);
        contractService.validateOpinionManageable(opinion.getContractId());
        boolean wasAdopted = LegalOpinionStatusEnum.ADOPTED.getStatus().equals(opinion.getStatus());
        boolean documentApplicable = wasAdopted && LegalContractDocxRenderUtil.isAdoptApplicableToDocument(opinion);
        opinionMapper.updateById(new LegalAuditOpinionDO()
                .setId(id)
                .setStatus(LegalOpinionStatusEnum.PENDING.getStatus()));
        refreshContractRiskHighCount(opinion.getContractId());
        if (documentApplicable) {
            String revision = contractVersionService.rebuildWorkingFromAdoptedOpinions(opinion.getContractId());
            return new LegalOpinionDocumentApplyResultVO(true, revision);
        }
        return new LegalOpinionDocumentApplyResultVO(false,
                contractVersionService.readWorkingDocumentRevision(opinion.getContractId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LegalOpinionDocumentApplyResultVO batchAdopt(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new LegalOpinionDocumentApplyResultVO(false, null);
        }
        Set<Long> contractIds = new HashSet<>();
        List<LegalAuditOpinionDO> adopted = new java.util.ArrayList<>();
        for (Long id : ids) {
            LegalAuditOpinionDO opinion = validateOpinionExists(id);
            contractService.validateOpinionManageable(opinion.getContractId());
            contractIds.add(opinion.getContractId());
            opinionMapper.updateById(new LegalAuditOpinionDO()
                    .setId(id)
                    .setStatus(LegalOpinionStatusEnum.ADOPTED.getStatus()));
            adopted.add(opinionMapper.selectById(id));
        }
        contractIds.forEach(this::refreshContractRiskHighCount);
        String revision = null;
        boolean updated = false;
        for (Long contractId : contractIds) {
            revision = contractVersionService.rebuildWorkingFromAdoptedOpinions(contractId);
            updated = updated || adopted.stream()
                    .filter(item -> contractId.equals(item.getContractId()))
                    .anyMatch(LegalContractDocxRenderUtil::isAdoptApplicableToDocument);
        }
        return new LegalOpinionDocumentApplyResultVO(updated, revision);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchIgnore(List<Long> ids) {
        batchUpdateStatus(ids, LegalOpinionStatusEnum.IGNORED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(LegalAuditOpinionSaveReqVO reqVO) {
        contractService.validateOpinionManageable(reqVO.getContractId());
        LegalContractDO contract = contractService.validateContractExists(reqVO.getContractId());
        Integer auditRound = reqVO.getAuditRound() != null ? reqVO.getAuditRound() : contract.getAuditRound();
        LegalAuditOpinionDO opinion = LegalAuditOpinionDO.builder()
                .contractId(reqVO.getContractId())
                .auditRound(auditRound)
                .clauseType(MANUAL_CLAUSE_TYPE)
                .riskLevel(reqVO.getRiskLevel())
                .title(reqVO.getTitle())
                .content(reqVO.getContent())
                .suggestion(reqVO.getSuggestion())
                .paragraphId(reqVO.getParagraphId())
                .sourceType(LegalOpinionSourceTypeEnum.MANUAL.getCode())
                .changeType(LegalOpinionChangeTypeEnum.NO_CHANGE.getCode())
                .status(LegalOpinionStatusEnum.PENDING.getStatus())
                .build();
        opinionMapper.insert(opinion);
        refreshContractRiskHighCount(reqVO.getContractId());
        return opinion.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateManual(LegalAuditOpinionSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw exception(OPINION_NOT_EXISTS);
        }
        LegalAuditOpinionDO opinion = validateOpinionExists(reqVO.getId());
        validateManualOpinion(opinion);
        contractService.validateOpinionManageable(opinion.getContractId());
        contractService.validateContractExists(reqVO.getContractId());
        opinionMapper.updateById(new LegalAuditOpinionDO()
                .setId(reqVO.getId())
                .setContractId(reqVO.getContractId())
                .setAuditRound(reqVO.getAuditRound() != null ? reqVO.getAuditRound() : opinion.getAuditRound())
                .setRiskLevel(reqVO.getRiskLevel())
                .setTitle(reqVO.getTitle())
                .setContent(reqVO.getContent())
                .setSuggestion(reqVO.getSuggestion())
                .setParagraphId(reqVO.getParagraphId()));
        refreshContractRiskHighCount(opinion.getContractId());
        if (!opinion.getContractId().equals(reqVO.getContractId())) {
            refreshContractRiskHighCount(reqVO.getContractId());
        }
    }

    private void updateOpinionStatus(Long id, LegalOpinionStatusEnum status) {
        LegalAuditOpinionDO opinion = validateOpinionExists(id);
        contractService.validateOpinionManageable(opinion.getContractId());
        opinionMapper.updateById(new LegalAuditOpinionDO()
                .setId(id)
                .setStatus(status.getStatus()));
        refreshContractRiskHighCount(opinion.getContractId());
    }

    private static LegalOpinionDocumentApplyResultVO buildApplyResult(String revision,
                                                                      LegalAuditOpinionDO opinion) {
        boolean updated = LegalContractDocxRenderUtil.isAdoptApplicableToDocument(opinion);
        return new LegalOpinionDocumentApplyResultVO(updated, revision);
    }

    private void batchUpdateStatus(List<Long> ids, LegalOpinionStatusEnum status) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        Set<Long> contractIds = new HashSet<>();
        for (Long id : ids) {
            LegalAuditOpinionDO opinion = validateOpinionExists(id);
            contractService.validateOpinionManageable(opinion.getContractId());
            opinionMapper.updateById(new LegalAuditOpinionDO()
                    .setId(id)
                    .setStatus(status.getStatus()));
            contractIds.add(opinion.getContractId());
        }
        contractIds.forEach(this::refreshContractRiskHighCount);
    }

    private void refreshContractRiskHighCount(Long contractId) {
        int highCount = (int) opinionMapper.selectHighRiskPendingCount(contractId);
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setRiskHighCount(highCount));
    }

    private LegalAuditOpinionDO validateOpinionExists(Long id) {
        LegalAuditOpinionDO opinion = opinionMapper.selectById(id);
        if (opinion == null) {
            throw exception(OPINION_NOT_EXISTS);
        }
        return opinion;
    }

    private void validateManualOpinion(LegalAuditOpinionDO opinion) {
        if (!MANUAL_CLAUSE_TYPE.equals(opinion.getClauseType())) {
            throw exception(OPINION_NOT_MANUAL);
        }
    }

}
