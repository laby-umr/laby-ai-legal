package com.laby.module.legal.service.opinion;

import com.laby.module.legal.controller.admin.opinion.vo.LegalAuditOpinionSaveReqVO;
import com.laby.module.legal.controller.admin.opinion.vo.LegalOpinionDocumentApplyResultVO;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 法务审核意见 Service 接口
 */
public interface LegalAuditOpinionService {

    List<LegalAuditOpinionDO> getOpinionListByContractId(Long contractId);

    LegalOpinionDocumentApplyResultVO adopt(Long id);

    void ignore(Long id);

    LegalOpinionDocumentApplyResultVO revoke(Long id);

    LegalOpinionDocumentApplyResultVO batchAdopt(List<Long> ids);

    void batchIgnore(List<Long> ids);

    Long createManual(@Valid LegalAuditOpinionSaveReqVO reqVO);

    void updateManual(@Valid LegalAuditOpinionSaveReqVO reqVO);

}
