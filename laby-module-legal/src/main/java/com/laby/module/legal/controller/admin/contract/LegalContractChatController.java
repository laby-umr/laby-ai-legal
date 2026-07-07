package com.laby.module.legal.controller.admin.contract;

import com.laby.framework.common.pojo.CommonResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatMessageRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractParagraphSkipReqVO;
import com.laby.framework.ratelimiter.core.annotation.RateLimiter;
import com.laby.framework.ratelimiter.core.keyresolver.impl.UserRateLimiterKeyResolver;
import com.laby.module.legal.service.contract.LegalContractChatMessageService;
import com.laby.module.legal.service.contract.LegalContractChatService;
import com.laby.module.legal.service.contract.LegalContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 管理后台 - 法务合同问答 Controller
 */
@Tag(name = "管理后台 - 法务合同问答")
@RestController
@RequestMapping("/legal/contract")
@Validated
public class LegalContractChatController {

    @Resource
    private LegalContractChatService contractChatService;
    @Resource
    private LegalContractChatMessageService chatMessageService;
    @Resource
    private LegalContractService contractService;

    @PostMapping("/chat")
    @Operation(summary = "合同问答（同步）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<LegalContractChatRespVO> chat(@Valid @RequestBody LegalContractChatReqVO reqVO) {
        return success(contractChatService.chat(reqVO));
    }

    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "合同问答（流式）")
    @RateLimiter(time = 60, count = 10, keyResolver = UserRateLimiterKeyResolver.class)
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public Flux<CommonResult<LegalContractChatRespVO>> chatStream(@Valid @RequestBody LegalContractChatReqVO reqVO) {
        return contractChatService.chatStream(reqVO);
    }

    @GetMapping("/chat-message/list")
    @Operation(summary = "合同问答消息列表")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<List<LegalContractChatMessageRespVO>> listChatMessages(
            @Parameter(description = "合同编号", required = true) @RequestParam("contractId") Long contractId,
            @Parameter(description = "会话编号，可选；传入则只返回该会话消息")
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        contractService.validateContractExists(contractId);
        return success(chatMessageService.listMessages(contractId, getLoginUserId(), sessionId));
    }

    @DeleteMapping("/chat-message/delete")
    @Operation(summary = "删除合同问答消息")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Boolean> deleteChatMessage(
            @Parameter(description = "消息编号", required = true) @RequestParam("id") Long id) {
        chatMessageService.deleteMessage(id, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/chat-message/delete-from")
    @Operation(summary = "从指定消息起删除后续问答（重新生成）")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Boolean> deleteChatMessagesFrom(
            @Parameter(description = "消息编号", required = true) @RequestParam("id") Long id) {
        chatMessageService.deleteFromMessage(id, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/chat-message/clear")
    @Operation(summary = "清空合同问答")
    @PreAuthorize("@ss.hasPermission('legal:contract:query')")
    public CommonResult<Boolean> clearChatMessages(
            @Parameter(description = "合同编号", required = true) @RequestParam("contractId") Long contractId) {
        contractService.validateContractExists(contractId);
        chatMessageService.clearMessages(contractId, getLoginUserId());
        return success(true);
    }

    @PutMapping("/paragraph/skip-audit")
    @Operation(summary = "标记段落是否不需 AI 审核")
    @PreAuthorize("@ss.hasPermission('legal:contract:update')")
    public CommonResult<Boolean> updateParagraphSkipAudit(@Valid @RequestBody LegalContractParagraphSkipReqVO reqVO) {
        contractService.updateParagraphSkipAudit(reqVO);
        return success(true);
    }

}
