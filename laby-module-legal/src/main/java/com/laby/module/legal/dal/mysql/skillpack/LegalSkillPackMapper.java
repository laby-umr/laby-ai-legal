package com.laby.module.legal.dal.mysql.skillpack;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackPageReqVO;
import com.laby.module.legal.dal.dataobject.skillpack.LegalSkillPackDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalSkillPackMapper extends BaseMapperX<LegalSkillPackDO> {

    default LegalSkillPackDO selectByCode(String code) {
        return selectOne(LegalSkillPackDO::getCode, code);
    }

    default PageResult<LegalSkillPackDO> selectPage(LegalSkillPackPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalSkillPackDO>()
                .likeIfPresent(LegalSkillPackDO::getName, reqVO.getName())
                .eqIfPresent(LegalSkillPackDO::getCode, reqVO.getCode())
                .eqIfPresent(LegalSkillPackDO::getScene, reqVO.getScene())
                .eqIfPresent(LegalSkillPackDO::getEnabled, reqVO.getEnabled())
                .orderByDesc(LegalSkillPackDO::getId));
    }

    default List<LegalSkillPackDO> selectListBySceneAndEnabled(String scene, Boolean enabled) {
        return selectList(new LambdaQueryWrapperX<LegalSkillPackDO>()
                .eq(LegalSkillPackDO::getScene, scene)
                .eqIfPresent(LegalSkillPackDO::getEnabled, enabled)
                .orderByDesc(LegalSkillPackDO::getId));
    }

}
