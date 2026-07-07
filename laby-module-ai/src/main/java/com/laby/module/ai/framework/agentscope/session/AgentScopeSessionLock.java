package com.laby.module.ai.framework.agentscope.session;

/**
 * Agent 会话互斥锁（进程内或 Redis 分布式）。
 */
public interface AgentScopeSessionLock {

    boolean tryAcquire(String sessionId);

    void release(String sessionId);

}
