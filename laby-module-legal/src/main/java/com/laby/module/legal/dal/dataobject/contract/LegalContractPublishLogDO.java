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
 * 合同归档发布日志 DO（DELIV-001 §14.1 / P2-2）。
 *
 * <p>记录每轮 immutable 发布包 ZIP 与 manifest 快照，供审计追溯。</p>
 */
@TableName("legal_contract_publish_log")
@KeySequence("legal_contract_publish_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractPublishLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 合同编号 */
    private Long contractId;

    /** 审核轮次 */
    private Integer auditRound;

    /** 归档 ZIP infra 文件编号 */
    private Long bundleFileId;

    /** manifest.json 全文 */
    private String manifestJson;

    /** 已采纳意见数 */
    private Integer adoptedCount;

    /** 标注版意见数（PENDING+ADOPTED） */
    private Integer annotatedCount;

    /** WORKING 内容 hash */
    private String workingHash;

    /** 源文件 hash */
    private String originalHash;

}
