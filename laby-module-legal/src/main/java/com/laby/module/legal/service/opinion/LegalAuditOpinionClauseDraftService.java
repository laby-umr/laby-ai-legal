package com.laby.module.legal.service.opinion;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 为「缺少条款」类意见补全可写入合同的 newText（LLM 起草）。
 */
@Slf4j
@Service
public class LegalAuditOpinionClauseDraftService {

    private static final int MAX_DRAFT_CHARS = 1200;

    public void enrichMissingClauseDrafts(List<LegalAiAuditOpinionItemBO> items,
                                          AiLlmClient llmClient,
                                          String paragraphText) {
        if (llmClient == null || items == null) {
            return;
        }
        for (LegalAiAuditOpinionItemBO item : items) {
            if (!LegalAuditOpinionRewriteSupport.needsClauseDraft(item)) {
                continue;
            }
            try {
                String drafted = draftClauseText(llmClient, item, paragraphText);
                if (StrUtil.isNotBlank(drafted)) {
                    item.setNewText(drafted);
                    item.setChangeType(LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode());
                    log.info("[enrichMissingClauseDrafts][title={}] clause draft generated", item.getTitle());
                }
            } catch (Exception ex) {
                log.warn("[enrichMissingClauseDrafts][title={}] draft failed: {}",
                        item.getTitle(), ex.getMessage());
            }
        }
    }

    private String draftClauseText(AiLlmClient llmClient, LegalAiAuditOpinionItemBO item,
                                   String paragraphText) {
        String userPrompt = """
                请根据以下审核意见，起草一段可直接写入合同的正式条款正文。
                要求：
                1. 输出完整条款表述，可直接作为合同条文插入
                2. 禁止以「建议」「应当补充」等说明性词语开头
                3. 只输出条款正文，不要 JSON、不要 markdown、不要编号外的多余解释
                
                意见标题：%s
                风险说明：%s
                修改说明：%s
                参考标准条款：%s
                定位段落上下文：%s
                """.formatted(
                StrUtil.blankToDefault(item.getTitle(), ""),
                StrUtil.blankToDefault(item.getContent(), ""),
                StrUtil.blankToDefault(item.getSuggestion(), ""),
                StrUtil.blankToDefault(item.getReferenceClause(), ""),
                StrUtil.sub(StrUtil.blankToDefault(paragraphText, ""), 0, 600));
        AiLlmRequest request = new AiLlmRequest()
                .setMessages(List.of(
                        new AiMessage().setRole(AiMessageRoleEnum.SYSTEM)
                                .setContent("你是资深法务合同起草专家，输出简洁、可落地的合同条款正文。"),
                        new AiMessage().setRole(AiMessageRoleEnum.USER).setContent(userPrompt)));
        String raw = llmClient.call(request);
        return LegalAuditOpinionRewriteSupport.cleanRewriteText(
                StrUtil.sub(StrUtil.trim(raw), 0, MAX_DRAFT_CHARS));
    }

}
