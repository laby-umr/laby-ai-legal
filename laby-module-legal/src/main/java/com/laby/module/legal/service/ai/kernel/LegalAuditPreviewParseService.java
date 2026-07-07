package com.laby.module.legal.service.ai.kernel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.exception.ServiceException;
import com.laby.framework.common.util.json.JsonUtils;
import com.laby.module.infra.api.file.FileApi;
import com.laby.module.legal.dal.dataobject.clause.LegalContractClauseDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;
import com.laby.module.legal.service.contract.LegalFormatConvertService;
import com.laby.module.legal.service.contract.bo.LegalClauseUnitBO;
import com.laby.module.legal.service.contract.util.LegalContractFormatUtils;
import com.laby.module.legal.service.contract.util.LegalContractStructureParser;
import com.laby.module.legal.service.contract.util.LegalContractStructureParser.StructureParseResult;
import com.laby.module.legal.service.contract.util.LegalContractWordParser.ParagraphItem;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.ORCHESTRATION_PREVIEW_PARSE_FAILED;

/**
 * 预览审核：从 infra 文件临时解析段落与条款（不落库）
 */
@Service
public class LegalAuditPreviewParseService {

    @Resource
    private FileApi fileApi;
    @Resource
    private LegalFormatConvertService formatConvertService;

    public PreviewParseResult parse(Long infraFileId, String fileName, int maxParagraphs) {
        byte[] content = fileApi.getFileContent(infraFileId);
        if (content == null || content.length == 0) {
            throw exception(ORCHESTRATION_PREVIEW_PARSE_FAILED);
        }
        if (LegalContractFormatUtils.isPdfFileName(fileName)) {
            throw exception(ORCHESTRATION_PREVIEW_PARSE_FAILED);
        }
        LegalContractSourceFormatEnum format = LegalContractFormatUtils.detectSourceFormat(fileName);
        try {
            byte[] docxBytes = content;
            if (format == LegalContractSourceFormatEnum.DOC) {
                docxBytes = formatConvertService.tryConvertDocToDocx(content, fileName);
                if (docxBytes == null) {
                    throw exception(ORCHESTRATION_PREVIEW_PARSE_FAILED);
                }
            }
            StructureParseResult structure = LegalContractStructureParser.parse(docxBytes);
            return buildResult(structure, maxParagraphs);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(ORCHESTRATION_PREVIEW_PARSE_FAILED);
        }
    }

    private static PreviewParseResult buildResult(StructureParseResult structure, int maxParagraphs) {
        return buildResultFromParagraphs(structure.getParagraphs(), maxParagraphs, structure.getClauses());
    }

    private static PreviewParseResult buildResultFromParagraphs(List<ParagraphItem> items, int maxParagraphs) {
        return buildResultFromParagraphs(items, maxParagraphs, List.of());
    }

    private static PreviewParseResult buildResultFromParagraphs(List<ParagraphItem> items, int maxParagraphs,
                                                                List<LegalClauseUnitBO> clauseUnits) {
        if (CollUtil.isEmpty(items)) {
            throw exception(ORCHESTRATION_PREVIEW_PARSE_FAILED);
        }
        int limit = maxParagraphs > 0 ? maxParagraphs : items.size();
        List<LegalContractParagraphDO> paragraphs = new ArrayList<>();
        for (int i = 0; i < Math.min(items.size(), limit); i++) {
            ParagraphItem item = items.get(i);
            paragraphs.add(LegalContractParagraphDO.builder()
                    .paragraphId(item.getParagraphId())
                    .sort(item.getSort())
                    .text(item.getText())
                    .path(item.getPath())
                    .skipAudit(false)
                    .build());
        }
        List<LegalContractClauseDO> clauses = toClauseDOs(clauseUnits);
        PreviewParseResult result = new PreviewParseResult();
        result.setParagraphs(paragraphs);
        result.setClauses(clauses);
        result.setTotalParagraphCount(items.size());
        return result;
    }

    private static List<LegalContractClauseDO> toClauseDOs(List<LegalClauseUnitBO> units) {
        if (CollUtil.isEmpty(units)) {
            return List.of();
        }
        List<LegalContractClauseDO> clauses = new ArrayList<>(units.size());
        for (LegalClauseUnitBO unit : units) {
            clauses.add(LegalContractClauseDO.builder()
                    .clauseId(unit.getClauseId())
                    .parentClauseId(unit.getParentClauseId())
                    .sort(unit.getSort())
                    .title(unit.getTitle())
                    .level(unit.getLevel())
                    .type(unit.getType() != null ? unit.getType().getCode() : null)
                    .path(unit.getPath())
                    .paragraphIds(CollUtil.isEmpty(unit.getParagraphIds())
                            ? null : JsonUtils.toJsonString(unit.getParagraphIds()))
                    .fullText(unit.getFullText())
                    .build());
        }
        return clauses;
    }

    @Data
    public static class PreviewParseResult {

        private List<LegalContractParagraphDO> paragraphs;

        private List<LegalContractClauseDO> clauses;

        private int totalParagraphCount;

    }

}
