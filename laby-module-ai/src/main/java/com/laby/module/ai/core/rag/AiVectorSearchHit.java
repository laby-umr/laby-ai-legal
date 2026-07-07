package com.laby.module.ai.core.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AiVectorSearchHit {

    private String id;
    private Double score;
    private Map<String, String> metadata = new HashMap<>();
    private String content;

}
