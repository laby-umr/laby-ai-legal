package com.laby.module.legal.service.contract.util;

import cn.hutool.core.collection.CollUtil;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import com.laby.module.legal.dal.mysql.clause.LegalContractClauseMapper;
import com.laby.module.legal.enums.clause.LegalClauseTypeEnum;
import com.laby.module.legal.service.contract.bo.LegalClauseUnitBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 条款结构持久化
 */
@Slf4j
@Component
public class LegalContractClausePersistHelper {

    @Resource
    private LegalContractClauseMapper clauseMapper;

    public void replaceClauses(Long contractId, List<LegalClauseUnitBO> clauses) {
        clauseMapper.deleteByContractId(contractId);
        if (CollUtil.isEmpty(clauses)) {
            return;
        }
        for (LegalClauseUnitBO clause : clauses) {
            clauseMapper.insert(LegalContractClauseDO.builder()
                    .contractId(contractId)
                    .clauseId(clause.getClauseId())
                    .parentClauseId(clause.getParentClauseId())
                    .sort(clause.getSort())
                    .title(clause.getTitle())
                    .level(clause.getLevel())
                    .type(clause.getType() != null ? clause.getType().getCode() : LegalClauseTypeEnum.CLAUSE.getCode())
                    .path(clause.getPath())
                    .paragraphIds(CollUtil.isEmpty(clause.getParagraphIds())
                            ? null : JsonUtils.toJsonString(clause.getParagraphIds()))
                    .fullText(clause.getFullText())
                    .build());
        }
        log.info("[replaceClauses][contractId={}] 写入 {} 个条款", contractId, clauses.size());
    }

    public String findClauseIdByParagraphId(Long contractId, String paragraphId) {
        if (contractId == null || paragraphId == null) {
            return null;
        }
        for (LegalContractClauseDO clause : clauseMapper.selectListByContractId(contractId)) {
            List<String> ids = JsonUtils.parseArray(clause.getParagraphIds(), String.class);
            if (CollUtil.contains(ids, paragraphId)) {
                return clause.getClauseId();
            }
        }
        return null;
    }

}
