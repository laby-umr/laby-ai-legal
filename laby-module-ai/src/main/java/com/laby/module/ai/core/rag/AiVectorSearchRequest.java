package com.laby.module.ai.core.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AiVectorSearchRequest {

    private String query;
    private int topK = 4;
    /** metadata 等值过滤，多条件 AND */
    private Map<String, String> metadataEquals = new HashMap<>();

}
