package com.laby.module.legal.service.eval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.enums.LegalEvalConstants;
import com.laby.module.legal.enums.contract.LegalRiskLevelEnum;
import com.laby.module.legal.service.contract.bo.LegalAuditOpinionDraftBO;
import com.laby.module.legal.service.eval.bo.LegalAuditEvalReportBO;
import com.laby.module.legal.service.eval.bo.LegalPlaybookEvalCaseBO;
import com.laby.module.legal.service.eval.bo.LegalPlaybookEvalExpectationBO;
import com.laby.module.legal.service.eval.bo.LegalPlaybookEvalParagraphBO;
import com.laby.module.legal.service.playbook.LegalDeterministicAuditEngine;
import com.laby.module.legal.service.playbook.bo.LegalReviewPlanBO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Playbook 黄金集离线评测（不依赖 DB / LLM）
 */
public class LegalPlaybookEvalRunner {

    private final LegalDeterministicAuditEngine engine = new LegalDeterministicAuditEngine();

    /**
     * 批量执行评测用例
     */
    public LegalAuditEvalReportBO runCases(List<LegalPlaybookEvalCaseBO> cases) {
        List<String> failed = new ArrayList<>();
        int passed = 0;
        for (LegalPlaybookEvalCaseBO evalCase : cases) {
            if (assertCase(evalCase)) {
                passed++;
            } else {
                failed.add(evalCase.getCaseId());
            }
        }
        return LegalAuditEvalReportBO.builder()
                .totalCases(cases.size())
                .passedCases(passed)
                .failedCaseIds(failed)
                .build();
    }

    /**
     * 运行默认 classpath 黄金集
     */
    public LegalAuditEvalReportBO runDefaultDataset() {
        return runFromClasspath(LegalEvalConstants.DEFAULT_PLAYBOOK_DATASET);
    }

    /**
     * 从 classpath 加载并运行黄金集
     */
    public LegalAuditEvalReportBO runFromClasspath(String resourcePath) {
        try (InputStream in = LegalPlaybookEvalRunner.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("评测资源不存在: " + resourcePath);
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<LegalPlaybookEvalCaseBO> cases = JsonUtils.parseObject(json,
                    new TypeReference<List<LegalPlaybookEvalCaseBO>>() {});
            return runCases(cases);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("加载评测集失败: " + resourcePath, ex);
        }
    }

    boolean assertCase(LegalPlaybookEvalCaseBO evalCase) {
        LegalReviewPlanBO plan = LegalReviewPlanBO.builder()
                .deterministicRules(evalCase.getRules())
                .build();
        List<LegalContractParagraphDO> paragraphs = toParagraphs(evalCase.getParagraphs());
        List<LegalAuditOpinionDraftBO> opinions = engine.run(plan, List.of(), paragraphs);
        LegalPlaybookEvalExpectationBO expectation = evalCase.getExpectation();
        if (expectation == null) {
            return CollUtil.isEmpty(opinions);
        }
        if (expectation.getMinOpinions() != null && opinions.size() < expectation.getMinOpinions()) {
            return false;
        }
        if (expectation.getMaxOpinions() != null && opinions.size() > expectation.getMaxOpinions()) {
            return false;
        }
        if (StrUtil.isNotBlank(expectation.getTitleContains())) {
            boolean titleHit = opinions.stream()
                    .anyMatch(o -> StrUtil.contains(o.getTitle(), expectation.getTitleContains()));
            if (!titleHit) {
                return false;
            }
        }
        if (StrUtil.isNotBlank(expectation.getSourceType())) {
            boolean sourceHit = opinions.stream()
                    .anyMatch(o -> expectation.getSourceType().equalsIgnoreCase(o.getSourceType()));
            if (!sourceHit) {
                return false;
            }
        }
        if (StrUtil.isNotBlank(expectation.getMinRiskLevel())) {
            int minScore = riskScore(expectation.getMinRiskLevel());
            boolean riskHit = opinions.stream()
                    .anyMatch(o -> riskScore(o.getRiskLevel()) >= minScore);
            if (!riskHit) {
                return false;
            }
        }
        return true;
    }

    private static List<LegalContractParagraphDO> toParagraphs(List<LegalPlaybookEvalParagraphBO> fixtures) {
        if (CollUtil.isEmpty(fixtures)) {
            return List.of();
        }
        List<LegalContractParagraphDO> rows = new ArrayList<>();
        for (LegalPlaybookEvalParagraphBO fixture : fixtures) {
            LegalContractParagraphDO row = new LegalContractParagraphDO();
            row.setParagraphId(fixture.getParagraphId());
            row.setText(fixture.getText());
            rows.add(row);
        }
        return rows;
    }

    private static int riskScore(String riskLevel) {
        return LegalRiskLevelEnum.normalize(riskLevel).getDeductScore();
    }

}
