package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.enums.clause.LegalClauseTypeEnum;
import com.laby.module.legal.service.contract.bo.LegalClauseUnitBO;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 由段落列表 + POI 段落样式构建条款树。
 */
public final class LegalClauseBuilder {

    private static final Pattern CN_ARTICLE = Pattern.compile("^第[一二三四五六七八九十百零〇]+条");
    private static final Pattern NUM_OUTLINE = Pattern.compile("^\\d+(\\.\\d+)+[\\s、.]");
    private static final Pattern NUM_SINGLE = Pattern.compile("^\\d+[、.．]\\s*\\S+");

    private LegalClauseBuilder() {
    }

    public static List<LegalClauseUnitBO> build(List<LegalContractWordParser.ParagraphItem> paragraphs,
                                                List<XWPFParagraph> documentParagraphs) {
        List<LegalClauseUnitBO> clauses = new ArrayList<>();
        if (paragraphs == null || paragraphs.isEmpty()) {
            return clauses;
        }
        int clauseSort = 0;
        LegalClauseUnitBO current = null;
        int docIndex = 0;
        for (LegalContractWordParser.ParagraphItem item : paragraphs) {
            XWPFParagraph poiParagraph = findNextNonBlankParagraph(documentParagraphs, docIndex);
            if (poiParagraph != null) {
                docIndex = documentParagraphs.indexOf(poiParagraph) + 1;
            }
            boolean heading = isHeading(poiParagraph, item.getText());
            if (heading) {
                current = flushClause(clauses, current);
                clauseSort++;
                int level = detectLevel(item.getText(), poiParagraph);
                current = LegalClauseUnitBO.builder()
                        .clauseId("c-" + clauseSort)
                        .sort(clauseSort)
                        .title(item.getText())
                        .level(level)
                        .type(level <= 1 ? LegalClauseTypeEnum.SECTION : LegalClauseTypeEnum.CLAUSE)
                        .path(item.getText())
                        .build();
                current.getParagraphIds().add(item.getParagraphId());
                appendText(current, item.getText());
            } else {
                if (current == null) {
                    clauseSort++;
                    current = LegalClauseUnitBO.builder()
                            .clauseId("c-" + clauseSort)
                            .sort(clauseSort)
                            .level(0)
                            .type(LegalClauseTypeEnum.CLAUSE)
                            .path("正文")
                            .build();
                }
                current.getParagraphIds().add(item.getParagraphId());
                appendText(current, item.getText());
            }
        }
        flushClause(clauses, current);
        return clauses;
    }

    private static LegalClauseUnitBO flushClause(List<LegalClauseUnitBO> clauses, LegalClauseUnitBO current) {
        if (current != null && !current.getParagraphIds().isEmpty()) {
            clauses.add(current);
        }
        return null;
    }

    private static void appendText(LegalClauseUnitBO clause, String text) {
        if (StrUtil.isBlank(text)) {
            return;
        }
        if (StrUtil.isBlank(clause.getFullText())) {
            clause.setFullText(text);
        } else {
            clause.setFullText(clause.getFullText() + "\n" + text);
        }
    }

    static boolean isHeading(XWPFParagraph paragraph, String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        if (paragraph != null && paragraph.getStyle() != null) {
            String style = paragraph.getStyle().toLowerCase();
            if (style.contains("heading") || style.contains("标题")) {
                return true;
            }
        }
        String trimmed = StrUtil.trim(text);
        if (CN_ARTICLE.matcher(trimmed).find()) {
            return true;
        }
        if (NUM_OUTLINE.matcher(trimmed).find()) {
            return true;
        }
        return trimmed.length() <= 40 && NUM_SINGLE.matcher(trimmed).find();
    }

    static int detectLevel(String text, XWPFParagraph paragraph) {
        if (paragraph != null && paragraph.getStyle() != null) {
            String style = paragraph.getStyle().toLowerCase();
            if (style.contains("heading1") || style.contains("1")) {
                return 1;
            }
            if (style.contains("heading2") || style.contains("2")) {
                return 2;
            }
            if (style.contains("heading3") || style.contains("3")) {
                return 3;
            }
        }
        String trimmed = StrUtil.trim(text);
        if (CN_ARTICLE.matcher(trimmed).find()) {
            return 3;
        }
        int dots = countOutlineDepth(trimmed);
        if (dots > 0) {
            return Math.min(dots, 3);
        }
        return 1;
    }

    private static int countOutlineDepth(String text) {
        var matcher = Pattern.compile("^(\\d+(\\.\\d+)*)").matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        return matcher.group(1).split("\\.").length;
    }

    private static XWPFParagraph findNextNonBlankParagraph(List<XWPFParagraph> paragraphs, int startIndex) {
        if (paragraphs == null) {
            return null;
        }
        for (int i = startIndex; i < paragraphs.size(); i++) {
            if (StrUtil.isNotBlank(StrUtil.trim(paragraphs.get(i).getText()))) {
                return paragraphs.get(i);
            }
        }
        return null;
    }

}
