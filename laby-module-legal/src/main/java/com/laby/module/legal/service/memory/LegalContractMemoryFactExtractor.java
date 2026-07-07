package com.laby.module.legal.service.memory;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.llm.AiLlmClient;
import com.laby.module.ai.core.llm.AiLlmRequest;
import com.laby.module.ai.core.llm.AiMessage;
import com.laby.module.ai.core.llm.AiMessageRoleEnum;
import com.laby.module.ai.service.model.AiModelService;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.enums.memory.LegalContractMemoryTypeEnum;
import com.laby.module.legal.framework.config.LegalContractMemoryProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Mem0 风格：从问答 transcript 异步抽取可复用事实。
 */
@Slf4j
@Service
public class LegalContractMemoryFactExtractor {

    private static final int MAX_FACT_CHARS = 500;
    private static final int MAX_LLM_FACTS = 3;

    @Resource
    private LegalContractMemoryProperties memoryProperties;
    @Resource
    private LegalContractEpisodicMemoryServiceImpl episodicMemoryService;
    @Resource
    private LegalUserFactServiceImpl userFactService;
    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private AiModelService aiModelService;

    @Async
    public void extractFactAsync(LegalContractChatMessageDO userMessage,
                                 LegalContractChatMessageDO assistantMessage) {
        extractFactInternal(userMessage, assistantMessage);
    }

    public void extractFactSync(LegalContractChatMessageDO userMessage,
                                LegalContractChatMessageDO assistantMessage) {
        extractFactInternal(userMessage, assistantMessage);
    }

    private void extractFactInternal(LegalContractChatMessageDO userMessage,
                                     LegalContractChatMessageDO assistantMessage) {
        if (!memoryProperties.isFactExtractionEnabled()
                || userMessage == null
                || assistantMessage == null
                || userMessage.getContractId() == null) {
            return;
        }
        String question = StrUtil.trim(userMessage.getContent());
        String answer = StrUtil.trim(assistantMessage.getContent());
        if (StrUtil.length(question) < memoryProperties.getFactExtractionMinChars()
                || StrUtil.isBlank(answer)) {
            return;
        }
        try {
            List<ExtractedMemory> memories = memoryProperties.isFactExtractionUseLlm()
                    ? extractMemoriesWithLlm(userMessage.getContractId(), question, answer)
                    : List.of(new ExtractedMemory(
                            LegalContractMemoryTypeEnum.FACT.getType(), buildFactSentence(question, answer)));
            for (ExtractedMemory memory : memories) {
                if (StrUtil.isBlank(memory.content())) {
                    continue;
                }
                persistExtractedMemory(userMessage, assistantMessage, memory);
            }
        } catch (Exception ex) {
            log.warn("[extractFactInternal][contractId={}] failed: {}",
                    userMessage.getContractId(), ex.getMessage());
        }
    }

    private void persistExtractedMemory(LegalContractChatMessageDO userMessage,
                                        LegalContractChatMessageDO assistantMessage,
                                        ExtractedMemory memory) {
        if (LegalContractMemoryTypeEnum.FACT.getType().equals(memory.memoryType())) {
            userFactService.saveUserFact(
                    userMessage.getUserId(),
                    userMessage.getContractId(),
                    userMessage.getSessionId(),
                    memory.content(),
                    assistantMessage.getId());
            return;
        }
        episodicMemoryService.saveMemory(
                userMessage.getContractId(),
                userMessage.getSessionId(),
                memory.memoryType(),
                memory.content(),
                assistantMessage.getId());
    }

    List<ExtractedMemory> extractMemoriesWithLlm(Long contractId, String question, String answer) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        if (contract == null || contract.getModelId() == null) {
            return List.of(new ExtractedMemory(
                    LegalContractMemoryTypeEnum.FACT.getType(), buildFactSentence(question, answer)));
        }
        AiLlmClient llmClient = aiModelService.getLlmClient(contract.getModelId());
        AiLlmRequest request = new AiLlmRequest()
                .setTemperature(0.2)
                .setMaxTokens(400);
        request.getMessages().add(new AiMessage()
                .setRole(AiMessageRoleEnum.SYSTEM)
                .setContent("""
                        你是法务合同问答记忆抽取器。根据用户问题与助手回答，提取 1~3 条可长期复用的摘要。
                        类型仅可使用：fact、milestone、risk、decision。
                        格式：每行一条，"- [类型] 摘要内容"；只写客观结论；每条不超过 120 字；无事实则只输出 NONE。
                        示例：- [risk] 违约金上限为合同总额 10%
                        """));
        request.getMessages().add(new AiMessage()
                .setRole(AiMessageRoleEnum.USER)
                .setContent("用户问题：\n" + question + "\n\n助手回答：\n" + answer));
        String response = StrUtil.trim(llmClient.call(request));
        List<ExtractedMemory> parsed = parseMemoryLines(response);
        if (parsed.isEmpty()) {
            return List.of(new ExtractedMemory(
                    LegalContractMemoryTypeEnum.FACT.getType(), buildFactSentence(question, answer)));
        }
        return parsed;
    }

    static List<ExtractedMemory> parseMemoryLines(String response) {
        if (StrUtil.isBlank(response) || "NONE".equalsIgnoreCase(StrUtil.trim(response))) {
            return List.of();
        }
        List<ExtractedMemory> memories = new ArrayList<>();
        for (String line : response.split("\\R")) {
            String trimmed = StrUtil.trim(line);
            if (StrUtil.isBlank(trimmed)) {
                continue;
            }
            if (trimmed.startsWith("- ")) {
                trimmed = trimmed.substring(2).trim();
            } else if (trimmed.startsWith("•")) {
                trimmed = trimmed.substring(1).trim();
            }
            ExtractedMemory memory = parseMemoryLine(trimmed);
            if (StrUtil.isNotBlank(memory.content())) {
                memories.add(memory);
            }
            if (memories.size() >= MAX_LLM_FACTS) {
                break;
            }
        }
        return memories;
    }

    static ExtractedMemory parseMemoryLine(String line) {
        if (line.startsWith("[") && line.contains("]")) {
            int end = line.indexOf(']');
            String type = StrUtil.trim(line.substring(1, end)).toLowerCase();
            String content = StrUtil.trim(line.substring(end + 1));
            if (ArrayUtil.contains(LegalContractMemoryTypeEnum.ARRAYS, type) && StrUtil.isNotBlank(content)) {
                return new ExtractedMemory(type, StrUtil.sub(content, 0, MAX_FACT_CHARS));
            }
        }
        return new ExtractedMemory(LegalContractMemoryTypeEnum.FACT.getType(),
                StrUtil.sub(line, 0, MAX_FACT_CHARS));
    }

    private static String buildFactSentence(String question, String answer) {
        String condensedAnswer = StrUtil.sub(answer.replaceAll("\\s+", " "), 0, MAX_FACT_CHARS);
        return "用户关注：" + StrUtil.sub(question, 0, 120) + "；结论：" + condensedAnswer;
    }

    record ExtractedMemory(String memoryType, String content) {
    }

}
