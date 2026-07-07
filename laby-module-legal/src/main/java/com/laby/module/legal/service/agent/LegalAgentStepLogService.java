package com.laby.module.legal.service.agent;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.agent.vo.LegalAgentStepLogPageReqVO;
import com.laby.module.legal.controller.admin.agent.vo.LegalAgentStepLogRespVO;
import com.laby.module.legal.dal.dataobject.agent.LegalAgentStepLogDO;
import com.laby.module.legal.dal.mysql.agent.LegalAgentStepLogMapper;
import com.laby.module.legal.enums.agent.LegalAgentStepTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 法务 Agent 步骤日志 Service
 */
@Slf4j
@Service
public class LegalAgentStepLogService {

    private static final int MAX_INPUT_JSON_CHARS = 512;
    private static final int MAX_OUTPUT_SUMMARY_CHARS = 512;

    @Resource
    private LegalAgentStepLogMapper stepLogMapper;

    public void logLlm(String summary) {
        append(LegalAgentStepTypeEnum.LLM.getType(), null, null, summary, null);
    }

    public void logTool(String toolName, Object toolInput, String outputSummary, long latencyMs) {
        String inputJson = truncateJson(toolInput);
        append(LegalAgentStepTypeEnum.TOOL.getType(), toolName, inputJson,
                truncateSummary(outputSummary), (int) latencyMs);
    }

    public void logError(String toolName, Object toolInput, String errorMessage) {
        String inputJson = truncateJson(toolInput);
        append(LegalAgentStepTypeEnum.ERROR.getType(), toolName, inputJson,
                truncateSummary(errorMessage), null);
    }

    /**
     * 记录提案生命周期（创建 / 执行 / 取消），不依赖当前线程 Agent 上下文。
     */
    public void logProposal(Long contractId, Long userId, String sessionId, String summary) {
        if (contractId == null || userId == null || StrUtil.isBlank(sessionId)) {
            return;
        }
        try {
            int stepIndex = stepLogMapper.selectMaxStepIndexBySessionId(sessionId) + 1;
            stepLogMapper.insert(LegalAgentStepLogDO.builder()
                    .contractId(contractId)
                    .userId(userId)
                    .sessionId(sessionId)
                    .stepIndex(stepIndex)
                    .stepType(LegalAgentStepTypeEnum.PROPOSAL.getType())
                    .toolOutputSummary(truncateSummary(summary))
                    .build());
        } catch (Exception ex) {
            log.warn("[logProposal][sessionId={}] 提案步骤日志写入失败: {}", sessionId, ex.getMessage());
        }
    }

    private void append(String stepType, String toolName, String toolInputJson,
                        String outputSummary, Integer latencyMs) {
        LegalAgentStepLogContext.State state = LegalAgentStepLogContext.getState();
        if (state == null) {
            return;
        }
        try {
            stepLogMapper.insert(LegalAgentStepLogDO.builder()
                    .contractId(state.getContractId())
                    .userId(state.getUserId())
                    .sessionId(state.getSessionId())
                    .stepIndex(LegalAgentStepLogContext.nextStepIndex())
                    .stepType(stepType)
                    .toolName(toolName)
                    .toolInputJson(toolInputJson)
                    .toolOutputSummary(outputSummary)
                    .latencyMs(latencyMs)
                    .build());
        } catch (Exception ex) {
            log.warn("[append][sessionId={}] 步骤日志写入失败: {}", state.getSessionId(), ex.getMessage());
        }
    }

    public PageResult<LegalAgentStepLogRespVO> getStepLogPage(LegalAgentStepLogPageReqVO pageReqVO) {
        PageResult<LegalAgentStepLogDO> page = stepLogMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(page, LegalAgentStepLogRespVO.class);
    }

    public List<LegalAgentStepLogRespVO> getStepLogListBySessionId(String sessionId) {
        return BeanUtils.toBean(stepLogMapper.selectListBySessionId(sessionId), LegalAgentStepLogRespVO.class);
    }

    private static String truncateJson(Object input) {
        if (input == null) {
            return null;
        }
        try {
            return StrUtil.sub(JsonUtils.toJsonString(input), 0, MAX_INPUT_JSON_CHARS);
        } catch (Exception ex) {
            return StrUtil.sub(String.valueOf(input), 0, MAX_INPUT_JSON_CHARS);
        }
    }

    private static String truncateSummary(String summary) {
        return StrUtil.isBlank(summary) ? null : StrUtil.sub(summary, 0, MAX_OUTPUT_SUMMARY_CHARS);
    }

}
