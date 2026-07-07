package com.laby.module.legal.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 用户事实记忆 Response VO")
@Data
public class LegalUserFactRespVO {

    private Long id;

    private Long userId;

    private Long contractId;

    private String sessionId;

    private String content;

    private Long sourceMessageId;

    private LocalDateTime createTime;

}
