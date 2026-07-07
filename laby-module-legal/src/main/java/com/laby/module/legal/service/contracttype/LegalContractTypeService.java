package com.laby.module.legal.service.contracttype;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypePageReqVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeSaveReqVO;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 法务合同类型 Service 接口
 */
public interface LegalContractTypeService {

    /**
     * 创建合同类型
     *
     * @param createReqVO 合同类型信息
     * @return 合同类型编号
     */
    Long createContractType(@Valid LegalContractTypeSaveReqVO createReqVO);

    /**
     * 更新合同类型
     *
     * @param updateReqVO 合同类型信息
     */
    void updateContractType(@Valid LegalContractTypeSaveReqVO updateReqVO);

    /**
     * 删除合同类型
     *
     * @param id 合同类型编号
     */
    void deleteContractType(Long id);

    /**
     * 批量删除合同类型
     *
     * @param ids 合同类型编号数组
     */
    void deleteContractTypeList(List<Long> ids);

    /**
     * 获得合同类型
     *
     * @param id 合同类型编号
     * @return 合同类型
     */
    LegalContractTypeDO getContractType(Long id);

    /**
     * 校验合同类型存在
     *
     * @param id 合同类型编号
     * @return 合同类型
     */
    LegalContractTypeDO validateContractTypeExists(Long id);

    /**
     * 获得合同类型分页
     *
     * @param pageReqVO 分页查询
     * @return 合同类型分页
     */
    PageResult<LegalContractTypeDO> getContractTypePage(LegalContractTypePageReqVO pageReqVO);

    /**
     * 获得合同类型精简列表
     *
     * @return 合同类型列表
     */
    List<LegalContractTypeDO> getContractTypeSimpleList();

}
