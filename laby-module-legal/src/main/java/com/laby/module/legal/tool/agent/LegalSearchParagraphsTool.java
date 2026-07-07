package com.laby.module.legal.tool.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.service.contract.LegalContractService;
import com.laby.module.legal.service.embedding.LegalContractParagraphEmbeddingService;
import com.laby.module.legal.service.embedding.bo.LegalParagraphVectorHitBO;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索合同段落（contractId 来自 RuntimeContext）。
 */
@Component("legal_search_paragraphs")
public class LegalSearchParagraphsTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final int MAX_TEXT_CHARS = 800;

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalContractParagraphEmbeddingService paragraphEmbeddingService;

    @Data
    @JsonClassDescription("搜索当前合同的段落正文")
    public static class Request {

        @JsonPropertyDescription("关键词，匹配段落正文或章节路径")
        private String keyword;

        @JsonPropertyDescription("精确段落编号，如 p-12")
        private String paragraphId;

        @JsonPropertyDescription("最多返回条数，默认 5，最大 10")
        private Integer limit;
    }

    @Data
    public static class Response {
        private List<ParagraphItem> items = new ArrayList<>();
    }

    @Data
    public static class ParagraphItem {
        private String paragraphId;
        private String path;
        private Integer sort;
        private String text;
        private Boolean skipAudit;
    }

    @Tool(name = "legal_search_paragraphs",
            description = "搜索当前合同的段落正文",
            readOnly = true, concurrencySafe = true)
    public Response searchParagraphs(
            @ToolParam(name = "keyword", description = "关键词，匹配段落正文或章节路径", required = false) String keyword,
            @ToolParam(name = "paragraphId", description = "精确段落编号，如 p-12", required = false) String paragraphId,
            @ToolParam(name = "limit", description = "最多返回条数，默认 5，最大 10", required = false) Integer limit,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setKeyword(keyword);
        request.setParagraphId(paragraphId);
        request.setLimit(limit);
        return doSearch(request, toolContext);
    }

    private Response doSearch(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalContractDO contract = LegalAgentToolSupport.requireContract(contractService, toolContext);
        int limit = request.getLimit() == null ? DEFAULT_LIMIT : Math.min(request.getLimit(), MAX_LIMIT);
        List<LegalContractParagraphDO> paragraphs = paragraphMapper.selectListByContractId(contract.getId());

        Response response = new Response();
        String keyword = StrUtil.trim(request.getKeyword());
        String paragraphId = StrUtil.trim(request.getParagraphId());

        if (StrUtil.isNotBlank(keyword) && StrUtil.isBlank(paragraphId)
                && paragraphEmbeddingService.hasEmbeddings(contract.getId())) {
            List<LegalParagraphVectorHitBO> vectorHits = paragraphEmbeddingService.searchByVector(
                    contract.getId(), keyword, limit);
            if (CollUtil.isNotEmpty(vectorHits)) {
                Map<String, LegalContractParagraphDO> paragraphById = paragraphs.stream()
                        .collect(Collectors.toMap(LegalContractParagraphDO::getParagraphId, p -> p,
                                (a, b) -> a, LinkedHashMap::new));
                for (LegalParagraphVectorHitBO hit : vectorHits) {
                    if (response.getItems().size() >= limit) {
                        break;
                    }
                    LegalContractParagraphDO paragraph = paragraphById.get(hit.getParagraphId());
                    if (paragraph != null) {
                        response.getItems().add(toParagraphItem(paragraph));
                    }
                }
                if (CollUtil.isNotEmpty(response.getItems())) {
                    return response;
                }
            }
        }

        for (LegalContractParagraphDO paragraph : paragraphs) {
            if (response.getItems().size() >= limit) {
                break;
            }
            if (StrUtil.isNotBlank(paragraphId)
                    && !StrUtil.equals(paragraphId, paragraph.getParagraphId())) {
                continue;
            }
            if (StrUtil.isNotBlank(keyword) && !matchesKeyword(paragraph, keyword)) {
                continue;
            }
            response.getItems().add(toParagraphItem(paragraph));
        }
        return response;
    }

    private static ParagraphItem toParagraphItem(LegalContractParagraphDO paragraph) {
        ParagraphItem item = new ParagraphItem();
        item.setParagraphId(paragraph.getParagraphId());
        item.setPath(paragraph.getPath());
        item.setSort(paragraph.getSort());
        item.setText(StrUtil.sub(StrUtil.blankToDefault(paragraph.getText(), ""), 0, MAX_TEXT_CHARS));
        item.setSkipAudit(paragraph.getSkipAudit());
        return item;
    }

    private static boolean matchesKeyword(LegalContractParagraphDO paragraph, String keyword) {
        String[] tokens = StrUtil.splitToArray(keyword.trim(), ' ');
        if (tokens.length == 0) {
            return true;
        }
        String text = StrUtil.blankToDefault(paragraph.getText(), "");
        String path = StrUtil.blankToDefault(paragraph.getPath(), "");
        String paragraphId = StrUtil.blankToDefault(paragraph.getParagraphId(), "");
        for (String token : tokens) {
            if (StrUtil.isBlank(token)) {
                continue;
            }
            String lower = token.toLowerCase();
            if (!StrUtil.containsIgnoreCase(text, lower)
                    && !StrUtil.containsIgnoreCase(path, lower)
                    && !StrUtil.equalsIgnoreCase(paragraphId, token)) {
                return false;
            }
        }
        return true;
    }

}
