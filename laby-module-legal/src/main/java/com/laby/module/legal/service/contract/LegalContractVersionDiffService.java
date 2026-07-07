package com.laby.module.legal.service.contract;

import com.laby.module.legal.controller.admin.contract.vo.LegalContractVersionDiffRespVO;

/**
 * 合同版本条款级 Diff
 */
public interface LegalContractVersionDiffService {

    LegalContractVersionDiffRespVO getVersionDiff(Long contractId, Long fromVersionId, Long toVersionId);

}
