package com.laby.module.legal.service.bpm;

import cn.hutool.core.util.StrUtil;
import com.laby.module.bpm.api.task.BpmProcessTaskApi;
import com.laby.module.legal.controller.admin.contract.vo.LegalAiAuditProgressRespVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.enums.contract.LegalContractBpmAuditConstants;
import com.laby.module.legal.service.contract.LegalAiAuditProgressService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 审核终态后唤醒 BPM ReceiveTask（替代 ServiceTask 内线程轮询）。
 */
@Slf4j
@Service
public class LegalContractBpmAuditSignalService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalAiAuditProgressService auditProgressService;
    @Resource
    private BpmProcessTaskApi bpmProcessTaskApi;

    /**
     * 若二轮审核 progress 已终态且流程在 ReceiveTask 等待，则 trigger 继续。
     */
    public void signalAwaitAiRound2IfSettled(Long contractId) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null || StrUtil.isBlank(contract.getProcessInstanceId())) {
            return;
        }
        if (!isAuditProgressSettled(contractId)) {
            return;
        }
        log.info("[signalAwaitAiRound2IfSettled][contractId={} processInstanceId={}]",
                contractId, contract.getProcessInstanceId());
        bpmProcessTaskApi.triggerTask(contract.getProcessInstanceId(),
                LegalContractBpmAuditConstants.RECEIVE_AWAIT_AI_ROUND2);
    }

    private boolean isAuditProgressSettled(Long contractId) {
        LegalAiAuditProgressRespVO progress = auditProgressService.get(contractId);
        String status = progress.getStatus();
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }

}
