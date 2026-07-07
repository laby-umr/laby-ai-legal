package com.laby.module.legal.service.skillpack;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackLegalToolRespVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackPageReqVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackRespVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackSaveReqVO;
import com.laby.module.legal.controller.admin.skillpack.vo.LegalSkillPackSimpleRespVO;

import java.util.List;

/**
 * 法务 AI 技能包 Service 接口
 */
public interface LegalSkillPackService {

    /**
     * 创建技能包
     *
     * @param createReqVO 创建信息
     * @return 技能包编号
     */
    Long createSkillPack(LegalSkillPackSaveReqVO createReqVO);

    /**
     * 更新技能包
     *
     * @param updateReqVO 更新信息
     */
    void updateSkillPack(LegalSkillPackSaveReqVO updateReqVO);

    /**
     * 更新技能包启用状态
     *
     * @param id      技能包编号
     * @param enabled 是否启用
     */
    void updateSkillPackEnabled(Long id, Boolean enabled);

    /**
     * 删除技能包
     *
     * @param id 技能包编号
     */
    void deleteSkillPack(Long id);

    /**
     * 批量删除技能包
     *
     * @param ids 技能包编号列表
     */
    void deleteSkillPackList(List<Long> ids);

    /**
     * 获得技能包
     *
     * @param id 技能包编号
     * @return 技能包
     */
    LegalSkillPackRespVO getSkillPack(Long id);

    /**
     * 获得技能包分页
     *
     * @param pageReqVO 分页查询
     * @return 技能包分页
     */
    PageResult<LegalSkillPackRespVO> getSkillPackPage(LegalSkillPackPageReqVO pageReqVO);

    /**
     * 获得精简技能包列表
     *
     * @param scene 场景，可为空
     * @return 精简列表
     */
    List<LegalSkillPackSimpleRespVO> getSkillPackSimpleList(String scene);

    /**
     * 复制技能包
     *
     * @param id 源技能包编号
     * @return 新技能包编号
     */
    Long copySkillPack(Long id);

    /**
     * 法务 Agent 可选工具（白名单 ∩ ai_tool 注册表）
     */
    List<LegalSkillPackLegalToolRespVO> getLegalAgentToolOptions();

}
