package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.ZipUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 合同归档 zip 打包。
 */
public final class LegalContractArchiveZipUtil {

    private LegalContractArchiveZipUtil() {
    }

    public static byte[] buildZip(Map<String, byte[]> entries) throws IOException {
        if (entries == null || entries.isEmpty()) {
            return new byte[0];
        }
        Map<String, byte[]> safeEntries = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            safeEntries.put(sanitizeEntryName(entry.getKey()), entry.getValue());
        }
        if (safeEntries.isEmpty()) {
            return new byte[0];
        }
        String[] paths = safeEntries.keySet().toArray(new String[0]);
        ByteArrayInputStream[] streams = safeEntries.values().stream()
                .map(ByteArrayInputStream::new)
                .toArray(ByteArrayInputStream[]::new);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipUtil.zip(outputStream, paths, streams);
        return outputStream.toByteArray();
    }

    private static String sanitizeEntryName(String name) {
        return name.replace('\\', '/').replaceAll("^/+", "");
    }

}
