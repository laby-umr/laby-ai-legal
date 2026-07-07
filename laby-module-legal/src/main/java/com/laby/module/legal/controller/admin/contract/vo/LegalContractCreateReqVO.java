package com.laby.module.legal.controller.admin.contract.vo;

import com.laby.framework.common.validation.InEnum;
import com.laby.module.legal.enums.contract.LegalAuditLevelEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 法务合同创建 Request VO")
@Data
public class LegalContractCreateReqVO {

    @Schema(description = "合同标题", required = true)
    @NotBlank(message = "合同标题不能为空")
    private String title;

    @Schema(description = "合同类型编号")
    private Long contractTypeId;

    @Schema(description = "技能包快照 JSON（编排冻结，可选）")
    private String skillPackSnapshotJson;

    @Schema(description = "我方立场 A/B/OTHER", required = true)
    @NotBlank(message = "我方立场不能为空")
    @Pattern(regexp = "^(A|B|OTHER)$", message = "我方立场必须是 A/B/OTHER")
    private String partyRole;

    @Schema(description = "审核强度", required = true)
    @NotBlank(message = "审核强度不能为空")
    @InEnum(LegalAuditLevelEnum.class)
    private String auditLevel;

    @Schema(description = "大模型编号")
    private Long modelId;

    @Schema(description = "首轮审核角色编号（ai_chat_role）")
    private Long auditRoleId;

    @Schema(description = "二轮审核角色编号（可选）")
    private Long reauditRoleId;

    @Schema(description = "是否可编辑", required = true)
    @NotNull(message = "是否可编辑不能为空")
    private Boolean editable;

    @Schema(description = "合同文件", required = true)
    @NotEmpty(message = "请上传合同文件")
    @Valid
    private List<LegalContractFileItemVO> files;

    @Schema(description = "发起人自选审批人")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Data
    public static class LegalContractFileItemVO {
        @NotNull(message = "文件编号不能为空")
        private Long fileId;
        @NotBlank(message = "文件名不能为空")
        private String fileName;
        private Boolean mainFlag;
    }

}
