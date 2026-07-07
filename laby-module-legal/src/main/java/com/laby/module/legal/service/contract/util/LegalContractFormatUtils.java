package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;

/**
 * 合同文件格式探测（扩展名）
 */
public final class LegalContractFormatUtils {

    private LegalContractFormatUtils() {
    }

    public static boolean isPdfFileName(String fileName) {
        return StrUtil.isNotBlank(fileName) && fileName.toLowerCase().endsWith(".pdf");
    }

    public static boolean isPdfContract(LegalContractDO contract) {
        if (contract == null) {
            return false;
        }
        return LegalContractSourceFormatEnum.PDF == LegalContractSourceFormatEnum.of(contract.getSourceFormat());
    }

    public static LegalContractSourceFormatEnum detectSourceFormat(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return LegalContractSourceFormatEnum.DOCX;
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".docx")) {
            return LegalContractSourceFormatEnum.DOCX;
        }
        if (lower.endsWith(".doc")) {
            return LegalContractSourceFormatEnum.DOC;
        }
        return LegalContractSourceFormatEnum.DOCX;
    }

    /**
     * OnlyOffice document.fileType
     */
    public static String toOnlyOfficeFileType(LegalContractSourceFormatEnum format) {
        if (format == null) {
            return "docx";
        }
        return switch (format) {
            case DOC -> "doc";
            case DOCX -> "docx";
            case PDF -> "pdf";
        };
    }

    /**
     * OnlyOffice DocsAPI documentType
     */
    public static String toOnlyOfficeDocumentType(LegalContractSourceFormatEnum format) {
        if (format == LegalContractSourceFormatEnum.PDF) {
            return "pdf";
        }
        return "word";
    }
}
