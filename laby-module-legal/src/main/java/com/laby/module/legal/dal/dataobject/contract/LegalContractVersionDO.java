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

 * 法务合同版本 DO

 */

@TableName("legal_contract_version")

@KeySequence("legal_contract_version_seq")

@Data

@EqualsAndHashCode(callSuper = true)

@Builder

@NoArgsConstructor

@AllArgsConstructor

public class LegalContractVersionDO extends TenantBaseDO {



    /**

     * 版本编号

     */

    @TableId

    private Long id;

    /**

     * 合同编号

     */

    private Long contractId;

    /**

     * 审核轮次

     */

    private Integer auditRound;

    /**

     * 合同版本号

     */

    private Integer versionNo;

    /**

     * 版本类型 ORIGINAL/AI_ANNOTATED/ADOPTED_TRACKED/ADOPTED_CLEAN

     */

    private String type;

    /**

     * 来源版本编号

     */

    private Long sourceVersionId;

    /**

     * infra 文件编号

     */

    private Long fileId;

    /**

     * 可见范围 INTERNAL/EXTERNAL

     */

    private String visibility;

    /**

     * 文件哈希

     */

    private String immutableHash;

    /**

     * 锚点快照 {@link LegalAnchorSnapshotDO#getId()}

     */

    private Long anchorSnapshotId;



}

