package com.laby.module.legal.framework.agentscope.skill;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.service.skillpack.bo.LegalSkillPackResolvedBO;
import io.agentscope.harness.agent.workspace.WorkspaceConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 将法务 SkillPack 同步到 Harness Workspace {@code skills/} 目录，供 AgentScope Dynamic Skills 加载。
 */
@Slf4j
@Component
public class LegalSkillPackSkillWriter {

    private static final String SKILL_FILE = "SKILL.md";

    /**
     * @param workspace 合同 Agent workspace 根目录
     * @param pack      运行时 SkillPack（可为 null）
     * @param scene     场景标识，如 chat / audit
     */
    public void syncSkillPack(Path workspace, LegalSkillPackResolvedBO pack, String scene) {
        if (workspace == null || pack == null || StrUtil.isBlank(pack.getCode())) {
            return;
        }
        try {
            String skillDirName = sanitizeDirName("legal-" + StrUtil.blankToDefault(scene, "chat") + "-" + pack.getCode());
            Path skillDir = workspace.resolve(WorkspaceConstants.SKILLS_DIR).resolve(skillDirName);
            Files.createDirectories(skillDir);
            Files.writeString(skillDir.resolve(SKILL_FILE), buildSkillMarkdown(pack, scene), StandardCharsets.UTF_8);
            log.debug("[syncSkillPack] wrote skill to {}", skillDir);
        } catch (IOException ex) {
            log.warn("[syncSkillPack] 写入 workspace skill 失败: {}", ex.getMessage());
        }
    }

    private static String buildSkillMarkdown(LegalSkillPackResolvedBO pack, String scene) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Legal SkillPack: ").append(pack.getCode()).append('\n').append('\n');
        sb.append("scene: ").append(StrUtil.blankToDefault(scene, "chat")).append('\n');
        if (pack.getVersion() != null) {
            sb.append("version: ").append(pack.getVersion()).append('\n');
        }
        if (pack.getSkillPackId() != null) {
            sb.append("skillPackId: ").append(pack.getSkillPackId()).append('\n');
        }
        sb.append('\n');
        sb.append("## 说明\n");
        sb.append("本技能来自法务 SkillPack 配置，指导 Agent 在合同").append(scene).append("场景下使用指定工具与策略。\n\n");
        if (CollUtil.isNotEmpty(pack.getToolNames())) {
            sb.append("## 推荐工具\n");
            for (String tool : pack.getToolNames()) {
                sb.append("- `").append(tool).append("`\n");
            }
            sb.append('\n');
        }
        if (StrUtil.isNotBlank(pack.getModelPolicy())) {
            sb.append("## 模型策略\n```json\n").append(pack.getModelPolicy()).append("\n```\n");
        }
        return sb.toString();
    }

    private static String sanitizeDirName(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

}
