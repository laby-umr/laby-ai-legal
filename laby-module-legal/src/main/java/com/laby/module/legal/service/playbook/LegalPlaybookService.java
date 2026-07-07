package com.laby.module.legal.service.playbook;

import com.laby.module.legal.controller.admin.playbook.vo.LegalPlaybookPreviewRespVO;
import com.laby.module.legal.controller.admin.playbook.vo.LegalPlaybookSimulateRespVO;

/**
 * Playbook 管理 Service
 */
public interface LegalPlaybookService {

    LegalPlaybookPreviewRespVO preview(Long contractTypeId);

    LegalPlaybookSimulateRespVO simulate(Long contractId);

}
