package com.laby.module.legal.service.contract;

import com.laby.module.legal.dal.dataobject.opinion.LegalAuditOpinionDO;
import com.laby.module.legal.enums.opinion.LegalOpinionChangeTypeEnum;
import com.laby.module.legal.enums.opinion.LegalOpinionStatusEnum;
import com.laby.module.legal.service.contract.util.LegalContractDocxRenderUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四件套交付物生成单测（DELIV-001 P0-7）。
 */
class LegalContractDeliverableServiceTest {

    @Test
    void filterAnnotatedOpinions_shouldExcludeIgnored() {
        List<LegalAuditOpinionDO> opinions = List.of(
                opinion(1L, LegalOpinionStatusEnum.PENDING.getStatus()),
                opinion(2L, LegalOpinionStatusEnum.ADOPTED.getStatus()),
                opinion(3L, LegalOpinionStatusEnum.IGNORED.getStatus()));
        assertEquals(2, LegalContractDeliverableServiceImpl.filterAnnotatedOpinions(opinions).size());
    }

    @Test
    void filterRevisionOpinions_shouldOnlyKeepAdoptedApplicable() {
        LegalAuditOpinionDO applicable = LegalAuditOpinionDO.builder()
                .id(1L)
                .status(LegalOpinionStatusEnum.ADOPTED.getStatus())
                .changeType(LegalOpinionChangeTypeEnum.REPLACE.getCode())
                .paragraphId("p-1")
                .oldText("甲")
                .newText("乙")
                .build();
        LegalAuditOpinionDO pending = opinion(2L, LegalOpinionStatusEnum.PENDING.getStatus());
        List<LegalAuditOpinionDO> result = LegalContractDeliverableServiceImpl.filterRevisionOpinions(
                List.of(applicable, pending));
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void renderAdoptedTracked_shouldApplyThreeAdoptedOpinions() throws Exception {
        byte[] source = buildDocx("甲公司承担全部责任。", "乙公司免责。", "丙公司担保。");
        List<LegalAuditOpinionDO> adopted = List.of(
                replaceOpinion(1L, "p-1", "甲公司承担全部责任。", "甲公司承担有限责任。"),
                replaceOpinion(2L, "p-2", "乙公司免责。", "乙公司部分免责。"),
                replaceOpinion(3L, "p-3", "丙公司担保。", "丙公司连带担保。"));
        byte[] bookmarked = LegalContractDocxRenderUtil.insertParagraphBookmarks(source);
        byte[] revision = LegalContractDocxRenderUtil.renderAdoptedTracked(bookmarked, adopted);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(revision))) {
            int insCount = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                insCount += paragraph.getCTP().sizeOfInsArray();
            }
            assertEquals(3, insCount, "修订版应含 3 处 w:ins 修订痕迹");
        }
    }

    @Test
    void renderAdoptedClean_shouldMatchWorkingAfterRebuild() throws Exception {
        byte[] source = buildDocx("甲公司承担全部责任。");
        LegalAuditOpinionDO adopted = replaceOpinion(1L, "p-1", "甲公司承担全部责任。", "甲公司承担有限责任。");
        byte[] bookmarked = LegalContractDocxRenderUtil.insertParagraphBookmarks(source);
        byte[] working = LegalContractDocxRenderUtil.renderAdoptedClean(bookmarked, List.of(adopted));
        byte[] adoptedExport = LegalContractDocxRenderUtil.stripComments(
                LegalContractDocxRenderUtil.stripTrackChanges(working));
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(adoptedExport))) {
            String text = document.getParagraphs().get(0).getText();
            assertTrue(text.contains("有限责任"), "采纳版应与 WORKING 干净正文一致");
        }
    }

    private static LegalAuditOpinionDO opinion(Long id, Integer status) {
        return LegalAuditOpinionDO.builder().id(id).status(status).build();
    }

    private static LegalAuditOpinionDO replaceOpinion(Long id, String paragraphId,
                                                      String oldText, String newText) {
        return LegalAuditOpinionDO.builder()
                .id(id)
                .status(LegalOpinionStatusEnum.ADOPTED.getStatus())
                .changeType(LegalOpinionChangeTypeEnum.REPLACE.getCode())
                .paragraphId(paragraphId)
                .oldText(oldText)
                .newText(newText)
                .build();
    }

    private static byte[] buildDocx(String... lines) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String line : lines) {
                document.createParagraph().createRun().setText(line);
            }
            document.write(out);
            return out.toByteArray();
        }
    }

}
