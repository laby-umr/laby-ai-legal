package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;

/**
 * 合同文本简易 Diff（无第三方 diff 库）
 */
public final class LegalContractTextDiffUtil {

    private LegalContractTextDiffUtil() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return StrUtil.trim(text).replaceAll("\\s+", " ");
    }

    public static boolean isSameText(String before, String after) {
        return normalize(before).equals(normalize(after));
    }

}
