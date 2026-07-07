package com.laby.module.legal.service.contract;

import cn.hutool.core.util.StrUtil;
import com.laby.module.bpm.enums.task.BpmTaskStatusEnum;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalContractTaskKeyEnum;
import com.laby.module.legal.enums.contract.LegalParseStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_PARSE_NOT_SUCCESS;

/**
 * 合同处理流水线：解析 → AI 审核（应用层）→ 成功后再启动 BPM（仅人工审批节点）。
 * 避免 BPM 同步 ServiceTask 调 AI 导致失败却已在列表落库、状态混乱。
 */
@Slf4j
@Service
public class LegalContractPipelineService {

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractParseService parseService;
    @Resource
    private LegalAiAuditService aiAuditService;
    @Resource
    private LegalContractVersionService contractVersionService;

    /**
     * 执行解析 + 首轮 AI；失败抛异常，由调用方标记 FAILED。
     */
    public void runParseAndFirstAudit(Long contractId) {
        runParse(contractId);
        runFirstAudit(contractId);
    }

    /**
     * AI 对话创建：仅解析并落工作版，不触发 AI 审核；用户在「我的合同」审阅页手动发起首轮审核。
     */
    public void runParseOnly(Long contractId) {
        runParse(contractId);
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setStatus(LegalContractStatusEnum.DRAFT.getStatus())
                .setCurrentTaskKey(null)
                .setBpmStatus(BpmTaskStatusEnum.NOT_START.getStatus()));
        log.info("[runParseOnly][contractId={}] 解析完成，待用户在审阅页发起首轮 AI 审核", contractId);
    }

    /**
     * 合同已解析完成后，执行首轮 AI 审核（供 AI 对话落库后的手动触发）。
     */
    public void runFirstAudit(Long contractId) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        if (!LegalParseStatusEnum.SUCCESS.getStatus().equals(contract.getParseStatus())) {
            throw exception(CONTRACT_PARSE_NOT_SUCCESS);
        }
        contractVersionService.ensureWorkingVersion(contractId);
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setStatus(LegalContractStatusEnum.AI_AUDITING.getStatus())
                .setCurrentTaskKey(LegalContractTaskKeyEnum.AI_ROUND1.getKey()));
        aiAuditService.auditForPipeline(contractId, 1);
    }

    private void runParse(Long contractId) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setStatus(LegalContractStatusEnum.PARSING.getStatus())
                .setCurrentTaskKey(LegalContractTaskKeyEnum.PARSE_CONTRACT.getKey())
                .setParseStatus(LegalParseStatusEnum.WAITING.getStatus()));

        parseService.parseForBpm(contractId);
        contract = contractMapper.selectById(contractId);
        if (contract == null || !LegalParseStatusEnum.SUCCESS.getStatus().equals(contract.getParseStatus())) {
            throw exception(CONTRACT_PARSE_NOT_SUCCESS);
        }
    }

    public void markFailed(Long contractId, Throwable ex) {
        String message = StrUtil.sub(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 0, 900);
        log.error("[markFailed][contractId={}] {}", contractId, message, ex);
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setStatus(LegalContractStatusEnum.FAILED.getStatus())
                .setCurrentTaskKey(LegalContractTaskKeyEnum.FAILED.getKey())
                .setFeedbackSummary(message));
    }

}
