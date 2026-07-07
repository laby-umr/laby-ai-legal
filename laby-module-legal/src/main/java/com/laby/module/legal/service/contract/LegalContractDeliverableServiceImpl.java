package com.laby.module.legal.service.contract;



import cn.hutool.core.collection.CollUtil;

import cn.hutool.core.util.StrUtil;

import com.laby.module.legal.dal.dataobject.contract.LegalContractDO;

import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;

import com.laby.module.legal.dal.dataobject.contract.LegalContractParagraphDO;

import com.laby.module.legal.dal.mysql.contract.LegalContractMapper;

import com.laby.module.legal.dal.mysql.contract.LegalContractParagraphMapper;

import com.laby.module.legal.dal.mysql.opinion.LegalAuditOpinionMapper;

import com.laby.module.legal.enums.contract.LegalContractDeliverableEnum;

import com.laby.module.legal.enums.contract.LegalContractSourceFormatEnum;

import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;

import com.laby.module.legal.service.contract.bo.LegalContractFileDownloadBO;

import com.laby.module.legal.service.contract.util.LegalContractDocxRenderUtil;

import com.laby.module.legal.service.contract.util.LegalContractFormatUtils;

import com.laby.module.legal.service.contract.util.LegalContractPdfAnnotateService;

import com.laby.module.legal.service.contract.util.LegalExceptionUtils;

import com.laby.module.legal.service.opinion.LegalAuditOpinionRewriteSupport;

import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;



import java.util.List;

import java.util.Map;

import java.util.function.Function;

import java.util.stream.Collectors;



import static com.laby.framework.common.exception.util.ServiceExceptionUtil.exception;

import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_FILE_FORMAT_NOT_SUPPORTED;

import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_NOT_EXISTS;

import static com.laby.module.legal.enums.ErrorCodeConstants.CONTRACT_WORKING_NOT_READY;



/**

 * 合同四件套按需生成 Service 实现（DELIV-001 §10/§11，仅 Word）。

 */

@Slf4j

@Service

public class LegalContractDeliverableServiceImpl implements LegalContractDeliverableService {



    @Resource

    private LegalContractMapper contractMapper;

    @Resource

    private LegalAuditOpinionMapper opinionMapper;

    @Resource

    private LegalContractVersionService contractVersionService;

    @Resource

    private LegalContractParagraphMapper paragraphMapper;

    @Resource

    private LegalContractPdfAnnotateService pdfAnnotateService;



    @Override

    public LegalContractFileDownloadBO generate(Long contractId, LegalContractDeliverableEnum deliverable,

                                                Integer auditRound) {

        LegalContractDO contract = requireContract(contractId);

        if (deliverable != LegalContractDeliverableEnum.ORIGINAL

                && !(deliverable == LegalContractDeliverableEnum.ANNOTATED

                && LegalContractFormatUtils.isPdfContract(contract))) {

            ensureWordContract(contract);

        }

        int round = auditRound != null ? auditRound : resolveAuditRound(contract);

        return switch (deliverable) {

            case ORIGINAL -> generateOriginal(contract);

            case ANNOTATED -> generateAnnotated(contract, round);

            case REVISION -> generateRevision(contract, round);

            case ADOPTED -> generateAdopted(contract, round);

        };

    }



    /**

     * 标注版意见集合：当前轮 {@code PENDING + ADOPTED}，排除 {@code IGNORED}（DELIV-001 §7.2）。

     */

    static List<LegalAuditOpinionDO> filterAnnotatedOpinions(List<LegalAuditOpinionDO> opinions) {

        return opinions.stream()

                .filter(item -> LegalOpinionStatusEnum.PENDING.getStatus().equals(item.getStatus())

                        || LegalOpinionStatusEnum.ADOPTED.getStatus().equals(item.getStatus()))

                .toList();

    }



    /**

     * 修订版意见集合：当前轮已采纳且可写回正文（DELIV-001 §7.2）。

     */

    static List<LegalAuditOpinionDO> filterRevisionOpinions(List<LegalAuditOpinionDO> opinions) {

        return opinions.stream()

                .filter(item -> LegalOpinionStatusEnum.ADOPTED.getStatus().equals(item.getStatus()))

                .filter(LegalAuditOpinionRewriteSupport::isAdoptApplicableToDocument)

                .toList();

    }



    private LegalContractFileDownloadBO generateOriginal(LegalContractDO contract) {

        byte[] bytes = contractVersionService.readOriginalBytes(contract.getId());

        String ext = resolveOriginalExtension(contract);

        String fileName = buildFileName(contract, "源文件", ext);

        return new LegalContractFileDownloadBO(fileName, bytes);

    }



    private LegalContractFileDownloadBO generateAnnotated(LegalContractDO contract, int auditRound) {

        List<LegalAuditOpinionDO> opinions = filterAnnotatedOpinions(

                opinionMapper.selectListByContractIdAndRound(contract.getId(), auditRound));

        byte[] base = contractVersionService.readOriginalBytes(contract.getId());

        if (LegalContractFormatUtils.isPdfContract(contract)) {

            byte[] rendered = renderAnnotatedPdf(contract.getId(), auditRound, base, opinions);

            return new LegalContractFileDownloadBO(

                    buildFileName(contract, "标注版-第" + auditRound + "轮", "pdf"), rendered);

        }

        byte[] rendered = renderAnnotatedDocx(contract.getId(), auditRound, base, opinions);

        return new LegalContractFileDownloadBO(

                buildFileName(contract, "标注版-第" + auditRound + "轮", "docx"), rendered);

    }



    private LegalContractFileDownloadBO generateRevision(LegalContractDO contract, int auditRound) {

        List<LegalAuditOpinionDO> adopted = filterRevisionOpinions(

                opinionMapper.selectListByContractIdAndRound(contract.getId(), auditRound));

        byte[] base = contractVersionService.readOriginalBytes(contract.getId());

        byte[] bookmarked;

        try {

            bookmarked = LegalContractDocxRenderUtil.insertParagraphBookmarks(base);

        } catch (Exception ex) {

            LegalExceptionUtils.rethrowServiceException(ex);

            log.warn("[generateRevision][contractId={} round={}] Bookmark 写入失败，降级源文件",

                    contract.getId(), auditRound, ex);

            bookmarked = base;

        }

        byte[] rendered = renderRevisionDocx(contract.getId(), auditRound, bookmarked, adopted);

        return new LegalContractFileDownloadBO(

                buildFileName(contract, "修订版-第" + auditRound + "轮", "docx"), rendered);

    }



    private LegalContractFileDownloadBO generateAdopted(LegalContractDO contract, int auditRound) {

        byte[] working = contractVersionService.readWorkingBytes(contract.getId(), auditRound);

        if (working == null) {

            throw exception(CONTRACT_WORKING_NOT_READY);

        }

        byte[] rendered;

        try {

            rendered = LegalContractDocxRenderUtil.stripTrackChanges(working);

            rendered = LegalContractDocxRenderUtil.stripComments(rendered);

        } catch (Exception ex) {

            LegalExceptionUtils.rethrowServiceException(ex);

            log.warn("[generateAdopted][contractId={} round={}] 去批注/修订失败，降级 WORKING 原件",

                    contract.getId(), auditRound, ex);

            rendered = working;

        }

        return new LegalContractFileDownloadBO(

                buildFileName(contract, "采纳版-第" + auditRound + "轮", "docx"), rendered);

    }



    private byte[] renderAnnotatedPdf(Long contractId, int auditRound, byte[] base,

                                      List<LegalAuditOpinionDO> opinions) {

        Map<String, LegalContractParagraphDO> paragraphMap = paragraphMapper.selectListByContractId(contractId)

                .stream()

                .collect(Collectors.toMap(LegalContractParagraphDO::getParagraphId, Function.identity(), (a, b) -> a));

        try {

            return pdfAnnotateService.annotate(base, opinions, paragraphMap);

        } catch (Exception ex) {

            LegalExceptionUtils.rethrowServiceException(ex);

            log.warn("[renderAnnotatedPdf][contractId={} round={}] PDF 标注失败，降级源文件", contractId, auditRound, ex);

            return base;

        }

    }



    private byte[] renderAnnotatedDocx(Long contractId, int auditRound, byte[] base,

                                       List<LegalAuditOpinionDO> opinions) {

        try {

            return LegalContractDocxRenderUtil.renderAnnotated(base, opinions);

        } catch (Exception ex) {

            LegalExceptionUtils.rethrowServiceException(ex);

            log.warn("[renderAnnotatedDocx][contractId={} round={}] 标注渲染失败，降级源文件", contractId, auditRound, ex);

            return base;

        }

    }



    private byte[] renderRevisionDocx(Long contractId, int auditRound, byte[] base,

                                      List<LegalAuditOpinionDO> adopted) {

        if (CollUtil.isEmpty(adopted)) {

            return base;

        }

        try {

            return LegalContractDocxRenderUtil.renderAdoptedTracked(base, adopted);

        } catch (Exception ex) {

            LegalExceptionUtils.rethrowServiceException(ex);

            log.warn("[renderRevisionDocx][contractId={} round={}] 修订渲染失败，降级源文件", contractId, auditRound, ex);

            return base;

        }

    }



    private LegalContractDO requireContract(Long contractId) {

        LegalContractDO contract = contractMapper.selectById(contractId);

        if (contract == null) {

            throw exception(CONTRACT_NOT_EXISTS);

        }

        return contract;

    }



    private static void ensureWordContract(LegalContractDO contract) {

        if (LegalContractFormatUtils.isPdfContract(contract)) {

            throw exception(CONTRACT_FILE_FORMAT_NOT_SUPPORTED);

        }

    }



    private static int resolveAuditRound(LegalContractDO contract) {

        return contract.getAuditRound() == null ? 1 : contract.getAuditRound();

    }



    private static String buildFileName(LegalContractDO contract, String suffix, String ext) {

        return StrUtil.blankToDefault(contract.getTitle(), "合同") + "-" + suffix + "." + ext;

    }



    private static String resolveOriginalExtension(LegalContractDO contract) {

        if (StrUtil.isNotBlank(contract.getOriginalFileName()) && contract.getOriginalFileName().contains(".")) {

            return contract.getOriginalFileName()

                    .substring(contract.getOriginalFileName().lastIndexOf('.') + 1)

                    .toLowerCase();

        }

        LegalContractSourceFormatEnum format = LegalContractSourceFormatEnum.of(contract.getSourceFormat());

        if (format == LegalContractSourceFormatEnum.DOC) {

            return "doc";

        }

        return "docx";

    }



}

