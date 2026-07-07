package com.laby.module.ai.service.knowledge.bo;

import com.laby.module.ai.framework.knowledge.retrieval.bo.RecallDiagnostics;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库段落检索结果（含可选诊断）
 */
@Data
@Accessors(chain = true)
public class AiKnowledgeSegmentSearchResultBO {

    private List<AiKnowledgeSegmentSearchRespBO> segments = new ArrayList<>();

    private RecallDiagnostics diagnostics;

}
