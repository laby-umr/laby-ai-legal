package com.laby.module.ai.service.model;

import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.ai.controller.admin.model.vo.tool.AiToolPageReqVO;
import com.laby.module.ai.controller.admin.model.vo.tool.AiToolSaveReqVO;
import com.laby.module.ai.dal.dataobject.model.AiToolDO;
import com.laby.module.ai.dal.mysql.model.AiToolMapper;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.ai.enums.ErrorCodeConstants.TOOL_NAME_NOT_EXISTS;
import static com.laby.module.ai.enums.ErrorCodeConstants.TOOL_NOT_EXISTS;

/**
 * AI 工具 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class AiToolServiceImpl implements AiToolService {

    @Resource
    private AiToolMapper toolMapper;

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public Long createTool(AiToolSaveReqVO createReqVO) {
        // 校验名称是否存在
        validateToolNameExists(createReqVO.getName());

        // 插入
        AiToolDO tool = BeanUtils.toBean(createReqVO, AiToolDO.class);
        toolMapper.insert(tool);
        return tool.getId();
    }

    @Override
    public void updateTool(AiToolSaveReqVO updateReqVO) {
        // 1.1 校验存在
        validateToolExists(updateReqVO.getId());
        // 1.2 校验名称是否存在
        validateToolNameExists(updateReqVO.getName());

        // 2. 更新
        AiToolDO updateObj = BeanUtils.toBean(updateReqVO, AiToolDO.class);
        toolMapper.updateById(updateObj);
    }

    @Override
    public void deleteTool(Long id) {
        // 校验存在
        validateToolExists(id);
        // 删除
        toolMapper.deleteById(id);
    }

    @Override
    public void validateToolExists(Long id) {
        if (toolMapper.selectById(id) == null) {
            throw exception(TOOL_NOT_EXISTS);
        }
    }

    private void validateToolNameExists(String name) {
        if (!applicationContext.containsBean(name)) {
            throw exception(TOOL_NAME_NOT_EXISTS, name);
        }
    }

    @Override
    public AiToolDO getTool(Long id) {
        return toolMapper.selectById(id);
    }

    @Override
    public List<AiToolDO> getToolList(Collection<Long> ids) {
        return toolMapper.selectByIds(ids);
    }

    @Override
    public PageResult<AiToolDO> getToolPage(AiToolPageReqVO pageReqVO) {
        return toolMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AiToolDO> getToolListByStatus(Integer status) {
        return toolMapper.selectListByStatus(status);
    }

}
