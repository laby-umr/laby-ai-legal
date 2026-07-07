package com.laby.module.ai.service.knowledge;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentCreateListReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentPageReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentUpdateReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.document.AiKnowledgeDocumentUpdateStatusReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiKnowledgeDocumentCreateReqVO;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.laby.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * AI 知识库文档 Service 接口
 *
 * @author xiaoxin
 */
public interface AiKnowledgeDocumentService {

    /**
     * 创建文档（单个）
     *
     * @param createReqVO 文档创建 Request VO
     * @return 文档编号
     */
    Long createKnowledgeDocument(AiKnowledgeDocumentCreateReqVO createReqVO);

    /**
     * 创建文档（多个）
     *
     * @param createListReqVO 批量创建 Request VO
     * @return 文档编号列表
     */
    List<Long> createKnowledgeDocumentList(AiKnowledgeDocumentCreateListReqVO createListReqVO);

    /**
     * 获取文档分页
     *
     * @param pageReqVO 分页参数
     * @return 文档分页
     */
    PageResult<AiKnowledgeDocumentDO> getKnowledgeDocumentPage(AiKnowledgeDocumentPageReqVO pageReqVO);

    /**
     * 获取文档详情
     *
     * @param id 文档编号
     * @return 文档详情
     */
    AiKnowledgeDocumentDO getKnowledgeDocument(Long id);

    /**
     * 更新文档
     *
     * @param reqVO 更新信息
     */
    void updateKnowledgeDocument(AiKnowledgeDocumentUpdateReqVO reqVO);

    /**
     * 更新文档状态
     *
     * @param reqVO 更新状态信息
     */
    void updateKnowledgeDocumentStatus(AiKnowledgeDocumentUpdateStatusReqVO reqVO);

    /**
     * 删除文档
     *
     * @param id 文档编号
     */
    void deleteKnowledgeDocument(Long id);

    /**
     * 根据知识库编号，批量删除文档
     *
     * @param knowledgeId 知识库编号
     */
    void deleteKnowledgeDocumentByKnowledgeId(Long knowledgeId);

    /**
     * 校验文档是否存在
     *
     * @param id 文档编号
     * @return 文档信息
     */
    AiKnowledgeDocumentDO validateKnowledgeDocumentExists(Long id);

    /**
     * 重新入库（删除旧分段并异步分段+向量化）
     *
     * @param id 文档编号
     */
    void retryKnowledgeDocumentIngest(Long id);

    /**
     * 更新文档入库状态
     *
     * @param id     文档编号
     * @param status 入库状态
     * @param error  失败原因（成功/进行中可传 null）
     */
    void updateKnowledgeDocumentIngestStatus(Long id, Integer status, String error);

    /**
     * 读取 URL 内容
     *
     * @param url URL
     * @return 内容
     */
    String readUrl(String url);

    /**
     * 解析 URL 文档（结构化高质量路径）
     */
    AiStructuredDocumentParseResult parseDocumentUrl(String url);

    /**
     * 获取文档列表
     *
     * @param ids 文档编号列表
     * @return 文档列表
     */
    List<AiKnowledgeDocumentDO> getKnowledgeDocumentList(Collection<Long> ids);

    /**
     * 根据知识库编号获取文档列表
     *
     * @param knowledgeId 知识库编号
     * @return 文档列表
     */
    List<AiKnowledgeDocumentDO> getKnowledgeDocumentListByKnowledgeId(Long knowledgeId);

    /**
     * 获取文档 Map
     *
     * @param ids 文档编号列表
     * @return 文档 Map
     */
    default Map<Long, AiKnowledgeDocumentDO> getKnowledgeDocumentMap(Collection<Long> ids) {
        return convertMap(getKnowledgeDocumentList(ids), AiKnowledgeDocumentDO::getId);
    }

}
