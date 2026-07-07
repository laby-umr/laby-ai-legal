package com.laby.module.ai.service.knowledge;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentPageReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentProcessRespVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentSaveReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentUpdateStatusReqVO;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import com.laby.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchResultBO;
import org.springframework.scheduling.annotation.Async;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.laby.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * AI 知识库段落 Service 接口
 *
 * @author xiaoxin
 */
public interface AiKnowledgeSegmentService {

    /**
     * 获取知识库段落详情
     *
     * @param id 段落编号
     * @return 段落详情
     */
    AiKnowledgeSegmentDO getKnowledgeSegment(Long id);

    /**
     * 获取知识库段落列表
     *
     * @param ids 段落编号列表
     * @return 段落列表
     */
    List<AiKnowledgeSegmentDO> getKnowledgeSegmentList(Collection<Long> ids);

    /**
     * 获取知识库段落 Map
     *
     * @param ids 段落编号列表
     * @return 段落 Map
     */
    default Map<Long, AiKnowledgeSegmentDO> getKnowledgeSegmentMap(Collection<Long> ids) {
        return convertMap(getKnowledgeSegmentList(ids), AiKnowledgeSegmentDO::getId);
    }

    /**
     * 获取段落分页
     *
     * @param pageReqVO 分页查询
     * @return 文档分页
     */
    PageResult<AiKnowledgeSegmentDO> getKnowledgeSegmentPage(AiKnowledgeSegmentPageReqVO pageReqVO);

    /**
     * 基于 content 内容，切片创建多个段落
     *
     * @param documentId 知识库文档编号
     * @param content    文档内容
     */
    void createKnowledgeSegmentBySplitContent(Long documentId, String content);

    /**
     * 【异步】基于 content 内容，切片创建多个段落
     *
     * @param documentId 知识库文档编号
     * @param content    文档内容
     */
    void createKnowledgeSegmentBySplitContentAsync(Long documentId, String content);

    /**
     * 基于结构化解析结果切片并入库（高质量 PDF 路径）
     *
     * @param documentId  文档编号
     * @param parseResult 解析结果
     */
    void createKnowledgeSegmentByParseResult(Long documentId, AiStructuredDocumentParseResult parseResult);

    /**
     * 【异步】基于结构化解析结果切片并入库
     */
    void createKnowledgeSegmentByParseResultAsync(Long documentId, AiStructuredDocumentParseResult parseResult);

    /**
     * 创建知识库段落
     *
     * @param createReqVO 创建信息
     * @return 段落编号
     */
    Long createKnowledgeSegment(AiKnowledgeSegmentSaveReqVO createReqVO);

    /**
     * 更新段落的内容
     *
     * @param reqVO 更新内容
     */
    void updateKnowledgeSegment(AiKnowledgeSegmentSaveReqVO reqVO);

    /**
     * 更新段落的状态
     *
     * @param reqVO 更新内容
     */
    void updateKnowledgeSegmentStatus(AiKnowledgeSegmentUpdateStatusReqVO reqVO);

    /**
     * 删除知识库段落
     *
     * @param id 段落编号
     */
    void deleteKnowledgeSegment(Long id);

    /**
     * 重新索引知识库下的所有文档段落
     *
     * @param knowledgeId 知识库编号
     */
    void reindexKnowledgeSegmentByKnowledgeId(Long knowledgeId);

    /**
     * 【异步】重新索引知识库下的所有文档段落
     *
     * @param knowledgeId 知识库编号
     */
    @Async
    default void reindexByKnowledgeIdAsync(Long knowledgeId) {
        reindexKnowledgeSegmentByKnowledgeId(knowledgeId);
    }

    /**
     * 根据文档编号删除段落
     *
     * @param documentId 文档编号
     */
    void deleteKnowledgeSegmentByDocumentId(Long documentId);

    /**
     * 搜索知识库段落，并返回结果
     *
     * @param reqBO 搜索请求信息
     * @return 搜索结果段落列表
     */
    List<AiKnowledgeSegmentSearchRespBO> searchKnowledgeSegment(AiKnowledgeSegmentSearchReqBO reqBO);

    /**
     * 搜索知识库段落，并返回诊断信息（Universal RAG 开启时有效）
     *
     * @param reqBO 搜索请求信息
     * @return 搜索结果与诊断
     */
    AiKnowledgeSegmentSearchResultBO searchKnowledgeSegmentWithDiagnostics(AiKnowledgeSegmentSearchReqBO reqBO);

    /**
     * 根据 URL 内容，切片创建多个段落
     *
     * @param url              URL 地址
     * @param segmentMaxTokens 段落最大 Token 数
     * @return 切片后的段落列表
     */
    List<AiKnowledgeSegmentDO> splitContent(String url, Integer segmentMaxTokens);

    /**
     * 获取文档处理进度（多个）
     *
     * @param documentIds 文档编号列表
     * @return 文档处理列表
     */
    List<AiKnowledgeSegmentProcessRespVO> getKnowledgeSegmentProcessList(List<Long> documentIds);

    /**
     * 列出知识库下启用状态的段落（健康检查用）
     */
    List<AiKnowledgeSegmentDO> listEnabledSegments(Long knowledgeId);

    /**
     * 列出文档下启用状态的段落（健康检查用）
     */
    List<AiKnowledgeSegmentDO> listEnabledSegmentsByDocument(Long documentId);

    /**
     * 删除旧向量并按当前正文重新 embed（健康检查修复用）
     */
    void repairSegmentVector(Long segmentId);

    /**
     * 按 embedText/content 回填 sparse_text（健康检查修复用）
     */
    void repairSegmentSparseText(Long segmentId);

}
