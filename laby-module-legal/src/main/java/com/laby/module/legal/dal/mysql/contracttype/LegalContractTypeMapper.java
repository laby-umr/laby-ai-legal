package com.laby.module.legal.dal.mysql.contracttype;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.mybatis.core.mapper.BaseMapperX;
import com.laby.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypePageReqVO;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LegalContractTypeMapper extends BaseMapperX<LegalContractTypeDO> {

    default PageResult<LegalContractTypeDO> selectPage(LegalContractTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LegalContractTypeDO>()
                .likeIfPresent(LegalContractTypeDO::getName, reqVO.getName())
                .eqIfPresent(LegalContractTypeDO::getStatus, reqVO.getStatus())
                .orderByAsc(LegalContractTypeDO::getSort)
                .orderByDesc(LegalContractTypeDO::getId));
    }

    default List<LegalContractTypeDO> selectListEnabled() {
        return selectList(new LambdaQueryWrapperX<LegalContractTypeDO>()
                .eq(LegalContractTypeDO::getStatus, 0)
                .orderByAsc(LegalContractTypeDO::getSort)
                .orderByDesc(LegalContractTypeDO::getId));
    }

    default LegalContractTypeDO selectByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return selectOne(LegalContractTypeDO::getCode, code);
    }

}
