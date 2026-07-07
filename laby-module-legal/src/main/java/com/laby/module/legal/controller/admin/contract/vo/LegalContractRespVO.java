package com.laby.module.legal.controller.admin.contract.vo;

import com.laby.framework.excel.core.annotations.DictFormat;
import com.laby.module.legal.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 法务合同 Response VO")
@Data
public class LegalContractRespVO {

    private Long id;
    private String title;
    private Long contractTypeId;

    @DictFormat(DictTypeConstants.LEGAL_PARTY_ROLE)
    private String partyRole;

    @DictFormat(DictTypeConstants.LEGAL_AUDIT_LEVEL)
    private String auditLevel;

    private Long modelId;
    private Boolean editable;

    @DictFormat(DictTypeConstants.LEGAL_CONTRACT_STATUS)
    private Integer status;

    @Schema(description = "业务状态名称")
    private String statusName;
    private Integer bpmStatus;
    private String processInstanceId;
    private String currentTaskKey;
    @Schema(description = "是否可处置意见（采纳/保存/二轮）")
    private Boolean opinionEditable;
    @Schema(description = "是否可申请二轮 AI")
    private Boolean secondRoundApplicable;
    @Schema(description = "是否可保存处置/完成复核")
    private Boolean opinionCompletable;
    @Schema(description = "是否显示审核/办理入口")
    private Boolean reviewActionVisible;
    @Schema(description = "是否可发起首轮 AI 审核（AI 对话创建且已解析）")
    private Boolean startAuditVisible;
    @Schema(description = "是否可重试处理流水线")
    private Boolean retryVisible;
    @Schema(description = "处理失败原因（status=处理失败时）")
    private String failReason;
    @Schema(description = "是否已有 AI 审核报告（任一轮次）")
    private Boolean hasAuditReport;
    @Schema(description = "AI 审核意见条数")
    private Integer auditOpinionCount;
    @Schema(description = "最近一份报告的 AI 轮次")
    private Integer latestAuditReportRound;
    private Integer auditRound;
    private Boolean needSecondRound;
    private String feedbackSummary;
    private Integer riskHighCount;
    private Long mainFileId;

    @DictFormat(DictTypeConstants.LEGAL_CONTRACT_SOURCE_FORMAT)
    private String sourceFormat;

    private Integer parseStatus;
    private Long userId;

    @Schema(description = "创建来源 MANUAL/AI_CHAT")
    private String createSource;

    @Schema(description = "AI 对话来源编号")
    private Long createConversationId;

    private LocalDateTime createTime;

    private List<LegalContractFileRespVO> files;

    @Data
    public static class LegalContractFileRespVO {
        private Long id;
        private Long fileId;
        private String fileName;
        private Boolean mainFlag;
        @Schema(description = "文件角色：ORIGINAL 为用户上传；其余为系统衍生件")
        private String role;
        @Schema(description = "文件格式：DOCX / DOC / PDF")
        private String format;
        @Schema(description = "文件访问地址")
        private String url;
    }

}
