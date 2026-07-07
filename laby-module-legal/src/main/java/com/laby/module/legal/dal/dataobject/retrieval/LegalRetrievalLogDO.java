package com.laby.module.legal.dal.dataobject.retrieval;



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

 * 法务 RAG 检索日志 DO

 */

@TableName("legal_retrieval_log")

@KeySequence("legal_retrieval_log_seq")

@Data

@EqualsAndHashCode(callSuper = true)

@Builder

@NoArgsConstructor

@AllArgsConstructor

public class LegalRetrievalLogDO extends TenantBaseDO {



    /**

     * 编号

     */

    @TableId

    private Long id;

    /**

     * AUDIT / QA

     */

    private String bizType;

    /**

     * 合同编号等业务主键

     */

    private Long bizId;

    /**

     * 审核轮次

     */

    private Integer auditRound;

    /**

     * AI 审核批次序号

     */

    private Integer batchIndex;

    /**

     * 检索 query

     */

    private String query;

    /**

     * TopK

     */

    private Integer topK;

    /**

     * JSON 数组：检索到的 chunk/segment id

     */

    private String retrievedChunkIds;

    /**

     * JSON 数组：对应 rerank/score

     */

    private String rerankScores;



}

