package com.laby.module.ai.core.image;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class AiImageGenerateRequest {

    private String prompt;
    private Integer width;
    private Integer height;
    private String model;
    private Map<String, String> options = new HashMap<>();

}
