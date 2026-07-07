package com.laby.module.legal.service.ai.kernel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationFileItemMapper;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationSessionMapper;
import com.laby.module.legal.enums.contract.LegalContractCreateSourceEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import com.laby.module.legal.service.ai.kernel.bo.LegalAuditPreviewMergeResult;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationAuditPreviewItemBO;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationAuditPreviewSnapshotBO;
import com.laby.module.legal.service.orchestrator.bo.LegalAiAuditOpinionItemBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 编排预览意见复用到正式 Pipeline 审核
 */
@Slf4j
@Service
public class LegalAuditPreviewReuseService {

    @Resource
    private LegalOrchestrationSessionMapper sessionMapper;
    @Resource
    private LegalOrchestrationFileItemMapper fileItemMapper;

    /**
     * 解析合同关联的预览意见（仅首轮 AI 审核、AI 对话来源且 policy 一致）
     */
    public Optional<List<LegalAiAuditOpinionItemBO>> resolvePreviewItems(LegalContractDO contract, int auditRound) {
        if (auditRound != 1) {
            return Optional.empty();
        }
        if (!LegalContractCreateSourceEnum.AI_CHAT.getSource().equals(contract.getCreateSource())) {
            return Optional.empty();
        }
        if (contract.getCreateConversationId() == null || contract.getId() == null) {
            return Optional.empty();
        }
        LegalOrchestrationSessionDO session =
                sessionMapper.selectByConversationId(contract.getCreateConversationId());
        if (session == null || StrUtil.isBlank(session.getPreviewOpinionJson())) {
            return Optional.empty();
        }
        if (!policyMatches(session, contract)) {
            log.info("[resolvePreviewItems][contractId={}] 预览 policy 与合同不一致，跳过复用", contract.getId());
            return Optional.empty();
        }
        LegalOrchestrationFileItemDO fileItem = fileItemMapper.selectByContractId(contract.getId());
        if (fileItem == null) {
            return Optional.empty();
        }
        LegalOrchestrationAuditPreviewSnapshotBO snapshot = JsonUtils.parseObject(
                session.getPreviewOpinionJson(), LegalOrchestrationAuditPreviewSnapshotBO.class);
        if (snapshot == null || CollUtil.isEmpty(snapshot.getFiles())) {
            return Optional.empty();
        }
        LegalOrchestrationAuditPreviewSnapshotBO.FilePreview filePreview = snapshot.getFiles().stream()
                .filter(file -> Objects.equals(file.getFileItemId(), fileItem.getId()))
                .findFirst()
                .orElse(null);
        if (filePreview == null || CollUtil.isEmpty(filePreview.getOpinions())) {
            return Optional.empty();
        }
        return Optional.of(toOpinionItems(filePreview.getOpinions()));
    }

    /**
     * 合并正式审核结果与预览：正式优先，预览补全未覆盖项，按 paragraphId+title 去重
     */
    public LegalAuditPreviewMergeResult merge(List<LegalAiAuditOpinionItemBO> formal,
                                              List<LegalAiAuditOpinionItemBO> previewItems) {
        if (CollUtil.isEmpty(previewItems)) {
            return LegalAuditPreviewMergeResult.empty(formal);
        }
        List<LegalAiAuditOpinionItemBO> safeFormal = formal != null ? formal : List.of();
        Map<String, LegalAiAuditOpinionItemBO> mergedMap = new LinkedHashMap<>();
        Map<String, LegalAiAuditOpinionItemBO> formalByKey = new LinkedHashMap<>();

        for (LegalAiAuditOpinionItemBO item : safeFormal) {
            String key = dedupeKey(item);
            formalByKey.put(key, item);
            mergedMap.putIfAbsent(key, item);
        }

        int dedupeCount = 0;
        int reusedFromPreview = 0;
        for (LegalAiAuditOpinionItemBO preview : previewItems) {
            String key = dedupeKey(preview);
            if (formalByKey.containsKey(key)) {
                dedupeCount++;
                continue;
            }
            if (!mergedMap.containsKey(key)) {
                mergedMap.put(key, preview);
                reusedFromPreview++;
            }
        }

        return LegalAuditPreviewMergeResult.builder()
                .items(new ArrayList<>(mergedMap.values()))
                .formalCount(safeFormal.size())
                .previewCount(previewItems.size())
                .dedupeCount(dedupeCount)
                .reusedFromPreviewCount(reusedFromPreview)
                .build();
    }

    private static boolean policyMatches(LegalOrchestrationSessionDO session, LegalContractDO contract) {
        LegalAiPolicyBO policy = JsonUtils.parseObject(session.getPolicyJson(), LegalAiPolicyBO.class);
        if (policy != null) {
            return Objects.equals(policy.getModelId(), contract.getModelId())
                    && StrUtil.equalsIgnoreCase(policy.getPartyRole(), contract.getPartyRole())
                    && StrUtil.equalsIgnoreCase(policy.getAuditLevel(), contract.getAuditLevel());
        }
        return Objects.equals(session.getModelId(), contract.getModelId())
                && StrUtil.equalsIgnoreCase(session.getPartyRole(), contract.getPartyRole())
                && StrUtil.equalsIgnoreCase(session.getAuditLevel(), contract.getAuditLevel());
    }

    private static List<LegalAiAuditOpinionItemBO> toOpinionItems(
            List<LegalOrchestrationAuditPreviewItemBO> previews) {
        List<LegalAiAuditOpinionItemBO> items = new ArrayList<>(previews.size());
        for (LegalOrchestrationAuditPreviewItemBO preview : previews) {
            LegalAiAuditOpinionItemBO item = new LegalAiAuditOpinionItemBO();
            item.setTitle(StrUtil.blankToDefault(preview.getTitle(), "审核意见"));
            item.setRiskLevel(preview.getRiskLevel());
            item.setClauseType(preview.getClauseType());
            item.setParagraphId(preview.getParagraphId());
            item.setContent(StrUtil.blankToDefault(preview.getContent(),
                    "编排预览意见；正式审核未重复检出同类问题。"));
            item.setSuggestion(preview.getSuggestion());
            item.setSourceType(StrUtil.blankToDefault(preview.getSourceType(),
                    LegalOpinionSourceTypeEnum.PREVIEW.getCode()));
            items.add(item);
        }
        return items;
    }

    static String dedupeKey(LegalAiAuditOpinionItemBO item) {
        return StrUtil.blankToDefault(item.getParagraphId(), "")
                + "|"
                + normalizeTitle(item.getTitle());
    }

    private static String normalizeTitle(String title) {
        return StrUtil.blankToDefault(title, "").trim().toLowerCase();
    }

}
