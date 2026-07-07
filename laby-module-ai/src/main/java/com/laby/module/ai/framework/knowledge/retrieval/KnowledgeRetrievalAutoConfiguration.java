package com.laby.module.ai.framework.knowledge.retrieval;



import com.laby.module.ai.dal.mysql.knowledge.AiKnowledgeSegmentMapper;

import com.laby.module.ai.framework.document.DocumentParseProperties;

import com.laby.module.ai.service.knowledge.AiKnowledgeService;

import com.laby.module.ai.service.model.AiModelService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;



@Configuration

@ConditionalOnProperty(prefix = "laby.ai.knowledge-retrieval", name = "enabled", havingValue = "true")

public class KnowledgeRetrievalAutoConfiguration {



    @Bean

    @ConditionalOnMissingBean

    public QueryExpansionService queryExpansionService(KnowledgeRetrievalProperties properties) {

        return new QueryExpansionService(properties);

    }



    @Bean

    @ConditionalOnMissingBean

    public SparseRetrievalEngine sparseRetrievalEngine(AiKnowledgeSegmentMapper segmentMapper,

                                                       KnowledgeRetrievalProperties properties) {

        return new SparseRetrievalEngine(segmentMapper, properties);

    }



    @Bean

    @ConditionalOnMissingBean

    public RecallDiagnosticsCollector recallDiagnosticsCollector(KnowledgeRetrievalProperties properties) {

        return new RecallDiagnosticsCollector(properties);

    }



    @Bean

    @ConditionalOnMissingBean

    public NoAnswerGuard noAnswerGuard(KnowledgeRetrievalProperties properties) {

        return new NoAnswerGuard(properties);

    }



    @Bean

    @ConditionalOnMissingBean

    public AiKnowledgeRetrievalService aiKnowledgeRetrievalService(KnowledgeRetrievalProperties properties,

                                                                   AiKnowledgeService knowledgeService,

                                                                   AiModelService modelService,

                                                                   AiKnowledgeSegmentMapper segmentMapper,

                                                                   QueryExpansionService queryExpansionService,

                                                                   SparseRetrievalEngine sparseRetrievalEngine,

                                                                   RecallDiagnosticsCollector diagnosticsCollector,

                                                                   DocumentParseProperties documentParseProperties) {

        return new AiKnowledgeRetrievalServiceImpl(properties, knowledgeService, modelService, segmentMapper,

                queryExpansionService, sparseRetrievalEngine, diagnosticsCollector, documentParseProperties);

    }



}

