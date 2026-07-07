package com.laby.module.ai.service.model;



import com.laby.module.ai.core.image.AiImageClient;

import com.laby.module.ai.core.llm.AiLlmClient;

import com.laby.module.ai.core.rag.AiEmbeddingClient;

import com.laby.module.ai.core.rag.AiVectorStoreClient;

import com.laby.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;

import com.laby.module.ai.framework.ai.core.model.suno.api.SunoApi;

import com.laby.framework.common.pojo.PageResult;

import com.laby.module.ai.controller.admin.model.vo.model.AiModelPageReqVO;

import com.laby.module.ai.controller.admin.model.vo.model.AiModelSaveReqVO;

import com.laby.module.ai.dal.dataobject.model.AiModelDO;

import dev.tinyflow.core.Tinyflow;

import jakarta.validation.Valid;



import javax.annotation.Nullable;

import java.util.List;



/**

 * AI 模型 Service 接口

 *

 * @author fansili

 * @since 2024/4/24 19:42

 */

public interface AiModelService {



    /**

     * 创建模型

     *

     * @param createReqVO 创建信息

     * @return 编号

     */

    Long createModel(@Valid AiModelSaveReqVO createReqVO);



    /**

     * 更新模型

     *

     * @param updateReqVO 更新信息

     */

    void updateModel(@Valid AiModelSaveReqVO updateReqVO);



    /**

     * 删除模型

     *

     * @param id 编号

     */

    void deleteModel(Long id);



    /**

     * 获得模型

     *

     * @param id 编号

     * @return 模型

     */

    AiModelDO getModel(Long id);



    /**

     * 获得默认的模型

     *

     * 如果获取不到，则抛出 {@link com.laby.framework.common.exception.ServiceException} 业务异常

     *

     * @return 模型

     */

    AiModelDO getRequiredDefaultModel(Integer type);



    /**

     * 获得模型分页

     *

     * @param pageReqVO 分页查询

     * @return 模型分页

     */

    PageResult<AiModelDO> getModelPage(AiModelPageReqVO pageReqVO);



    /**

     * 校验模型是否可使用

     *

     * @param id 编号

     * @return 模型

     */

    AiModelDO validateModel(Long id);



    /**

     * 获得模型列表

     *

     * @param status 状态

     * @param type 类型

     * @param platform 平台，允许空

     * @return 模型列表

     */

    List<AiModelDO> getModelListByStatusAndType(Integer status, Integer type,

                                                @Nullable String platform);



    /**

     * 获取 AgentScope 驱动的 LLM 客户端（新运行时）

     *

     * @param modelId 模型编号

     * @return LLM 客户端

     */

    AiLlmClient getLlmClient(Long modelId);



    /**

     * 获取 Embedding 客户端（新运行时）

     *

     * @param modelId 向量模型编号

     * @return Embedding 客户端

     */

    AiEmbeddingClient getEmbeddingClient(Long modelId);



    /**

     * 获取向量存储客户端（Qdrant 直连）

     *

     * @param embeddingModelId 向量模型编号

     * @return 向量存储客户端

     */

    AiVectorStoreClient getVectorStoreClient(Long embeddingModelId);



    /**

     * 获取图像生成客户端（新运行时）

     *

     * @param modelId 模型编号

     * @return 图像客户端

     */

    AiImageClient getImageClient(Long modelId);



    /**

     * 获得 MidjourneyApi 对象

     *

     * @param id 编号

     * @return MidjourneyApi 对象

     */

    MidjourneyApi getMidjourneyApi(Long id);



    /**

     * 获得 SunoApi 对象

     *

     * @return SunoApi 对象

     */

    SunoApi getSunoApi();



    /**

     * 获取 TinyFlow 所需 LLm Provider

     *

     * @param tinyflow tinyflow

     * @param modelId AI 模型 ID

     */

    void getLLmProvider4Tinyflow(Tinyflow tinyflow, Long modelId);



}

