package com.laby.module.legal.service.contract;

import com.laby.framework.common.exception.ServiceException;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.enums.contract.LegalParseStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * OPT-001 Wave 1：解析前状态守卫单测
 */
class LegalContractParseGuardTest {

    @Test
    void validateBeforeParse_nullContract_throwsNotExists() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> LegalContractParseServiceImpl.validateBeforeParse(null));
        assertEquals(1_050_000_000, ex.getCode());
    }

    @Test
    void validateBeforeParse_running_throwsInProgress() {
        LegalContractDO contract = new LegalContractDO()
                .setId(1L)
                .setParseStatus(LegalParseStatusEnum.RUNNING.getStatus());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> LegalContractParseServiceImpl.validateBeforeParse(contract));
        assertEquals(1_050_000_064, ex.getCode());
    }

    @Test
    void validateBeforeParse_waiting_ok() {
        LegalContractDO contract = new LegalContractDO()
                .setId(1L)
                .setParseStatus(LegalParseStatusEnum.WAITING.getStatus());

        assertDoesNotThrow(() -> LegalContractParseServiceImpl.validateBeforeParse(contract));
    }

    @Test
    void validateBeforeParse_success_ok() {
        LegalContractDO contract = new LegalContractDO()
                .setId(1L)
                .setParseStatus(LegalParseStatusEnum.SUCCESS.getStatus());

        assertDoesNotThrow(() -> LegalContractParseServiceImpl.validateBeforeParse(contract));
    }

}
