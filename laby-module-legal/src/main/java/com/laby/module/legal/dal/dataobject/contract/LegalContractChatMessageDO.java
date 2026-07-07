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
 * 法务合同问答消息 DO
 */
@TableName("legal_contract_chat_message")
@KeySequence("legal_contract_chat_message_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalContractChatMessageDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 合同编号 */
    private Long contractId;

    /** 发起问答的用户编号 */
    private Long userId;

    /** assistant 回复对应的 user 消息编号 */
    private Long replyId;

    /** user / assistant */
    private String type;

    /** 消息正文 */
    private String content;

    /** 推理过程 */
    private String reasoningContent;

    /** 是否 Agent 模式 */
    private Boolean agentMode;

    /** 问答 session */
    private String sessionId;

}
