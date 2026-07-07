package com.laby.module.legal.tool.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.service.auditrule.LegalAuditContextService;
import com.laby.module.legal.service.auditrule.bo.LegalAuditContextResult;
import com.laby.module.legal.service.contract.LegalContractService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索法务知识库（contractId 来自 RuntimeContext）。
 */
@Component("legal_search_knowledge")
public class LegalSearchKnowledgeTool {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;
    private static final int MAX_QUERY_CHARS = 500;

    @Resource
    private LegalContractService contractService;
    @Resource
    private LegalAuditContextService auditContextService;

    @Data
    @JsonClassDescription("按问题检索法务知识库片段")
    public static class Request {

        @JsonPropertyDescription("检索问题，必填")
        private String query;

        @JsonPropertyDescription("返回条数，默认 5")
        private Integer topK;
    }

    @Data
    public static class Response {
        private List<KnowledgeItem> segments = new ArrayList<>();
    }

    @Data
    public static class KnowledgeItem {
        private Long segmentId;
        private Long documentId;
        private Double score;
        private String excerpt;
    }

    @Tool(name = "legal_search_knowledge",
            description = "按问题检索法务知识库片段",
            readOnly = true, concurrencySafe = true)
    public Response searchKnowledge(
            @ToolParam(name = "query", description = "检索问题，必填") String query,
            @ToolParam(name = "topK", description = "返回条数，默认 5", required = false) Integer topK,
            LegalAgentToolRuntimeContext toolContext) {
        Request request = new Request();
        request.setQuery(query);
        request.setTopK(topK);
        return doSearch(request, toolContext);
    }

    private Response doSearch(Request request, LegalAgentToolRuntimeContext toolContext) {
        LegalContractDO contract = LegalAgentToolSupport.requireContract(contractService, toolContext);
        Response response = new Response();
        String query = StrUtil.sub(StrUtil.trim(request.getQuery()), 0, MAX_QUERY_CHARS);
        if (StrUtil.isBlank(query)) {
            return response;
        }
        LegalAuditContextResult context = auditContextService.buildChatKnowledgeContext(contract, query);
        if (CollUtil.isEmpty(context.getKnowledgeSegments())) {
            return response;
        }
        int topK = request.getTopK() == null ? DEFAULT_TOP_K : Math.min(request.getTopK(), MAX_TOP_K);
        int count = 0;
        for (LegalAuditContextResult.KnowledgeRef ref : context.getKnowledgeSegments()) {
            if (count++ >= topK) {
                break;
            }
            KnowledgeItem item = new KnowledgeItem();
            item.setSegmentId(ref.getSegmentId());
            item.setDocumentId(ref.getDocumentId());
            item.setScore(ref.getScore());
            item.setExcerpt(ref.getExcerpt());
            response.getSegments().add(item);
        }
        return response;
    }

}
