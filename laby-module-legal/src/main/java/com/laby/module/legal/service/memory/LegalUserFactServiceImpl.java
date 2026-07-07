package com.laby.module.legal.service.memory;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.laby.framework.common.pojo.PageResult;
import com.laby.framework.common.util.object.BeanUtils;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactRespVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalUserFactSaveReqVO;
import com.laby.module.legal.dal.dataobject.memory.LegalUserFactDO;
import com.laby.module.legal.dal.mysql.memory.LegalUserFactMapper;
import com.laby.module.legal.framework.config.LegalContractMemoryProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_MEMORY_NOT_EXISTS;

@Service
public class LegalUserFactServiceImpl implements LegalUserFactService {

    @Resource
    private LegalUserFactMapper userFactMapper;
    @Resource
    private LegalContractMemoryProperties memoryProperties;

    @Override
    public PageResult<LegalUserFactRespVO> getUserFactPage(LegalUserFactPageReqVO pageReqVO) {
        PageResult<LegalUserFactDO> page = userFactMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(page, LegalUserFactRespVO.class);
    }

    @Override
    public Long createUserFact(LegalUserFactSaveReqVO reqVO) {
        String normalized = StrUtil.trim(reqVO.getContent());
        LegalUserFactDO row = LegalUserFactDO.builder()
                .userId(reqVO.getUserId())
                .contractId(reqVO.getContractId())
                .sessionId(reqVO.getSessionId())
                .content(normalized)
                .contentHash(DigestUtil.sha256Hex(normalized))
                .build();
        userFactMapper.insert(row);
        return row.getId();
    }

    @Override
    public void updateUserFact(LegalUserFactSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw exception(CONTRACT_MEMORY_NOT_EXISTS);
        }
        LegalUserFactDO existing = userFactMapper.selectById(reqVO.getId());
        if (existing == null) {
            throw exception(CONTRACT_MEMORY_NOT_EXISTS);
        }
        String normalized = StrUtil.trim(reqVO.getContent());
        userFactMapper.updateById(new LegalUserFactDO()
                .setId(existing.getId())
                .setContent(normalized)
                .setContentHash(DigestUtil.sha256Hex(normalized))
                .setContractId(reqVO.getContractId())
                .setSessionId(reqVO.getSessionId()));
    }

    @Override
    public void deleteUserFact(Long id) {
        if (userFactMapper.selectById(id) == null) {
            throw exception(CONTRACT_MEMORY_NOT_EXISTS);
        }
        userFactMapper.deleteById(id);
    }

    @Override
    public void saveUserFact(Long userId, Long contractId, String sessionId, String content, Long sourceMessageId) {
        if (userId == null || StrUtil.isBlank(content)) {
            return;
        }
        String normalized = StrUtil.trim(content);
        String contentHash = DigestUtil.sha256Hex(normalized);
        LegalUserFactDO existing = userFactMapper.selectByUserAndHash(userId, contentHash);
        if (existing != null) {
            userFactMapper.updateById(new LegalUserFactDO()
                    .setId(existing.getId())
                    .setContent(normalized)
                    .setContractId(contractId)
                    .setSessionId(StrUtil.blankToDefault(sessionId, existing.getSessionId()))
                    .setSourceMessageId(sourceMessageId));
            return;
        }
        userFactMapper.insert(LegalUserFactDO.builder()
                .userId(userId)
                .contractId(contractId)
                .sessionId(sessionId)
                .content(normalized)
                .contentHash(contentHash)
                .sourceMessageId(sourceMessageId)
                .build());
    }

    @Override
    public String buildUserFactAppendix(Long contractId, Long userId, int limit) {
        if (userId == null) {
            return "";
        }
        List<LegalUserFactDO> facts = userFactMapper.selectListByContractAndUser(contractId, userId, limit);
        if (facts.isEmpty()) {
            return "";
        }
        String body = facts.stream()
                .map(item -> "- " + StrUtil.trim(item.getContent()))
                .collect(Collectors.joining("\n"));
        return """

                <UserFacts>
                %s
                </UserFacts>
                """.formatted(body);
    }

    @Override
    public List<LegalUserFactRespVO> listByContractAndUser(Long contractId, Long userId, int limit) {
        return BeanUtils.toBean(userFactMapper.selectListByContractAndUser(contractId, userId, limit),
                LegalUserFactRespVO.class);
    }

}
