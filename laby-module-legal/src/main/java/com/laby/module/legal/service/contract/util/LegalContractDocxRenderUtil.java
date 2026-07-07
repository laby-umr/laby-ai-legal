package com.laby.module.legal.service.contract.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.service.opinion.LegalAuditOpinionAnnotationFormatter;
import com.laby.module.legal.service.opinion.LegalAuditOpinionRewriteSupport;
import org.apache.poi.xwpf.usermodel.XWPFComments;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTComment;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTComments;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRunTrackChange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrackChange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 合同 docx 渲染工具（MVP）。
 *
 * <p>标注优先 Word 原生 Comment；采纳 TRACKED 使用 w:ins/w:del 修订痕迹。</p>
 */
public final class LegalContractDocxRenderUtil {

    private static final String TRACK_CHANGE_AUTHOR = "legal-ai";

    private static final String BOOKMARK_PREFIX = "laby_p_";

    /** 采纳后是否应写入 WORKING 正文（须含可执行 newText/oldText，不再使用 suggestion 兜底） */
    public static boolean isAdoptApplicableToDocument(LegalAuditOpinionDO opinion) {
        return LegalAuditOpinionRewriteSupport.isAdoptApplicableToDocument(opinion);
    }

    private LegalContractDocxRenderUtil() {
    }

    public static String bookmarkNameForParagraph(String paragraphId) {
        return BOOKMARK_PREFIX + StrUtil.blankToDefault(paragraphId, "");
    }

    /**
     * 外发版：去除全部 Word 批注（设计 §2.3 / §6.2）。
     */
    public static byte[] stripComments(byte[] source) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                CTP ctp = paragraph.getCTP();
                while (ctp.sizeOfCommentRangeStartArray() > 0) {
                    ctp.removeCommentRangeStart(0);
                }
                while (ctp.sizeOfCommentRangeEndArray() > 0) {
                    ctp.removeCommentRangeEnd(0);
                }
                for (int i = ctp.sizeOfRArray() - 1; i >= 0; i--) {
                    CTR run = ctp.getRArray(i);
                    if (run.sizeOfCommentReferenceArray() > 0) {
                        ctp.removeR(i);
                    }
                }
            }
            XWPFComments comments = document.getDocComments();
            if (comments != null) {
                CTComments ctComments = resolveCTComments(comments);
                if (ctComments != null) {
                    while (ctComments.sizeOfCommentArray() > 0) {
                        ctComments.removeComment(0);
                    }
                }
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 采纳版导出：接受全部修订痕迹，输出干净正文（DELIV-001 §10.5）。
     */
    public static byte[] stripTrackChanges(byte[] source) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                acceptTrackChangesInParagraph(paragraph.getCTP());
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    private static void acceptTrackChangesInParagraph(CTP ctp) {
        while (ctp.sizeOfDelArray() > 0) {
            ctp.removeDel(0);
        }
        for (int i = 0; i < ctp.sizeOfInsArray(); i++) {
            CTRunTrackChange ins = ctp.getInsArray(i);
            if (ins.sizeOfRArray() == 0) {
                continue;
            }
            for (int r = 0; r < ins.sizeOfRArray(); r++) {
                ctp.addNewR().set(ins.getRArray(r));
            }
        }
        while (ctp.sizeOfInsArray() > 0) {
            ctp.removeIns(0);
        }
    }

    public static byte[] insertParagraphBookmarks(byte[] source) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            AtomicLong bookmarkId = new AtomicLong(0);
            int paragraphSort = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = StrUtil.trim(paragraph.getText());
                if (StrUtil.isBlank(paragraphText)) {
                    continue;
                }
                paragraphSort++;
                wrapParagraphWithBookmark(paragraph, BOOKMARK_PREFIX + "p-" + paragraphSort,
                        bookmarkId.incrementAndGet());
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    private static void wrapParagraphWithBookmark(XWPFParagraph paragraph, String bookmarkName, long id) {
        CTP ctp = paragraph.getCTP();
        BigInteger bmId = BigInteger.valueOf(id);
        CTBookmark start = ctp.insertNewBookmarkStart(0);
        start.setId(bmId);
        start.setName(bookmarkName);
        CTMarkupRange end = ctp.addNewBookmarkEnd();
        end.setId(bmId);
    }

    /**
     * 读取 docx 中段落级 Bookmark 名称（{@code laby_p_p-*}）。
     */
    public static java.util.LinkedHashSet<String> listParagraphBookmarkNames(byte[] source) throws IOException {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                CTP ctp = paragraph.getCTP();
                if (ctp.sizeOfBookmarkStartArray() <= 0) {
                    continue;
                }
                for (CTBookmark bookmark : ctp.getBookmarkStartList()) {
                    String name = bookmark.getName();
                    if (StrUtil.isNotBlank(name) && name.startsWith(BOOKMARK_PREFIX)) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    public static String toBookmarkName(String paragraphId) {
        return BOOKMARK_PREFIX + paragraphId;
    }

    public static String toParagraphId(String bookmarkName) {
        if (StrUtil.isBlank(bookmarkName) || !bookmarkName.startsWith(BOOKMARK_PREFIX)) {
            return null;
        }
        return bookmarkName.substring(BOOKMARK_PREFIX.length());
    }

    private enum SegmentKind {
        NORMAL, DELETE, INSERT
    }

    private static final class TextSegment {
        private SegmentKind kind;
        private String text;

        private static TextSegment of(SegmentKind kind, String text) {
            TextSegment segment = new TextSegment();
            segment.kind = kind;
            segment.text = text;
            return segment;
        }
    }

    public static byte[] renderAnnotated(byte[] source,
                                         List<LegalAuditOpinionDO> opinions) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (CollUtil.isNotEmpty(opinions)) {
                XWPFComments comments = ensureComments(document);
                BigInteger nextCommentId = nextCommentId(comments);
                Map<String, List<LegalAuditOpinionDO>> opinionByParagraphId = opinions.stream()
                        .filter(item -> StrUtil.isNotBlank(item.getParagraphId()))
                        .collect(Collectors.groupingBy(LegalAuditOpinionDO::getParagraphId));
                List<LegalAuditOpinionDO> tailOpinions = opinions.stream()
                        .filter(item -> StrUtil.isBlank(item.getParagraphId()))
                        .toList();
                List<XWPFParagraph> paragraphs = new ArrayList<>(document.getParagraphs());
                int paragraphSort = 0;
                for (XWPFParagraph paragraph : paragraphs) {
                    String paragraphText = StrUtil.trim(paragraph.getText());
                    if (StrUtil.isBlank(paragraphText)) {
                        continue;
                    }
                    paragraphSort++;
                    List<LegalAuditOpinionDO> paragraphOpinions = opinionByParagraphId.get("p-" + paragraphSort);
                    if (CollUtil.isEmpty(paragraphOpinions)) {
                        continue;
                    }
                    for (LegalAuditOpinionDO item : paragraphOpinions) {
                        try {
                            appendWordComment(paragraph, comments, nextCommentId, item);
                            nextCommentId = nextCommentId.add(BigInteger.ONE);
                        } catch (Exception ex) {
                            // 降级：无法写原生批注时，追加段后标注，确保业务可继续
                            appendAnnotationAfterParagraph(document, paragraph, item);
                        }
                    }
                }
                for (LegalAuditOpinionDO opinion : tailOpinions) {
                    appendAnnotationBlock(document.createParagraph(), opinion);
                }
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    public static byte[] renderAdoptedClean(byte[] source,
                                            List<LegalAuditOpinionDO> adoptedOpinions) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (CollUtil.isNotEmpty(adoptedOpinions)) {
                Map<String, List<LegalAuditOpinionDO>> opinionByParagraphId = adoptedOpinions.stream()
                        .filter(item -> StrUtil.isNotBlank(item.getParagraphId()))
                        .collect(Collectors.groupingBy(LegalAuditOpinionDO::getParagraphId));
                List<XWPFParagraph> paragraphs = new ArrayList<>(document.getParagraphs());
                int paragraphSort = 0;
                for (XWPFParagraph paragraph : paragraphs) {
                    String paragraphText = StrUtil.trim(paragraph.getText());
                    if (StrUtil.isBlank(paragraphText)) {
                        continue;
                    }
                    paragraphSort++;
                    String paragraphId = "p-" + paragraphSort;
                    List<LegalAuditOpinionDO> paragraphOpinions = opinionByParagraphId.get(paragraphId);
                    if (CollUtil.isEmpty(paragraphOpinions)) {
                        continue;
                    }
                    paragraphOpinions.sort((a, b) -> Long.compare(a.getId(), b.getId()));
                    String merged = applyOpinions(paragraphText, paragraphOpinions);
                    replaceParagraphText(paragraph, merged);
                }
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    public static byte[] renderAdoptedTracked(byte[] source,
                                              List<LegalAuditOpinionDO> adoptedOpinions) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(source));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (CollUtil.isNotEmpty(adoptedOpinions)) {
                AtomicLong revisionId = new AtomicLong(0);
                Map<String, List<LegalAuditOpinionDO>> opinionByParagraphId = adoptedOpinions.stream()
                        .filter(item -> StrUtil.isNotBlank(item.getParagraphId()))
                        .collect(Collectors.groupingBy(LegalAuditOpinionDO::getParagraphId));
                List<XWPFParagraph> paragraphs = new ArrayList<>(document.getParagraphs());
                int paragraphSort = 0;
                for (XWPFParagraph paragraph : paragraphs) {
                    String paragraphText = StrUtil.trim(paragraph.getText());
                    if (StrUtil.isBlank(paragraphText)) {
                        continue;
                    }
                    paragraphSort++;
                    String paragraphId = "p-" + paragraphSort;
                    List<LegalAuditOpinionDO> paragraphOpinions = opinionByParagraphId.get(paragraphId);
                    if (CollUtil.isEmpty(paragraphOpinions)) {
                        continue;
                    }
                    paragraphOpinions.sort((a, b) -> Long.compare(a.getId(), b.getId()));
                    List<TextSegment> segments = buildTrackedSegments(paragraphText, paragraphOpinions);
                    renderTrackedSegments(paragraph, segments, revisionId);
                }
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    private static String applyOpinions(String sourceText, List<LegalAuditOpinionDO> opinions) {
        String result = StrUtil.blankToDefault(sourceText, "");
        for (LegalAuditOpinionDO opinion : opinions) {
            String changeType = normalize(opinion.getChangeType(), LegalOpinionChangeTypeEnum.NO_CHANGE.getCode());
            String oldText = StrUtil.blankToDefault(opinion.getOldText(), "");
            String newText = StrUtil.blankToDefault(opinion.getNewText(), "");
            if (LegalOpinionChangeTypeEnum.NO_CHANGE.getCode().equals(changeType)) {
                if (StrUtil.isNotBlank(newText)) {
                    result = StrUtil.isNotBlank(oldText) && result.contains(oldText)
                            ? StrUtil.replace(result, oldText, newText)
                            : newText;
                }
                continue;
            }
            if (LegalOpinionChangeTypeEnum.REPLACE.getCode().equals(changeType)) {
                if (StrUtil.isNotBlank(oldText) && result.contains(oldText)) {
                    result = StrUtil.replace(result, oldText, newText);
                } else if (StrUtil.isNotBlank(newText)) {
                    result = newText;
                }
            } else if (LegalOpinionChangeTypeEnum.INSERT_BEFORE.getCode().equals(changeType)) {
                result = newText + result;
            } else if (LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode().equals(changeType)) {
                result = result + newText;
            } else if (LegalOpinionChangeTypeEnum.DELETE.getCode().equals(changeType)) {
                if (StrUtil.isNotBlank(oldText) && result.contains(oldText)) {
                    result = StrUtil.replace(result, oldText, "");
                } else {
                    result = "";
                }
            }
        }
        return result;
    }

    private static void appendAnnotationAfterParagraph(XWPFDocument document, XWPFParagraph anchor,
                                                       LegalAuditOpinionDO opinion) {
        XmlCursor cursor = anchor.getCTP().newCursor();
        cursor.toEndToken();
        XWPFParagraph p = document.insertNewParagraph(cursor);
        appendAnnotationBlock(p, opinion);
    }

    private static List<TextSegment> buildTrackedSegments(String originalText,
                                                          List<LegalAuditOpinionDO> opinions) {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(TextSegment.of(SegmentKind.NORMAL, originalText));
        for (LegalAuditOpinionDO opinion : opinions) {
            segments = applyOpinionToTrackedSegments(segments, opinion);
        }
        return mergeAdjacentSegments(segments);
    }

    private static List<TextSegment> applyOpinionToTrackedSegments(List<TextSegment> segments,
                                                                   LegalAuditOpinionDO opinion) {
        String changeType = normalize(opinion.getChangeType(), LegalOpinionChangeTypeEnum.NO_CHANGE.getCode());
        String oldText = StrUtil.blankToDefault(opinion.getOldText(), "");
        String newText = StrUtil.blankToDefault(opinion.getNewText(), "");
        if (LegalOpinionChangeTypeEnum.NO_CHANGE.getCode().equals(changeType)) {
            if (StrUtil.isBlank(newText)) {
                return segments;
            }
            if (StrUtil.isNotBlank(oldText)) {
                return replaceInNormalSegments(segments, oldText,
                        TextSegment.of(SegmentKind.DELETE, oldText),
                        TextSegment.of(SegmentKind.INSERT, newText));
            }
            List<TextSegment> result = new ArrayList<>();
            String original = joinNormalSegmentText(segments);
            if (StrUtil.isNotBlank(original)) {
                result.add(TextSegment.of(SegmentKind.DELETE, original));
            }
            result.add(TextSegment.of(SegmentKind.INSERT, newText));
            return result;
        }
        if (LegalOpinionChangeTypeEnum.INSERT_BEFORE.getCode().equals(changeType)) {
            if (StrUtil.isBlank(newText)) {
                return segments;
            }
            List<TextSegment> result = new ArrayList<>();
            result.add(TextSegment.of(SegmentKind.INSERT, newText));
            result.addAll(segments);
            return result;
        }
        if (LegalOpinionChangeTypeEnum.INSERT_AFTER.getCode().equals(changeType)) {
            if (StrUtil.isBlank(newText)) {
                return segments;
            }
            List<TextSegment> result = new ArrayList<>(segments);
            result.add(TextSegment.of(SegmentKind.INSERT, newText));
            return result;
        }
        if (LegalOpinionChangeTypeEnum.DELETE.getCode().equals(changeType)) {
            if (StrUtil.isBlank(oldText)) {
                return segments;
            }
            return replaceInNormalSegments(segments, oldText, TextSegment.of(SegmentKind.DELETE, oldText), null);
        }
        if (LegalOpinionChangeTypeEnum.REPLACE.getCode().equals(changeType)) {
            if (StrUtil.isNotBlank(oldText)) {
                return replaceInNormalSegments(segments, oldText,
                        TextSegment.of(SegmentKind.DELETE, oldText),
                        TextSegment.of(SegmentKind.INSERT, newText));
            }
            if (StrUtil.isNotBlank(newText)) {
                String combined = joinNormalText(segments);
                List<TextSegment> result = new ArrayList<>();
                if (StrUtil.isNotBlank(combined)) {
                    result.add(TextSegment.of(SegmentKind.DELETE, combined));
                }
                result.add(TextSegment.of(SegmentKind.INSERT, newText));
                return result;
            }
        }
        return segments;
    }

    private static List<TextSegment> replaceInNormalSegments(List<TextSegment> segments, String target,
                                                             TextSegment deleteSegment, TextSegment insertSegment) {
        List<TextSegment> result = new ArrayList<>();
        boolean replaced = false;
        for (TextSegment segment : segments) {
            if (segment.kind != SegmentKind.NORMAL || !segment.text.contains(target)) {
                result.add(segment);
                continue;
            }
            int index = segment.text.indexOf(target);
            String before = segment.text.substring(0, index);
            String after = segment.text.substring(index + target.length());
            if (StrUtil.isNotBlank(before)) {
                result.add(TextSegment.of(SegmentKind.NORMAL, before));
            }
            result.add(deleteSegment);
            if (insertSegment != null && StrUtil.isNotBlank(insertSegment.text)) {
                result.add(insertSegment);
            }
            if (StrUtil.isNotBlank(after)) {
                result.add(TextSegment.of(SegmentKind.NORMAL, after));
            }
            replaced = true;
            break;
        }
        return replaced ? result : segments;
    }

    private static List<TextSegment> mergeAdjacentSegments(List<TextSegment> segments) {
        List<TextSegment> merged = new ArrayList<>();
        for (TextSegment segment : segments) {
            if (StrUtil.isBlank(segment.text)) {
                continue;
            }
            if (!merged.isEmpty()) {
                TextSegment last = merged.get(merged.size() - 1);
                if (last.kind == segment.kind) {
                    last.text = last.text + segment.text;
                    continue;
                }
            }
            TextSegment copy = new TextSegment();
            copy.kind = segment.kind;
            copy.text = segment.text;
            merged.add(copy);
        }
        return merged;
    }

    private static String joinNormalText(List<TextSegment> segments) {
        StringBuilder builder = new StringBuilder();
        for (TextSegment segment : segments) {
            if (segment.kind == SegmentKind.NORMAL) {
                builder.append(segment.text);
            }
        }
        return builder.toString();
    }

    private static void renderTrackedSegments(XWPFParagraph paragraph, List<TextSegment> segments,
                                              AtomicLong revisionId) {
        clearParagraphContent(paragraph);
        CTP ctp = paragraph.getCTP();
        for (TextSegment segment : segments) {
            if (StrUtil.isBlank(segment.text)) {
                continue;
            }
            switch (segment.kind) {
                case NORMAL -> {
                    XWPFRun run = paragraph.createRun();
                    run.setText(segment.text);
                }
                case DELETE -> {
                    CTRunTrackChange del = ctp.addNewDel();
                    configureTrackChange(del, revisionId.incrementAndGet());
                    CTR run = del.addNewR();
                    run.addNewDelText().setStringValue(segment.text);
                }
                case INSERT -> {
                    CTRunTrackChange ins = ctp.addNewIns();
                    configureTrackChange(ins, revisionId.incrementAndGet());
                    CTR run = ins.addNewR();
                    run.addNewT().setStringValue(segment.text);
                }
                default -> {
                }
            }
        }
    }

    private static void configureTrackChange(CTTrackChange trackChange, long id) {
        trackChange.setId(BigInteger.valueOf(id));
        trackChange.setAuthor(TRACK_CHANGE_AUTHOR);
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        trackChange.setDate(calendar);
    }

    private static void clearParagraphContent(XWPFParagraph paragraph) {
        CTP ctp = paragraph.getCTP();
        while (ctp.sizeOfRArray() > 0) {
            ctp.removeR(0);
        }
        while (ctp.sizeOfInsArray() > 0) {
            ctp.removeIns(0);
        }
        while (ctp.sizeOfDelArray() > 0) {
            ctp.removeDel(0);
        }
    }

    private static void appendAnnotationBlock(XWPFParagraph p, LegalAuditOpinionDO opinion) {
        LegalAuditOpinionAnnotationFormatter.writeFallbackAnnotation(p, opinion);
    }

    private static void appendWordComment(XWPFParagraph paragraph, XWPFComments comments,
                                          BigInteger commentId, LegalAuditOpinionDO opinion) {
        CTComments ctComments = resolveCTComments(comments);
        if (ctComments == null) {
            throw new IllegalStateException("XWPFComments does not expose CTComments");
        }
        CTComment ctComment = ctComments.addNewComment();
        ctComment.setId(commentId);
        ctComment.setAuthor("legal-ai");
        ctComment.setInitials("LA");
        LegalAuditOpinionAnnotationFormatter.populateWordComment(ctComment, opinion);

        CTMarkupRange start = paragraph.getCTP().addNewCommentRangeStart();
        start.setId(commentId);
        CTMarkupRange end = paragraph.getCTP().addNewCommentRangeEnd();
        end.setId(commentId);

        CTR referenceRun = paragraph.getCTP().addNewR();
        referenceRun.addNewCommentReference().setId(commentId);
    }

    private static XWPFComments ensureComments(XWPFDocument document) {
        XWPFComments comments = document.getDocComments();
        if (comments != null) {
            return comments;
        }
        return document.createComments();
    }

    private static BigInteger nextCommentId(XWPFComments comments) {
        CTComments ctComments = resolveCTComments(comments);
        if (ctComments == null) {
            return BigInteger.ONE;
        }
        BigInteger max = BigInteger.ZERO;
        for (CTComment item : ctComments.getCommentList()) {
            if (item.getId().compareTo(max) > 0) {
                max = item.getId();
            }
        }
        return max.add(BigInteger.ONE);
    }

    private static CTComments resolveCTComments(XWPFComments comments) {
        try {
            Object value;
            try {
                value = XWPFComments.class.getMethod("getCTComments").invoke(comments);
            } catch (NoSuchMethodException ex) {
                value = XWPFComments.class.getMethod("getCtComments").invoke(comments);
            }
            return value instanceof CTComments ? (CTComments) value : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static void replaceParagraphText(XWPFParagraph paragraph, String text) {
        int runSize = paragraph.getRuns() == null ? 0 : paragraph.getRuns().size();
        for (int i = runSize - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        XWPFRun run = paragraph.createRun();
        run.setText(StrUtil.blankToDefault(text, ""));
    }

    private static String joinNormalSegmentText(List<TextSegment> segments) {
        StringBuilder builder = new StringBuilder();
        for (TextSegment segment : segments) {
            if (segment.kind == SegmentKind.NORMAL && StrUtil.isNotBlank(segment.text)) {
                builder.append(segment.text);
            }
        }
        return builder.toString();
    }

    private static String normalize(String value, String defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        return value.trim().toUpperCase();
    }

}
