package com.laby.module.legal.controller.admin.agent.vo;

import com.laby.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.laby.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - Agent 步骤日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalAgentStepLogPageReqVO extends PageParam {

    @Schema(description = "合同编号")
    private Long contractId;

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "会话编号")
    private String sessionId;

    @Schema(description = "步骤类型 LLM/TOOL/ERROR")
    private String stepType;

    @Schema(description = "Tool 名称")
    private String toolName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
