package com.laby.module.ai.dal.mysql.knowledge;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.ai.controller.admin.knowledge.vo.knowledge.AiKnowledgePageReqVO;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI 知识库 Mapper
 *
 * @author xiaoxin
 */
@Mapper
public interface AiKnowledgeMapper extends BaseMapperX<AiKnowledgeDO> {

    default PageResult<AiKnowledgeDO> selectPage(AiKnowledgePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<AiKnowledgeDO>()
                .likeIfPresent(AiKnowledgeDO::getName, pageReqVO.getName())
                .eqIfPresent(AiKnowledgeDO::getStatus, pageReqVO.getStatus())
                .betweenIfPresent(AiKnowledgeDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(AiKnowledgeDO::getId));
    }

    default List<AiKnowledgeDO> selectListByStatus(Integer status) {
        return selectList(AiKnowledgeDO::getStatus, status);
    }

}
