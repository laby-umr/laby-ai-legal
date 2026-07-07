package com.laby.module.legal.service.document;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OnlyOffice document.key 编解码（tenant_contract_file_revision）。
 */
public final class LegalOnlyOfficeDocumentKeyHelper {

    private LegalOnlyOfficeDocumentKeyHelper() {
    }

    public static String build(Long tenantId, Long contractId, Long fileId, String contentRevision) {
        return tenantId + "_" + contractId + "_" + fileId + "_"
                + StrUtil.blankToDefault(contentRevision, "0");
    }

    public static Parsed parse(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        String[] parts = key.split("_", 4);
        if (parts.length != 4) {
            return null;
        }
        try {
            return new Parsed(Long.parseLong(parts[0]), Long.parseLong(parts[1]),
                    Long.parseLong(parts[2]), parts[3]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Parsed {
        private final Long tenantId;
        private final Long contractId;
        private final Long fileId;
        private final String contentRevision;
    }
}
