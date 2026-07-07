package com.laby.module.legal.tool.agent;

import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Agent SSE Tool/提案事件队列（按 sessionId 存储，避免 Tool 异步线程与 SSE 线程 ThreadLocal 不一致）。
 */
public final class LegalAgentSseEventHolder {

    private static final ConcurrentHashMap<String, Deque<SseEvent>> SESSION_QUEUES = new ConcurrentHashMap<>();

    private LegalAgentSseEventHolder() {
    }

    public static void bindSession(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return;
        }
        SESSION_QUEUES.computeIfAbsent(sessionId, key -> new ConcurrentLinkedDeque<>());
    }

    public static void unbindSession(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            SESSION_QUEUES.remove(sessionId);
        }
    }

    public static void pushToolStart(String sessionId, String toolName) {
        append(sessionId, event("tool_start", toolName, null, null, null, null, null));
    }

    public static void pushToolEnd(String sessionId, String toolName, String summary) {
        append(sessionId, event("tool_end", toolName, summary, null, null, null, null));
    }

    public static void pushProposal(String sessionId, String proposalNo, String action, String title, Object payload) {
        SseEvent event = new SseEvent();
        event.setEventType("proposal");
        event.setProposalNo(proposalNo);
        event.setProposalAction(action);
        event.setProposalTitle(title);
        event.setProposalPayload(JsonUtils.toJsonString(payload));
        append(sessionId, event);
    }

    public static void pushConfirmRequired(String sessionId, String confirmId, String toolName, String summary) {
        SseEvent event = new SseEvent();
        event.setEventType("confirm_required");
        event.setConfirmId(confirmId);
        event.setToolName(toolName);
        event.setToolSummary(summary);
        append(sessionId, event);
    }

    public static List<SseEvent> pollAll(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return Collections.emptyList();
        }
        Deque<SseEvent> deque = SESSION_QUEUES.get(sessionId);
        if (deque == null || deque.isEmpty()) {
            return Collections.emptyList();
        }
        List<SseEvent> list = new ArrayList<>(deque);
        deque.clear();
        return list;
    }

    /** @deprecated 使用 {@link #pollAll(String)} */
    @Deprecated
    public static List<SseEvent> pollAll() {
        return Collections.emptyList();
    }

    public static void clear(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            Deque<SseEvent> deque = SESSION_QUEUES.get(sessionId);
            if (deque != null) {
                deque.clear();
            }
        }
    }

    /** @deprecated 使用 {@link #unbindSession(String)} */
    @Deprecated
    public static void clear() {
        // no-op，兼容旧调用
    }

    /** @deprecated 使用 {@link #unbindSession(String)} */
    @Deprecated
    public static void remove() {
        // no-op，兼容旧调用
    }

    private static void append(String sessionId, SseEvent event) {
        if (StrUtil.isBlank(sessionId) || event == null) {
            return;
        }
        SESSION_QUEUES.computeIfAbsent(sessionId, key -> new ConcurrentLinkedDeque<>()).addLast(event);
    }

    private static SseEvent event(String eventType, String toolName, String toolSummary,
                                  String proposalNo, String proposalAction, String proposalTitle,
                                  String proposalPayload) {
        SseEvent event = new SseEvent();
        event.setEventType(eventType);
        event.setToolName(toolName);
        event.setToolSummary(toolSummary);
        event.setProposalNo(proposalNo);
        event.setProposalAction(proposalAction);
        event.setProposalTitle(proposalTitle);
        event.setProposalPayload(proposalPayload);
        return event;
    }

    @Data
    public static class SseEvent {
        private String eventType;
        private String toolName;
        private String toolSummary;
        private String confirmId;
        private String proposalNo;
        private String proposalAction;
        private String proposalTitle;
        private String proposalPayload;
    }

}
