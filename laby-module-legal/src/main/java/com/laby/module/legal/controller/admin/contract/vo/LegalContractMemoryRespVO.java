package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 合同情节记忆 Response VO")
@Data
public class LegalContractMemoryRespVO {

    private Long id;

    private Long contractId;

    private String sessionId;

    private String memoryType;

    private String content;

    private Long sourceMessageId;

    private LocalDateTime createTime;

}
