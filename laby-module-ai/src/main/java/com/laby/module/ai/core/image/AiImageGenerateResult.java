package com.laby.module.ai.core.image;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AiImageGenerateResult {

    private String b64Json;
    private String url;

}
