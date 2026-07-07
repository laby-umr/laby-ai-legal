"""
Laby Docling 文档解析 HTTP 适配层（归一化 JSON 协议 stub）

生产环境可替换为 Docling CLI / API，保持响应 schema 不变。
协议与 Java 侧 AbstractHttpDocumentParseClient 对齐。
"""

from __future__ import annotations

import base64
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="Laby Docling Parse Adapter", version="1.0.0")

ENGINE = "docling"
QUALITY = "high"


class ParseRequest(BaseModel):
    fileName: str = Field(default="document.docx")
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

    text = raw.decode("utf-8", errors="ignore").strip()
    if not text:
        text = f"[Docling stub] Parsed {request.fileName or 'document'} ({len(raw)} bytes)"

    elements = [{"type": "text", "text": text, "page": 1}]
    return ParseResponse(markdown=text, elements=elements)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8001)
