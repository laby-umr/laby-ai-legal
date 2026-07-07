package com.laby.module.legal.service.orchestration;

import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationClassificationItemBO;

import java.util.List;

/**
 * 法务编排语义分类 Service
 */
public interface LegalOrchestrationClassificationService {

    List<LegalOrchestrationClassificationItemBO> classifyFiles(Long sessionId, Long modelId);

}
