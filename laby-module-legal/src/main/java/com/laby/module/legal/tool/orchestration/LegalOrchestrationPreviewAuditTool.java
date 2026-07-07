package com.laby.module.legal.tool.orchestration;

import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationSessionDO;
import com.laby.module.legal.service.orchestration.LegalOrchestrationPreviewAuditService;
import com.laby.module.legal.service.orchestration.LegalOrchestrationSessionService;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationAuditPreviewSnapshotBO;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component("legal_orchestration_preview_audit")
public class LegalOrchestrationPreviewAuditTool {

    @Resource
    private LegalOrchestrationPreviewAuditService previewAuditService;
    @Resource
    private LegalOrchestrationSessionService sessionService;

    @Data
    public static class Response {
        private int fileCount;
        private int totalOpinions;
        private int highRiskCount;
        private String message;
        private LegalOrchestrationAuditPreviewSnapshotBO snapshot;
    }

    @Tool(name = "legal_orchestration_preview_audit",
            description = "对已分类的编排文件执行 AI 预览审核（不写合同/正式意见表）。用户询问风险时必须先调用本工具。"
                    + "sessionId 取自登记/分类工具返回的 sessionId（非 conversationId），可省略。")
    public Response previewAudit(
            @ToolParam(name = "sessionId", description = "编排会话编号（register/classify 返回值；可省略）", required = false) Long sessionId,
            @ToolParam(name = "fileItemId", description = "指定文件项编号，空则审核全部已分类文件", required = false) Long fileItemId,
            LegalOrchestrationToolRuntimeContext toolContext) {
        LegalOrchestrationToolRuntimeContext ctx = LegalOrchestrationToolSupport.resolve(toolContext);
        LegalOrchestrationSessionDO session = LegalOrchestrationToolSupport.resolveOrchestrationSession(
                sessionService, sessionId, ctx);
        LegalOrchestrationAuditPreviewSnapshotBO snapshot = previewAuditService.preview(
                session.getId(), fileItemId, ctx != null ? ctx.getModelId() : null);

        int totalOpinions = 0;
        int highRisk = 0;
        if (snapshot.getFiles() != null) {
            for (LegalOrchestrationAuditPreviewSnapshotBO.FilePreview file : snapshot.getFiles()) {
                totalOpinions += file.getOpinionCount() != null ? file.getOpinionCount() : 0;
                highRisk += file.getHighRiskCount() != null ? file.getHighRiskCount() : 0;
            }
        }

        Response response = new Response();
        response.setFileCount(snapshot.getFiles() != null ? snapshot.getFiles().size() : 0);
        response.setTotalOpinions(totalOpinions);
        response.setHighRiskCount(highRisk);
        response.setSnapshot(snapshot);
        response.setMessage("预览审核完成：共 " + totalOpinions + " 条意见（高风险 " + highRisk
                + " 条）。此为预览结果，正式意见以创建合同后的审核流程为准。");
        return response;
    }

}
