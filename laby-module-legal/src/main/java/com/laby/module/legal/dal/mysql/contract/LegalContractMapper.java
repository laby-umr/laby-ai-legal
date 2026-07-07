package com.laby.module.legal.dal.mysql.contract;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractPageReqVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegalContractMapper extends BaseMapperX<LegalContractDO> {

    default PageResult<LegalContractDO> selectPage(Long userId, LegalContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalContractDO>()
                .eqIfPresent(LegalContractDO::getUserId, userId)
                .eqIfPresent(LegalContractDO::getStatus, reqVO.getStatus())
                .eqIfPresent(LegalContractDO::getPartyRole, reqVO.getPartyRole())
                .likeIfPresent(LegalContractDO::getTitle, reqVO.getTitle())
                .betweenIfPresent(LegalContractDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(LegalContractDO::getId));
    }

}
