package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.dataobject.report.LegalAuditReportDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.dal.mysql.report.LegalAuditReportMapper;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.service.contract.bo.LegalContractAuditPersistCommand;
import com.laby.module.legal.service.contract.util.LegalAuditEvidenceRefsBuilder;
import com.laby.module.legal.service.opinion.LegalAuditOpinionRewriteSupport;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import com.laby.module.legal.service.report.LegalAuditReportBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 审核意见与报告持久化；二轮幂等跳过判定。
 */
@Slf4j
@Service
public class LegalAuditOpinionPersistService {

    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalAuditReportMapper reportMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalContractVersionService contractVersionService;

    public boolean hasExistingOpinionsForRound(Long contractId, int auditRound) {
        return CollUtil.isNotEmpty(opinionMapper.selectListByContractIdAndRound(contractId, auditRound));
    }

    public void persist(LegalContractAuditPersistCommand command) {
        Long contractId = command.getContractId();
        LegalContractDO contract = command.getContract();
        int auditRound = command.getAuditRound();
        List<LegalAiAuditOpinionItemBO> items = command.getItems();
        Long fromVersionId = contractVersionService.resolveFromVersionIdForOpinions(contractId, auditRound);
        opinionMapper.deleteByContractIdAndRound(contractId, auditRound);
        if (CollUtil.isNotEmpty(items)) {
            Map<String, String> paragraphTexts = paragraphMapper.selectListByContractId(contractId).stream()
                    .filter(p -> StrUtil.isNotBlank(p.getParagraphId()))
                    .collect(Collectors.toMap(LegalContractParagraphDO::getParagraphId,
                            LegalContractParagraphDO::getText, (a, b) -> a));
            for (LegalAiAuditOpinionItemBO item : items) {
                LegalAuditOpinionRewriteSupport.normalizeAiOpinionItem(
                        item, paragraphTexts.get(item.getParagraphId()));
                opinionMapper.insert(LegalAuditOpinionDO.builder()
                        .contractId(contractId)
                        .auditRound(auditRound)
                        .clauseType(item.getClauseType())
                        .riskLevel(normalizeRiskLevel(item.getRiskLevel()))
                        .title(StrUtil.blankToDefault(item.getTitle(), "审核意见"))
                        .content(StrUtil.blankToDefault(item.getContent(), ""))
                        .suggestion(item.getSuggestion())
                        .paragraphId(item.getParagraphId())
                        .clauseId(item.getClauseId())
                        .referenceClause(StrUtil.sub(StrUtil.blankToDefault(item.getReferenceClause(), ""), 0, 512))
                        .sourceType(normalizeSourceType(item.getSourceType()))
                        .sourceId(StrUtil.blankToDefault(item.getSourceId(), null))
                        .sourceVersion(String.valueOf(auditRound))
                        .fromVersionId(fromVersionId)
                        .changeType(normalizeChangeType(item.getChangeType()))
                        .oldText(item.getOldText())
                        .newText(item.getNewText())
                        .evidenceRefs(buildEvidenceRefs(item))
                        .status(0)
                        .build());
            }
        }
        String reportMarkdown = buildReportMarkdown(contract, auditRound,
                CollUtil.isEmpty(items) ? List.of() : items, LocalDateTime.now());
        reportMapper.deleteByContractIdAndRound(contractId, auditRound);
        reportMapper.insert(LegalAuditReportDO.builder()
                .contractId(contractId)
                .auditRound(auditRound)
                .content(reportMarkdown)
                .build());
        log.info("[persist][contractId={} round={}] 意见 {} 条，报告已落库",
                contractId, auditRound, CollUtil.size(items));
    }

    public void appendFailureNoteToReport(Long contractId, int auditRound, String note) {
        LegalAuditReportDO report = reportMapper.selectByContractIdAndRound(contractId, auditRound);
        if (report != null && StrUtil.isNotBlank(report.getContent())) {
            reportMapper.updateById(new LegalAuditReportDO()
                    .setId(report.getId())
                    .setContent(report.getContent() + "\n\n> ⚠️ **AI 审核异常（已生成空报告骨架）：** " + note + "\n"));
        }
    }

    public LegalAuditReportDO rebuildAuditReportIfMissing(Long contractId, int auditRound,
                                                            LegalContractDO contract) {
        LegalAuditReportDO existing = reportMapper.selectByContractIdAndRound(contractId, auditRound);
        if (existing != null && StrUtil.isNotBlank(existing.getContent())) {
            return existing;
        }
        List<LegalAuditOpinionDO> opinionRows = opinionMapper.selectListByContractIdAndRound(contractId, auditRound);
        if (contract == null) {
            return existing;
        }
        List<LegalAiAuditOpinionItemBO> items = new ArrayList<>();
        if (CollUtil.isNotEmpty(opinionRows)) {
            for (LegalAuditOpinionDO row : opinionRows) {
                LegalAiAuditOpinionItemBO item = new LegalAiAuditOpinionItemBO();
                item.setClauseType(row.getClauseType());
                item.setRiskLevel(row.getRiskLevel());
                item.setTitle(row.getTitle());
                item.setContent(row.getContent());
                item.setSuggestion(row.getSuggestion());
                item.setParagraphId(row.getParagraphId());
                item.setReferenceClause(row.getReferenceClause());
                items.add(item);
            }
        }
        String reportMarkdown = buildReportMarkdown(contract, auditRound, items, LocalDateTime.now());
        reportMapper.deleteByContractIdAndRound(contractId, auditRound);
        LegalAuditReportDO report = LegalAuditReportDO.builder()
                .contractId(contractId)
                .auditRound(auditRound)
                .content(reportMarkdown)
                .build();
        reportMapper.insert(report);
        log.info("[rebuildAuditReportIfMissing][contractId={} round={}] 已从 {} 条意见重建报告",
                contractId, auditRound, items.size());
        return report;
    }

    private String buildReportMarkdown(LegalContractDO contract, int auditRound,
                                       List<LegalAiAuditOpinionItemBO> items, LocalDateTime auditTime) {
        List<LegalAuditReportBuilder.OpinionView> views = new ArrayList<>();
        if (CollUtil.isNotEmpty(items)) {
            for (LegalAiAuditOpinionItemBO item : items) {
                LegalAuditReportBuilder.OpinionView view = new LegalAuditReportBuilder.OpinionView();
                view.setClauseType(item.getClauseType());
                view.setRiskLevel(normalizeRiskLevel(item.getRiskLevel()));
                view.setTitle(item.getTitle());
                view.setContent(item.getContent());
                view.setSuggestion(item.getSuggestion());
                view.setParagraphId(item.getParagraphId());
                view.setReferenceClause(item.getReferenceClause());
                views.add(view);
            }
        }
        return LegalAuditReportBuilder.build(contract, auditRound, views, auditTime);
    }

    private static String normalizeRiskLevel(String riskLevel) {
        return LegalRiskLevelEnum.normalize(riskLevel).getCode();
    }

    private static String normalizeChangeType(String changeType) {
        return LegalAuditOpinionRewriteSupport.normalizeChangeTypeCode(changeType);
    }

    private static String normalizeSourceType(String sourceType) {
        if (StrUtil.isBlank(sourceType)) {
            return LegalOpinionSourceTypeEnum.AI.getCode();
        }
        for (LegalOpinionSourceTypeEnum item : LegalOpinionSourceTypeEnum.values()) {
            if (item.getCode().equalsIgnoreCase(sourceType)) {
                return item.getCode();
            }
        }
        return LegalOpinionSourceTypeEnum.AI.getCode();
    }

    private static String buildEvidenceRefs(LegalAiAuditOpinionItemBO item) {
        List<Map<String, String>> refs = LegalAuditEvidenceRefsBuilder.build(
                item.getEvidenceRefs(),
                item.getSourceType(),
                item.getSourceId(),
                item.getReferenceClause(),
                null);
        return CollUtil.isEmpty(refs) ? null : JsonUtils.toJsonString(refs);
    }

}
