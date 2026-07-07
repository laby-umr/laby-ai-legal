package com.laby.module.ai.core.llm;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AiLlmRequest {
    private List<AiMessage> messages = new ArrayList<>();
    private Double temperature;
    private Integer maxTokens;
    /** 审核 Pipeline 要求 JSON 数组输出 */
    private boolean jsonMode;
    private Map<String, Object> metadata = new HashMap<>();
}
