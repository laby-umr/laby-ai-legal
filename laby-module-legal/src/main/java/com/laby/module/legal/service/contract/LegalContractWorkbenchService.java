package com.laby.module.legal.service.contract;

import com.laby.module.legal.controller.admin.contract.vo.LegalContractWorkbenchRespVO;

/**
 * 合同审阅工作台聚合 Service
 */
public interface LegalContractWorkbenchService {

    LegalContractWorkbenchRespVO getWorkbench(Long contractId);

}
