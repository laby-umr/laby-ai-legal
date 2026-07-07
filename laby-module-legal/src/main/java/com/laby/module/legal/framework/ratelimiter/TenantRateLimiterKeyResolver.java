package com.laby.module.legal.framework.ratelimiter;

import cn.hutool.crypto.SecureUtil;
import com.laby.framework.common.util.string.StrUtils;
import com.laby.framework.ratelimiter.core.annotation.RateLimiter;
import com.laby.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import com.laby.framework.tenant.core.context.TenantContextHolder;
import org.aspectj.lang.JoinPoint;

/**
 * 租户级限流 Key（方法 + 参数 + tenantId）。
 */
public class TenantRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        Long tenantId = TenantContextHolder.getTenantId();
        return SecureUtil.md5(methodName + argsStr + tenantId);
    }

}
