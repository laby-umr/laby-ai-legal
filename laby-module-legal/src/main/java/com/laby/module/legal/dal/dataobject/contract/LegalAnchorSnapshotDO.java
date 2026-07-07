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

 * 合同锚点快照 DO

 */

@TableName("legal_anchor_snapshot")

@KeySequence("legal_anchor_snapshot_seq")

@Data

@EqualsAndHashCode(callSuper = true)

@Builder

@NoArgsConstructor

@AllArgsConstructor

public class LegalAnchorSnapshotDO extends TenantBaseDO {



    /**

     * 快照编号

     */

    @TableId

    private Long id;

    /**

     * 合同编号

     */

    private Long contractId;

    /**

     * WORKING 版本 legal_contract_version.id

     */

    private Long versionId;

    /**

     * 段落文本聚合哈希

     */

    private String contentHash;



}

