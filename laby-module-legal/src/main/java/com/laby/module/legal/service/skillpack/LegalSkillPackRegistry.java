package com.laby.module.legal.service.skillpack;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.enums.CommonStatusEnum;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.dal.dataobject.skillpack.LegalSkillPackDO;
import com.laby.module.legal.dal.mysql.contracttype.LegalContractTypeMapper;
import com.laby.module.legal.dal.mysql.skillpack.LegalSkillPackMapper;
import com.laby.module.legal.enums.skillpack.LegalSkillPackSceneEnum;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackModelPolicyBO;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SkillPack 运行时解析
 */
@Slf4j
@Component
public class LegalSkillPackRegistry {

    @Resource
    private LegalSkillPackMapper skillPackMapper;
    @Resource
    private LegalContractTypeMapper contractTypeMapper;

    public Optional<LegalSkillPackDO> resolveForContractType(Long contractTypeId, String scene) {
        if (contractTypeId == null || StrUtil.isBlank(scene)) {
            return Optional.empty();
        }
        LegalContractTypeDO contractType = contractTypeMapper.selectById(contractTypeId);
        if (contractType == null) {
            return Optional.empty();
        }
        LegalSkillPackSceneEnum sceneEnum = LegalSkillPackSceneEnum.of(scene);
        if (sceneEnum == null) {
            return Optional.empty();
        }
        Long packId = switch (sceneEnum) {
            case AUDIT -> contractType.getDefaultSkillPackIdAudit();
            case CHAT -> contractType.getDefaultSkillPackIdChat();
            default -> null;
        };
        if (packId == null) {
            return Optional.empty();
        }
        LegalSkillPackDO pack = skillPackMapper.selectById(packId);
        if (pack == null || !Boolean.TRUE.equals(pack.getEnabled())) {
            log.warn("[resolveForContractType][contractTypeId={} scene={} packId={}] SkillPack 不可用",
                    contractTypeId, scene, packId);
            return Optional.empty();
        }
        if (!scene.equalsIgnoreCase(pack.getScene())) {
            log.warn("[resolveForContractType][packId={}] 场景不匹配 expect={} actual={}",
                    packId, scene, pack.getScene());
        }
        return Optional.of(pack);
    }

    public List<String> sanitizeToolNames(String toolNamesJson) {
        List<String> raw = parseStringList(toolNamesJson);
        if (CollUtil.isEmpty(raw)) {
            return List.of();
        }
        List<String> valid = new ArrayList<>();
        for (String name : raw) {
            if (StrUtil.isBlank(name)) {
                continue;
            }
            String trimmed = name.trim();
            if (LegalSkillPackToolNames.ALLOWED.contains(trimmed)) {
                valid.add(trimmed);
            } else {
                log.warn("[sanitizeToolNames] 非法 tool 已忽略: {}", trimmed);
            }
        }
        return valid;
    }

    public List<String> resolveChatToolNames(Long contractTypeId) {
        return resolveForContractType(contractTypeId, LegalSkillPackSceneEnum.CHAT.getCode())
                .map(pack -> sanitizeToolNames(pack.getToolNames()))
                .orElse(List.of());
    }

    public Optional<LegalSkillPackModelPolicyBO> parseModelPolicy(LegalSkillPackResolvedBO resolved) {
        if (resolved == null || StrUtil.isBlank(resolved.getModelPolicy())) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(JsonUtils.parseObject(resolved.getModelPolicy(), LegalSkillPackModelPolicyBO.class));
        } catch (Exception ex) {
            log.warn("[parseModelPolicy][skillPackId={}] 解析失败: {}", resolved.getSkillPackId(), ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<LegalSkillPackModelPolicyBO> parseModelPolicy(LegalSkillPackDO pack) {
        if (pack == null || StrUtil.isBlank(pack.getModelPolicy())) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(JsonUtils.parseObject(pack.getModelPolicy(), LegalSkillPackModelPolicyBO.class));
        } catch (Exception ex) {
            log.warn("[parseModelPolicy][packId={}] 解析失败: {}", pack.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    public static List<String> parseStringList(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            List<String> list = JsonUtils.parseArray(json, String.class);
            return list == null ? List.of() : list;
        } catch (Exception ex) {
            return List.of();
        }
    }

}
