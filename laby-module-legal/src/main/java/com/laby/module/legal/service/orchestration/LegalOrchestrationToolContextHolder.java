package com.laby.module.legal.service.orchestration;



import com.laby.module.legal.tool.orchestration.LegalOrchestrationToolRuntimeContext;



/**

 * 编排 Tool 运行时上下文缓存（按 conversationId / sessionId，供 Reactor 异步 Tool 线程 fallback）

 */

public final class LegalOrchestrationToolContextHolder {



    private static final ThreadLocal<LegalOrchestrationToolRuntimeContext> CONTEXT = new ThreadLocal<>();



    private static LegalOrchestrationToolContextStore store = new InMemoryLegalOrchestrationToolContextStore();



    private LegalOrchestrationToolContextHolder() {

    }



    public static void setStore(LegalOrchestrationToolContextStore contextStore) {

        if (contextStore != null) {

            store = contextStore;

        }

    }



    public static void bindConversation(Long conversationId, LegalOrchestrationToolRuntimeContext context) {

        store.bindConversation(conversationId, context);

    }



    public static void set(LegalOrchestrationToolRuntimeContext context) {

        CONTEXT.set(context);

    }



    public static LegalOrchestrationToolRuntimeContext get() {

        return CONTEXT.get();

    }



    public static LegalOrchestrationToolRuntimeContext getByConversationId(Long conversationId) {

        return store.getByConversationId(conversationId);

    }



    public static LegalOrchestrationToolRuntimeContext getBySessionId(String sessionId) {

        return store.getBySessionId(sessionId);

    }



    public static void clear() {

        CONTEXT.remove();

    }



}

