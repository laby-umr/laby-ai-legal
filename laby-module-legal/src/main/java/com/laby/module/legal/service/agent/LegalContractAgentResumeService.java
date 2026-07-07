package com.laby.module.legal.service.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractAgentConfirmReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractChatRespVO;
import com.laby.module.legal.service.agent.bo.LegalContractAgentPendingConfirmBO;
import com.laby.module.legal.tool.agent.LegalAgentSseEventHolder;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.module.legal.enums.ErrorCodeConstants.AGENT_PROPOSAL_NOT_EXISTS;

/**
 * AgentScope Permission HITL：用户 Confirm 后通过 {@link HarnessAgent#observe} 恢复 Agent。
 * <p>
 * 首条问答 SSE 仍在等待 Confirm 时，须 observe 注入 {@link ConfirmResult}，不可再开 streamEvents。
 */
@Slf4j
@Service
public class LegalContractAgentResumeService {

    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SIDE_EVENT_POLL = Duration.ofMillis(500);

    @Resource
    private LegalContractAgentRuntimeRegistry runtimeRegistry;
    @Resource
    private LegalAgentSessionGuard agentSessionGuard;
    @Resource
    private LegalContractAgentPendingConfirmStore pendingConfirmStore;
    @Resource
    private LegalContractAgentRebuildService rebuildService;

    public void confirm(LegalContractAgentConfirmReqVO reqVO) {
        submitConfirm(reqVO);
    }

    /**
     * 将 Confirm 注入仍在等待的 Agent（原 chat SSE 会继续推送后续事件）。
     */
    public void submitConfirm(LegalContractAgentConfirmReqVO reqVO) {
        LegalContractAgentRuntimeRegistry.PendingRun pending = resolvePendingRun(reqVO);
        validatePending(reqVO, pending);

        boolean approved = Boolean.TRUE.equals(reqVO.getApproved());
        UserMessage resumeMsg = UserMessage.builder()
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS,
                        buildConfirmResults(pending.getConfirmEvent(), approved)))
                .build();

        HarnessAgent agent = pending.getAgent();
        LegalAgentSseEventHolder.bindSession(reqVO.getSessionId());

        try {
            agent.observe(resumeMsg).timeout(CONFIRM_TIMEOUT).block();
            pending.setAwaitingConfirm(false);
            log.info("[submitConfirm][sessionId={}] Confirm 已注入, approved={}",
                    reqVO.getSessionId(), approved);
        } catch (Exception ex) {
            log.warn("[submitConfirm][sessionId={}] observe 失败: {}", reqVO.getSessionId(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * 降级续流：原 chat SSE 已断开时使用 streamEvents（跨实例 / 重建 Agent）。
     */
    public Flux<CommonResult<LegalContractChatRespVO>> resumeStream(LegalContractAgentConfirmReqVO reqVO) {
        String sessionId = reqVO.getSessionId();
        if (!agentSessionGuard.tryAcquire(sessionId)) {
            return Flux.just(success(new LegalContractChatRespVO()
                    .setEventType("error")
                    .setContent("Agent 正在处理上一条消息，请稍后再试")
                    .setSessionId(sessionId)));
        }
        LegalContractAgentRuntimeRegistry.PendingRun pending = resolvePendingRun(reqVO);
        if (pending == null || pending.getAgent() == null || pending.getConfirmEvent() == null) {
            agentSessionGuard.release(sessionId);
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
        validatePending(reqVO, pending);
        try {
            submitConfirm(reqVO);
            return Flux.just(success(new LegalContractChatRespVO().setSessionId(sessionId)))
                    .concatWith(pollSideEvents(sessionId))
                    .doFinally(signal -> agentSessionGuard.release(sessionId));
        } catch (Exception observeEx) {
            log.info("[resumeStream][sessionId={}] observe 未完成，走 streamEvents 降级", sessionId, observeEx);
            return streamEventsResume(reqVO, pending);
        }
    }

    private Flux<CommonResult<LegalContractChatRespVO>> streamEventsResume(
            LegalContractAgentConfirmReqVO reqVO,
            LegalContractAgentRuntimeRegistry.PendingRun pending) {
        String sessionId = reqVO.getSessionId();
        boolean approved = Boolean.TRUE.equals(reqVO.getApproved());
        UserMessage resumeMsg = UserMessage.builder()
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS,
                        buildConfirmResults(pending.getConfirmEvent(), approved)))
                .build();
        pending.setAwaitingConfirm(false);
        runtimeRegistry.register(sessionId, pending.getAgent(), pending.getRuntimeContext(),
                pending.getContractId(), pending.isAllowProposal(), pending.getAnswerMode());
        LegalAgentSseEventHolder.bindSession(sessionId);

        HarnessAgent agent = pending.getAgent();
        Flux<CommonResult<LegalContractChatRespVO>> body = agent.streamEvents(
                        List.of(resumeMsg), pending.getRuntimeContext())
                .flatMap(event -> {
                    CommonResult<LegalContractChatRespVO> mapped =
                            LegalContractAgentServiceImpl.mapAgentEventToSsePublic(
                                    event, sessionId, runtimeRegistry);
                    return mapped != null ? Flux.just(mapped) : Flux.empty();
                })
                .concatWith(Flux.defer(() -> Flux.fromIterable(
                        LegalContractAgentServiceImpl.drainSseEventsPublic(sessionId, false))))
                .timeout(Duration.ofSeconds(LegalContractAgentService.AGENT_TIMEOUT_SECONDS))
                .onErrorResume(ex -> {
                    log.warn("[streamEventsResume][sessionId={}] Agent 恢复失败", sessionId, ex);
                    return Flux.just(success(new LegalContractChatRespVO()
                            .setEventType("error")
                            .setContent("Agent 恢复失败，请稍后重试")
                            .setSessionId(sessionId)));
                })
                .doFinally(signal -> cleanup(sessionId));

        return Flux.just(success(new LegalContractChatRespVO().setSessionId(sessionId))).concatWith(body);
    }

    private Flux<CommonResult<LegalContractChatRespVO>> pollSideEvents(String sessionId) {
        return Flux.interval(SIDE_EVENT_POLL)
                .take(Duration.ofSeconds(LegalContractAgentService.AGENT_TIMEOUT_SECONDS))
                .concatMap(tick -> Flux.fromIterable(
                        LegalContractAgentServiceImpl.drainSseEventsPublic(sessionId, true)))
                .takeWhile(resp -> resp.getData() != null
                        && StrUtil.isBlank(resp.getData().getEventType()));
    }

    private LegalContractAgentRuntimeRegistry.PendingRun resolvePendingRun(LegalContractAgentConfirmReqVO reqVO) {
        String sessionId = reqVO.getSessionId();
        LegalContractAgentRuntimeRegistry.PendingRun local = runtimeRegistry.get(sessionId);
        if (local != null && local.getAgent() != null && local.getConfirmEvent() != null) {
            return local;
        }
        return pendingConfirmStore.find(sessionId)
                .map(this::rebuildPendingRun)
                .orElse(local);
    }

    private LegalContractAgentRuntimeRegistry.PendingRun rebuildPendingRun(LegalContractAgentPendingConfirmBO snapshot) {
        log.info("[rebuildPendingRun][sessionId={}] 从 Redis 快照重建 Agent", snapshot.getSessionId());
        LegalContractAgentRuntimeRegistry.PendingRun pending = rebuildService.rebuild(snapshot);
        runtimeRegistry.register(pending.getSessionId(), pending.getAgent(), pending.getRuntimeContext(),
                pending.getContractId(), pending.isAllowProposal(), pending.getAnswerMode());
        runtimeRegistry.saveConfirmEvent(pending.getSessionId(), pending.getConfirmEvent());
        return pending;
    }

    private static void validatePending(LegalContractAgentConfirmReqVO reqVO,
                                        LegalContractAgentRuntimeRegistry.PendingRun pending) {
        if (pending == null || pending.getAgent() == null || pending.getConfirmEvent() == null) {
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
        if (StrUtil.isNotBlank(reqVO.getConfirmId())
                && !StrUtil.equals(reqVO.getConfirmId(), pending.getConfirmEvent().getReplyId())) {
            throw exception(AGENT_PROPOSAL_NOT_EXISTS);
        }
    }

    private static List<ConfirmResult> buildConfirmResults(RequireUserConfirmEvent event, boolean approved) {
        List<ConfirmResult> results = new ArrayList<>();
        List<ToolUseBlock> toolCalls = event.getToolCalls();
        if (CollUtil.isEmpty(toolCalls)) {
            return results;
        }
        for (ToolUseBlock toolCall : toolCalls) {
            results.add(new ConfirmResult(approved, toolCall));
        }
        return results;
    }

    private void cleanup(String sessionId) {
        runtimeRegistry.remove(sessionId);
        agentSessionGuard.release(sessionId);
        LegalAgentStepLogContext.removeSession(sessionId);
    }

}
