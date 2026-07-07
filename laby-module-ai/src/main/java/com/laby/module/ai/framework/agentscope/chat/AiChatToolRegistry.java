package com.laby.module.ai.framework.agentscope.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.framework.agentscope.mcp.AgentScopeMcpToolRegistrar;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * AI 聊天角色 Tool 注册：根据 {@link com.laby.module.ai.dal.dataobject.model.AiToolDO#getName()} 解析 Spring Bean 并注册到 Toolkit。
 */
@Slf4j
@Component
public class AiChatToolRegistry {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private AgentScopeMcpToolRegistrar mcpToolRegistrar;

    public Toolkit buildToolkit(List<String> toolBeanNames) {
        return buildToolkit(toolBeanNames, null, null);
    }

    public Toolkit buildToolkit(List<String> toolBeanNames, List<String> mcpClientNames, Path workspace) {
        Toolkit toolkit = new Toolkit();
        if (CollUtil.isNotEmpty(toolBeanNames)) {
            for (String name : toolBeanNames) {
                if (StrUtil.isBlank(name)) {
                    continue;
                }
                String beanName = name.trim();
                if (!applicationContext.containsBean(beanName)) {
                    log.warn("[buildToolkit] 未解析到 Tool Bean: {}", beanName);
                    continue;
                }
                toolkit.registerTool(applicationContext.getBean(beanName));
            }
        }
        mcpToolRegistrar.registerMcpTools(toolkit, mcpClientNames, workspace);
        return toolkit;
    }

}
