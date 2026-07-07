package com.laby.module.ai.core.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AiVectorPointInfo {

    private String id;
    private boolean exists;
    private Map<String, String> metadata = new HashMap<>();

}
