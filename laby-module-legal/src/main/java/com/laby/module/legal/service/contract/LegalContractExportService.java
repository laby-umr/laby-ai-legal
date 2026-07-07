package com.laby.module.legal.service.contract;

import com.laby.module.legal.controller.admin.contract.vo.LegalContractExportPrecheckRespVO;
import com.laby.module.legal.enums.contract.LegalContractExportModeEnum;
import com.laby.module.legal.enums.contract.LegalContractExportVisibilityEnum;

/**
 * 合同审核报告导出（Word 归档）。
 */
public interface LegalContractExportService {

    /**
     * 根据最新审核轮次 Markdown 报告生成 docx，并写入 {@code legal_contract_file}。
     *
     * @return infra 文件编号
     */
    Long exportReportDocx(Long contractId);

    /**
     * 导出合同标注版（Word）
     */
    Long exportAnnotatedContractDocx(Long contractId, LegalContractExportVisibilityEnum visibility);

    /**
     * 导出合同采纳版（TRACKED/CLEAN）。
     */
    Long exportAdoptedContractDocx(Long contractId, LegalContractExportModeEnum mode,
                                   LegalContractExportVisibilityEnum visibility);

    /**
     * 采纳导出前预检（自动写回/冲突/人工确认）。
     */
    LegalContractExportPrecheckRespVO precheckAdoptedExport(Long contractId);

    /**
     * 归档导出：生成 immutable 发布包 ZIP（四件套 + 报告 + manifest，DELIV-001 §17）。
     * <p>同一合同轮次幂等：已存在 {@code PUBLISHED} 版本时直接返回。</p>
     *
     * @return 归档 zip 的 infra 文件编号
     */
    Long exportArchivePackage(Long contractId);

    /**
     * 导出交付包：与归档包内容结构一致，供用户手动打包下载（不写 PUBLISHED 版本行）。
     */
    Long exportDeliveryBundle(Long contractId);

}
