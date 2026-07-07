package com.laby.module.legal.service.skillpack.bo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * SkillPack 快照条目
 */
@Data
@Builder
public class LegalSkillPackSnapshotEntryBO {

    private Long skillPackId;

    private String code;

    private Integer version;

    private Long chatRoleId;

    private List<String> toolNames;

    private String modelPolicy;

}
