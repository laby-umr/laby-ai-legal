package com.laby.module.legal.service.memory;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.contract.LegalContractChatMessageDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractChatMessageMapper;
import com.laby.module.legal.dal.mysql.memory.LegalContractMemoryMapper;
import com.laby.module.legal.dal.mysql.memory.LegalUserFactMapper;
import com.laby.module.legal.framework.config.LegalContractMemoryProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定时补抽：为尚未生成记忆的问答对异步提取事实/情节记忆。
 */
@Slf4j
@Service
public class LegalContractMemoryExtractionBackfillService {

    @Resource
    private LegalContractMemoryProperties memoryProperties;
    @Resource
    private LegalContractChatMessageMapper chatMessageMapper;
    @Resource
    private LegalContractMemoryMapper contractMemoryMapper;
    @Resource
    private LegalUserFactMapper userFactMapper;
    @Resource
    private LegalContractMemoryFactExtractor memoryFactExtractor;

    public int backfillRecentUnprocessed(int batchSize) {
        if (!memoryProperties.isFactExtractionEnabled()) {
            return 0;
        }
        List<LegalContractChatMessageDO> candidates = chatMessageMapper
                .selectListUnprocessedForMemoryExtraction(batchSize);
        int processed = 0;
        for (LegalContractChatMessageDO assistant : candidates) {
            if (assistant == null || assistant.getReplyId() == null) {
                continue;
            }
            if (contractMemoryMapper.existsBySourceMessageId(assistant.getId())
                    || userFactMapper.existsBySourceMessageId(assistant.getId())) {
                continue;
            }
            LegalContractChatMessageDO user = chatMessageMapper.selectById(assistant.getReplyId());
            if (user == null || StrUtil.isBlank(user.getContent()) || StrUtil.isBlank(assistant.getContent())) {
                continue;
            }
            memoryFactExtractor.extractFactSync(user, assistant);
            processed++;
        }
        if (processed > 0) {
            log.info("[backfillRecentUnprocessed] processed {} chat turns", processed);
        }
        return processed;
    }

}
