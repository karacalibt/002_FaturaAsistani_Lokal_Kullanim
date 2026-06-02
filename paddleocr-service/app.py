"""
Lokal OCR HTTP servisi (PaddleOCR veya EasyOCR).
Backend multipart dosya gonderir; bu servis metin dondurur.
"""

from __future__ import annotations

import logging
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel

from ocr_engine import get_engine_name, get_engine_note, ocr_image_bytes, ocr_pdf_bytes

logging.basicConfig(level=logging.INFO)
app = FastAPI(title="Fatura OCR Demo", version="1.1.0")


class OcrResponse(BaseModel):
    text: str
    page_count: int = 1
    source: str = "ocr"


@app.get("/health")
def health():
    note = get_engine_note()
    return {
        "status": "ok",
        "engine": get_engine_name(),
        "note": note,
    }


@app.post("/ocr", response_model=OcrResponse)
async def ocr_endpoint(file: UploadFile = File(...)):
    if not file.filename:
        raise HTTPException(status_code=400, detail="Dosya adi gerekli.")

    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Bos dosya.")

    name = file.filename.lower()

    try:
        if name.endswith(".pdf") or file.content_type == "application/pdf":
            text, pages = ocr_pdf_bytes(content)
            engine = get_engine_name()
            return OcrResponse(text=text, page_count=pages, source=f"{engine}-pdf")

        if name.endswith((".jpg", ".jpeg", ".png")) or (file.content_type or "").startswith("image/"):
            text = ocr_image_bytes(content)
            return OcrResponse(text=text, page_count=1, source=get_engine_name())

        raise HTTPException(status_code=400, detail="Desteklenmeyen dosya. PDF, JPG veya PNG kullanin.")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR hatasi: {e}") from e
