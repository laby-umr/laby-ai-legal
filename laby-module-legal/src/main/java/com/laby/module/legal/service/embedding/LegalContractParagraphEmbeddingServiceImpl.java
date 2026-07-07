package com.laby.module.legal.service.embedding;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.ai.core.rag.AiVectorDocument;
import com.laby.module.ai.core.rag.AiVectorSearchHit;
import com.laby.module.ai.core.rag.AiVectorSearchRequest;
import com.laby.module.ai.core.rag.AiVectorStoreClient;
import com.laby.module.ai.core.rag.AiVectorStoreMetadataKeys;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiModelTypeEnum;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.dataobject.embedding.LegalContractParagraphEmbeddingDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.dal.mysql.embedding.LegalContractParagraphEmbeddingMapper;
import com.laby.module.legal.enums.contract.LegalParagraphEmbeddingStatusEnum;
import com.laby.module.legal.service.embedding.bo.LegalParagraphVectorHitBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 法务合同段落向量索引 Service 实现
 */
@Slf4j
@Service
public class LegalContractParagraphEmbeddingServiceImpl implements LegalContractParagraphEmbeddingService {

    private static final String METADATA_CONTRACT_ID = "contractId";
    private static final String METADATA_PARAGRAPH_ID = "paragraphId";
    private static final String METADATA_TENANT_ID = "tenantId";

    private static final int CONTENT_PREVIEW_MAX_CHARS = 200;

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalContractParagraphEmbeddingMapper embeddingMapper;
    @Resource
    private AiModelService aiModelService;

    /**
     * 可选：指定 Embedding 模型 ID；未配置时使用 AI 模块默认 EMBEDDING 模型
     */
    @Value("${laby.legal.paragraph-embedding-model-id:}")
    private Long paragraphEmbeddingModelId;

    @Override
    @Async
    public void embedContractAsync(Long contractId) {
        LegalContractDO contract = TenantUtils.executeIgnore(() -> contractMapper.selectById(contractId));
        if (contract == null) {
            return;
        }
        TenantUtils.execute(contract.getTenantId(), () -> doEmbedContract(contractId, contract.getTenantId()));
    }

    @Override
    public List<LegalParagraphVectorHitBO> searchByVector(Long contractId, String query, int limit) {
        if (contractId == null || StrUtil.isBlank(query) || limit <= 0) {
            return List.of();
        }
        AiVectorStoreClient vectorStoreClient = resolveVectorStore();
        if (vectorStoreClient == null) {
            return List.of();
        }
        try {
            AiVectorSearchRequest searchRequest = new AiVectorSearchRequest()
                    .setQuery(query.trim())
                    .setTopK(limit)
                    .setMetadataEquals(Map.of(METADATA_CONTRACT_ID, contractId.toString()));
            List<AiVectorSearchHit> hits = vectorStoreClient.search(searchRequest);
            if (CollUtil.isEmpty(hits)) {
                return List.of();
            }
            List<LegalParagraphVectorHitBO> results = new ArrayList<>();
            for (AiVectorSearchHit hit : hits) {
                String paragraphId = extractParagraphId(hit);
                if (StrUtil.isBlank(paragraphId)) {
                    continue;
                }
                LegalParagraphVectorHitBO result = new LegalParagraphVectorHitBO();
                result.setParagraphId(paragraphId);
                result.setScore(hit.getScore());
                results.add(result);
            }
            results.sort(Comparator.comparing(LegalParagraphVectorHitBO::getScore,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return results;
        } catch (Exception ex) {
            log.warn("[searchByVector][contractId={}] 向量检索失败，将降级关键词匹配", contractId, ex);
            return List.of();
        }
    }

    @Override
    public void deleteByContractId(Long contractId) {
        if (contractId == null) {
            return;
        }
        List<LegalContractParagraphEmbeddingDO> embeddings = embeddingMapper.selectListByContractId(contractId);
        if (CollUtil.isEmpty(embeddings)) {
            return;
        }
        AiVectorStoreClient vectorStoreClient = resolveVectorStore();
        if (vectorStoreClient != null) {
            List<String> vectorIds = embeddings.stream()
                    .map(LegalContractParagraphEmbeddingDO::getVectorId)
                    .filter(StrUtil::isNotBlank)
                    .filter(vectorId -> !LegalContractParagraphEmbeddingDO.VECTOR_ID_EMPTY.equals(vectorId))
                    .toList();
            if (CollUtil.isNotEmpty(vectorIds)) {
                try {
                    vectorStoreClient.delete(vectorIds);
                } catch (Exception ex) {
                    log.warn("[deleteByContractId][contractId={}] 删除向量库记录失败", contractId, ex);
                }
            }
        }
        embeddingMapper.deleteByContractId(contractId);
    }

    @Override
    public boolean hasEmbeddings(Long contractId) {
        return contractId != null && embeddingMapper.countSuccessByContractId(contractId) > 0;
    }

    private void doEmbedContract(Long contractId, Long tenantId) {
        AiVectorStoreClient vectorStoreClient = resolveVectorStore();
        if (vectorStoreClient == null) {
            return;
        }
        deleteByContractId(contractId);

        List<LegalContractParagraphDO> paragraphs = paragraphMapper.selectListByContractId(contractId);
        if (CollUtil.isEmpty(paragraphs)) {
            return;
        }
        int success = 0;
        for (LegalContractParagraphDO paragraph : paragraphs) {
            if (StrUtil.isBlank(paragraph.getText())) {
                continue;
            }
            LegalContractParagraphEmbeddingDO record = LegalContractParagraphEmbeddingDO.builder()
                    .contractId(contractId)
                    .paragraphId(paragraph.getParagraphId())
                    .paragraphDbId(paragraph.getId())
                    .vectorId(LegalContractParagraphEmbeddingDO.VECTOR_ID_EMPTY)
                    .textHash(DigestUtil.md5Hex(paragraph.getText()))
                    .contentPreview(StrUtil.sub(paragraph.getText(), 0, CONTENT_PREVIEW_MAX_CHARS))
                    .status(LegalParagraphEmbeddingStatusEnum.PENDING.getStatus())
                    .build();
            embeddingMapper.insert(record);
            try {
                writeVectorStore(vectorStoreClient, record, paragraph, tenantId);
                success++;
            } catch (Exception ex) {
                log.warn("[doEmbedContract][contractId={}][paragraphId={}] 向量化失败",
                        contractId, paragraph.getParagraphId(), ex);
                embeddingMapper.updateById(new LegalContractParagraphEmbeddingDO()
                        .setId(record.getId())
                        .setStatus(LegalParagraphEmbeddingStatusEnum.FAILED.getStatus()));
            }
        }
        log.info("[doEmbedContract][contractId={}] 段落向量化完成，成功 {}/{}",
                contractId, success, paragraphs.size());
    }

    private void writeVectorStore(AiVectorStoreClient vectorStoreClient, LegalContractParagraphEmbeddingDO record,
                                  LegalContractParagraphDO paragraph, Long tenantId) {
        Long embeddingModelId = resolveEmbeddingModelId();
        Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_CONTRACT_ID, record.getContractId().toString());
        metadata.put(METADATA_PARAGRAPH_ID, paragraph.getParagraphId());
        metadata.put(METADATA_TENANT_ID, tenantId.toString());
        if (embeddingModelId != null) {
            metadata.put(AiVectorStoreMetadataKeys.EMBEDDING_MODEL_ID, embeddingModelId.toString());
        }
        metadata.put(AiVectorStoreMetadataKeys.INDEXED_AT, LocalDateTime.now().toString());
        AiVectorDocument document = new AiVectorDocument()
                .setContent(paragraph.getText())
                .setMetadata(metadata);
        String vectorId = vectorStoreClient.add(List.of(document));

        embeddingMapper.updateById(new LegalContractParagraphEmbeddingDO()
                .setId(record.getId())
                .setVectorId(vectorId)
                .setStatus(LegalParagraphEmbeddingStatusEnum.SUCCESS.getStatus()));
    }

    private AiVectorStoreClient resolveVectorStore() {
        try {
            Long modelId = resolveEmbeddingModelId();
            if (modelId == null) {
                log.warn("[resolveVectorStore] 未配置默认 EMBEDDING 模型，跳过段落向量化");
                return null;
            }
            return aiModelService.getVectorStoreClient(modelId);
        } catch (Exception ex) {
            log.warn("[resolveVectorStore] 获取向量库失败，跳过段落向量化", ex);
            return null;
        }
    }

    private Long resolveEmbeddingModelId() {
        if (paragraphEmbeddingModelId != null && paragraphEmbeddingModelId > 0) {
            return paragraphEmbeddingModelId;
        }
        AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.EMBEDDING.getType());
        return model != null ? model.getId() : null;
    }

    private static String extractParagraphId(AiVectorSearchHit hit) {
        if (hit == null || hit.getMetadata() == null) {
            return null;
        }
        return hit.getMetadata().get(METADATA_PARAGRAPH_ID);
    }

}
