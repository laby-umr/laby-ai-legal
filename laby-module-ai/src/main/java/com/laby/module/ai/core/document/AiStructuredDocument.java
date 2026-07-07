package com.laby.module.ai.core.document;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化文档（解析中间态）
 */
@Data
@Accessors(chain = true)
public class AiStructuredDocument {

    private List<AiStructuredDocumentElement> elements = new ArrayList<>();

    public boolean hasStructuredElements() {
        return elements != null && !elements.isEmpty();
    }

}
