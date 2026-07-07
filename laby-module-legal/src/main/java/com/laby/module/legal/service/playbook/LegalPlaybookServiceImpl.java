package com.laby.module.legal.service.playbook;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.legal.controller.admin.playbook.vo.LegalPlaybookPreviewRespVO;
import com.laby.module.legal.controller.admin.playbook.vo.LegalPlaybookSimulateRespVO;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.mysql.clause.LegalContractClauseMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LegalPlaybookServiceImpl implements LegalPlaybookService {

    @Resource
    private LegalReviewPlanCompiler reviewPlanCompiler;
    @Resource
    private LegalDeterministicAuditEngine deterministicAuditEngine;
    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalContractClauseMapper clauseMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;

    @Override
    public LegalPlaybookPreviewRespVO preview(Long contractTypeId) {
        LegalPlaybookPreviewRespVO resp = new LegalPlaybookPreviewRespVO();
        resp.setContractTypeId(contractTypeId);
        resp.setPlan(reviewPlanCompiler.compile(contractTypeId));
        return resp;
    }

    @Override
    public LegalPlaybookSimulateRespVO simulate(Long contractId) {
        LegalContractDO contract = contractService.validateContractExists(contractId);
        LegalReviewPlanBO plan = reviewPlanCompiler.compile(contract.getContractTypeId());
        List<LegalContractClauseDO> clauses = clauseMapper.selectListByContractId(contractId);
        List<LegalContractParagraphDO> paragraphs = paragraphMapper.selectListByContractId(contractId);
        List<LegalAuditOpinionDraftBO> opinions = deterministicAuditEngine.run(plan, clauses, paragraphs);

        LegalPlaybookSimulateRespVO resp = new LegalPlaybookSimulateRespVO();
        resp.setContractId(contractId);
        resp.setDeterministicCount(CollUtil.size(opinions));
        resp.setOpinions(opinions);
        return resp;
    }

}
