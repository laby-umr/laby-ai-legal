package com.laby.module.legal.service.skillpack;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.skillpack.LegalSkillPackDO;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.service.ai.policy.bo.LegalAiPolicyBO;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackSnapshotBO;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackSnapshotEntryBO;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 合同 SkillPack 快照构建与解析
 */
@Component
public class LegalSkillPackSnapshotService {

    private final LegalSkillPackRegistry skillPackRegistry;

    public LegalSkillPackSnapshotService(LegalSkillPackRegistry skillPackRegistry) {
        this.skillPackRegistry = skillPackRegistry;
    }

    public String buildSnapshotJson(Long contractTypeId) {
        LegalSkillPackSnapshotBO snapshot = new LegalSkillPackSnapshotBO();
        skillPackRegistry.resolveForContractType(contractTypeId, LegalSkillPackSceneEnum.AUDIT.getCode())
                .map(this::toEntry)
                .ifPresent(snapshot::setAudit);
        skillPackRegistry.resolveForContractType(contractTypeId, LegalSkillPackSceneEnum.CHAT.getCode())
                .map(this::toEntry)
                .ifPresent(snapshot::setChat);
        if (snapshot.getAudit() == null && snapshot.getChat() == null) {
            return null;
        }
        return JsonUtils.toJsonString(snapshot);
    }

    /**
     * 创建合同时解析技能包快照：优先使用 Policy 中冻结的快照（预览/提案时刻），否则按类型实时构建。
     */
    public String resolveSnapshotForCreate(Long contractTypeId, LegalAiPolicyBO policy) {
        if (policy != null
                && StrUtil.isNotBlank(policy.getSkillPackSnapshotJson())
                && contractTypeId != null
                && contractTypeId.equals(policy.getSkillPackSnapshotContractTypeId())) {
            return policy.getSkillPackSnapshotJson();
        }
        return buildSnapshotJson(contractTypeId);
    }

    public void freezeSnapshotOnPolicy(LegalAiPolicyBO policy, Long contractTypeId) {
        if (policy == null || contractTypeId == null) {
            return;
        }
        policy.setSkillPackSnapshotJson(buildSnapshotJson(contractTypeId));
        policy.setSkillPackSnapshotContractTypeId(contractTypeId);
    }

    public Optional<LegalSkillPackResolvedBO> resolveFromContract(LegalContractDO contract, String scene) {
        if (contract == null) {
            return Optional.empty();
        }
        Optional<LegalSkillPackResolvedBO> fromSnapshot = resolveFromSnapshot(contract.getSkillPackSnapshot(), scene);
        if (fromSnapshot.isPresent()) {
            return fromSnapshot;
        }
        return skillPackRegistry.resolveForContractType(contract.getContractTypeId(), scene)
                .map(pack -> toResolved(pack, skillPackRegistry.sanitizeToolNames(pack.getToolNames()), false));
    }

    public Optional<LegalSkillPackResolvedBO> resolveFromSnapshot(String snapshotJson, String scene) {
        if (StrUtil.isBlank(snapshotJson) || StrUtil.isBlank(scene)) {
            return Optional.empty();
        }
        try {
            LegalSkillPackSnapshotBO snapshot = JsonUtils.parseObject(snapshotJson, LegalSkillPackSnapshotBO.class);
            if (snapshot == null) {
                return Optional.empty();
            }
            LegalSkillPackSnapshotEntryBO entry = resolveSnapshotEntry(snapshot, scene);
            if (entry == null || entry.getSkillPackId() == null) {
                return Optional.empty();
            }
            return Optional.of(toResolved(entry));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static LegalSkillPackSnapshotEntryBO resolveSnapshotEntry(LegalSkillPackSnapshotBO snapshot, String scene) {
        LegalSkillPackSceneEnum sceneEnum = LegalSkillPackSceneEnum.of(scene);
        if (sceneEnum == null) {
            return null;
        }
        return switch (sceneEnum) {
            case AUDIT -> snapshot.getAudit();
            case CHAT -> snapshot.getChat();
            default -> null;
        };
    }

    private LegalSkillPackSnapshotEntryBO toEntry(LegalSkillPackDO pack) {
        return LegalSkillPackSnapshotEntryBO.builder()
                .skillPackId(pack.getId())
                .code(pack.getCode())
                .version(pack.getVersion())
                .chatRoleId(pack.getChatRoleId())
                .toolNames(skillPackRegistry.sanitizeToolNames(pack.getToolNames()))
                .modelPolicy(pack.getModelPolicy())
                .build();
    }

    private static LegalSkillPackResolvedBO toResolved(LegalSkillPackDO pack, List<String> toolNames,
                                                       boolean fromSnapshot) {
        return LegalSkillPackResolvedBO.builder()
                .skillPackId(pack.getId())
                .code(pack.getCode())
                .version(pack.getVersion())
                .chatRoleId(pack.getChatRoleId())
                .toolNames(toolNames == null ? java.util.List.of() : toolNames)
                .modelPolicy(pack.getModelPolicy())
                .fromSnapshot(fromSnapshot)
                .build();
    }

    private static LegalSkillPackResolvedBO toResolved(LegalSkillPackSnapshotEntryBO entry) {
        return LegalSkillPackResolvedBO.builder()
                .skillPackId(entry.getSkillPackId())
                .code(entry.getCode())
                .version(entry.getVersion())
                .chatRoleId(entry.getChatRoleId())
                .toolNames(entry.getToolNames() == null ? java.util.List.of() : entry.getToolNames())
                .modelPolicy(entry.getModelPolicy())
                .fromSnapshot(true)
                .build();
    }

}
