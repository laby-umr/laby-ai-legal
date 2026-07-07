package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractWorkbenchNavigationNodeVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractWorkbenchReportSummaryVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractWorkbenchRespVO;
import com.laby.module.legal.controller.admin.opinion.vo.LegalAuditOpinionRespVO;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.clause.LegalContractClauseMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.dal.mysql.report.LegalAuditReportMapper;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.service.opinion.LegalAuditOpinionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 合同审阅工作台聚合实现
 */
@Service
public class LegalContractWorkbenchServiceImpl implements LegalContractWorkbenchService {

    private static final int REPORT_PREVIEW_MAX = 2000;

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalContractClauseMapper clauseMapper;
    @Resource
    private LegalAuditReportMapper auditReportMapper;
    @Resource
    private LegalAuditOpinionService opinionService;

    @Override
    public LegalContractWorkbenchRespVO getWorkbench(Long contractId) {
        contractService.validateContractExists(contractId);

        LegalContractWorkbenchRespVO resp = new LegalContractWorkbenchRespVO();
        resp.setContract(contractService.getContractResp(contractId));

        List<LegalContractParagraphDO> paragraphDOs = paragraphMapper.selectListByContractId(contractId);
        resp.setParagraphs(BeanUtils.toBean(paragraphDOs, LegalContractParagraphRespVO.class));

        List<LegalAuditOpinionDO> opinionDOs = opinionService.getOpinionListByContractId(contractId);
        resp.setOpinions(BeanUtils.toBean(opinionDOs, LegalAuditOpinionRespVO.class));

        List<LegalContractClauseDO> clauses = clauseMapper.selectListByContractId(contractId);
        if (CollUtil.isNotEmpty(clauses)) {
            resp.setNavigationMode("CLAUSE");
            resp.setNavigationNodes(buildClauseNavigation(clauses));
        } else {
            resp.setNavigationMode("PARAGRAPH");
            resp.setNavigationNodes(buildParagraphNavigation(paragraphDOs));
        }

        resp.setReportSummary(buildReportSummary(contractId, opinionDOs));
        return resp;
    }

    private List<LegalContractWorkbenchNavigationNodeVO> buildParagraphNavigation(
            List<LegalContractParagraphDO> paragraphs) {
        List<LegalContractWorkbenchNavigationNodeVO> nodes = new ArrayList<>();
        if (CollUtil.isEmpty(paragraphs)) {
            return nodes;
        }
        for (LegalContractParagraphDO paragraph : paragraphs) {
            LegalContractWorkbenchNavigationNodeVO node = new LegalContractWorkbenchNavigationNodeVO();
            node.setId(paragraph.getParagraphId());
            node.setLevel(0);
            String label = StrUtil.maxLength(paragraph.getText(), 32);
            node.setLabel(StrUtil.isBlank(label) ? paragraph.getParagraphId() : label);
            node.getParagraphIds().add(paragraph.getParagraphId());
            nodes.add(node);
        }
        return nodes;
    }

    private List<LegalContractWorkbenchNavigationNodeVO> buildClauseNavigation(
            List<LegalContractClauseDO> clauses) {
        List<LegalContractWorkbenchNavigationNodeVO> nodes = new ArrayList<>();
        for (LegalContractClauseDO clause : clauses) {
            LegalContractWorkbenchNavigationNodeVO node = new LegalContractWorkbenchNavigationNodeVO();
            node.setId(clause.getClauseId());
            node.setLevel(clause.getLevel() != null ? clause.getLevel() : 0);
            String label = StrUtil.blankToDefault(clause.getTitle(), clause.getClauseId());
            node.setLabel(StrUtil.maxLength(label, 64));
            List<String> paragraphIds = JsonUtils.parseArray(clause.getParagraphIds(), String.class);
            if (CollUtil.isNotEmpty(paragraphIds)) {
                node.getParagraphIds().addAll(paragraphIds);
            }
            nodes.add(node);
        }
        return nodes;
    }

    private LegalContractWorkbenchReportSummaryVO buildReportSummary(Long contractId,
                                                                     List<LegalAuditOpinionDO> opinions) {
        LegalContractWorkbenchReportSummaryVO summary = new LegalContractWorkbenchReportSummaryVO();
        LegalAuditReportDO report = auditReportMapper.selectLatestByContractId(contractId);
        summary.setHasReport(report != null && StrUtil.isNotBlank(report.getContent()));
        if (report != null && StrUtil.isNotBlank(report.getContent())) {
            summary.setPreviewMarkdown(StrUtil.sub(report.getContent(), 0, REPORT_PREVIEW_MAX));
        }
        long highCount = opinions.stream()
                .filter(o -> LegalRiskLevelEnum.HIGH.getCode().equals(o.getRiskLevel()))
                .count();
        summary.setRiskHighCount((int) highCount);
        return summary;
    }

}
