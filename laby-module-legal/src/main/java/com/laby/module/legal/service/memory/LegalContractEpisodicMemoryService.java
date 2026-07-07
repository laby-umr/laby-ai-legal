package com.laby.module.legal.service.memory;

import com.laby.framework.common.pojo.PageResult;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemoryPageReqVO;
import com.laby.module.legal.controller.admin.contract.vo.LegalContractMemoryRespVO;
import com.laby.module.legal.dal.dataobject.memory.LegalContractMemoryDO;

import java.util.List;

/**
 * 合同情节记忆（P2）：里程碑、风险、决策等可检索摘要。
 */
public interface LegalContractEpisodicMemoryService {

    /**
     * 检索与合同相关的情节记忆摘要，供 Agent system 附录注入。
     */
    String buildMemoryAppendix(Long contractId, String sessionId);

    /**
     * 查询合同情节记忆列表（管理端 / 调试）。
     */
    List<LegalContractMemoryDO> listMemories(Long contractId, String sessionId);

    void deleteMemory(Long id, Long contractId);

    Long createMemory(Long contractId, String sessionId, String memoryType, String content);

    void updateMemory(Long id, Long contractId, String memoryType, String content);

    PageResult<LegalContractMemoryRespVO> getMemoryPage(LegalContractMemoryPageReqVO pageReqVO);

}
