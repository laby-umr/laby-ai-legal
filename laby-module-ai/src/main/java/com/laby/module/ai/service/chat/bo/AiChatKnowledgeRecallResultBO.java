package com.laby.module.ai.service.chat.bo;

import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对话知识库召回结果（多知识库合并 + 诊断）
 */
@Data
@Accessors(chain = true)
public class AiChatKnowledgeRecallResultBO {

    private List<AiKnowledgeSegmentSearchRespBO> segments = new ArrayList<>();

    private Map<String, Object> recallDiagnostics;

}
