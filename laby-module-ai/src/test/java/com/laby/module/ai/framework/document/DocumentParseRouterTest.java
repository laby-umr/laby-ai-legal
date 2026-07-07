package com.laby.module.ai.framework.document;



import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;

import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;

import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;

import org.junit.jupiter.api.Test;



import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;



class DocumentParseRouterTest {



    @Test

    void resolveClient_pdfUsesMinerUWhenEnabled() {

        DocumentParseProperties properties = baseProperties();

        properties.getMineru().setEnabled(true);

        DocumentParseRouter router = newRouter(properties);



        assertEquals(AiDocumentParseEngineEnum.MINERU, router.resolveClient("contract.pdf").engine());

    }



    @Test

    void resolveClient_pdfUsesDoclingWhenMinerUDisabled() {

        DocumentParseProperties properties = baseProperties();

        properties.getDocling().setEnabled(true);

        DocumentParseRouter router = newRouter(properties);



        assertEquals(AiDocumentParseEngineEnum.DOCLING, router.resolveClient("policy.pdf").engine());

    }



    @Test

    void parse_fallsBackToTikaWhenHttpFails() {

        DocumentParseProperties properties = baseProperties();

        properties.getMineru().setEnabled(true);

        properties.getMineru().setBaseUrl("http://127.0.0.1:1");

        DocumentParseRouter router = newRouter(properties);



        byte[] bytes = "hello pdf".getBytes();

        AiStructuredDocumentParseResult result = router.parse(bytes, "sample.pdf");



        assertEquals(AiDocumentParseEngineEnum.TIKA, result.getEngine());

        assertEquals(AiDocumentParseQualityEnum.LOW, result.getQuality());

        assertTrue(result.isDegraded());

    }



    private static DocumentParseRouter newRouter(DocumentParseProperties properties) {

        return new DocumentParseRouter(

                properties,

                new TikaDocumentParseClient(),

                new HttpMinerUDocumentParseClient(properties),

                new HttpDoclingDocumentParseClient(properties),

                new HtmlStructuredDocumentParseClient(),

                new SpreadsheetDocumentParseClient(),

                new EmailDocumentParseClient());

    }



    private static DocumentParseProperties baseProperties() {

        DocumentParseProperties properties = new DocumentParseProperties();

        properties.setEnabled(true);

        properties.setDefaultEngine(AiDocumentParseEngineEnum.AUTO.getCode());

        return properties;

    }



}

