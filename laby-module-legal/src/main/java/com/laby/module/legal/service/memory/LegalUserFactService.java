package com.laby.module.legal.service.memory;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactSaveReqVO;

import java.util.List;

public interface LegalUserFactService {

    PageResult<LegalUserFactRespVO> getUserFactPage(LegalUserFactPageReqVO pageReqVO);

    Long createUserFact(LegalUserFactSaveReqVO reqVO);

    void updateUserFact(LegalUserFactSaveReqVO reqVO);

    void deleteUserFact(Long id);

    void saveUserFact(Long userId, Long contractId, String sessionId, String content, Long sourceMessageId);

    String buildUserFactAppendix(Long contractId, Long userId, int limit);

    List<LegalUserFactRespVO> listByContractAndUser(Long contractId, Long userId, int limit);

}
