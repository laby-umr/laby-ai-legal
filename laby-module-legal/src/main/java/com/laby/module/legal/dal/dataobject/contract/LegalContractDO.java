package com.laby.module.legal.dal.dataobject.contract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.laby.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 法务合同审核 DO
 */
@TableName("legal_contract")
@KeySequence("legal_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractDO extends TenantBaseDO {

    /**
     * 合同审核编号
     */
    @TableId
    private Long id;
    /**
     * 合同标题
     */
    private String title;
    /**
     * 合同类型编号
     */
    private Long contractTypeId;
    /**
     * 我方立场
     *
     * 枚举类 {@link com.laby.module.legal.enums.contract.LegalPartyRoleEnum}
     */
    private String partyRole;
    /**
     * 审核强度
     */
    private String auditLevel;
    /**
     * 大模型编号
     */
    private Long modelId;
    /**
     * 首轮 AI 审核角色（ai_chat_role.id）
     */
    private Long auditRoleId;
    /**
     * 二轮 AI 审核角色；空则使用系统二轮模板
     */
    private Long reauditRoleId;
    /**
     * 是否可编辑
     */
    private Boolean editable;
    /**
     * 业务状态
     *
     * 枚举类 {@link com.laby.module.legal.enums.contract.LegalContractStatusEnum}
     */
    private Integer status;
    /**
     * 流程状态，对齐 BpmTaskStatusEnum
     */
    private Integer bpmStatus;
    /**
     * Flowable 流程实例编号
     */
    private String processInstanceId;
    /**
     * 当前流程节点 Key
     */
    private String currentTaskKey;
    /**
     * AI 审核轮次
     */
    private Integer auditRound;
    /**
     * 创建时 SkillPack 快照 JSON（AUDIT/CHAT）
     */
    private String skillPackSnapshot;
    /**
     * 是否需要二轮 AI
     */
    private Boolean needSecondRound;
    /**
     * 二轮反馈摘要
     */
    private String feedbackSummary;
    /**
     * 高风险意见数
     */
    private Integer riskHighCount;
    /**
     * 主文件 infra_file.id
     */
    private Long mainFileId;
    /**
     * 上传原件 SHA-256（设计 §4.3，不可变存证）
     */
    private String originalHash;
    /**
     * 上传原件文件名
     */
    private String originalFileName;
    /**
     * 原件格式：DOCX / DOC / PDF
     */
    private String sourceFormat;
    /**
     * 解析状态
     *
     * 枚举类 {@link com.laby.module.legal.enums.contract.LegalParseStatusEnum}
     */
    private Integer parseStatus;
    /**
     * 发起人用户编号
     */
    private Long userId;

    /**
     * 创建来源
     *
     * 枚举类 {@link com.laby.module.legal.enums.contract.LegalContractCreateSourceEnum}
     */
    private String createSource;

    /**
     * AI 对话来源编号
     */
    private Long createConversationId;

}
