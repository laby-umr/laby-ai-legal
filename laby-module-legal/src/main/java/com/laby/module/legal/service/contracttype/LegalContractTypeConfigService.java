package com.laby.module.legal.service.contracttype;

import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigOverviewRespVO;
import com.laby.module.legal.controller.admin.contracttype.vo.LegalContractTypeConfigResolveRespVO;

/**
 * 合同类型配置中枢（CFG-001）
 */
public interface LegalContractTypeConfigService {

    LegalContractTypeConfigOverviewRespVO getConfigOverview(Long contractTypeId);

    LegalContractTypeConfigResolveRespVO resolveConfig(Long contractTypeId);

}
