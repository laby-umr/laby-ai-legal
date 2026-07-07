package com.laby.module.legal.service.orchestration.bo;

import lombok.Data;

@Data
public class LegalOrchestrationClassificationItemBO {

    private Long fileItemId;

    private String fileName;

    private Long suggestedTypeId;

    private String suggestedTypeName;

    private String suggestedTypeCode;

    private String reason;

    private boolean needNewType;

}
