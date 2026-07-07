package com.laby.module.legal.service.orchestrator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.service.auditrule.LegalAuditContextService;
import com.laby.module.legal.service.auditrule.bo.LegalAuditContextResult;
import com.laby.module.legal.service.contract.LegalAiAuditProgressService;
import com.laby.module.legal.service.contract.util.LegalAuditEvidenceRefsBuilder;
import com.laby.module.legal.service.opinion.LegalAuditOpinionClauseDraftService;
import com.laby.module.legal.service.opinion.LegalAuditOpinionRewriteSupport;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditPipelineCommand;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_AI_AUDIT_FAILED;

/**
 * LLM 分批审核管道（从 LegalAiAuditServiceImpl 抽离）
 */
@Slf4j
@Service
public class LegalAiAuditPipelineService {

    private static final int MAX_PARAGRAPHS_PER_REQUEST = 5;
    private static final int MAX_PARAGRAPH_TEXT_LENGTH = 200;
    private static final int BATCH_RETRY_TIMES = 3;
    private static final long BATCH_RETRY_DELAY_MS = 3000L;
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    /** 法务审核 JSON 输出格式约束（保留在 legal 模块，不写入 AiLlmClient） */
    private static final String AUDIT_JSON_FORMAT_SUFFIX = """

            【输出格式】仅输出 JSON，不要 markdown 代码块或解释。优先使用 {"opinions":[...]}；无风险则 {"opinions":[]}。
            """;

    @Resource
    private LegalAuditContextService auditContextService;
    @Resource
    private LegalAuditOpinionMapper opinionMapper;
    @Resource
    private LegalAiAuditProgressService auditProgressService;
    @Resource
    private LegalAuditOpinionClauseDraftService clauseDraftService;

    public List<LegalAiAuditOpinionItemBO> runLlmAuditBatches(LegalAiAuditPipelineCommand command) {
        LegalContractDO contract = command.getContract();
        List<LegalContractParagraphDO> paragraphs = command.getParagraphs();
        if (command.getLlmClient() == null) {
            throw exception(CONTRACT_AI_AUDIT_FAILED);
        }
        if (contract == null || CollUtil.isEmpty(paragraphs)) {
            return List.of();
        }
        List<LegalAiAuditOpinionItemBO> merged = new ArrayList<>();
        int totalBatches = (paragraphs.size() + MAX_PARAGRAPHS_PER_REQUEST - 1) / MAX_PARAGRAPHS_PER_REQUEST;
        List<String> batchErrors = new ArrayList<>();
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int from = batchIndex * MAX_PARAGRAPHS_PER_REQUEST;
            int to = Math.min(from + MAX_PARAGRAPHS_PER_REQUEST, paragraphs.size());
            List<LegalContractParagraphDO> batch = paragraphs.subList(from, to);
            int currentBatch = batchIndex + 1;
            String userPrompt = buildAuditPrompt(contract, batch, command.getAuditRound(), currentBatch, totalBatches);
            auditProgressService.onBatch(contract.getId(), currentBatch, totalBatches,
                    "正在审核第 " + currentBatch + "/" + totalBatches + " 批段落");
            LegalAuditContextResult auditContext = auditContextService.buildAuditContext(contract, batch);
            auditContextService.logRetrieval(contract.getId(), command.getAuditRound(), currentBatch, auditContext);
            String batchSystemPrompt = command.getSystemPrompt() + auditContext.getSupplementMarkdown();
            try {
                List<LegalAiAuditOpinionItemBO> batchItems = callAuditBatchWithRetry(
                        contract.getId(), command.getLlmClient(),
                        batchSystemPrompt, userPrompt, currentBatch, totalBatches,
                        command.getMaxTokens());
                enrichOpinionsWithContext(batchItems, auditContext);
                normalizeOpinionRewrites(batchItems, batch);
                enrichMissingClauseDrafts(batchItems, batch, command.getLlmClient());
                merged.addAll(batchItems);
            } catch (Exception ex) {
                batchErrors.add("批次 " + (batchIndex + 1) + ": " + ex.getMessage());
                log.warn("[runLlmAuditBatches][contractId={} batch={}/{}] 跳过失败批次: {}",
                        contract.getId(), batchIndex + 1, totalBatches, ex.getMessage());
            }
        }
        if (CollUtil.isEmpty(merged) && CollUtil.isNotEmpty(batchErrors) && command.isFailFast()) {
            throw exception(CONTRACT_AI_AUDIT_FAILED);
        }
        if (CollUtil.isEmpty(merged) && totalBatches > 0) {
            log.warn("[runLlmAuditBatches][contractId={}] 全部批次均未解析出审核意见", contract.getId());
            if (command.isFailFast()) {
                throw exception(CONTRACT_AI_AUDIT_FAILED);
            }
        }
        return merged;
    }

    private List<LegalAiAuditOpinionItemBO> callAuditBatchWithRetry(Long contractId, AiLlmClient llmClient,
                                                                  String systemPrompt, String userPrompt,
                                                                  int batchIndex, int totalBatches,
                                                                  Integer maxTokens) {
        Exception last = null;
        for (int attempt = 1; attempt <= BATCH_RETRY_TIMES; attempt++) {
            try {
                AiLlmRequest request = new AiLlmRequest()
                        .setJsonMode(true)
                        .setMaxTokens(maxTokens)
                        .setMessages(List.of(
                                new AiMessage().setRole(AiMessageRoleEnum.SYSTEM)
                                        .setContent(systemPrompt + AUDIT_JSON_FORMAT_SUFFIX),
                                new AiMessage().setRole(AiMessageRoleEnum.USER).setContent(userPrompt)));
                String rawContent = llmClient.call(request);
                List<LegalAiAuditOpinionItemBO> items = parseOpinions(rawContent);
                if (CollUtil.isEmpty(items)) {
                    if (StrUtil.isBlank(rawContent)) {
                        throw exception(CONTRACT_AI_AUDIT_FAILED);
                    }
                    if (!isExplicitEmptyOpinionJson(rawContent)) {
                        log.warn("[callAuditBatchWithRetry][batch={}/{}] AI 有响应但未解析到 JSON，raw 前 200 字: {}",
                                batchIndex, totalBatches, StrUtil.sub(rawContent, 0, 200));
                        throw exception(CONTRACT_AI_AUDIT_FAILED);
                    }
                }
                return items;
            } catch (Exception ex) {
                last = ex;
                log.warn("[callAuditBatchWithRetry][batch={}/{} attempt={}] {}", batchIndex, totalBatches, attempt,
                        ex.getMessage());
                if (attempt < BATCH_RETRY_TIMES) {
                    sleepQuietly(BATCH_RETRY_DELAY_MS);
                }
            }
        }
        if (last != null) {
            throw exception(CONTRACT_AI_AUDIT_FAILED);
        }
        return List.of();
    }

    private String buildAuditPrompt(LegalContractDO contract, List<LegalContractParagraphDO> paragraphs,
                                    int auditRound, int batchIndex, int totalBatches) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contract.getId());
        payload.put("title", contract.getTitle());
        payload.put("partyRole", contract.getPartyRole());
        payload.put("auditLevel", contract.getAuditLevel());
        payload.put("auditRound", auditRound);
        if (totalBatches > 1) {
            payload.put("batchIndex", batchIndex);
            payload.put("totalBatches", totalBatches);
        }
        if (auditRound > 1 && StrUtil.isNotBlank(contract.getFeedbackSummary())) {
            payload.put("feedbackSummary", StrUtil.sub(contract.getFeedbackSummary(), 0, 2000));
            payload.put("round1Disposition", buildRound1DispositionSummary(contract.getId()));
        }
        List<Map<String, Object>> paragraphViews = new ArrayList<>();
        for (LegalContractParagraphDO paragraph : paragraphs) {
            Map<String, Object> view = new HashMap<>();
            view.put("paragraphId", paragraph.getParagraphId());
            view.put("sort", paragraph.getSort());
            view.put("path", paragraph.getPath());
            view.put("text", truncateParagraphText(paragraph.getText()));
            paragraphViews.add(view);
        }
        payload.put("paragraphs", paragraphViews);
        payload.put("outputSchema", Map.of(
                "opinions", """
                        JSON 数组。每条有风险的意见必须包含可写入合同的改写字段，整体输出 {"opinions":[...]}，无风险则 {"opinions":[]}。
                        字段说明：
                        - clauseType,riskLevel,title,content,suggestion,paragraphId,referenceClause,sourceType,sourceId
                        - changeType：修改既有表述用 REPLACE；缺少/缺失条款、需新增条文时用 INSERT_AFTER（此时可无 oldText）；删除用 DELETE
                        - oldText：从 paragraphs[].text 精确复制的待改原文子串（REPLACE/DELETE 必填；INSERT 类意见留空）
                        - newText：修改后的正式合同条款全文（可直接写入 docx）。缺少付款方式/违约责任等条款时，必须写出完整新增条款，禁止只写「建议补充」
                        - suggestion：给人看的简短修改说明（可选，不得替代 newText）
                        示例-修改：{"changeType":"REPLACE","oldText":"包换期3个月","newText":"包换期为6个月","suggestion":"建议延长包换期"}
                        示例-缺条款：{"changeType":"INSERT_AFTER","paragraphId":"p-20","newText":"付款方式及期限：甲方应于…","suggestion":"建议补充付款方式、期限及条件","title":"缺少付款条款"}
                        """));
        return JsonUtils.toJsonString(payload);
    }

    private Map<String, Object> buildRound1DispositionSummary(Long contractId) {
        List<LegalAuditOpinionDO> round1 = opinionMapper.selectListByContractIdAndRound(contractId, 1);
        long adopted = round1.stream()
                .filter(o -> LegalOpinionStatusEnum.ADOPTED.getStatus().equals(o.getStatus())).count();
        long ignored = round1.stream()
                .filter(o -> LegalOpinionStatusEnum.IGNORED.getStatus().equals(o.getStatus())).count();
        long pending = round1.stream()
                .filter(o -> Integer.valueOf(0).equals(o.getStatus())).count();
        Map<String, Object> summary = new HashMap<>();
        summary.put("adoptedCount", adopted);
        summary.put("ignoredCount", ignored);
        summary.put("pendingCount", pending);
        List<Map<String, String>> ignoredSamples = round1.stream()
                .filter(o -> LegalOpinionStatusEnum.IGNORED.getStatus().equals(o.getStatus()))
                .limit(5)
                .map(o -> Map.of(
                        "paragraphId", StrUtil.blankToDefault(o.getParagraphId(), ""),
                        "title", StrUtil.sub(StrUtil.blankToDefault(o.getTitle(), ""), 0, 80)))
                .toList();
        summary.put("ignoredSamples", ignoredSamples);
        return summary;
    }

    private static String truncateParagraphText(String text) {
        return StrUtil.isBlank(text) ? "" : StrUtil.sub(text, 0, MAX_PARAGRAPH_TEXT_LENGTH);
    }

    private List<LegalAiAuditOpinionItemBO> parseOpinions(String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            return List.of();
        }
        String json = extractJson(rawContent);
        try {
            if (json.startsWith("{")) {
                Map<?, ?> root = JsonUtils.parseObject(json, Map.class);
                if (root != null) {
                    for (String key : List.of("opinions", "items", "data", "results")) {
                        Object value = root.get(key);
                        if (value != null) {
                            return JsonUtils.parseArray(JsonUtils.toJsonString(value), LegalAiAuditOpinionItemBO.class);
                        }
                    }
                }
            }
            List<LegalAiAuditOpinionItemBO> items = JsonUtils.parseArray(json, LegalAiAuditOpinionItemBO.class);
            return CollUtil.isEmpty(items) ? List.of() : items;
        } catch (Exception ex) {
            log.warn("[parseOpinions] JSON 解析失败: {}", ex.getMessage());
            return List.of();
        }
    }

    private static boolean isExplicitEmptyOpinionJson(String rawContent) {
        String json = extractJson(rawContent).trim();
        if ("[]".equals(json)) {
            return true;
        }
        return json.contains("\"opinions\"")
                && (json.contains("\"opinions\":[]") || json.contains("\"opinions\": []")
                || json.contains("\"opinions\": null") || json.contains("\"opinions\":null"));
    }

    private static String extractJson(String rawContent) {
        Matcher matcher = JSON_BLOCK.matcher(rawContent.trim());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String trimmed = rawContent.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private void enrichMissingClauseDrafts(List<LegalAiAuditOpinionItemBO> items,
                                         List<LegalContractParagraphDO> paragraphs,
                                         AiLlmClient llmClient) {
        if (CollUtil.isEmpty(items) || llmClient == null) {
            return;
        }
        Map<String, String> paragraphTexts = new HashMap<>();
        if (CollUtil.isNotEmpty(paragraphs)) {
            for (LegalContractParagraphDO paragraph : paragraphs) {
                if (paragraph != null && StrUtil.isNotBlank(paragraph.getParagraphId())) {
                    paragraphTexts.put(paragraph.getParagraphId(), paragraph.getText());
                }
            }
        }
        for (LegalAiAuditOpinionItemBO item : items) {
            if (!LegalAuditOpinionRewriteSupport.needsClauseDraft(item)) {
                continue;
            }
            clauseDraftService.enrichMissingClauseDrafts(
                    List.of(item), llmClient, paragraphTexts.get(item.getParagraphId()));
        }
    }

    private static void normalizeOpinionRewrites(List<LegalAiAuditOpinionItemBO> items,
                                                 List<LegalContractParagraphDO> paragraphs) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<String, String> paragraphTexts = new HashMap<>();
        if (CollUtil.isNotEmpty(paragraphs)) {
            for (LegalContractParagraphDO paragraph : paragraphs) {
                if (paragraph != null && StrUtil.isNotBlank(paragraph.getParagraphId())) {
                    paragraphTexts.put(paragraph.getParagraphId(), paragraph.getText());
                }
            }
        }
        for (LegalAiAuditOpinionItemBO item : items) {
            LegalAuditOpinionRewriteSupport.normalizeAiOpinionItem(
                    item, paragraphTexts.get(item.getParagraphId()));
        }
    }

    private static void enrichOpinionsWithContext(List<LegalAiAuditOpinionItemBO> items,
                                                  LegalAuditContextResult context) {
        if (CollUtil.isEmpty(items) || context == null) {
            return;
        }
        for (LegalAiAuditOpinionItemBO item : items) {
            List<Map<String, String>> refs = LegalAuditEvidenceRefsBuilder.build(
                    item.getEvidenceRefs(),
                    item.getSourceType(),
                    item.getSourceId(),
                    item.getReferenceClause(),
                    context);
            if (CollUtil.isNotEmpty(refs)) {
                item.setEvidenceRefs(refs);
                applyPrimaryEvidenceSource(item, refs);
            }
        }
    }

    private static void applyPrimaryEvidenceSource(LegalAiAuditOpinionItemBO item,
                                                   List<Map<String, String>> refs) {
        boolean invalidKnowledge = LegalOpinionSourceTypeEnum.KNOWLEDGE.getCode()
                .equalsIgnoreCase(item.getSourceType())
                && !isNumericSegmentId(item.getSourceId());
        if (!StrUtil.isBlank(item.getSourceType()) && !invalidKnowledge) {
            return;
        }
        for (Map<String, String> ref : refs) {
            if (ref == null || ref.isEmpty()) {
                continue;
            }
            String type = ref.get("sourceType");
            String id = ref.get("sourceId");
            if (LegalOpinionSourceTypeEnum.KNOWLEDGE.getCode().equalsIgnoreCase(type)
                    && !isNumericSegmentId(id)) {
                continue;
            }
            item.setSourceType(type);
            item.setSourceId(id);
            return;
        }
        if (!refs.isEmpty()) {
            Map<String, String> first = refs.get(0);
            item.setSourceType(first.get("sourceType"));
            item.setSourceId(first.get("sourceId"));
        }
    }

    private static boolean isNumericSegmentId(String sourceId) {
        if (StrUtil.isBlank(sourceId)) {
            return false;
        }
        for (int i = 0; i < sourceId.length(); i++) {
            if (!Character.isDigit(sourceId.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
