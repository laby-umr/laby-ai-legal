package com.laby.module.legal.service.skillpack.bo;

import lombok.Data;

/**
 * 合同创建时嵌入的 SkillPack 快照（JSON 根对象）
 */
@Data
public class LegalSkillPackSnapshotBO {

    /** AUDIT 场景快照 */
    private LegalSkillPackSnapshotEntryBO audit;

    /** CHAT 场景快照 */
    private LegalSkillPackSnapshotEntryBO chat;

}
