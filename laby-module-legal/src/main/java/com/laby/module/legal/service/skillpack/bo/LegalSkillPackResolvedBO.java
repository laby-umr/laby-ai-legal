package com.laby.module.legal.service.skillpack.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时解析后的 SkillPack 配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalSkillPackResolvedBO {

    private Long skillPackId;

    private String code;

    private Integer version;

    private Long chatRoleId;

    @Builder.Default
    private List<String> toolNames = new ArrayList<>();

    private String modelPolicy;

    private boolean fromSnapshot;

}
