package com.laby.module.legal.controller.admin.contracttype.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "合同类型 - 配置检查项")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractTypeConfigCheckItemVO {

    @Schema(description = "检查项键")
    private String key;

    @Schema(description = "展示标签")
    private String label;

    @Schema(description = "是否满足")
    private Boolean ok;

    @Schema(description = "说明")
    private String hint;

}
