package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "审阅工作台 - 导航节点")
@Data
public class LegalContractWorkbenchNavigationNodeVO {

    private String id;

    private String label;

    private Integer level;

    @Schema(description = "关联段落 ID")
    private List<String> paragraphIds = new ArrayList<>();

    private List<LegalContractWorkbenchNavigationNodeVO> children = new ArrayList<>();

}
