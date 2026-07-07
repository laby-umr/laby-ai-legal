package com.laby.module.legal.dal.mysql.auditrule;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRulePageReqVO;
import com.laby.module.legal.dal.dataobject.auditrule.LegalAuditRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalAuditRuleMapper extends BaseMapperX<LegalAuditRuleDO> {

    default PageResult<LegalAuditRuleDO> selectPage(LegalAuditRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalAuditRuleDO>()
                .likeIfPresent(LegalAuditRuleDO::getName, reqVO.getName())
                .eqIfPresent(LegalAuditRuleDO::getContractTypeId, reqVO.getContractTypeId())
                .eqIfPresent(LegalAuditRuleDO::getEnabled, reqVO.getEnabled())
                .orderByDesc(LegalAuditRuleDO::getPriority)
                .orderByDesc(LegalAuditRuleDO::getId));
    }

    default List<LegalAuditRuleDO> selectEnabledForAudit(Long contractTypeId) {
        LambdaQueryWrapperX<LegalAuditRuleDO> wrapper = new LambdaQueryWrapperX<LegalAuditRuleDO>()
                .eq(LegalAuditRuleDO::getEnabled, true);
        if (contractTypeId == null) {
            wrapper.isNull(LegalAuditRuleDO::getContractTypeId);
        } else {
            wrapper.and(w -> w.isNull(LegalAuditRuleDO::getContractTypeId)
                    .or()
                    .eq(LegalAuditRuleDO::getContractTypeId, contractTypeId));
        }
        return selectList(wrapper
                .orderByDesc(LegalAuditRuleDO::getPriority)
                .orderByAsc(LegalAuditRuleDO::getId));
    }

    default LegalAuditRuleDO selectByName(String name) {
        return selectOne(LegalAuditRuleDO::getName, name);
    }

}
