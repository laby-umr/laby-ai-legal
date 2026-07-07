package com.laby.module.legal.controller.admin.trace;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.trace.vo.LegalAiTracePageReqVO;
import com.laby.module.legal.controller.admin.trace.vo.LegalAiTraceRespVO;
import com.laby.module.legal.service.trace.LegalAiTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.laby.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 法务 AI 追踪
 * <p>
 * 只读查询 AI 审核 / 问答链路追踪记录。
 */
@Tag(name = "管理后台 - 法务 AI 追踪")
@RestController
@RequestMapping("/legal/ai-trace")
@Validated
public class LegalAiTraceController {

    @Resource
    private LegalAiTraceService aiTraceService;

    @GetMapping("/page")
    @Operation(summary = "AI 审核链路追踪分页")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<PageResult<LegalAiTraceRespVO>> getTracePage(@Valid LegalAiTracePageReqVO pageReqVO) {
        return success(aiTraceService.getTracePage(pageReqVO));
    }

}
