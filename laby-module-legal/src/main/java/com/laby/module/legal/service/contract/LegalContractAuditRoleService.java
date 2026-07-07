package com.laby.module.legal.service.contract;

import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;

/**
 * 合同审核时解析 AI 聊天角色（提示词在 AI 模块维护）
 */
public interface LegalContractAuditRoleService {

    /**
     * 解析审核系统提示词
     *
     * @param contract   合同
     * @param auditRound 审核轮次
     * @return 系统提示词
     */
    String resolveSystemMessage(LegalContractDO contract, int auditRound);

    /**
     * 获得首轮默认 AI 角色编号
     *
     * @return 角色编号
     */
    Long resolveDefaultRound1RoleId();

    /**
     * 解析合同问答 Agent 系统提示词（优先 ai_chat_role 表配置）
     *
     * @return 系统提示词
     */
    String resolveQaAgentSystemMessage();

    /**
     * 按合同类型解析问答 Agent 系统提示词（优先 CHAT SkillPack）
     */
    String resolveQaAgentSystemMessage(Long contractTypeId);

    /**
     * 按合同解析（优先合同创建时 SkillPack 快照）
     */
    String resolveQaAgentSystemMessage(LegalContractDO contract);

}
