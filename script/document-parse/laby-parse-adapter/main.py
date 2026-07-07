"""
Laby 文档解析 HTTP 适配层（归一化 JSON 协议）

- 开发环境：pdfplumber 提取文本 + 简单表格
- 生产环境：可替换为 MinerU CLI / API，保持响应 schema 不变

协议与 Java 侧 AbstractHttpDocumentParseClient 对齐：
POST /api/v1/parse
{
  "fileName": "demo.pdf",
  "contentBase64": "..."
}
"""

from __future__ import annotations

import base64
import io
import re
from typing import Any

import pdfplumber
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="Laby Document Parse Adapter", version="1.0.0")

ENGINE = "mineru"
QUALITY = "high"


class ParseRequest(BaseModel):
    fileName: str = Field(default="document.pdf")
    contentBase64: str


class ParseResponse(BaseModel):
    engine: str = ENGINE
    quality: str = QUALITY
    markdown: str = ""
    elements: list[dict[str, Any]] = Field(default_factory=list)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "engine": ENGINE}


@app.post("/api/v1/parse", response_model=ParseResponse)
def parse_document(request: ParseRequest) -> ParseResponse:
    try:
        raw = base64.b64decode(request.contentBase64)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=400, detail=f"invalid base64: {exc}") from exc

    file_name = (request.fileName or "").lower()
    if not file_name.endswith(".pdf"):
        text = raw.decode("utf-8", errors="ignore")
        return ParseResponse(markdown=text, elements=[{"type": "text", "text": text, "page": 1}])

    return parse_pdf(raw)


def parse_pdf(raw: bytes) -> ParseResponse:
    elements: list[dict[str, Any]] = []
    markdown_parts: list[str] = []

    with pdfplumber.open(io.BytesIO(raw)) as pdf:
        for page_index, page in enumerate(pdf.pages, start=1):
            text = (page.extract_text() or "").strip()
            if text:
                for paragraph in split_paragraphs(text):
                    elements.append({"type": "text", "text": paragraph, "page": page_index})
                    markdown_parts.append(paragraph)

            tables = page.extract_tables() or []
            for table_index, table in enumerate(tables, start=1):
                if not table:
                    continue
                markdown = table_to_markdown(table)
                if not markdown:
                    continue
                caption = f"表{page_index}-{table_index}"
                elements.append({
                    "type": "table",
                    "markdown": markdown,
                    "caption": caption,
                    "page": page_index,
                })
                markdown_parts.append(f"### {caption}\n{markdown}")

    markdown = "\n\n".join(markdown_parts).strip()
    return ParseResponse(markdown=markdown, elements=elements)


def split_paragraphs(text: str) -> list[str]:
    parts = [part.strip() for part in re.split(r"\n{2,}", text) if part.strip()]
    return parts if parts else [text]


def table_to_markdown(table: list[list[Any]]) -> str:
    rows: list[list[str]] = []
    for row in table:
        if not row:
            continue
        cells = [normalize_cell(cell) for cell in row]
        if any(cells):
            rows.append(cells)
    if len(rows) < 1:
        return ""
    col_count = max(len(row) for row in rows)
    normalized = [row + [""] * (col_count - len(row)) for row in rows]
    header = normalized[0]
    body = normalized[1:] if len(normalized) > 1 else []
    lines = [
        "| " + " | ".join(header) + " |",
        "| " + " | ".join(["---"] * col_count) + " |",
    ]
    for row in body:
        lines.append("| " + " | ".join(row) + " |")
    return "\n".join(lines)


def normalize_cell(value: Any) -> str:
    if value is None:
        return ""
    return re.sub(r"\s+", " ", str(value)).strip()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
