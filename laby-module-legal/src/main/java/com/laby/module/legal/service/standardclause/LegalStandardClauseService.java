package com.laby.module.legal.service.standardclause;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClausePageReqVO;
import com.laby.module.legal.controller.admin.standardclause.vo.LegalStandardClauseSaveReqVO;
import com.laby.module.legal.dal.dataobject.standardclause.LegalStandardClauseDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 法务标准条款 Service 接口
 */
public interface LegalStandardClauseService {

    /**
     * 创建标准条款
     *
     * @param createReqVO 标准条款信息
     * @return 标准条款编号
     */
    Long createStandardClause(@Valid LegalStandardClauseSaveReqVO createReqVO);

    /**
     * 更新标准条款
     *
     * @param updateReqVO 标准条款信息
     */
    void updateStandardClause(@Valid LegalStandardClauseSaveReqVO updateReqVO);

    /**
     * 删除标准条款
     *
     * @param id 标准条款编号
     */
    void deleteStandardClause(Long id);

    /**
     * 批量删除标准条款
     *
     * @param ids 标准条款编号数组
     */
    void deleteStandardClauseList(List<Long> ids);

    /**
     * 获得标准条款
     *
     * @param id 标准条款编号
     * @return 标准条款
     */
    LegalStandardClauseDO getStandardClause(Long id);

    /**
     * 校验标准条款存在
     *
     * @param id 标准条款编号
     * @return 标准条款
     */
    LegalStandardClauseDO validateStandardClauseExists(Long id);

    /**
     * 获得标准条款分页
     *
     * @param pageReqVO 分页查询
     * @return 标准条款分页
     */
    PageResult<LegalStandardClauseDO> getStandardClausePage(LegalStandardClausePageReqVO pageReqVO);

    /**
     * 获得标准条款精简列表
     *
     * @return 标准条款列表
     */
    List<LegalStandardClauseDO> getStandardClauseSimpleList();

    /**
     * 获得指定编号的标准条款 Map
     *
     * @param ids 标准条款编号数组
     * @return 标准条款 Map
     */
    Map<Long, LegalStandardClauseDO> getStandardClauseMap(Collection<Long> ids);

}
