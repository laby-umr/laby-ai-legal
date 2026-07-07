package com.laby.module.legal.service.orchestration;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.dal.dataobject.contracttype.LegalContractTypeDO;
import com.laby.module.legal.dal.dataobject.orchestration.LegalOrchestrationFileItemDO;
import com.laby.module.legal.dal.mysql.orchestration.LegalOrchestrationFileItemMapper;
import com.laby.module.legal.enums.orchestration.LegalOrchestrationFileItemStatusEnum;
import com.laby.module.legal.service.contracttype.LegalContractTypeService;
import com.laby.module.legal.service.orchestration.bo.LegalOrchestrationClassificationItemBO;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_CLASSIFY_EMPTY;

@Slf4j
@Service
@Validated
public class LegalOrchestrationClassificationServiceImpl implements LegalOrchestrationClassificationService {

    @Resource
    private LegalOrchestrationSessionService sessionService;
    @Resource
    private LegalOrchestrationFileItemMapper fileItemMapper;
    @Resource
    private LegalContractTypeService contractTypeService;
    @Resource
    private AiModelService aiModelService;

    @Override
    public List<LegalOrchestrationClassificationItemBO> classifyFiles(Long sessionId, Long modelId) {
        List<LegalOrchestrationFileItemDO> files = sessionService.listFileItems(sessionId).stream()
                .filter(item -> LegalOrchestrationFileItemStatusEnum.REGISTERED.getStatus().equals(item.getStatus())
                        || LegalOrchestrationFileItemStatusEnum.CLASSIFIED.getStatus().equals(item.getStatus()))
                .toList();
        if (CollUtil.isEmpty(files)) {
            throw exception(ORCHESTRATION_CLASSIFY_EMPTY);
        }

        List<LegalContractTypeDO> types = contractTypeService.getContractTypeSimpleList();
        Map<Long, LegalContractTypeDO> typeMap = types.stream()
                .collect(Collectors.toMap(LegalContractTypeDO::getId, t -> t, (a, b) -> a));

        List<LegalOrchestrationClassificationItemBO> results = new ArrayList<>();
        AiLlmClient llmClient = modelId != null ? aiModelService.getLlmClient(modelId) : null;

        for (LegalOrchestrationFileItemDO file : files) {
            LegalOrchestrationClassificationItemBO item = classifySingle(file, types, llmClient);
            if (item.getSuggestedTypeId() != null) {
                fileItemMapper.updateById(new LegalOrchestrationFileItemDO()
                        .setId(file.getId())
                        .setSuggestedTypeId(item.getSuggestedTypeId())
                        .setStatus(LegalOrchestrationFileItemStatusEnum.CLASSIFIED.getStatus()));
                LegalContractTypeDO type = typeMap.get(item.getSuggestedTypeId());
                if (type != null) {
                    item.setSuggestedTypeName(type.getName());
                    item.setSuggestedTypeCode(type.getCode());
                }
            } else {
                item.setNeedNewType(true);
            }
            results.add(item);
        }
        return results;
    }

    private LegalOrchestrationClassificationItemBO classifySingle(LegalOrchestrationFileItemDO file,
                                                                  List<LegalContractTypeDO> types,
                                                                  AiLlmClient llmClient) {
        LegalOrchestrationClassificationItemBO item = new LegalOrchestrationClassificationItemBO();
        item.setFileItemId(file.getId());
        item.setFileName(file.getFileName());

        if (llmClient == null || CollUtil.isEmpty(types)) {
            item.setReason("未配置模型或租户无合同类型，需人工确认");
            item.setNeedNewType(true);
            return item;
        }

        String typeCatalog = types.stream()
                .map(t -> t.getId() + ":" + t.getCode() + ":" + t.getName())
                .collect(Collectors.joining("\n"));
        String prompt = """
                你是企业法务合同分类助手。根据文件名判断最匹配的合同类型。
                仅返回 JSON：{"typeId":数字或null,"reason":"简短理由","needNewType":true/false}
                可选类型列表（id:code:name）：
                %s
                文件名：%s
                """.formatted(typeCatalog, file.getFileName());

        try {
            AiLlmRequest request = new AiLlmRequest()
                    .setMessages(List.of(new AiMessage().setRole(AiMessageRoleEnum.USER).setContent(prompt)));
            String raw = llmClient.call(request);
            LlmClassifyResult parsed = JsonUtils.parseObject(extractJson(raw), LlmClassifyResult.class);
            if (parsed != null) {
                item.setSuggestedTypeId(parsed.getTypeId());
                item.setReason(StrUtil.blankToDefault(parsed.getReason(), "模型分类"));
                item.setNeedNewType(Boolean.TRUE.equals(parsed.getNeedNewType()) || parsed.getTypeId() == null);
            } else {
                item.setNeedNewType(true);
                item.setReason("模型返回无法解析");
            }
        } catch (Exception ex) {
            log.warn("[classifySingle] fileItemId={} failed", file.getId(), ex);
            item.setNeedNewType(true);
            item.setReason("分类失败：" + ex.getMessage());
        }
        return item;
    }

    private static String extractJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    @Data
    private static class LlmClassifyResult {
        private Long typeId;
        private String reason;
        private Boolean needNewType;
    }

}
