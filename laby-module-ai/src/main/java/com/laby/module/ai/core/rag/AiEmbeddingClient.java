package com.laby.module.ai.core.rag;

import java.util.List;

public interface AiEmbeddingClient {

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);

}
