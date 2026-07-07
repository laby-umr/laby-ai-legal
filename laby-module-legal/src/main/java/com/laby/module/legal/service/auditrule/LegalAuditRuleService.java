package com.laby.module.legal.service.auditrule;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRulePageReqVO;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRuleRespVO;
import com.laby.module.legal.controller.admin.auditrule.vo.LegalAuditRuleSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 法务审核规则 Service 接口
 */
public interface LegalAuditRuleService {

    /**
     * 创建审核规则
     *
     * @param createReqVO 审核规则信息
     * @return 审核规则编号
     */
    Long createAuditRule(@Valid LegalAuditRuleSaveReqVO createReqVO);

    /**
     * 更新审核规则
     *
     * @param updateReqVO 审核规则信息
     */
    void updateAuditRule(@Valid LegalAuditRuleSaveReqVO updateReqVO);

    /**
     * 更新审核规则启用状态
     *
     * @param id      审核规则编号
     * @param enabled 是否启用
     */
    void updateAuditRuleEnabled(Long id, Boolean enabled);

    /**
     * 删除审核规则
     *
     * @param id 审核规则编号
     */
    void deleteAuditRule(Long id);

    /**
     * 批量删除审核规则
     *
     * @param ids 审核规则编号数组
     */
    void deleteAuditRuleList(List<Long> ids);

    /**
     * 获得审核规则
     *
     * @param id 审核规则编号
     * @return 审核规则
     */
    LegalAuditRuleRespVO getAuditRule(Long id);

    /**
     * 获得审核规则分页
     *
     * @param pageReqVO 分页查询
     * @return 审核规则分页
     */
    PageResult<LegalAuditRuleRespVO> getAuditRulePage(LegalAuditRulePageReqVO pageReqVO);

}
