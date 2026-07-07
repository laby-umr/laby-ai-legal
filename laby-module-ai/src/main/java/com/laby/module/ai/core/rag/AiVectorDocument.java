package com.laby.module.ai.core.rag;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AiVectorDocument {

    /** 可为空，写入时自动生成 UUID */
    private String id;
    private String content;
    private Map<String, String> metadata = new HashMap<>();

}
