package com.laby.module.ai.service.eval.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索用例期望
 */
@Data
public class AiRagEvalExpectationBO {

    /** 期望出现在 TopK 中的 segmentId（任一命中即 Hit@K） */
    private List<Long> expectedSegmentIds = new ArrayList<>();

    /** 期望 TopK 结果内容包含的关键词（任一命中即可） */
    private List<String> expectedContentContains = new ArrayList<>();

    /** Recall@K 最低比例（0~1），默认有 expectedSegmentIds 时为 0.5 */
    private Double minRecallAtK;

    /** Top1 结果最低相似度分数 */
    private Double minTopScore;

}
