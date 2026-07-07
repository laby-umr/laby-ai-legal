package com.laby.module.legal.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 合同锚点项 DO
 */
@TableName("legal_anchor_item")
@KeySequence("legal_anchor_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalAnchorItemDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 快照编号
     */
    private Long snapshotId;
    /**
     * 段落业务 ID，与 legal_contract_paragraph.paragraph_id 一致
     */
    private String anchorId;
    /**
     * 书签名，如 laby_p_ 前缀加 paragraphId
     */
    private String bookmarkName;
    /**
     * 段落文本 SHA-256
     */
    private String paragraphHash;
    /**
     * 段落排序序号
     */
    private Integer paragraphIndex;
    /**
     * 层级路径
     */
    private String path;

}
