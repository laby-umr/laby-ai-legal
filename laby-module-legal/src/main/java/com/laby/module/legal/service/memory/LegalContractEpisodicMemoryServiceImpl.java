package com.laby.module.legal.service.memory;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemoryPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemoryRespVO;
import com.laby.module.legal.dal.dataobject.memory.LegalContractMemoryDO;
import com.laby.module.legal.dal.mysql.memory.LegalContractMemoryMapper;
import com.laby.module.legal.enums.memory.LegalContractMemoryTypeEnum;
import com.laby.module.legal.framework.config.LegalContractMemoryProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_MEMORY_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_MEMORY_TYPE_INVALID;

/**
 * 合同情节记忆 CRUD 与 Agent 附录构建。
 */
@Service
public class LegalContractEpisodicMemoryServiceImpl implements LegalContractEpisodicMemoryService {

    @Resource
    private LegalContractMemoryMapper memoryMapper;
    @Resource
    private LegalContractMemoryProperties memoryProperties;

    private static final int DEFAULT_LIST_LIMIT = 50;

    @Override
    public PageResult<LegalContractMemoryRespVO> getMemoryPage(LegalContractMemoryPageReqVO pageReqVO) {
        PageResult<LegalContractMemoryDO> page = memoryMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(page, LegalContractMemoryRespVO.class);
    }

    @Override
    public List<LegalContractMemoryDO> listMemories(Long contractId, String sessionId) {
        if (contractId == null) {
            return List.of();
        }
        return memoryMapper.selectListByContractId(contractId, sessionId, DEFAULT_LIST_LIMIT);
    }

    @Override
    public String buildMemoryAppendix(Long contractId, String sessionId) {
        if (contractId == null) {
            return "";
        }
        List<LegalContractMemoryDO> memories = memoryMapper.selectListByContractId(
                contractId, sessionId, memoryProperties.getMaxAppendixItems());
        if (memories.isEmpty()) {
            return "";
        }
        String body = memories.stream()
                .map(item -> "- [" + item.getMemoryType() + "] " + StrUtil.trim(item.getContent()))
                .collect(Collectors.joining("\n"));
        return """

                <ContractMemory>
                %s
                </ContractMemory>
                """.formatted(body);
    }

    public void saveMemory(Long contractId, String sessionId, String memoryType, String content,
                           Long sourceMessageId) {
        if (contractId == null || StrUtil.isBlank(content)) {
            return;
        }
        String normalized = StrUtil.trim(content);
        String contentHash = DigestUtil.sha256Hex(normalized);
        LegalContractMemoryDO existing = memoryMapper.selectByContractIdAndHash(contractId, contentHash);
        if (existing != null) {
            memoryMapper.updateById(new LegalContractMemoryDO()
                    .setId(existing.getId())
                    .setContent(normalized)
                    .setSourceMessageId(sourceMessageId)
                    .setSessionId(StrUtil.blankToDefault(sessionId, existing.getSessionId())));
            return;
        }
        memoryMapper.insert(LegalContractMemoryDO.builder()
                .contractId(contractId)
                .sessionId(sessionId)
                .memoryType(memoryType)
                .content(normalized)
                .sourceMessageId(sourceMessageId)
                .contentHash(contentHash)
                .build());
    }

    public void saveFact(Long contractId, String sessionId, String fact, Long sourceMessageId) {
        saveMemory(contractId, sessionId, LegalContractMemoryTypeEnum.FACT.getType(), fact, sourceMessageId);
    }

    @Override
    public void deleteMemory(Long id, Long contractId) {
        validateMemory(id, contractId);
        memoryMapper.deleteById(id);
    }

    @Override
    public Long createMemory(Long contractId, String sessionId, String memoryType, String content) {
        validateMemoryType(memoryType);
        if (LegalContractMemoryTypeEnum.FACT.getType().equals(memoryType)) {
            throw exception(CONTRACT_MEMORY_TYPE_INVALID);
        }
        String normalized = StrUtil.trim(content);
        LegalContractMemoryDO row = LegalContractMemoryDO.builder()
                .contractId(contractId)
                .sessionId(sessionId)
                .memoryType(memoryType)
                .content(normalized)
                .contentHash(DigestUtil.sha256Hex(normalized))
                .build();
        memoryMapper.insert(row);
        return row.getId();
    }

    @Override
    public void updateMemory(Long id, Long contractId, String memoryType, String content) {
        if (id == null) {
            throw exception(CONTRACT_MEMORY_NOT_EXISTS);
        }
        validateMemoryType(memoryType);
        LegalContractMemoryDO existing = validateMemory(id, contractId);
        String normalized = StrUtil.trim(content);
        String contentHash = DigestUtil.sha256Hex(normalized);
        LegalContractMemoryDO duplicate = memoryMapper.selectByContractIdAndHash(contractId, contentHash);
        if (duplicate != null && !duplicate.getId().equals(existing.getId())) {
            memoryMapper.deleteById(duplicate.getId());
        }
        memoryMapper.updateById(new LegalContractMemoryDO()
                .setId(existing.getId())
                .setMemoryType(memoryType)
                .setContent(normalized)
                .setContentHash(contentHash));
    }

    private LegalContractMemoryDO validateMemory(Long id, Long contractId) {
        LegalContractMemoryDO memory = memoryMapper.selectById(id);
        if (memory == null || !memory.getContractId().equals(contractId)) {
            throw exception(CONTRACT_MEMORY_NOT_EXISTS);
        }
        return memory;
    }

    private void validateMemoryType(String memoryType) {
        if (!ArrayUtil.contains(LegalContractMemoryTypeEnum.ARRAYS, memoryType)) {
            throw exception(CONTRACT_MEMORY_TYPE_INVALID);
        }
    }

}
