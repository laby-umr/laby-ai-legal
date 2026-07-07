package com.laby.module.legal.service.contract.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalContractTextDiffUtilTest {

    @Test
    void isSameText_shouldIgnoreWhitespace() {
        assertTrue(LegalContractTextDiffUtil.isSameText("hello  world", "hello\nworld"));
    }

    @Test
    void isSameText_shouldDetectChange() {
        assertFalse(LegalContractTextDiffUtil.isSameText("30日付款", "60日付款"));
    }

}
