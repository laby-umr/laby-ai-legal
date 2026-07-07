package com.laby.module.ai.service.knowledge.splitter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Markdown 表格解析（行级 JSON + 摘要）
 */
public final class MarkdownTableChunkSupport {

    private MarkdownTableChunkSupport() {
    }

    public static TableChunkArtifacts parse(String markdown, String caption) {
        if (StrUtil.isBlank(markdown)) {
            return new TableChunkArtifacts();
        }
        List<String> lines = Arrays.stream(markdown.split("\n"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .filter(MarkdownTableChunkSupport::isTableLine)
                .toList();
        if (lines.size() < 2) {
            return new TableChunkArtifacts();
        }
        List<String> headers = splitCells(lines.get(0));
        if (headers.isEmpty()) {
            return new TableChunkArtifacts();
        }
        List<String> rowJsonLines = new ArrayList<>();
        int dataRowCount = 0;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isSeparatorLine(line)) {
                continue;
            }
            List<String> cells = splitCells(line);
            if (cells.isEmpty()) {
                continue;
            }
            JSONObject row = new JSONObject();
            for (int col = 0; col < headers.size(); col++) {
                String header = headers.get(col);
                String value = col < cells.size() ? cells.get(col) : "";
                row.set(header, value);
            }
            rowJsonLines.add(JSONUtil.toJsonStr(row));
            dataRowCount++;
        }
        String safeCaption = StrUtil.blankToDefault(caption, "表格");
        String summary = String.format("表格摘要：%s，列字段包括 %s，共 %d 行数据。",
                safeCaption, String.join("、", headers), dataRowCount);
        return new TableChunkArtifacts()
                .setHeaders(headers)
                .setRowJsonLines(rowJsonLines)
                .setSummary(summary);
    }

    private static boolean isTableLine(String line) {
        return line.startsWith("|") && line.endsWith("|");
    }

    private static boolean isSeparatorLine(String line) {
        String normalized = line.replace("|", "").replace(":", "").replace("-", "").trim();
        return normalized.isEmpty();
    }

    private static List<String> splitCells(String line) {
        String trimmed = StrUtil.trim(line);
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return Arrays.stream(trimmed.split("\\|"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Data
    @Accessors(chain = true)
    public static class TableChunkArtifacts {

        private List<String> headers = List.of();
        private List<String> rowJsonLines = List.of();
        private String summary;

        public boolean hasRows() {
            return !rowJsonLines.isEmpty();
        }

    }

}
