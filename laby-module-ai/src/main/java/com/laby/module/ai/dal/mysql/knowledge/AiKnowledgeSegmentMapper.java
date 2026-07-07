package com.laby.module.ai.dal.mysql.knowledge;

import cn.hutool.core.collection.CollUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentPageReqVO;
import com.laby.module.ai.controller.admin.knowledge.vo.segment.AiKnowledgeSegmentProcessRespVO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeSegmentDO;
import com.laby.module.ai.dal.mysql.knowledge.dto.AiKnowledgeSparseSearchRow;
import com.laby.module.ai.enums.knowledge.AiKnowledgeSegmentBlockTypeEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * AI 知识库分片 Mapper
 *
 * @author xiaoxin
 */
@Mapper
public interface AiKnowledgeSegmentMapper extends BaseMapperX<AiKnowledgeSegmentDO> {

    default PageResult<AiKnowledgeSegmentDO> selectPage(AiKnowledgeSegmentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                .eq(AiKnowledgeSegmentDO::getDocumentId, reqVO.getDocumentId())
                .likeIfPresent(AiKnowledgeSegmentDO::getContent, reqVO.getContent())
                .eqIfPresent(AiKnowledgeSegmentDO::getStatus, reqVO.getStatus())
                .orderByDesc(AiKnowledgeSegmentDO::getId));
    }

    default List<AiKnowledgeSegmentDO> selectListByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                .in(AiKnowledgeSegmentDO::getId, ids));
    }

    default AiKnowledgeSegmentDO selectTableWholeSegment(Long documentId, String headingPath) {
        return selectOne(new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                .eq(AiKnowledgeSegmentDO::getDocumentId, documentId)
                .eq(AiKnowledgeSegmentDO::getBlockType, AiKnowledgeSegmentBlockTypeEnum.TABLE_WHOLE.getCode())
                .eq(AiKnowledgeSegmentDO::getHeadingPath, headingPath)
                .orderByDesc(AiKnowledgeSegmentDO::getId)
                .last("LIMIT 1"));
    }

    default List<AiKnowledgeSegmentDO> selectListByVectorIds(List<String> vectorIds) {
        return selectList(new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                .in(AiKnowledgeSegmentDO::getVectorId, vectorIds)
                .orderByDesc(AiKnowledgeSegmentDO::getId));
    }

    default List<AiKnowledgeSegmentDO> selectListByDocumentId(Long documentId) {
        return selectList(new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                .eq(AiKnowledgeSegmentDO::getDocumentId, documentId)
                .orderByDesc(AiKnowledgeSegmentDO::getId));
    }

    default List<AiKnowledgeSegmentDO> selectListByDocumentIdAndStatus(Long documentId, Integer status) {
        return selectList(new LambdaQueryWrapperX<AiKnowledgeSegmentDO>()
                .eq(AiKnowledgeSegmentDO::getDocumentId, documentId)
                .eq(AiKnowledgeSegmentDO::getStatus, status)
                .orderByDesc(AiKnowledgeSegmentDO::getId));
    }

    default List<AiKnowledgeSegmentDO> selectListByKnowledgeIdAndStatus(Long knowledgeId, Integer status) {
        return selectList(AiKnowledgeSegmentDO::getKnowledgeId, knowledgeId,
                AiKnowledgeSegmentDO::getStatus, status);
    }

    default List<AiKnowledgeSegmentProcessRespVO> selectProcessList(Collection<Long> documentIds) {
        MPJLambdaWrapper<AiKnowledgeSegmentDO> wrapper = new MPJLambdaWrapperX<AiKnowledgeSegmentDO>()
                .selectAs(AiKnowledgeSegmentDO::getDocumentId, AiKnowledgeSegmentProcessRespVO::getDocumentId)
                .selectCount(AiKnowledgeSegmentDO::getId, "count")
                .select("COUNT(CASE WHEN vector_id IS NOT NULL AND vector_id <> '" + AiKnowledgeSegmentDO.VECTOR_ID_EMPTY + "' THEN 1 ELSE NULL END) AS embeddingCount")
                .in(AiKnowledgeSegmentDO::getDocumentId, documentIds)
                .groupBy(AiKnowledgeSegmentDO::getDocumentId);
        return selectJoinList(AiKnowledgeSegmentProcessRespVO.class, wrapper);
    }

    default void updateRetrievalCountIncrByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        update(new LambdaUpdateWrapper<AiKnowledgeSegmentDO>()
                .setSql(" retrieval_count = retrieval_count + 1")
                .in(AiKnowledgeSegmentDO::getId, ids));
    }

    @Select("""
            SELECT id,
                   MATCH(sparse_text, embed_text, content) AGAINST (#{query} IN NATURAL LANGUAGE MODE) AS sparse_score
            FROM ai_knowledge_segment
            WHERE knowledge_id = #{knowledgeId}
              AND deleted = 0
              AND status = 0
              AND MATCH(sparse_text, embed_text, content) AGAINST (#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY sparse_score DESC
            LIMIT #{limit}
            """)
    List<AiKnowledgeSparseSearchRow> selectListBySparseSearch(@Param("knowledgeId") Long knowledgeId,
                                                              @Param("query") String query,
                                                              @Param("limit") int limit);

    /**
     * sparse_text 单列 FULLTEXT 降级检索（无组合索引时使用）
     */
    @Select("""
            SELECT id,
                   MATCH(sparse_text) AGAINST (#{query} IN NATURAL LANGUAGE MODE) AS sparse_score
            FROM ai_knowledge_segment
            WHERE knowledge_id = #{knowledgeId}
              AND deleted = 0
              AND status = 0
              AND MATCH(sparse_text) AGAINST (#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY sparse_score DESC
            LIMIT #{limit}
            """)
    List<AiKnowledgeSparseSearchRow> selectListBySparseTextOnlySearch(@Param("knowledgeId") Long knowledgeId,
                                                                    @Param("query") String query,
                                                                    @Param("limit") int limit);

}
