package com.laby.module.ai.framework.document;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.laby.module.ai.core.document.AiStructuredDocument;
import com.laby.module.ai.core.document.AiStructuredDocumentElement;
import com.laby.module.ai.core.document.AiStructuredDocumentParseResult;
import com.laby.module.ai.enums.knowledge.AiDocumentParseEngineEnum;
import com.laby.module.ai.enums.knowledge.AiDocumentParseQualityEnum;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 电子表格解析（Apache POI xlsx/xls + 简单 CSV）
 */
public class SpreadsheetDocumentParseClient implements DocumentParseClient {

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public AiDocumentParseEngineEnum engine() {
        return AiDocumentParseEngineEnum.SPREADSHEET;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiStructuredDocumentParseResult parse(byte[] bytes, String fileName) {
        String ext = FileNameUtil.extName(fileName);
        if (StrUtil.isNotBlank(ext) && "csv".equalsIgnoreCase(ext)) {
            return parseCsv(bytes, fileName);
        }
        return parseWorkbook(bytes, fileName);
    }

    private AiStructuredDocumentParseResult parseCsv(byte[] bytes, String fileName) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        List<List<String>> rows = parseCsvRows(text);
        String sheetName = StrUtil.blankToDefault(FileNameUtil.mainName(fileName), "CSV");
        return buildResult(sheetName, rows);
    }

    private AiStructuredDocumentParseResult parseWorkbook(byte[] bytes, String fileName) {
        List<AiStructuredDocumentElement> elements = new ArrayList<>();
        StringBuilder markdown = new StringBuilder();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet == null) {
                    continue;
                }
                List<List<String>> rows = readSheetRows(sheet);
                if (rows.isEmpty()) {
                    continue;
                }
                String sheetName = StrUtil.blankToDefault(sheet.getSheetName(), "Sheet" + (i + 1));
                String tableMarkdown = rowsToMarkdown(rows);
                elements.add(new AiStructuredDocumentElement()
                        .setType("table")
                        .setMarkdown(tableMarkdown)
                        .setCaption(sheetName)
                        .setPage(i + 1));
                markdown.append("### ").append(sheetName).append("\n").append(tableMarkdown).append("\n\n");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Spreadsheet parse failed: " + ex.getMessage(), ex);
        }
        if (elements.isEmpty()) {
            throw new IllegalStateException("Spreadsheet contains no data");
        }
        return new AiStructuredDocumentParseResult()
                .setMarkdown(markdown.toString().trim())
                .setEngine(AiDocumentParseEngineEnum.SPREADSHEET)
                .setQuality(AiDocumentParseQualityEnum.HIGH)
                .setStructuredDocument(new AiStructuredDocument().setElements(elements));
    }

    private AiStructuredDocumentParseResult buildResult(String sheetName, List<List<String>> rows) {
        String tableMarkdown = rowsToMarkdown(rows);
        AiStructuredDocumentElement element = new AiStructuredDocumentElement()
                .setType("table")
                .setMarkdown(tableMarkdown)
                .setCaption(sheetName)
                .setPage(1);
        String markdown = "### " + sheetName + "\n" + tableMarkdown;
        return new AiStructuredDocumentParseResult()
                .setMarkdown(markdown.trim())
                .setEngine(AiDocumentParseEngineEnum.SPREADSHEET)
                .setQuality(AiDocumentParseQualityEnum.HIGH)
                .setStructuredDocument(new AiStructuredDocument().setElements(List.of(element)));
    }

    private List<List<String>> readSheetRows(Sheet sheet) {
        List<List<String>> rows = new ArrayList<>();
        for (Row row : sheet) {
            if (row == null) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            int lastCell = row.getLastCellNum();
            for (int i = 0; i < lastCell; i++) {
                Cell cell = row.getCell(i);
                cells.add(cell == null ? "" : dataFormatter.formatCellValue(cell).trim());
            }
            if (cells.stream().anyMatch(StrUtil::isNotBlank)) {
                rows.add(cells);
            }
        }
        return rows;
    }

    static List<List<String>> parseCsvRows(String text) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (StrUtil.isBlank(trimmed)) {
                continue;
            }
            rows.add(parseCsvLine(trimmed));
        }
        return rows;
    }

    static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString().trim());
        return cells;
    }

    static String rowsToMarkdown(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        int colCount = rows.stream().mapToInt(List::size).max().orElse(0);
        List<String> lines = new ArrayList<>();
        List<String> header = padRow(rows.get(0), colCount);
        lines.add("| " + String.join(" | ", header) + " |");
        lines.add("| " + String.join(" | ", Collections.nCopies(colCount, "---")) + " |");
        for (int i = 1; i < rows.size(); i++) {
            lines.add("| " + String.join(" | ", padRow(rows.get(i), colCount)) + " |");
        }
        return String.join("\n", lines);
    }

    private static List<String> padRow(List<String> row, int colCount) {
        List<String> padded = new ArrayList<>(row);
        while (padded.size() < colCount) {
            padded.add("");
        }
        return padded;
    }

}
