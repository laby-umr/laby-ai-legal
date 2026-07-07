package com.laby.module.legal.dal.mysql.standardclause;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClausePageReqVO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface LegalStandardClauseMapper extends BaseMapperX<LegalStandardClauseDO> {

    default PageResult<LegalStandardClauseDO> selectPage(LegalStandardClausePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalStandardClauseDO>()
                .likeIfPresent(LegalStandardClauseDO::getName, reqVO.getName())
                .eqIfPresent(LegalStandardClauseDO::getClauseType, reqVO.getClauseType())
                .eqIfPresent(LegalStandardClauseDO::getCategoryScope, reqVO.getCategoryScope())
                .eqIfPresent(LegalStandardClauseDO::getStatus, reqVO.getStatus())
                .orderByAsc(LegalStandardClauseDO::getSort)
                .orderByDesc(LegalStandardClauseDO::getId));
    }

    default List<LegalStandardClauseDO> selectListByIds(Collection<Long> ids) {
        return selectList(LegalStandardClauseDO::getId, ids);
    }

    default LegalStandardClauseDO selectByName(String name) {
        return selectOne(LegalStandardClauseDO::getName, name);
    }

}
