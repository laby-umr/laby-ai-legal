package com.laby.module.legal.service.orchestration;

import com.laby.module.legal.service.ai.LegalAiChatFacade;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationResolvedAttachmentBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalOrchestrationAttachmentServiceTest {

    @InjectMocks
    private LegalOrchestrationAttachmentService attachmentService;

    @Mock
    private LegalAiChatFacade legalAiChatFacade;

    @Test
    void resolveLatestUserAttachments_noUrls_returnsEmpty() {
        when(legalAiChatFacade.listLatestUserAttachmentUrls(10L)).thenReturn(List.of());

        assertTrue(attachmentService.resolveLatestUserAttachments(10L).isEmpty());
        verify(legalAiChatFacade).listLatestUserAttachmentUrls(10L);
    }

    @Test
    void resolveUrls_blankSkipped() {
        List<LegalOrchestrationResolvedAttachmentBO> result =
                attachmentService.resolveUrls(List.of(" ", ""));
        assertEquals(0, result.size());
    }

}
