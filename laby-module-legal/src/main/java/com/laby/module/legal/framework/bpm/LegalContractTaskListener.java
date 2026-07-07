package com.laby.module.legal.framework.bpm;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.service.bpm.LegalContractBpmSyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 用户任务创建/完成时同步合同业务状态（需在 BPMN userTask 上配置）
 */
@Slf4j
@Component("legalContractTaskListener")
public class LegalContractTaskListener implements TaskListener {

    @Resource
    private LegalContractBpmSyncService bpmSyncService;
    @Resource
    private RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        Long contractId = resolveContractId(delegateTask);
        if (contractId == null) {
            return;
        }
        String taskKey = delegateTask.getTaskDefinitionKey();
        String event = delegateTask.getEventName();
        if (TaskListener.EVENTNAME_CREATE.equals(event)) {
            bpmSyncService.onUserTaskCreate(contractId, taskKey);
        } else if (TaskListener.EVENTNAME_COMPLETE.equals(event)) {
            bpmSyncService.onUserTaskComplete(contractId, taskKey);
        }
    }

    private Long resolveContractId(DelegateTask delegateTask) {
        Object contractIdVar = delegateTask.getVariable("contractId");
        if (contractIdVar instanceof Long l) {
            return l;
        }
        if (contractIdVar != null) {
            return Long.valueOf(String.valueOf(contractIdVar));
        }
        String processInstanceId = delegateTask.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            return null;
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance != null && StrUtil.isNotBlank(instance.getBusinessKey())) {
            return Long.valueOf(instance.getBusinessKey());
        }
        return null;
    }

}
