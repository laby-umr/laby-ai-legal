package com.laby.module.legal.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.laby.module.ai.service.model.AiChatRoleService;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.service.skillpack.LegalSkillPackRegistry;
import com.laby.module.legal.service.skillpack.LegalSkillPackSnapshotService;
import com.laby.module.legal.enums.LegalAiChatRoleConstants;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 法务合同审核角色 Service 实现类
 */
@Slf4j
@Service
public class LegalContractAuditRoleServiceImpl implements LegalContractAuditRoleService {

    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private LegalSkillPackRegistry skillPackRegistry;
    @Resource
    private LegalSkillPackSnapshotService skillPackSnapshotService;

    @Override
    public String resolveSystemMessage(LegalContractDO contract, int auditRound) {
        AiChatRoleDO skillPackRole = resolveRoleFromSkillPack(contract, auditRound);
        if (skillPackRole != null && StrUtil.isNotBlank(skillPackRole.getSystemMessage())) {
            log.debug("[resolveSystemMessage][contractId={} round={}] source=SKILL_PACK roleId={}",
                    contract != null ? contract.getId() : null, auditRound, skillPackRole.getId());
            return skillPackRole.getSystemMessage();
        }
        AiChatRoleDO role = resolveRoleForContract(contract, auditRound);
        if (role != null && StrUtil.isNotBlank(role.getSystemMessage())) {
            log.debug("[resolveSystemMessage][contractId={} round={}] source=CONTRACT_ROLE roleId={}",
                    contract != null ? contract.getId() : null, auditRound, role.getId());
            return role.getSystemMessage();
        }
        log.debug("[resolveSystemMessage][contractId={} round={}] source=DEFAULT_BUILTIN",
                contract != null ? contract.getId() : null, auditRound);
        return auditRound > 1
                ? LegalAiChatRoleConstants.DEFAULT_SYSTEM_MESSAGE_ROUND2
                : LegalAiChatRoleConstants.DEFAULT_SYSTEM_MESSAGE_ROUND1;
    }

    @Override
    public String resolveQaAgentSystemMessage() {
        return resolveQaAgentSystemMessage((Long) null);
    }

    @Override
    public String resolveQaAgentSystemMessage(Long contractTypeId) {
        LegalSkillPackResolvedBO resolved = skillPackRegistry
                .resolveForContractType(contractTypeId, LegalSkillPackSceneEnum.CHAT.getCode())
                .map(pack -> LegalSkillPackResolvedBO.builder()
                        .chatRoleId(pack.getChatRoleId())
                        .build())
                .orElse(null);
        AiChatRoleDO skillPackRole = resolveRoleFromResolved(resolved);
        if (skillPackRole != null && StrUtil.isNotBlank(skillPackRole.getSystemMessage())) {
            return skillPackRole.getSystemMessage();
        }
        return defaultQaAgentMessage();
    }

    @Override
    public String resolveQaAgentSystemMessage(LegalContractDO contract) {
        AiChatRoleDO skillPackRole = resolveRoleFromResolved(
                skillPackSnapshotService.resolveFromContract(contract, LegalSkillPackSceneEnum.CHAT.getCode())
                        .orElse(null));
        if (skillPackRole != null && StrUtil.isNotBlank(skillPackRole.getSystemMessage())) {
            return skillPackRole.getSystemMessage();
        }
        if (contract != null && contract.getContractTypeId() != null) {
            return resolveQaAgentSystemMessage(contract.getContractTypeId());
        }
        return defaultQaAgentMessage();
    }

    private String defaultQaAgentMessage() {
        AiChatRoleDO role = CollUtil.getFirst(
                chatRoleService.getChatRoleListByName(LegalAiChatRoleConstants.ROLE_NAME_QA_AGENT));
        if (role != null && CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())
                && StrUtil.isNotBlank(role.getSystemMessage())) {
            return role.getSystemMessage();
        }
        return LegalAiChatRoleConstants.DEFAULT_SYSTEM_MESSAGE_QA_AGENT;
    }

    @Override
    public Long resolveDefaultRound1RoleId() {
        AiChatRoleDO role = CollUtil.getFirst(
                chatRoleService.getChatRoleListByName(LegalAiChatRoleConstants.ROLE_NAME_ROUND1));
        if (role != null && CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())) {
            return role.getId();
        }
        return null;
    }

    private AiChatRoleDO resolveRoleFromSkillPack(LegalContractDO contract, int auditRound) {
        if (contract == null) {
            return null;
        }
        LegalSkillPackResolvedBO resolved = skillPackSnapshotService
                .resolveFromContract(contract, LegalSkillPackSceneEnum.AUDIT.getCode())
                .orElse(null);
        return resolveRoleFromResolved(resolved);
    }

    private AiChatRoleDO resolveRoleFromResolved(LegalSkillPackResolvedBO resolved) {
        if (resolved == null || resolved.getChatRoleId() == null) {
            return null;
        }
        try {
            return chatRoleService.validateChatRole(resolved.getChatRoleId());
        } catch (Exception ex) {
            log.warn("[resolveRoleFromResolved][roleId={}] {}", resolved.getChatRoleId(), ex.getMessage());
            return null;
        }
    }

    private AiChatRoleDO resolveRoleForContract(LegalContractDO contract, int auditRound) {
        if (contract == null) {
            return null;
        }
        Long roleId = auditRound > 1 ? contract.getReauditRoleId() : contract.getAuditRoleId();
        if (roleId == null && auditRound > 1) {
            roleId = contract.getAuditRoleId();
        }
        if (roleId != null) {
            try {
                AiChatRoleDO role = chatRoleService.validateChatRole(roleId);
                if (!LegalAiChatRoleConstants.CATEGORY.equals(role.getCategory())) {
                    log.warn("[resolveRoleForContract][contractId={} roleId={}] 非法务合同分类，仍使用该角色",
                            contract.getId(), roleId);
                }
                return role;
            } catch (Exception ex) {
                log.warn("[resolveRoleForContract][contractId={} roleId={}] {}", contract.getId(), roleId,
                        ex.getMessage());
            }
        }
        String name = auditRound > 1
                ? LegalAiChatRoleConstants.ROLE_NAME_ROUND2
                : LegalAiChatRoleConstants.ROLE_NAME_ROUND1;
        return CollUtil.getFirst(chatRoleService.getChatRoleListByName(name));
    }

}
