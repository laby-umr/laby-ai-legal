package com.laby.module.ai.service.model;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.image.AiImageClient;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.rag.AiEmbeddingClient;
import com.laby.module.ai.core.rag.AiVectorStoreClient;
import com.laby.module.ai.framework.agentscope.image.AgentScopeImageClientFactory;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.module.ai.framework.agentscope.config.AgentScopeProperties;
import com.laby.module.ai.framework.agentscope.model.AgentScopeLlmClient;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelConfig;
import com.laby.module.ai.framework.agentscope.model.AgentScopeModelFactory;
import com.laby.module.ai.framework.agentscope.rag.AgentScopeEmbeddingClientFactory;
import com.laby.module.ai.framework.agentscope.rag.QdrantVectorStoreClient;
import com.laby.module.ai.framework.agentscope.rag.QdrantVectorStoreProperties;
import com.laby.module.ai.framework.ai.config.LabyAiProperties;
import com.laby.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;
import com.laby.module.ai.framework.ai.core.model.suno.api.SunoApi;
import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.model.vo.model.AiModelPageReqVO;
import com.laby.module.ai.controller.admin.model.vo.model.AiModelSaveReqVO;
import com.laby.module.ai.dal.dataobject.model.AiApiKeyDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.dal.mysql.model.AiChatMapper;
import com.laby.module.ai.framework.agentscope.chat.TinyflowLlmProviderFactory;
import dev.tinyflow.core.Tinyflow;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.ai.enums.ErrorCodeConstants.*;

/**
 * AI 模型 Service 实现类
 *
 * @author fansili
 */
@Service
@Validated
public class AiModelServiceImpl implements AiModelService {

    @Resource
    private AiApiKeyService apiKeyService;

    @Resource
    private AiChatMapper modelMapper;

    @Resource
    private AgentScopeProperties agentScopeProperties;

    @Resource
    private QdrantVectorStoreProperties qdrantVectorStoreProperties;

    @Resource
    private LabyAiProperties labyAiProperties;

    @Override
    public Long createModel(AiModelSaveReqVO createReqVO) {
        // 1. 校验
        AiPlatformEnum.validatePlatform(createReqVO.getPlatform());
        apiKeyService.validateApiKey(createReqVO.getKeyId());

        // 2. 插入
        AiModelDO model = BeanUtils.toBean(createReqVO, AiModelDO.class);
        modelMapper.insert(model);
        return model.getId();
    }

    @Override
    public void updateModel(AiModelSaveReqVO updateReqVO) {
        // 1. 校验
        validateModelExists(updateReqVO.getId());
        AiPlatformEnum.validatePlatform(updateReqVO.getPlatform());
        apiKeyService.validateApiKey(updateReqVO.getKeyId());

        // 2. 更新
        AiModelDO updateObj = BeanUtils.toBean(updateReqVO, AiModelDO.class);
        modelMapper.updateById(updateObj);
    }

    @Override
    public void deleteModel(Long id) {
        // 校验存在
        validateModelExists(id);
        // 删除
        modelMapper.deleteById(id);
    }

    private AiModelDO validateModelExists(Long id) {
        AiModelDO model = modelMapper.selectById(id);
        if (modelMapper.selectById(id) == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        return model;
    }

    @Override
    public AiModelDO getModel(Long id) {
        return modelMapper.selectById(id);
    }

    @Override
    public AiModelDO getRequiredDefaultModel(Integer type) {
        AiModelDO model = modelMapper.selectFirstByStatus(type, CommonStatusEnum.ENABLE.getStatus());
        if (model == null) {
            throw exception(MODEL_DEFAULT_NOT_EXISTS);
        }
        return model;
    }

    @Override
    public PageResult<AiModelDO> getModelPage(AiModelPageReqVO pageReqVO) {
        return modelMapper.selectPage(pageReqVO);
    }

    @Override
    public AiModelDO validateModel(Long id) {
        AiModelDO model = validateModelExists(id);
        if (CommonStatusEnum.isDisable(model.getStatus())) {
            throw exception(MODEL_DISABLE);
        }
        return model;
    }

    @Override
    public List<AiModelDO> getModelListByStatusAndType(Integer status, Integer type, String platform) {
        return modelMapper.selectListByStatusAndType(status, type, platform);
    }

    @Override
    public AiLlmClient getLlmClient(Long id) {
        AiModelDO model = validateModel(id);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        String cacheKey = buildClientCacheKey(AgentScopeLlmClient.class, id, apiKey.getId(), model.getModel());
        return Singleton.get(cacheKey, (cn.hutool.core.lang.func.Func0<AiLlmClient>) () -> {
            AgentScopeModelConfig config = AgentScopeModelFactory.from(model, apiKey);
            Path workspace = Paths.get(agentScopeProperties.getWorkspacePath(), "model-" + id);
            return new AgentScopeLlmClient(config, workspace);
        });
    }

    @Override
    public AiEmbeddingClient getEmbeddingClient(Long modelId) {
        AiModelDO model = validateModel(modelId);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        String cacheKey = buildClientCacheKey(AiEmbeddingClient.class, modelId, apiKey.getId(), model.getModel());
        return Singleton.get(cacheKey, (cn.hutool.core.lang.func.Func0<AiEmbeddingClient>) () ->
                AgentScopeEmbeddingClientFactory.build(model, apiKey));
    }

    @Override
    public AiVectorStoreClient getVectorStoreClient(Long embeddingModelId) {
        AiEmbeddingClient embeddingClient = getEmbeddingClient(embeddingModelId);
        String cacheKey = buildClientCacheKey(AiVectorStoreClient.class, embeddingModelId,
                qdrantVectorStoreProperties.getCollectionName());
        return Singleton.get(cacheKey, (cn.hutool.core.lang.func.Func0<AiVectorStoreClient>) () ->
                new QdrantVectorStoreClient(qdrantVectorStoreProperties, embeddingClient));
    }

    @Override
    public AiImageClient getImageClient(Long modelId) {
        AiModelDO model = validateModel(modelId);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        String cacheKey = buildClientCacheKey(AiImageClient.class, modelId, apiKey.getId(), model.getModel());
        return Singleton.get(cacheKey, (cn.hutool.core.lang.func.Func0<AiImageClient>) () ->
                AgentScopeImageClientFactory.build(model, apiKey));
    }

    @Override
    public MidjourneyApi getMidjourneyApi(Long id) {
        AiModelDO model = validateModel(id);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        String cacheKey = buildClientCacheKey(MidjourneyApi.class, id, apiKey.getId(), apiKey.getUrl());
        return Singleton.get(cacheKey, (cn.hutool.core.lang.func.Func0<MidjourneyApi>) () -> {
            LabyAiProperties.Midjourney properties = labyAiProperties.getMidjourney();
            String notifyUrl = properties != null ? properties.getNotifyUrl() : null;
            return new MidjourneyApi(apiKey.getUrl(), apiKey.getApiKey(), notifyUrl);
        });
    }

    @Override
    public SunoApi getSunoApi() {
        AiApiKeyDO apiKey = apiKeyService.getRequiredDefaultApiKey(
                AiPlatformEnum.SUNO.getPlatform(), CommonStatusEnum.ENABLE.getStatus());
        String cacheKey = buildClientCacheKey(SunoApi.class, apiKey.getId(), apiKey.getUrl());
        return Singleton.get(cacheKey, (cn.hutool.core.lang.func.Func0<SunoApi>) () -> new SunoApi(apiKey.getUrl()));
    }

    private static String buildClientCacheKey(Class<?> clazz, Object... params) {
        if (ArrayUtil.isEmpty(params)) {
            return clazz.getName();
        }
        return StrUtil.format("{}#{}", clazz.getName(), ArrayUtil.join(params, "_"));
    }

    @Override
    public void getLLmProvider4Tinyflow(Tinyflow tinyflow, Long modelId) {
        AiModelDO model = validateModel(modelId);
        AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
        tinyflow.setLlmProvider(TinyflowLlmProviderFactory.build(model, apiKey));
    }

}
