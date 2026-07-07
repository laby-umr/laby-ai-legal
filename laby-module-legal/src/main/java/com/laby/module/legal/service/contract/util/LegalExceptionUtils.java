package com.laby.module.legal.service.contract.util;

import com.laby.framework.common.exception.ServiceException;

/**
 * 法务模块异常处理工具类
 * <p>
 * 统一 {@link ServiceException} 的重抛逻辑，避免各 Service 中重复 instanceof 判断。
 */
public final class LegalExceptionUtils {

    private LegalExceptionUtils() {
    }

    /**
     * 若异常为业务异常 {@link ServiceException}，则原样抛出；否则不做处理，由调用方继续降级或包装。
     *
     * @param ex 捕获的异常
     */
    public static void rethrowServiceException(Throwable ex) {
        if (ex instanceof ServiceException serviceException) {
            throw serviceException;
        }
    }

}
