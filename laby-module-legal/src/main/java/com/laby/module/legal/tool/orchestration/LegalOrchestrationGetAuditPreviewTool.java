package com.laby.module.legal.tool.orchestration;

import cn.hutool.core.collection.CollUtil;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationPreviewAuditService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationAuditPreviewSnapshotBO;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component("legal_orchestration_get_audit_preview")
public class LegalOrchestrationGetAuditPreviewTool {

    @Resource
    private LegalOrchestrationPreviewAuditService previewAuditService;
    @Resource
    private LegalOrchestrationSessionService sessionService;

    @Data
    public static class Response {
        private LegalOrchestrationAuditPreviewSnapshotBO snapshot;
        private String message;
    }

    @Tool(name = "legal_orchestration_get_audit_preview",
            description = "读取当前编排会话已缓存的预览审核结果摘要（只读）。sessionId 可省略。")
    public Response getAuditPreview(
            @ToolParam(name = "sessionId", description = "编排会话编号（可省略，自动从当前对话解析）", required = false) Long sessionId,
            LegalOrchestrationToolRuntimeContext toolContext) {
        LegalOrchestrationToolRuntimeContext ctx = LegalOrchestrationToolSupport.resolve(toolContext);
        LegalOrchestrationSessionDO session = LegalOrchestrationToolSupport.resolveOrchestrationSession(
                sessionService, sessionId, ctx);
        LegalOrchestrationAuditPreviewSnapshotBO snapshot = previewAuditService.getSnapshot(session.getId());

        Response response = new Response();
        response.setSnapshot(snapshot);
        if (snapshot == null || CollUtil.isEmpty(snapshot.getFiles())) {
            response.setMessage("暂无预览审核缓存，请先调用 legal_orchestration_preview_audit。"
                    + " 预览基于 AI 建议分类即可，无需等待前端分类确认。");
            return response;
        }
        response.setMessage("已读取预览审核缓存，共 "
                + snapshot.getFiles().size() + " 个文件。正式意见以创建合同后的审核流程为准。");
        return response;
    }

}
