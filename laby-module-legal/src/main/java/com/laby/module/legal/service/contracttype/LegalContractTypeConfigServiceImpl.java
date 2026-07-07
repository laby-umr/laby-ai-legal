package com.laby.module.legal.service.contracttype;

import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.dal.dataobject.knowledge.AiKnowledgeDO;
import com.laby.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.laby.module.ai.service.knowledge.AiKnowledgeService;
import com.laby.module.ai.service.model.AiChatRoleService;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigCheckItemVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigOverviewRespVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigResolveRespVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeSkillPackSummaryVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.dal.dataobject.skillpack.LegalSkillPackDO;
import com.laby.module.legal.dal.mysql.auditrule.LegalAuditRuleMapper;
import com.laby.module.legal.enums.LegalAiChatRoleConstants;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.service.contract.LegalContractAuditRoleService;
import com.laby.module.legal.dal.mysql.skillpack.LegalSkillPackMapper;
import com.laby.module.legal.service.skillpack.LegalSkillPackRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 合同类型配置中枢 Service 实现（CFG-001）
 */
@Slf4j
@Service
public class LegalContractTypeConfigServiceImpl implements LegalContractTypeConfigService {

    private static final int SYSTEM_MESSAGE_PREVIEW_LEN = 200;

    @Resource
    private LegalContractTypeService contractTypeService;
    @Resource
    private LegalAuditRuleMapper auditRuleMapper;
    @Resource
    private LegalSkillPackRegistry skillPackRegistry;
    @Resource
    private LegalSkillPackMapper skillPackMapper;
    @Resource
    private AiKnowledgeService aiKnowledgeService;
    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private LegalContractAuditRoleService auditRoleService;

    @Override
    public LegalContractTypeConfigOverviewRespVO getConfigOverview(Long contractTypeId) {
        LegalContractTypeDO contractType = contractTypeService.validateContractTypeExists(contractTypeId);
        int ruleCount = auditRuleMapper.selectEnabledForAudit(contractTypeId).size();
        LegalContractTypeSkillPackSummaryVO auditPack = buildSkillPackSummary(
                contractType.getDefaultSkillPackIdAudit(), LegalSkillPackSceneEnum.AUDIT.getCode());
        LegalContractTypeSkillPackSummaryVO chatPack = buildSkillPackSummary(
                contractType.getDefaultSkillPackIdChat(), LegalSkillPackSceneEnum.CHAT.getCode());
        return LegalContractTypeConfigOverviewRespVO.builder()
                .contractTypeId(contractType.getId())
                .contractTypeName(contractType.getName())
                .knowledgeId(contractType.getKnowledgeId())
                .knowledgeName(resolveKnowledgeName(contractType.getKnowledgeId()))
                .enabledAuditRuleCount(ruleCount)
                .auditSkillPack(auditPack)
                .chatSkillPack(chatPack)
                .checklist(buildChecklist(contractType, ruleCount, auditPack, chatPack))
                .build();
    }

    @Override
    public LegalContractTypeConfigResolveRespVO resolveConfig(Long contractTypeId) {
        LegalContractTypeDO contractType = contractTypeService.validateContractTypeExists(contractTypeId);
        Optional<LegalSkillPackDO> auditPackOpt = skillPackRegistry.resolveForContractType(
                contractTypeId, LegalSkillPackSceneEnum.AUDIT.getCode());
        LegalContractTypeSkillPackSummaryVO chatSummary = buildSkillPackSummary(
                contractType.getDefaultSkillPackIdChat(), LegalSkillPackSceneEnum.CHAT.getCode());

        String promptSource = "DEFAULT_ROLE";
        Long chatRoleId = null;
        String chatRoleName = null;
        String systemPreview = null;
        List<String> auditTools = List.of();

        if (auditPackOpt.isPresent()) {
            LegalSkillPackDO pack = auditPackOpt.get();
            promptSource = "SKILL_PACK";
            chatRoleId = pack.getChatRoleId();
            auditTools = skillPackRegistry.sanitizeToolNames(pack.getToolNames());
            AiChatRoleDO role = resolveChatRole(chatRoleId);
            if (role != null) {
                chatRoleName = role.getName();
                systemPreview = previewSystemMessage(role.getSystemMessage());
            }
        } else {
            systemPreview = previewSystemMessage(
                    auditRoleService.resolveSystemMessage(buildProbeContract(contractType), 1));
            chatRoleName = LegalAiChatRoleConstants.ROLE_NAME_ROUND1;
            promptSource = "DEFAULT_ROLE";
        }

        log.debug("[resolveConfig][contractTypeId={}] promptSource={} chatRoleId={} toolCount={} knowledgeId={}",
                contractTypeId, promptSource, chatRoleId, auditTools.size(), contractType.getKnowledgeId());

        return LegalContractTypeConfigResolveRespVO.builder()
                .contractTypeId(contractType.getId())
                .contractTypeName(contractType.getName())
                .knowledgeId(contractType.getKnowledgeId())
                .knowledgeName(resolveKnowledgeName(contractType.getKnowledgeId()))
                .auditPromptSource(promptSource)
                .auditChatRoleId(chatRoleId)
                .auditChatRoleName(chatRoleName)
                .auditSystemMessagePreview(systemPreview)
                .auditToolNames(auditTools)
                .chatSkillPack(chatSummary)
                .build();
    }

    private LegalContractTypeSkillPackSummaryVO buildSkillPackSummary(Long packId, String scene) {
        if (packId == null) {
            return LegalContractTypeSkillPackSummaryVO.builder()
                    .scene(scene)
                    .configured(false)
                    .toolNames(List.of())
                    .build();
        }
        LegalSkillPackDO pack = skillPackMapper.selectById(packId);
        if (pack == null || !Boolean.TRUE.equals(pack.getEnabled())) {
            return LegalContractTypeSkillPackSummaryVO.builder()
                    .id(packId)
                    .scene(scene)
                    .configured(false)
                    .toolNames(List.of())
                    .build();
        }
        AiChatRoleDO role = resolveChatRole(pack.getChatRoleId());
        return LegalContractTypeSkillPackSummaryVO.builder()
                .id(pack.getId())
                .name(pack.getName())
                .scene(pack.getScene())
                .chatRoleId(pack.getChatRoleId())
                .chatRoleName(role != null ? role.getName() : null)
                .toolNames(skillPackRegistry.sanitizeToolNames(pack.getToolNames()))
                .configured(true)
                .build();
    }

    private List<LegalContractTypeConfigCheckItemVO> buildChecklist(
            LegalContractTypeDO contractType, int ruleCount,
            LegalContractTypeSkillPackSummaryVO auditPack,
            LegalContractTypeSkillPackSummaryVO chatPack) {
        List<LegalContractTypeConfigCheckItemVO> items = new ArrayList<>();
        items.add(LegalContractTypeConfigCheckItemVO.builder()
                .key("knowledge")
                .label("关联知识库")
                .ok(contractType.getKnowledgeId() != null)
                .hint("法务 RAG 语料绑定在合同类型，不读聊天角色 knowledgeIds")
                .build());
        items.add(LegalContractTypeConfigCheckItemVO.builder()
                .key("audit_skill_pack")
                .label("审核技能包")
                .ok(Boolean.TRUE.equals(auditPack.getConfigured()))
                .hint("新建合同时快照；提示词来自包内聊天角色")
                .build());
        items.add(LegalContractTypeConfigCheckItemVO.builder()
                .key("chat_skill_pack")
                .label("对话技能包")
                .ok(Boolean.TRUE.equals(chatPack.getConfigured()))
                .hint("合同详情 Agent 问答的工具与提示词来源")
                .build());
        items.add(LegalContractTypeConfigCheckItemVO.builder()
                .key("audit_rules")
                .label("审核规则")
                .ok(ruleCount > 0)
                .hint("含本类型 + 全局规则，至少 1 条启用")
                .build());
        items.add(LegalContractTypeConfigCheckItemVO.builder()
                .key("audit_role")
                .label("审核技能包已绑角色")
                .ok(auditPack.getChatRoleId() != null)
                .hint("在 AI 技能包中选择「关联 AI 角色」")
                .build());
        return items;
    }

    private String resolveKnowledgeName(Long knowledgeId) {
        if (knowledgeId == null) {
            return null;
        }
        AiKnowledgeDO knowledge = aiKnowledgeService.getKnowledge(knowledgeId);
        return knowledge != null ? knowledge.getName() : null;
    }

    private AiChatRoleDO resolveChatRole(Long chatRoleId) {
        if (chatRoleId == null) {
            return null;
        }
        try {
            return chatRoleService.getChatRole(chatRoleId);
        } catch (Exception ex) {
            log.warn("[resolveChatRole][roleId={}] {}", chatRoleId, ex.getMessage());
            return null;
        }
    }

    private static String previewSystemMessage(String systemMessage) {
        if (StrUtil.isBlank(systemMessage)) {
            return null;
        }
        String trimmed = systemMessage.trim();
        if (trimmed.length() <= SYSTEM_MESSAGE_PREVIEW_LEN) {
            return trimmed;
        }
        return trimmed.substring(0, SYSTEM_MESSAGE_PREVIEW_LEN) + "…";
    }

    private static LegalContractDO buildProbeContract(LegalContractTypeDO contractType) {
        return LegalContractDO.builder()
                .contractTypeId(contractType.getId())
                .build();
    }

}
