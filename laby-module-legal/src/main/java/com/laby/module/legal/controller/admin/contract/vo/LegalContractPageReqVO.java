package com.laby.module.legal.controller.admin.contract.vo;

import com.laby.framework.common.pojo.PageParam;
import com.laby.framework.common.validation.InEnum;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.laby.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 法务合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LegalContractPageReqVO extends PageParam {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "业务状态")
    @InEnum(LegalContractStatusEnum.class)
    private Integer status;

    @Schema(description = "我方立场")
    @Pattern(regexp = "^(A|B|OTHER)$", message = "我方立场必须是 A/B/OTHER")
    private String partyRole;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
