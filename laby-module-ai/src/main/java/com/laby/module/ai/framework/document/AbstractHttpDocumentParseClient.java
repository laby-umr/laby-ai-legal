package com.laby.module.ai.framework.document;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laby.module.ai.core.document.AiStructuredDocument;
import com.laby.module.ai.core.document.AiStructuredDocumentElement;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * HTTP 文档解析客户端基类（MinerU / Docling 归一化 JSON 协议）
 */
public abstract class AbstractHttpDocumentParseClient implements DocumentParseClient {

    protected final DocumentParseProperties properties;

    protected AbstractHttpDocumentParseClient(DocumentParseProperties properties) {
        this.properties = properties;
    }

    protected abstract DocumentParseProperties.EngineConfig engineConfig();

    protected abstract AiDocumentParseEngineEnum engineEnum();

    protected abstract AiDocumentParseQualityEnum defaultQuality();

    @Override
    public boolean isAvailable() {
        DocumentParseProperties.EngineConfig config = engineConfig();
        return config.isEnabled() && StrUtil.isNotBlank(config.getBaseUrl());
    }

    @Override
    public AiStructuredDocumentParseResult parse(byte[] bytes, String fileName) {
        DocumentParseProperties.EngineConfig config = engineConfig();
        String url = StrUtil.removeSuffix(config.getBaseUrl(), "/") + config.getParsePath();
        JSONObject requestBody = new JSONObject()
                .set("fileName", fileName)
                .set("contentBase64", Base64.getEncoder().encodeToString(bytes));
        HttpResponse response = HttpRequest.post(url)
                .body(requestBody.toString())
                .contentType("application/json")
                .timeout(config.getTimeoutMs())
                .execute();
        if (!response.isOk()) {
            throw new IllegalStateException("HTTP " + response.getStatus() + ": " + response.body());
        }
        return parseResponseJson(response.body());
    }

    protected AiStructuredDocumentParseResult parseResponseJson(String body) {
        JSONObject json = JSONUtil.parseObj(body);
        AiStructuredDocumentParseResult result = new AiStructuredDocumentParseResult()
                .setMarkdown(json.getStr("markdown", ""))
                .setEngine(engineEnum())
                .setQuality(AiDocumentParseQualityEnum.valueOfCode(
                        json.getStr("quality", defaultQuality().getCode())));
        if (result.getQuality() == AiDocumentParseQualityEnum.LOW) {
            result.setEngine(AiDocumentParseEngineEnum.TIKA);
        }
        JSONArray elements = json.getJSONArray("elements");
        if (elements != null && !elements.isEmpty()) {
            List<AiStructuredDocumentElement> elementList = new ArrayList<>();
            for (Object item : elements) {
                if (!(item instanceof JSONObject elementJson)) {
                    continue;
                }
                elementList.add(new AiStructuredDocumentElement()
                        .setType(elementJson.getStr("type"))
                        .setText(elementJson.getStr("text"))
                        .setMarkdown(elementJson.getStr("markdown"))
                        .setHtml(elementJson.getStr("html"))
                        .setCaption(elementJson.getStr("caption"))
                        .setDescription(elementJson.getStr("description"))
                        .setImageUrl(elementJson.getStr("imageUrl"))
                        .setPage(elementJson.getInt("page"))
                        .setLevel(elementJson.getInt("level")));
            }
            result.setStructuredDocument(new AiStructuredDocument().setElements(elementList));
        }
        return result;
    }

}
