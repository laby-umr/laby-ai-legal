package com.laby.module.bpm.api.task;

import com.laby.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.laby.module.bpm.service.task.BpmProcessInstanceService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Flowable 流程实例 Api 实现类
 *
 * @author 芋道源码
 * @author jason
 */
@Service
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Override
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO) {
        return processInstanceService.createProcessInstance(userId, reqDTO);
    }

    @Override
    public void updateProcessInstanceVariables(String processInstanceId, Map<String, Object> variables) {
        processInstanceService.updateProcessInstanceVariables(processInstanceId, variables);
    }

}
