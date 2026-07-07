package com.laby.module.legal.service.ai;

import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiModelTypeEnum;
import com.laby.module.ai.service.model.AiModelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalAiModelResolverTest {

    @Mock
    private AiModelService aiModelService;

    @InjectMocks
    private LegalAiModelResolver resolver;

    @Test
    void requireChatModel_shouldValidateWhenIdPresent() {
        AiModelDO model = new AiModelDO().setId(42L).setType(AiModelTypeEnum.CHAT.getType());
        when(aiModelService.validateModel(42L)).thenReturn(model);

        assertEquals(42L, resolver.requireChatModel(42L).getId());
        verify(aiModelService).validateModel(42L);
    }

    @Test
    void resolveChatModel_shouldFallbackWhenNotChatType() {
        AiModelDO embedding = new AiModelDO().setId(7L).setType(AiModelTypeEnum.EMBEDDING.getType());
        AiModelDO defaultChat = new AiModelDO().setId(1L).setType(AiModelTypeEnum.CHAT.getType());
        when(aiModelService.getModel(7L)).thenReturn(embedding);
        when(aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType())).thenReturn(defaultChat);

        assertEquals(1L, resolver.resolveChatModel(7L).getId());
    }

    @Test
    void isModelFallback_shouldDetectNullRequestedId() {
        assertTrue(LegalAiModelResolver.isModelFallback(null, 1L));
    }

}
