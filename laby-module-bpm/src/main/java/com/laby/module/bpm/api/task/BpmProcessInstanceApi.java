package com.laby.module.bpm.api.task;

import com.laby.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * 流程实例 Api 接口
 *
 * @author 芋道源码
 */
public interface BpmProcessInstanceApi {

    /**
     * 创建流程实例（提供给内部）
     *
     * @param userId 用户编号
     * @param reqDTO 创建信息
     * @return 实例的编号
     */
    String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO);

    /**
     * 更新流程实例变量（提供给内部业务模块，在完成任务前写入网关分支变量等）
     */
    void updateProcessInstanceVariables(@NotEmpty String processInstanceId, Map<String, Object> variables);

}
