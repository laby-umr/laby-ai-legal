package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.enums.contract.LegalContractDeliverableEnum;
import com.laby.module.legal.enums.contract.LegalContractExportModeEnum;
import com.laby.module.legal.enums.contract.LegalContractExportVisibilityEnum;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_DELIVERABLE_INVALID;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_EXPORT_MODE_INVALID;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_EXPORT_VISIBILITY_INVALID;

/**
 * 合同导出参数规范化
 */
public final class LegalContractExportParamHelper {

    private LegalContractExportParamHelper() {
    }

    public static LegalContractExportVisibilityEnum normalizeVisibility(String visibility) {
        String value = StrUtil.blankToDefault(visibility, LegalContractExportVisibilityEnum.INTERNAL.getCode());
        LegalContractExportVisibilityEnum result = LegalContractExportVisibilityEnum.valueOfCode(value);
        if (result == null) {
            throw exception(CONTRACT_EXPORT_VISIBILITY_INVALID);
        }
        return result;
    }

    public static LegalContractExportModeEnum normalizeAdoptedMode(String mode,
                                                                   LegalContractExportVisibilityEnum visibility) {
        if (visibility == LegalContractExportVisibilityEnum.EXTERNAL) {
            return LegalContractExportModeEnum.CLEAN;
        }
        String value = StrUtil.blankToDefault(mode, LegalContractExportModeEnum.CLEAN.getCode());
        LegalContractExportModeEnum result = LegalContractExportModeEnum.valueOfCode(value);
        if (result == null) {
            throw exception(CONTRACT_EXPORT_MODE_INVALID);
        }
        return result;
    }

    /**
     * 解析四件套交付物参数（DELIV-001 §15.1）。
     */
    public static LegalContractDeliverableEnum normalizeDeliverable(String deliverable) {
        LegalContractDeliverableEnum result = LegalContractDeliverableEnum.of(deliverable);
        if (result == null) {
            throw exception(CONTRACT_DELIVERABLE_INVALID);
        }
        return result;
    }

}
