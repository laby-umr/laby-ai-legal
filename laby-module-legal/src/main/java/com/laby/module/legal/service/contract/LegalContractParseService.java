package com.laby.module.legal.service.contract;

/**
 * 合同解析服务
 */
public interface LegalContractParseService {

    /**
     * BPM / 应用管道内同步解析（不触发 AI，审核由 Pipeline 下一节点执行）
     */
    void parseForBpm(Long contractId);

    /**
     * 从指定 docx 字节重解析段落（二轮审核前基于 ADOPTED_CLEAN 刷新段落表）。
     */
    void reparseFromDocxBytes(Long contractId, byte[] content);

}
