package com.laby.module.ai.framework.agentscope.rag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.rag.AiEmbeddingClient;
import com.laby.module.ai.core.rag.AiVectorDocument;
import com.laby.module.ai.core.rag.AiVectorPointInfo;
import com.laby.module.ai.core.rag.AiVectorSearchHit;
import com.laby.module.ai.core.rag.AiVectorSearchRequest;
import com.laby.module.ai.core.rag.AiVectorStoreClient;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
public class QdrantVectorStoreClient implements AiVectorStoreClient {

    private final QdrantClient qdrantClient;
    private final AiEmbeddingClient embeddingClient;
    private final QdrantVectorStoreProperties properties;
    private final String collectionName;
    private final String contentFieldName;

    private volatile boolean collectionEnsured;

    public QdrantVectorStoreClient(QdrantVectorStoreProperties properties, AiEmbeddingClient embeddingClient) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
        this.collectionName = properties.getCollectionName();
        this.contentFieldName = properties.getContentFieldName();
        this.qdrantClient = buildQdrantClient(properties);
    }

    @Override
    @SneakyThrows
    public String add(List<AiVectorDocument> docs) {
        if (CollUtil.isEmpty(docs)) {
            return "";
        }
        List<String> texts = docs.stream().map(AiVectorDocument::getContent).toList();
        List<float[]> embeddings = embeddingClient.embedBatch(texts);
        if (embeddings.size() != docs.size()) {
            throw new IllegalStateException(StrUtil.format(
                    "Embedding count mismatch: docs={} embeddings={}", docs.size(), embeddings.size()));
        }
        ensureCollection(embeddings.get(0).length);

        List<String> ids = new ArrayList<>(docs.size());
        List<PointStruct> points = new ArrayList<>(docs.size());
        for (int i = 0; i < docs.size(); i++) {
            AiVectorDocument doc = docs.get(i);
            String id = StrUtil.blankToDefault(doc.getId(), UUID.randomUUID().toString());
            doc.setId(id);
            ids.add(id);

            Map<String, Value> payload = toPayload(doc);
            points.add(PointStruct.newBuilder()
                    .setId(PointIdFactory.id(UUID.fromString(id)))
                    .setVectors(VectorsFactory.vectors(embeddings.get(i)))
                    .putAllPayload(payload)
                    .build());
        }
        qdrantClient.upsertAsync(collectionName, points).get();
        return String.join(",", ids);
    }

    @Override
    @SneakyThrows
    public void delete(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        var pointIds = ids.stream()
                .map(id -> PointIdFactory.id(UUID.fromString(id)))
                .toList();
        qdrantClient.deleteAsync(collectionName, pointIds).get();
    }

    @Override
    @SneakyThrows
    public List<AiVectorSearchHit> search(AiVectorSearchRequest req) {
        float[] queryEmbedding = embeddingClient.embed(req.getQuery());
        ensureCollection(queryEmbedding.length);

        Filter filter = buildMetadataFilter(req.getMetadataEquals());
        SearchPoints searchPoints = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .setLimit(req.getTopK())
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .addAllVector(toFloatList(queryEmbedding))
                .setFilter(filter)
                .build();

        List<ScoredPoint> results = qdrantClient.searchAsync(searchPoints).get();
        return results.stream().map(this::toHit).toList();
    }

    @Override
    @SneakyThrows
    public List<AiVectorPointInfo> retrievePoints(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        Map<String, AiVectorPointInfo> resultMap = new LinkedHashMap<>();
        for (String id : ids) {
            resultMap.put(id, new AiVectorPointInfo().setId(id).setExists(false));
        }
        List<String> validIds = ids.stream().filter(QdrantVectorStoreClient::isValidUuid).toList();
        if (CollUtil.isEmpty(validIds)) {
            return new ArrayList<>(resultMap.values());
        }
        var pointIds = validIds.stream()
                .map(id -> PointIdFactory.id(UUID.fromString(id)))
                .toList();
        List<RetrievedPoint> retrieved = qdrantClient.retrieveAsync(
                collectionName,
                pointIds,
                true,
                false,
                null).get();
        if (CollUtil.isNotEmpty(retrieved)) {
            for (RetrievedPoint point : retrieved) {
                String id = point.getId().getUuid();
                AiVectorPointInfo info = resultMap.get(id);
                if (info == null) {
                    info = new AiVectorPointInfo().setId(id);
                    resultMap.put(id, info);
                }
                info.setExists(true);
                info.setMetadata(payloadToMetadata(point.getPayloadMap()));
            }
        }
        return ids.stream().map(resultMap::get).toList();
    }

    private Map<String, String> payloadToMetadata(Map<String, Value> payloadMap) {
        Map<String, String> metadata = new HashMap<>();
        if (payloadMap == null) {
            return metadata;
        }
        for (Map.Entry<String, Value> entry : payloadMap.entrySet()) {
            if (contentFieldName.equals(entry.getKey())) {
                continue;
            }
            metadata.put(entry.getKey(), valueToString(entry.getValue()));
        }
        return metadata;
    }

    private static boolean isValidUuid(String id) {
        if (StrUtil.isBlank(id)) {
            return false;
        }
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private AiVectorSearchHit toHit(ScoredPoint point) {
        Map<String, Value> payloadMap = point.getPayloadMap();
        Map<String, String> metadata = new HashMap<>();
        String content = null;
        for (Map.Entry<String, Value> entry : payloadMap.entrySet()) {
            if (contentFieldName.equals(entry.getKey())) {
                content = valueToString(entry.getValue());
            } else {
                metadata.put(entry.getKey(), valueToString(entry.getValue()));
            }
        }
        return new AiVectorSearchHit()
                .setId(point.getId().getUuid())
                .setScore((double) point.getScore())
                .setMetadata(metadata)
                .setContent(content);
    }

    private Map<String, Value> toPayload(AiVectorDocument doc) {
        Map<String, Value> payload = new HashMap<>();
        if (doc.getMetadata() != null) {
            doc.getMetadata().forEach((key, value) -> payload.put(key, ValueFactory.value(value)));
        }
        payload.put(contentFieldName, ValueFactory.value(doc.getContent()));
        return payload;
    }

    private Filter buildMetadataFilter(Map<String, String> metadataEquals) {
        if (metadataEquals == null || metadataEquals.isEmpty()) {
            return Filter.getDefaultInstance();
        }
        Filter.Builder builder = Filter.newBuilder();
        metadataEquals.forEach((key, value) ->
                builder.addMust(ConditionFactory.matchKeyword(key, value)));
        return builder.build();
    }

    private synchronized void ensureCollection(int vectorSize) {
        if (collectionEnsured) {
            return;
        }
        try {
            List<String> collections = qdrantClient.listCollectionsAsync().get();
            if (!collections.contains(collectionName)) {
                if (!properties.isInitializeSchema()) {
                    throw new IllegalStateException("Qdrant collection does not exist: " + collectionName);
                }
                VectorParams vectorParams = VectorParams.newBuilder()
                        .setDistance(Distance.Cosine)
                        .setSize(vectorSize)
                        .build();
                qdrantClient.createCollectionAsync(collectionName, vectorParams).get();
                log.info("[ensureCollection][创建 Qdrant 集合 {} size={}]", collectionName, vectorSize);
            }
            collectionEnsured = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static QdrantClient buildQdrantClient(QdrantVectorStoreProperties properties) {
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(
                properties.getHost(), properties.getPort(), properties.isUseTls());
        if (StrUtil.isNotEmpty(properties.getApiKey())) {
            grpcClientBuilder.withApiKey(properties.getApiKey());
        }
        return new QdrantClient(grpcClientBuilder.build());
    }

    private static List<Float> toFloatList(float[] embedding) {
        List<Float> vector = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            vector.add(value);
        }
        return vector;
    }

    private static String valueToString(Value value) {
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case INTEGER_VALUE -> String.valueOf(value.getIntegerValue());
            case DOUBLE_VALUE -> String.valueOf(value.getDoubleValue());
            case BOOL_VALUE -> String.valueOf(value.getBoolValue());
            default -> value.toString();
        };
    }

}
