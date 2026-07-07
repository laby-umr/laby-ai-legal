package com.laby.module.legal.service.ai;

import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.enums.model.AiModelTypeEnum;
import com.laby.module.ai.service.model.AiModelService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 法务链路大模型解析：合同 modelId → 校验模型，缺省时回退默认 CHAT 模型。
 */
@Component
public class LegalAiModelResolver {

    @Resource
    private AiModelService aiModelService;

    /**
     * 严格解析：modelId 非空时校验存在性（审核链路）。
     */
    public AiModelDO requireChatModel(Long modelId) {
        if (modelId != null) {
            return aiModelService.validateModel(modelId);
        }
        return aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
    }

    /**
     * 宽松解析：仅接受 CHAT 类型，否则回退默认聊天模型（合同问答链路）。
     */
    public AiModelDO resolveChatModel(Long modelId) {
        if (modelId != null) {
            AiModelDO model = aiModelService.getModel(modelId);
            if (model != null && AiModelTypeEnum.CHAT.getType().equals(model.getType())) {
                return model;
            }
        }
        return aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
    }

    /**
     * 宽松解析：任意类型模型存在即用，否则回退默认（Agent 链路）。
     */
    public AiModelDO resolveModelOrDefault(Long modelId) {
        if (modelId != null) {
            AiModelDO model = aiModelService.getModel(modelId);
            if (model != null) {
                return model;
            }
        }
        return aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
    }

    /**
     * 是否使用了默认模型回退（合同未指定 modelId 或指定模型不可用后回退）。
     */
    public static boolean isModelFallback(Long requestedModelId, Long resolvedModelId) {
        if (requestedModelId == null) {
            return true;
        }
        return !requestedModelId.equals(resolvedModelId);
    }

}
