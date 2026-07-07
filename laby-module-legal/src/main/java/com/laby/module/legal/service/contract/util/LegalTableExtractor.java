package com.laby.module.legal.service.contract.util;

import cn.hutool.core.util.StrUtil;
import com.laby.module.legal.enums.clause.LegalClauseTypeEnum;
import com.laby.module.legal.service.contract.bo.LegalClauseUnitBO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.ArrayList;
import java.util.List;

/**
 * 表格 → TABLE 类型 ClauseUnit
 */
public final class LegalTableExtractor {

    private LegalTableExtractor() {
    }

    public static List<LegalClauseUnitBO> extract(XWPFDocument document, int startSort) {
        List<LegalClauseUnitBO> tables = new ArrayList<>();
        if (document == null) {
            return tables;
        }
        List<XWPFTable> docTables = document.getTables();
        int sort = startSort;
        for (XWPFTable table : docTables) {
            sort++;
            String fullText = flattenTable(table);
            if (StrUtil.isBlank(fullText)) {
                continue;
            }
            tables.add(LegalClauseUnitBO.builder()
                    .clauseId("c-" + sort)
                    .sort(sort)
                    .title("表格 " + (sort - startSort))
                    .level(0)
                    .type(LegalClauseTypeEnum.TABLE)
                    .path("表格")
                    .fullText(fullText)
                    .build());
        }
        return tables;
    }

    private static String flattenTable(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = StrUtil.trim(cell.getText());
                if (StrUtil.isNotBlank(cellText)) {
                    if (!sb.isEmpty()) {
                        sb.append(" | ");
                    }
                    sb.append(cellText);
                }
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

}
