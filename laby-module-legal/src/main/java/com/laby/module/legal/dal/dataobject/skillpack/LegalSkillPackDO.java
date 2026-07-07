package com.laby.module.legal.dal.dataobject.skillpack;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务 AI 技能包 DO
 */
@TableName("legal_skill_pack")
@KeySequence("legal_skill_pack_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalSkillPackDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String code;

    private String name;

    private String scene;

    private Long chatRoleId;

    /** JSON 数组字符串 */
    private String toolNames;

    private String mcpClientNames;

    private String modelPolicy;

    private Long playbookId;

    private String description;

    private Boolean enabled;

    private Integer version;

}
