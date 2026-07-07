package com.laby.module.legal.service.opinion;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionSourceTypeEnum;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTComment;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;

/**
 * 审核意见 Word 批注 / 段后标注排版（与审阅页意见卡片结构一致）。
 */
public final class LegalAuditOpinionAnnotationFormatter {

    private static final int MAX_CONTENT_CHARS = 600;
    private static final int MAX_SUGGESTION_CHARS = 400;
    private static final int MAX_REWRITE_CHARS = 500;
    private static final String META_COLOR = "666666";
    private static final String REWRITE_LABEL_COLOR = "0F766E";
    private static final String SUGGESTION_COLOR = "1D4ED8";

    private LegalAuditOpinionAnnotationFormatter() {
    }

    public static void populateWordComment(CTComment ctComment, LegalAuditOpinionDO opinion) {
        if (ctComment == null || opinion == null) {
            return;
        }
        while (ctComment.sizeOfPArray() > 0) {
            ctComment.removeP(0);
        }
        writeTitleParagraph(ctComment, opinion);
        writeLabeledParagraph(ctComment, "风险说明", truncate(opinion.getContent(), MAX_CONTENT_CHARS),
                false, false, null);
        writeLabeledParagraph(ctComment, "修改说明", truncate(opinion.getSuggestion(), MAX_SUGGESTION_CHARS),
                false, false, SUGGESTION_COLOR);
        if (StrUtil.isNotBlank(opinion.getOldText()) || StrUtil.isNotBlank(opinion.getNewText())) {
            if (isInsertClauseOpinion(opinion)) {
                writeSectionHeading(ctComment, "新增条款");
                writeLabeledParagraph(ctComment, "条款正文", truncate(opinion.getNewText(), MAX_REWRITE_CHARS),
                        false, true, REWRITE_LABEL_COLOR);
            } else {
                writeSectionHeading(ctComment, "采纳后改写");
                writeLabeledParagraph(ctComment, "原文", truncate(opinion.getOldText(), MAX_REWRITE_CHARS),
                        true, false, null);
                writeLabeledParagraph(ctComment, "改后正文", truncate(opinion.getNewText(), MAX_REWRITE_CHARS),
                        false, true, REWRITE_LABEL_COLOR);
            }
        }
        writeMetaParagraph(ctComment, opinion);
    }

    public static void writeFallbackAnnotation(XWPFParagraph paragraph, LegalAuditOpinionDO opinion) {
        if (paragraph == null || opinion == null) {
            return;
        }
        XWPFRun title = paragraph.createRun();
        title.setBold(true);
        title.setColor(resolveRiskColor(opinion.getRiskLevel()));
        title.setText(buildTitleLine(opinion));
        title.addBreak();

        appendFallbackLabeledLine(paragraph, "风险说明", truncate(opinion.getContent(), MAX_CONTENT_CHARS), false);
        appendFallbackLabeledLine(paragraph, "修改说明", truncate(opinion.getSuggestion(), MAX_SUGGESTION_CHARS), false);

        if (StrUtil.isNotBlank(opinion.getOldText()) || StrUtil.isNotBlank(opinion.getNewText())) {
            XWPFRun section = paragraph.createRun();
            section.setBold(true);
            section.setColor(REWRITE_LABEL_COLOR);
            section.setText(isInsertClauseOpinion(opinion) ? "新增条款" : "采纳后改写");
            section.addBreak();
            if (isInsertClauseOpinion(opinion)) {
                appendFallbackLabeledLine(paragraph, "条款正文", truncate(opinion.getNewText(), MAX_REWRITE_CHARS), false);
            } else {
                appendFallbackLabeledLine(paragraph, "原文", truncate(opinion.getOldText(), MAX_REWRITE_CHARS), true);
                appendFallbackLabeledLine(paragraph, "改后正文", truncate(opinion.getNewText(), MAX_REWRITE_CHARS), false);
            }
        }

        XWPFRun meta = paragraph.createRun();
        meta.setColor(META_COLOR);
        meta.setFontSize(8);
        meta.setText(buildMetaLine(opinion));
    }

    /** 供单测与预览使用的纯文本行（换行分隔） */
    public static String previewText(LegalAuditOpinionDO opinion) {
        if (opinion == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(buildTitleLine(opinion)).append('\n');
        appendPreviewLine(builder, "风险说明", opinion.getContent());
        appendPreviewLine(builder, "修改说明", opinion.getSuggestion());
        if (StrUtil.isNotBlank(opinion.getOldText()) || StrUtil.isNotBlank(opinion.getNewText())) {
            if (isInsertClauseOpinion(opinion)) {
                builder.append("新增条款\n");
                appendPreviewLine(builder, "条款正文", opinion.getNewText());
            } else {
                builder.append("采纳后改写\n");
                appendPreviewLine(builder, "原文", opinion.getOldText());
                appendPreviewLine(builder, "改后正文", opinion.getNewText());
            }
        }
        builder.append(buildMetaLine(opinion));
        return builder.toString().trim();
    }

    private static void writeTitleParagraph(CTComment ctComment, LegalAuditOpinionDO opinion) {
        CTP paragraph = ctComment.addNewP();
        appendRun(paragraph, buildTitleLine(opinion), true, resolveRiskColor(opinion.getRiskLevel()), false);
    }

    private static void writeSectionHeading(CTComment ctComment, String heading) {
        CTP paragraph = ctComment.addNewP();
        appendRun(paragraph, heading, true, REWRITE_LABEL_COLOR, false);
    }

    private static void writeLabeledParagraph(CTComment ctComment, String label, String body,
                                              boolean strike, boolean bodyBold, String bodyColor) {
        if (StrUtil.isBlank(body)) {
            return;
        }
        CTP paragraph = ctComment.addNewP();
        appendRun(paragraph, label + "：", true, null, false);
        appendRun(paragraph, body, bodyBold, bodyColor, strike);
    }

    private static void writeMetaParagraph(CTComment ctComment, LegalAuditOpinionDO opinion) {
        CTP paragraph = ctComment.addNewP();
        appendRun(paragraph, buildMetaLine(opinion), false, META_COLOR, false);
    }

    private static void appendRun(CTP paragraph, String text, boolean bold, String color, boolean strike) {
        if (StrUtil.isBlank(text)) {
            return;
        }
        CTR run = paragraph.addNewR();
        CTRPr rPr = run.addNewRPr();
        if (bold) {
            rPr.addNewB();
        }
        if (StrUtil.isNotBlank(color)) {
            rPr.addNewColor().setVal(color);
        }
        if (strike) {
            rPr.addNewStrike();
        }
        run.addNewT().setStringValue(text);
    }

    private static void appendFallbackLabeledLine(XWPFParagraph paragraph, String label, String body,
                                                  boolean strike) {
        if (StrUtil.isBlank(body)) {
            return;
        }
        XWPFRun labelRun = paragraph.createRun();
        labelRun.setBold(true);
        labelRun.setText(label + "：");
        XWPFRun bodyRun = paragraph.createRun();
        if (strike) {
            bodyRun.setStrikeThrough(true);
        }
        bodyRun.setText(body);
        bodyRun.addBreak();
    }

    private static void appendPreviewLine(StringBuilder builder, String label, String body) {
        if (StrUtil.isBlank(body)) {
            return;
        }
        builder.append(label).append('：').append(body.trim()).append('\n');
    }

    private static String buildTitleLine(LegalAuditOpinionDO opinion) {
        String risk = StrUtil.blankToDefault(opinion.getRiskLevel(), "-").toUpperCase();
        String title = StrUtil.blankToDefault(opinion.getTitle(), "审核意见");
        return "【" + risk + "】" + title;
    }

    private static String buildMetaLine(LegalAuditOpinionDO opinion) {
        StringBuilder meta = new StringBuilder();
        if (StrUtil.isNotBlank(opinion.getParagraphId())) {
            meta.append("段落 ").append(opinion.getParagraphId());
        }
        meta.append(meta.length() > 0 ? " · " : "");
        meta.append("改写 ").append(resolveChangeTypeLabel(opinion.getChangeType()));
        meta.append(" · 来源 ").append(resolveSourceLabel(opinion.getSourceType()));
        if (opinion.getAuditRound() != null) {
            meta.append(" · 第 ").append(opinion.getAuditRound()).append(" 轮");
        }
        return meta.toString();
    }

    private static String resolveChangeTypeLabel(String changeType) {
        String code = LegalAuditOpinionRewriteSupport.normalizeChangeTypeCode(changeType);
        for (LegalOpinionChangeTypeEnum item : LegalOpinionChangeTypeEnum.values()) {
            if (item.getCode().equals(code)) {
                return item.getName();
            }
        }
        return "仅提示";
    }

    private static String resolveSourceLabel(String sourceType) {
        if (StrUtil.isBlank(sourceType)) {
            return LegalOpinionSourceTypeEnum.AI.getName();
        }
        for (LegalOpinionSourceTypeEnum item : LegalOpinionSourceTypeEnum.values()) {
            if (item.getCode().equalsIgnoreCase(sourceType)) {
                return item.getName();
            }
        }
        return sourceType;
    }

    private static String resolveRiskColor(String riskLevel) {
        String risk = StrUtil.blankToDefault(riskLevel, "").toUpperCase();
        if ("HIGH".equals(risk)) {
            return "DC2626";
        }
        if ("MEDIUM".equals(risk)) {
            return "EA580C";
        }
        if ("LOW".equals(risk)) {
            return "2563EB";
        }
        return "1F2937";
    }

    private static boolean isInsertClauseOpinion(LegalAuditOpinionDO opinion) {
        if (opinion == null || StrUtil.isBlank(opinion.getNewText())) {
            return false;
        }
        String changeType = LegalAuditOpinionRewriteSupport.normalizeChangeTypeCode(opinion.getChangeType());
        if (LegalOpinionChangeTypeEnum.INSERT_BEFORE.getCode().equals(changeType)
                || LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode().equals(changeType)) {
            return true;
        }
        return StrUtil.isBlank(opinion.getOldText())
                && LegalAuditOpinionRewriteSupport.isMissingClauseOpinion(opinion);
    }

    private static String truncate(String text, int max) {
        return StrUtil.isBlank(text) ? "" : StrUtil.sub(text.trim(), 0, max);
    }

}
