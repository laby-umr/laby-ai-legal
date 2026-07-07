package com.laby.module.ai.framework.document;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocumentParseProperties.class)
public class DocumentParseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TikaDocumentParseClient tikaDocumentParseClient() {
        return new TikaDocumentParseClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpMinerUDocumentParseClient httpMinerUDocumentParseClient(DocumentParseProperties properties) {
        return new HttpMinerUDocumentParseClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpDoclingDocumentParseClient httpDoclingDocumentParseClient(DocumentParseProperties properties) {
        return new HttpDoclingDocumentParseClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public HtmlStructuredDocumentParseClient htmlStructuredDocumentParseClient() {
        return new HtmlStructuredDocumentParseClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpreadsheetDocumentParseClient spreadsheetDocumentParseClient() {
        return new SpreadsheetDocumentParseClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailDocumentParseClient emailDocumentParseClient() {
        return new EmailDocumentParseClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentParseRouter documentParseRouter(DocumentParseProperties properties,
                                                   TikaDocumentParseClient tikaClient,
                                                   HttpMinerUDocumentParseClient mineruClient,
                                                   HttpDoclingDocumentParseClient doclingClient,
                                                   HtmlStructuredDocumentParseClient htmlClient,
                                                   SpreadsheetDocumentParseClient spreadsheetClient,
                                                   EmailDocumentParseClient emailClient) {
        return new DocumentParseRouter(properties, tikaClient, mineruClient, doclingClient,
                htmlClient, spreadsheetClient, emailClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentTypeChunkRouter documentTypeChunkRouter(DocumentParseProperties properties) {
        return new DocumentTypeChunkRouter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentParseService documentParseService(DocumentParseRouter router) {
        return new DocumentParseService(router);
    }

}
