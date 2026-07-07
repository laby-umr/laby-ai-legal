package com.laby.module.legal.tool.orchestration;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationAttachmentService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationResolvedAttachmentBO;
import io.agentscope.core.tool.Tool;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component("legal_orchestration_register_latest_attachments")
public class LegalOrchestrationRegisterLatestAttachmentsTool {

    @Resource
    private LegalOrchestrationAttachmentService attachmentService;
    @Resource
    private LegalOrchestrationSessionService sessionService;

    @Data
    public static class Response {
        private Long sessionId;
        private List<Long> fileItemIds;
        private List<String> unresolvedUrls;
        private String message;
    }

    @Tool(name = "legal_orchestration_register_latest_attachments",
            description = "登记当前对话最近一条用户消息中的附件为编排文件项（优先于手动传 fileId）")
    public Response registerLatestAttachments(LegalOrchestrationToolRuntimeContext toolContext) {
        Long conversationId = LegalOrchestrationToolSupport.requireConversationId(toolContext);
        Long userId = LegalOrchestrationToolSupport.requireUserId(toolContext);

        List<LegalOrchestrationResolvedAttachmentBO> attachments =
                attachmentService.resolveLatestUserAttachments(conversationId);
        LegalOrchestrationSessionDO session = sessionService.getOrCreateSession(
                conversationId, userId, toolContext != null ? toolContext.getModelId() : null);

        List<Long> fileIds = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (LegalOrchestrationResolvedAttachmentBO item : attachments) {
            if (item.getInfraFileId() != null) {
                fileIds.add(item.getInfraFileId());
                fileNames.add(item.getFileName());
            } else {
                unresolved.add(item.getUrl());
            }
        }

        List<LegalOrchestrationFileItemDO> created = CollUtil.isEmpty(fileIds)
                ? List.of()
                : sessionService.registerFiles(session.getId(), fileIds, fileNames);

        Response response = new Response();
        response.setSessionId(session.getId());
        response.setFileItemIds(created.stream().map(LegalOrchestrationFileItemDO::getId).collect(Collectors.toList()));
        response.setUnresolvedUrls(unresolved);
        response.setMessage(CollUtil.isEmpty(created)
                ? "未解析到可登记附件，请确认用户已上传合同文件"
                : "已登记 " + created.size() + " 个附件，sessionId=" + session.getId());
        return response;
    }

}
