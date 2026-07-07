package com.laby.module.legal.framework.bpm;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.enums.LegalContractConstants;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时确保 {@link LegalContractConstants#PROCESS_KEY} 与仓库 BPMN 一致。
 * <p>
 * 对齐规则：start → opinionReview、无 parseContract、aiRound2 为异步 ServiceTask。
 * 避免后台设计器旧版与业务代码不一致，或二轮 AI 仍同步阻塞 HTTP。
 * <p>
 * 生命周期：实现 {@link ApplicationRunner}，在 Spring Boot 上下文就绪后、应用对外服务前执行（{@code @Order(100)}）。
 */
@Slf4j
@Component
@Order(100)
public class LegalContractBpmAutoDeployRunner implements ApplicationRunner {

    private static final String BPMN_CLASSPATH = "processes/legal_contract_review.bpmn20.xml";

    private final RepositoryService repositoryService;

    public LegalContractBpmAutoDeployRunner(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isLatestDefinitionAligned()) {
            log.debug("[LegalContractBpmAutoDeploy] 流程 {} 已对齐仓库 BPMN，跳过部署", LegalContractConstants.PROCESS_KEY);
            return;
        }
        Deployment deployment = repositoryService.createDeployment()
                .name("legal_contract_review-auto")
                .addClasspathResource(BPMN_CLASSPATH)
                .deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        log.info("[LegalContractBpmAutoDeploy] 已部署 {} version={} deploymentId={}",
                LegalContractConstants.PROCESS_KEY,
                definition != null ? definition.getVersion() : "?",
                deployment.getId());
    }

    private boolean isLatestDefinitionAligned() {
        ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(LegalContractConstants.PROCESS_KEY)
                .latestVersion()
                .singleResult();
        if (latest == null) {
            return false;
        }
        BpmnModel model = repositoryService.getBpmnModel(latest.getId());
        if (model == null) {
            return false;
        }
        FlowElement start = model.getFlowElement("start");
        if (!(start instanceof StartEvent startEvent)) {
            return false;
        }
        if (startEvent.getOutgoingFlows() == null || startEvent.getOutgoingFlows().size() != 1) {
            return false;
        }
        SequenceFlow flow = startEvent.getOutgoingFlows().get(0);
        if (flow == null || !StrUtil.equals("opinionReview", flow.getTargetRef())) {
            return false;
        }
        if (model.getFlowElement("parseContract") != null) {
            return false;
        }
        FlowElement aiRound2 = model.getFlowElement("aiRound2");
        return aiRound2 instanceof ServiceTask serviceTask && serviceTask.isAsynchronous();
    }

}
