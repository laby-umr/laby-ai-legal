package com.laby.module.legal.controller.admin.agent;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.agent.vo.LegalAgentStepLogPageReqVO;
import com.laby.module.legal.controller.admin.agent.vo.LegalAgentStepLogRespVO;
import com.laby.module.legal.service.agent.LegalAgentStepLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.laby.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 法务 Agent 步骤日志 Controller
 */
@Tag(name = "管理后台 - 法务 Agent 步骤日志")
@RestController
@RequestMapping("/legal/agent-step-log")
@Validated
public class LegalAgentStepLogController {

    @Resource
    private LegalAgentStepLogService agentStepLogService;

    @GetMapping("/page")
    @Operation(summary = "Agent 步骤日志分页")
    @PreAuthorize("@ss.hasPermission('legal:agent-log:query')")
    public CommonResult<PageResult<LegalAgentStepLogRespVO>> getStepLogPage(
            @Valid LegalAgentStepLogPageReqVO pageReqVO) {
        return success(agentStepLogService.getStepLogPage(pageReqVO));
    }

    @GetMapping("/list-by-session")
    @Operation(summary = "按 sessionId 查询步骤链")
    @Parameter(name = "sessionId", description = "会话编号", required = true)
    @PreAuthorize("@ss.hasPermission('legal:agent-log:query')")
    public CommonResult<List<LegalAgentStepLogRespVO>> getStepLogListBySession(
            @RequestParam("sessionId") String sessionId) {
        return success(agentStepLogService.getStepLogListBySessionId(sessionId));
    }

}
