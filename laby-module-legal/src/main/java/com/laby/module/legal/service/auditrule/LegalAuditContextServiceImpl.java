package com.laby.module.legal.service.auditrule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.service.knowledge.AiKnowledgeSegmentService;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.legal.dal.dataobject.auditrule.LegalAuditRuleDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import com.laby.module.legal.dal.mysql.auditrule.LegalAuditRuleMapper;
import com.laby.module.legal.service.auditrule.bo.LegalAuditContextResult;
import com.laby.module.legal.service.contracttype.LegalContractTypeService;
import com.laby.module.legal.service.retrieval.LegalRetrievalLogService;
import com.laby.module.legal.service.standardclause.LegalStandardClauseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 法务审核上下文 Service 实现类
 */
@Service
public class LegalAuditContextServiceImpl implements LegalAuditContextService {

    private static final int RAG_TOP_K = 5;
    private static final int MAX_RAG_QUERY_CHARS = 800;
    private static final int MAX_CHAT_QUERY_CHARS = 500;
    private static final int MAX_RULE_BLOCK_CHARS = 6_000;

    @Resource
    private LegalAuditRuleMapper auditRuleMapper;
    @Resource
    private LegalContractTypeService contractTypeService;
    @Resource
    private LegalStandardClauseService standardClauseService;
    @Resource
    private AiKnowledgeSegmentService knowledgeSegmentService;
    @Resource
    private LegalRetrievalLogService retrievalLogService;

    @Override
    public String buildAuditSupplement(LegalContractDO contract, List<LegalContractParagraphDO> batchParagraphs) {
        return buildAuditContext(contract, batchParagraphs).getSupplementMarkdown();
    }

    @Override
    public LegalAuditContextResult buildAuditContext(LegalContractDO contract,
                                                     List<LegalContractParagraphDO> batchParagraphs) {
        LegalAuditContextResult result = new LegalAuditContextResult();
        StringBuilder sb = new StringBuilder();
        appendRules(sb, contract, result);
        appendKnowledgeRag(sb, contract, batchParagraphs, result);
        if (!sb.isEmpty()) {
            result.setSupplementMarkdown("\n\n## 审核依据（全局规则与标准条款）\n" + sb);
        }
        return result;
    }

    @Override
    public LegalAuditContextResult buildChatKnowledgeContext(LegalContractDO contract, String userMessage) {
        LegalAuditContextResult result = new LegalAuditContextResult();
        String query = StrUtil.sub(StrUtil.trim(userMessage), 0, MAX_CHAT_QUERY_CHARS);
        result.setRagQuery(query);
        if (StrUtil.isBlank(query)) {
            return result;
        }
        Long knowledgeId = resolveKnowledgeId(contract);
        if (knowledgeId == null) {
            return result;
        }
        List<AiKnowledgeSegmentSearchRespBO> segments = knowledgeSegmentService.searchKnowledgeSegment(
                new AiKnowledgeSegmentSearchReqBO()
                        .setKnowledgeId(knowledgeId)
                        .setContent(query)
                        .setTopK(RAG_TOP_K));
        if (CollUtil.isEmpty(segments)) {
            return result;
        }
        StringBuilder sb = new StringBuilder("\n### 知识库检索参考\n");
        Set<String> dedup = new LinkedHashSet<>();
        int index = 1;
        for (AiKnowledgeSegmentSearchRespBO segment : segments) {
            String content = StrUtil.trim(segment.getRetrievalContent());
            if (StrUtil.isBlank(content) || !dedup.add(content)) {
                continue;
            }
            LegalAuditContextResult.KnowledgeRef ref = new LegalAuditContextResult.KnowledgeRef();
            ref.setSegmentId(segment.getId());
            ref.setDocumentId(segment.getDocumentId());
            ref.setScore(segment.getScore());
            ref.setExcerpt(StrUtil.sub(content, 0, 400));
            result.getKnowledgeSegments().add(ref);
            sb.append(index++).append(". ");
            if (segment.getId() != null) {
                sb.append("`[segmentId=").append(segment.getId()).append("]` ");
            }
            sb.append(StrUtil.sub(content, 0, 400)).append("\n");
        }
        result.setSupplementMarkdown(sb.toString());
        return result;
    }

    @Override
    public void logRetrieval(Long contractId, int auditRound, int batchIndex, LegalAuditContextResult context) {
        retrievalLogService.logKnowledgeRetrieval(LegalRetrievalLogService.BIZ_TYPE_AUDIT,
                contractId, auditRound, batchIndex, context != null ? context.getRagQuery() : null,
                RAG_TOP_K, context);
    }

    private void appendRules(StringBuilder sb, LegalContractDO contract, LegalAuditContextResult result) {
        List<LegalAuditRuleDO> rules = auditRuleMapper.selectEnabledForAudit(contract.getContractTypeId());
        if (CollUtil.isEmpty(rules)) {
            return;
        }
        Set<Long> clauseIds = rules.stream()
                .map(LegalAuditRuleDO::getStandardClauseId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, LegalStandardClauseDO> clauseMap = standardClauseService.getStandardClauseMap(clauseIds);
        sb.append("\n### 已启用的全局审核规则\n");
        int index = 1;
        for (LegalAuditRuleDO rule : rules) {
            if (sb.length() > MAX_RULE_BLOCK_CHARS) {
                sb.append("\n> （规则过多，已截断）\n");
                break;
            }
            LegalAuditContextResult.RuleRef ref = new LegalAuditContextResult.RuleRef();
            ref.setRuleId(rule.getId());
            ref.setStandardClauseId(rule.getStandardClauseId());
            ref.setName(rule.getName());
            ref.setClauseType(rule.getClauseType());
            result.getRules().add(ref);

            sb.append(index++).append(". **").append(rule.getName()).append("**");
            sb.append(" `[ruleId=").append(rule.getId()).append("]`");
            if (StrUtil.isNotBlank(rule.getClauseType())) {
                sb.append("（").append(rule.getClauseType()).append("）");
            }
            sb.append("\n");
            if (StrUtil.isNotBlank(rule.getRuleContent())) {
                sb.append("   - 要求：").append(rule.getRuleContent().trim()).append("\n");
            }
            if (rule.getStandardClauseId() != null) {
                LegalStandardClauseDO clause = clauseMap.get(rule.getStandardClauseId());
                if (clause != null) {
                    sb.append("   - 标准条款「").append(clause.getName()).append("」")
                            .append(" `[clauseId=").append(clause.getId()).append("]`：")
                            .append(StrUtil.sub(clause.getContent(), 0, 500)).append("\n");
                }
            }
        }
        sb.append("\n请在输出 JSON 意见中填写 `referenceClause`；若依据某条规则或知识库片段，")
                .append("请填写 `sourceType`（RULE/STANDARD_CLAUSE/KNOWLEDGE/AI）与 `sourceId`（对应 ruleId/clauseId/segmentId）。\n");
    }

    private void appendKnowledgeRag(StringBuilder sb, LegalContractDO contract,
                                    List<LegalContractParagraphDO> batchParagraphs,
                                    LegalAuditContextResult result) {
        Long knowledgeId = resolveKnowledgeId(contract);
        if (knowledgeId == null || CollUtil.isEmpty(batchParagraphs)) {
            return;
        }
        String query = buildRagQuery(batchParagraphs);
        result.setRagQuery(query);
        if (StrUtil.isBlank(query)) {
            return;
        }
        List<AiKnowledgeSegmentSearchRespBO> segments = knowledgeSegmentService.searchKnowledgeSegment(
                new AiKnowledgeSegmentSearchReqBO()
                        .setKnowledgeId(knowledgeId)
                        .setContent(query)
                        .setTopK(RAG_TOP_K));
        if (CollUtil.isEmpty(segments)) {
            return;
        }
        sb.append("\n### 知识库检索参考（合同类型优先）\n");
        Set<String> dedup = new LinkedHashSet<>();
        int index = 1;
        for (AiKnowledgeSegmentSearchRespBO segment : segments) {
            String content = StrUtil.trim(segment.getRetrievalContent());
            if (StrUtil.isBlank(content) || !dedup.add(content)) {
                continue;
            }
            LegalAuditContextResult.KnowledgeRef ref = new LegalAuditContextResult.KnowledgeRef();
            ref.setSegmentId(segment.getId());
            ref.setDocumentId(segment.getDocumentId());
            ref.setScore(segment.getScore());
            ref.setExcerpt(StrUtil.sub(content, 0, 400));
            result.getKnowledgeSegments().add(ref);

            sb.append(index++).append(". ");
            if (segment.getId() != null) {
                sb.append("`[segmentId=").append(segment.getId()).append("]` ");
            }
            sb.append(StrUtil.sub(content, 0, 400)).append("\n");
        }
    }

    private Long resolveKnowledgeId(LegalContractDO contract) {
        if (contract.getContractTypeId() == null) {
            return null;
        }
        LegalContractTypeDO type = contractTypeService.getContractType(contract.getContractTypeId());
        return type != null ? type.getKnowledgeId() : null;
    }

    private static String buildRagQuery(List<LegalContractParagraphDO> batchParagraphs) {
        StringBuilder query = new StringBuilder();
        for (LegalContractParagraphDO paragraph : batchParagraphs) {
            if (StrUtil.isNotBlank(paragraph.getText())) {
                query.append(paragraph.getText()).append(' ');
            }
            if (query.length() >= MAX_RAG_QUERY_CHARS) {
                break;
            }
        }
        return StrUtil.sub(query.toString().trim(), 0, MAX_RAG_QUERY_CHARS);
    }

}
