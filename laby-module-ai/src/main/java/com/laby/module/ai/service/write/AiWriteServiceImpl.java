package com.laby.module.ai.service.write;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.dict.core.DictFrameworkUtils;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiLlmStreamEvent;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.ai.enums.model.AiModelTypeEnum;
import com.laby.module.ai.enums.model.AiPlatformEnum;
import com.laby.framework.common.pojo.CommonResult;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.framework.tenant.core.util.TenantUtils;
import com.laby.module.ai.controller.admin.write.vo.AiWriteGenerateReqVO;
import com.laby.module.ai.controller.admin.write.vo.AiWritePageReqVO;
import com.laby.module.ai.dal.dataobject.model.AiChatRoleDO;
import com.laby.module.ai.dal.dataobject.model.AiModelDO;
import com.laby.module.ai.dal.dataobject.write.AiWriteDO;
import com.laby.module.ai.dal.mysql.write.AiWriteMapper;
import com.laby.module.ai.enums.AiChatRoleEnum;
import com.laby.module.ai.enums.DictTypeConstants;
import com.laby.module.ai.enums.ErrorCodeConstants;
import com.laby.module.ai.enums.write.AiWriteTypeEnum;
import com.laby.module.ai.service.model.AiChatRoleService;
import com.laby.module.ai.service.model.AiModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.framework.common.pojo.CommonResult.error;
import static com.laby.framework.common.pojo.CommonResult.success;
import static com.laby.module.ai.enums.ErrorCodeConstants.*;

/**
 * AI 写作 Service 实现类
 *
 * @author xiaoxin
 */
@Service
@Slf4j
public class AiWriteServiceImpl implements AiWriteService {

    @Resource
    private AiModelService modalService;
    @Resource
    private AiChatRoleService chatRoleService;

    @Resource
    private AiWriteMapper writeMapper;

    @Override
    public Flux<CommonResult<String>> generateWriteContent(AiWriteGenerateReqVO generateReqVO, Long userId) {
        // 1 获取写作模型。尝试获取写作助手角色，没有则使用默认模型
        AiChatRoleDO writeRole = CollUtil.getFirst(
                chatRoleService.getChatRoleListByName(AiChatRoleEnum.AI_WRITE_ROLE.getName()));
        // 1.1 获取写作执行模型
        AiModelDO model = getModel(writeRole);
        // 1.2 获取角色设定消息
        String systemMessage = Objects.nonNull(writeRole) && StrUtil.isNotBlank(writeRole.getSystemMessage())
                ? writeRole.getSystemMessage() : AiChatRoleEnum.AI_WRITE_ROLE.getSystemMessage();
        // 1.3 校验平台
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        AiLlmClient llmClient = modalService.getLlmClient(model.getId());

        // 2. 插入写作信息
        AiWriteDO writeDO = BeanUtils.toBean(generateReqVO, AiWriteDO.class, write -> write.setUserId(userId)
                        .setPlatform(platform.getPlatform()).setModelId(model.getId()).setModel(model.getModel()));
        writeMapper.insert(writeDO);

        // 3.1 构建请求，并进行调用
        AiLlmRequest request = buildLlmRequest(generateReqVO, model, systemMessage);
        Flux<AiLlmStreamEvent> streamResponse = llmClient.stream(request);

        // 3.2 流式返回
        StringBuffer contentBuffer = new StringBuffer();
        return streamResponse.flatMap(event -> {
            if (event.getType() == AiLlmStreamEvent.Type.ERROR) {
                return Flux.error(new RuntimeException(event.getErrorMessage()));
            }
            if (event.getType() != AiLlmStreamEvent.Type.CONTENT) {
                return Flux.empty();
            }
            String newContent = StrUtil.nullToDefault(event.getDelta(), "");
            contentBuffer.append(newContent);
            return Flux.just(success(newContent));
        }).doOnComplete(() -> {
            // 忽略租户，因为 Flux 异步无法透传租户
            TenantUtils.executeIgnore(() ->
                    writeMapper.updateById(new AiWriteDO().setId(writeDO.getId()).setGeneratedContent(contentBuffer.toString())));
        }).doOnError(throwable -> {
            log.error("[generateWriteContent][generateReqVO({}) 发生异常]", generateReqVO, throwable);
            // 忽略租户，因为 Flux 异步无法透传租户
            TenantUtils.executeIgnore(() ->
                    writeMapper.updateById(new AiWriteDO().setId(writeDO.getId()).setErrorMessage(throwable.getMessage())));
        }).onErrorResume(error -> Flux.just(error(ErrorCodeConstants.WRITE_STREAM_ERROR)));
    }

    private AiModelDO getModel(AiChatRoleDO writeRole) {
        AiModelDO model = null;
        if (Objects.nonNull(writeRole) && Objects.nonNull(writeRole.getModelId())) {
            model = modalService.getModel(writeRole.getModelId());
        }
        if (model == null) {
            model = modalService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
        }
        // 校验模型存在、且合法
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        if (ObjUtil.notEqual(model.getType(), AiModelTypeEnum.CHAT.getType())) {
            throw exception(MODEL_USE_TYPE_ERROR);
        }
        return model;
    }

    private AiLlmRequest buildLlmRequest(AiWriteGenerateReqVO generateReqVO, AiModelDO model, String systemMessage) {
        return new AiLlmRequest()
                .setMessages(buildMessages(generateReqVO, systemMessage))
                .setTemperature(model.getTemperature())
                .setMaxTokens(model.getMaxTokens());
    }

    private List<AiMessage> buildMessages(AiWriteGenerateReqVO generateReqVO, String systemMessage) {
        List<AiMessage> chatMessages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemMessage)) {
            chatMessages.add(new AiMessage().setRole(AiMessageRoleEnum.SYSTEM).setContent(systemMessage));
        }
        chatMessages.add(new AiMessage().setRole(AiMessageRoleEnum.USER).setContent(buildUserMessage(generateReqVO)));
        return chatMessages;
    }

    private String buildUserMessage(AiWriteGenerateReqVO generateReqVO) {
        String format = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_FORMAT, generateReqVO.getFormat());
        String tone = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_TONE, generateReqVO.getTone());
        String language = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_LANGUAGE, generateReqVO.getLanguage());
        String length = DictFrameworkUtils.parseDictDataLabel(DictTypeConstants.AI_WRITE_LENGTH, generateReqVO.getLength());
        // 格式化 prompt
        String prompt = generateReqVO.getPrompt();
        if (Objects.equals(generateReqVO.getType(), AiWriteTypeEnum.WRITING.getType())) {
            return StrUtil.format(AiWriteTypeEnum.WRITING.getPrompt(), prompt, format, tone, language, length);
        } else {
            return StrUtil.format(AiWriteTypeEnum.REPLY.getPrompt(), generateReqVO.getOriginalContent(), prompt, format, tone, language, length);
        }
    }

    @Override
    public void deleteWrite(Long id) {
        // 校验存在
        validateWriteExists(id);
        // 删除
        writeMapper.deleteById(id);
    }

    private void validateWriteExists(Long id) {
        if (writeMapper.selectById(id) == null) {
            throw exception(WRITE_NOT_EXISTS);
        }
    }

    @Override
    public PageResult<AiWriteDO> getWritePage(AiWritePageReqVO pageReqVO) {
        return writeMapper.selectPage(pageReqVO);
    }

}
