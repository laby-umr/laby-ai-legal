package com.laby.module.legal.service.contract;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.framework.common.exception.ServiceException;
import com.laby.module.infra.api.file.FileApi;
import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractFileDO;
import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;
import com.laby.module.legal.dal.mysql.contract.LegalContractFileMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;
import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;
import com.laby.module.legal.enums.LegalContractConstants;
import com.laby.module.legal.enums.contract.LegalContractFileRoleEnum;
import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;
import com.laby.module.legal.enums.contract.LegalContractStatusEnum;
import com.laby.module.legal.enums.contract.LegalContractTaskKeyEnum;
import com.laby.module.legal.enums.contract.LegalParseStatusEnum;
import com.laby.module.legal.service.contract.bo.LegalClauseUnitBO;
import com.laby.module.legal.service.contract.util.LegalContractClausePersistHelper;
import com.laby.module.legal.service.contract.util.LegalContractFormatUtils;
import com.laby.module.legal.service.contract.util.LegalContractStructureParser;
import com.laby.module.legal.service.contract.util.LegalContractStructureParser.StructureParseResult;
import com.laby.module.legal.service.contract.util.LegalContractWordParser.ParagraphItem;
import com.laby.module.legal.service.embedding.LegalContractParagraphEmbeddingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_MAIN_FILE_EMPTY;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_PARSE_IN_PROGRESS;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_PARSE_NO_PARAGRAPH;
import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_PARSE_NOT_SUCCESS;

/**
 * 法务合同解析 Service 实现类
 */
@Slf4j
@Service
public class LegalContractParseServiceImpl implements LegalContractParseService {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Resource
    private LegalContractMapper contractMapper;
    @Resource
    private LegalContractParagraphMapper paragraphMapper;
    @Resource
    private LegalContractFileMapper contractFileMapper;
    @Resource
    private FileApi fileApi;
    @Resource
    private LegalContractParagraphEmbeddingService paragraphEmbeddingService;
    @Resource
    private LegalContractClausePersistHelper clausePersistHelper;
    @Resource
    private LegalFormatConvertService formatConvertService;

    @Override
    public void parseForBpm(Long contractId) {
        doParseOnly(contractId);
    }

    @Override
    public void reparseFromDocxBytes(Long contractId, byte[] content) {
        if (content == null || content.length == 0) {
            log.warn("[reparseFromDocxBytes][contractId={}] 内容为空，跳过", contractId);
            return;
        }
        try {
            StructureParseResult structure = LegalContractStructureParser.parse(content);
            List<ParagraphItem> items = structure.getParagraphs();
            if (items.isEmpty()) {
                throw exception(CONTRACT_PARSE_NO_PARAGRAPH);
            }
            paragraphEmbeddingService.deleteByContractId(contractId);
            paragraphMapper.deleteByContractId(contractId);
            persistParagraphs(contractId, items);
            clausePersistHelper.replaceClauses(contractId, structure.getClauses());
            paragraphEmbeddingService.embedContractAsync(contractId);
            log.info("[reparseFromDocxBytes][contractId={}] 已重解析 {} 个段落", contractId, items.size());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[reparseFromDocxBytes][contractId={}] 重解析失败", contractId, ex);
            throw exception(CONTRACT_PARSE_NOT_SUCCESS);
        }
    }

    private void doParseOnly(Long contractId) {
        LegalContractDO contract = contractMapper.selectById(contractId);
        validateBeforeParse(contract);

        if (LegalParseStatusEnum.SUCCESS.getStatus().equals(contract.getParseStatus())) {
            log.info("[doParseOnly][contractId={}] 已解析成功，跳过", contractId);
            return;
        }

        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setParseStatus(LegalParseStatusEnum.RUNNING.getStatus())
                .setStatus(LegalContractStatusEnum.PARSING.getStatus()));
        try {
            contract = contractMapper.selectById(contractId);
            if (contract == null) {
                throw exception(CONTRACT_NOT_EXISTS);
            }
            Long fileId = contract.getMainFileId();
            if (fileId == null) {
                throw exception(CONTRACT_MAIN_FILE_EMPTY);
            }
            byte[] content = fileApi.getFileContent(fileId);
            LegalContractFileDO mainFile = contractFileMapper.selectByFileId(fileId);
            LegalContractSourceFormatEnum sourceFormat = resolveSourceFormat(contract, mainFile);

            List<ParagraphItem> items;
            List<LegalClauseUnitBO> clauses = Collections.emptyList();

            if (sourceFormat == LegalContractSourceFormatEnum.PDF) {
                markPartial(contractId, "PDF_UNSUPPORTED");
                return;
            }
            if (sourceFormat == LegalContractSourceFormatEnum.DOC) {
                byte[] normalized = formatConvertService.tryConvertDocToDocx(
                        content, mainFile != null ? mainFile.getFileName() : "contract.doc");
                if (normalized == null) {
                    markPartial(contractId, "DOC_CONVERT_PENDING");
                    return;
                }
                saveNormalizedDocx(contractId, fileId, normalized,
                        mainFile != null ? mainFile.getFileName() : "contract.doc");
                StructureParseResult structure = LegalContractStructureParser.parse(normalized);
                items = structure.getParagraphs();
                clauses = structure.getClauses();
            } else {
                StructureParseResult structure = LegalContractStructureParser.parse(content);
                items = structure.getParagraphs();
                clauses = structure.getClauses();
            }

            if (items.isEmpty()) {
                throw exception(CONTRACT_PARSE_NO_PARAGRAPH);
            }

            paragraphEmbeddingService.deleteByContractId(contractId);
            paragraphMapper.deleteByContractId(contractId);
            persistParagraphs(contractId, items);
            clausePersistHelper.replaceClauses(contractId, clauses);

            contractMapper.updateById(new LegalContractDO()
                    .setId(contractId)
                    .setParseStatus(LegalParseStatusEnum.SUCCESS.getStatus())
                    .setCurrentTaskKey(LegalContractTaskKeyEnum.PARSE_CONTRACT.getKey()));
            paragraphEmbeddingService.embedContractAsync(contractId);
        } catch (ServiceException ex) {
            markParseFailed(contractId);
            throw ex;
        } catch (Exception ex) {
            log.error("[doParseOnly][contractId={}] 解析失败", contractId, ex);
            markParseFailed(contractId);
            throw exception(CONTRACT_PARSE_NOT_SUCCESS);
        }
    }

    /**
     * 解析前状态校验（单测可覆盖）
     */
    static void validateBeforeParse(LegalContractDO contract) {
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        if (LegalParseStatusEnum.RUNNING.getStatus().equals(contract.getParseStatus())) {
            throw exception(CONTRACT_PARSE_IN_PROGRESS);
        }
    }

    private void markParseFailed(Long contractId) {
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setParseStatus(LegalParseStatusEnum.FAILED.getStatus()));
    }

    private void markPartial(Long contractId, String detail) {
        contractMapper.updateById(new LegalContractDO()
                .setId(contractId)
                .setParseStatus(LegalParseStatusEnum.PARTIAL.getStatus())
                .setCurrentTaskKey(LegalContractTaskKeyEnum.PARSE_CONTRACT.getKey()));
        log.info("[doParseOnly][contractId={}] 解析部分可用：{}", contractId, detail);
    }

    private static LegalContractSourceFormatEnum resolveSourceFormat(LegalContractDO contract,
                                                                     LegalContractFileDO mainFile) {
        LegalContractSourceFormatEnum format = LegalContractSourceFormatEnum.of(contract.getSourceFormat());
        if (format != null) {
            return format;
        }
        if (mainFile != null && StrUtil.isNotBlank(mainFile.getFormat())) {
            format = LegalContractSourceFormatEnum.of(mainFile.getFormat());
        }
        if (format != null) {
            return format;
        }
        return LegalContractFormatUtils.detectSourceFormat(mainFile != null ? mainFile.getFileName() : null);
    }

    private void saveNormalizedDocx(Long contractId, Long sourceFileId, byte[] docxBytes, String originalName) {
        String fileName = FileUtil.mainName(StrUtil.blankToDefault(originalName, "contract")) + "-normalized.docx";
        Long normalizedFileId = fileApi.createFileReturnId(
                docxBytes, fileName, LegalContractConstants.CONTRACT_FILE_DIRECTORY, DOCX_CONTENT_TYPE);
        contractFileMapper.insert(LegalContractFileDO.builder()
                .contractId(contractId)
                .fileId(normalizedFileId)
                .fileName(fileName)
                .mainFlag(false)
                .role(LegalContractFileRoleEnum.NORMALIZED_DOCX.getRole())
                .format(LegalContractSourceFormatEnum.DOCX.getFormat())
                .sourceFileId(sourceFileId)
                .build());
    }

    private void persistParagraphs(Long contractId, List<ParagraphItem> items) {
        for (ParagraphItem item : items) {
            paragraphMapper.insert(LegalContractParagraphDO.builder()
                    .contractId(contractId)
                    .paragraphId(item.getParagraphId())
                    .sort(item.getSort())
                    .text(item.getText())
                    .path(item.getPath())
                    .build());
        }
    }

}
