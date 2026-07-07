package com.laby.module.legal.service.contract;

import com.laby.module.legal.enums.contract.LegalContractDeliverableEnum;
import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;

/**
 * 合同四件套按需生成 Service（DELIV-001）。
 *
 * <p>下载时不写入 {@code AI_ANNOTATED}/{@code ADOPTED_*} 版本缓存，实时渲染字节流。</p>
 */
public interface LegalContractDeliverableService {

    /**
     * 按需生成指定交付物。
     *
     * @param contractId  合同编号
     * @param deliverable 交付物类型
     * @param auditRound  审核轮次；{@code null} 时使用合同当前轮次
     * @return 文件名 + 字节内容
     */
    LegalContractFileDownloadBO generate(Long contractId, LegalContractDeliverableEnum deliverable,
                                         Integer auditRound);

}
