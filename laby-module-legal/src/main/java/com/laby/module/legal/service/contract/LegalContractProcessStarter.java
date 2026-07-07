package com.laby.module.legal.service.contract;

import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractCreateReqVO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 创建合同后的后台流水线：解析 + 首轮 AI 审核，成功后再启动 BPM（仅人工节点）。
 * AI 对话与「我的合同」创建均走同一正式审核流水线；编排对话内不做预览审核。
 */
@Slf4j
@Component
public class LegalContractProcessStarter {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractPipelineService pipelineService;
    @Resource
    @Lazy
    private LegalContractBpmService bpmService;

    @Async
    public void startProcessAsync(Long userId, LegalContractCreateReqVO createReqVO, Long contractId, Long tenantId) {
        TenantUtils.execute(tenantId, () -> {
            LegalContractDO contract = contractMapper.selectById(contractId);
            if (contract == null) {
                log.warn("[startProcessAsync][contractId={} tenantId={}] 合同不存在", contractId, tenantId);
                return;
            }
            try {
                pipelineService.runParseAndFirstAudit(contractId);
                LegalContractDO latest = contractMapper.selectById(contractId);
                if (latest == null) {
                    return;
                }
                bpmService.startBpmHumanPhase(userId, createReqVO, latest);
            } catch (Exception ex) {
                pipelineService.markFailed(contractId, ex);
            }
        });
    }

    /**
     * 补救：已解析但未完成首轮 AI 审核时手动触发（如历史数据或异常中断）。
     */
    @Async
    public void startFirstAuditAsync(Long userId, LegalContractCreateReqVO createReqVO, Long contractId, Long tenantId) {
        TenantUtils.execute(tenantId, () -> {
            LegalContractDO contract = contractMapper.selectById(contractId);
            if (contract == null) {
                log.warn("[startFirstAuditAsync][contractId={} tenantId={}] 合同不存在", contractId, tenantId);
                return;
            }
            try {
                pipelineService.runFirstAudit(contractId);
                LegalContractDO latest = contractMapper.selectById(contractId);
                if (latest == null) {
                    return;
                }
                bpmService.startBpmHumanPhase(userId, createReqVO, latest);
            } catch (Exception ex) {
                pipelineService.markFailed(contractId, ex);
            }
        });
    }

}
