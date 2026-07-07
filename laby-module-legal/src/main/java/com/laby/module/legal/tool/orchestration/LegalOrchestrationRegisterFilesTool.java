package com.laby.module.legal.tool.orchestration;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("legal_orchestration_register_files")
public class LegalOrchestrationRegisterFilesTool {

    @Resource
    private LegalOrchestrationSessionService sessionService;

    @Data
    public static class FileItemInput {
        private Long infraFileId;
        private String fileName;
    }

    @Data
    public static class Response {
        private Long sessionId;
        private List<Long> fileItemIds;
        private String message;
    }

    @Tool(name = "legal_orchestration_register_files",
            description = "登记 AI 对话附件为编排文件项，需传入 infra 文件编号与文件名")
    public Response registerFiles(
            @ToolParam(name = "infraFileIds", description = "infra 文件编号列表") List<Long> infraFileIds,
            @ToolParam(name = "fileNames", description = "文件名列表，与 infraFileIds 一一对应", required = false)
            List<String> fileNames,
            LegalOrchestrationToolRuntimeContext toolContext) {
        Long conversationId = LegalOrchestrationToolSupport.requireConversationId(toolContext);
        Long userId = LegalOrchestrationToolSupport.requireUserId(toolContext);

        LegalOrchestrationSessionDO session = sessionService.getOrCreateSession(
                conversationId, userId, toolContext != null ? toolContext.getModelId() : null);
        List<String> names = CollUtil.isEmpty(fileNames) ? new ArrayList<>() : fileNames;
        List<LegalOrchestrationFileItemDO> items = sessionService.registerFiles(session.getId(), infraFileIds, names);

        Response response = new Response();
        response.setSessionId(session.getId());
        response.setFileItemIds(items.stream().map(LegalOrchestrationFileItemDO::getId).toList());
        response.setMessage("已登记 " + items.size() + " 个文件，sessionId=" + session.getId());
        return response;
    }

}
