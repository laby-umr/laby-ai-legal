package com.laby.module.legal.service.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.retrieval.LegalRetrievalLogDO;
import com.laby.module.legal.dal.mysql.retrieval.LegalRetrievalLogMapper;
import com.laby.module.legal.service.auditrule.bo.LegalAuditContextResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LegalRetrievalLogService {

    public static final String BIZ_TYPE_AUDIT = "AUDIT";
    public static final String BIZ_TYPE_QA = "QA";

    @Resource
    private LegalRetrievalLogMapper retrievalLogMapper;

    public void logKnowledgeRetrieval(String bizType, Long bizId, Integer auditRound, Integer batchIndex,
                                      String query, int topK, LegalAuditContextResult context) {
        if (context == null || CollUtil.isEmpty(context.getKnowledgeSegments())) {
            return;
        }
        List<Long> chunkIds = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        for (LegalAuditContextResult.KnowledgeRef ref : context.getKnowledgeSegments()) {
            if (ref.getSegmentId() != null) {
                chunkIds.add(ref.getSegmentId());
                scores.add(ref.getScore());
            }
        }
        if (chunkIds.isEmpty()) {
            return;
        }
        retrievalLogMapper.insert(LegalRetrievalLogDO.builder()
                .bizType(bizType)
                .bizId(bizId)
                .auditRound(auditRound)
                .batchIndex(batchIndex)
                .query(StrUtil.sub(StrUtil.blankToDefault(query, context.getRagQuery()), 0, 800))
                .topK(topK)
                .retrievedChunkIds(JsonUtils.toJsonString(chunkIds))
                .rerankScores(JsonUtils.toJsonString(scores))
                .build());
    }

}
