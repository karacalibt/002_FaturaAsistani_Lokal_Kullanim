"""
OCR motoru: PaddleOCR (varsa) veya EasyOCR (Python 3.14 / paddle yok).
Ayni HTTP API; health endpoint hangi motorun calistigini gosterir.
"""

from __future__ import annotations

import io
import logging
import sys
from abc import ABC, abstractmethod
import shutil
from typing import Literal

import fitz
from PIL import Image

logger = logging.getLogger(__name__)

EngineName = Literal["paddleocr", "easyocr", "tesseract"]


class OcrEngine(ABC):
    name: EngineName

    @abstractmethod
    def extract_from_image_bytes(self, image_bytes: bytes) -> str:
        pass


class PaddleOcrEngine(OcrEngine):
    name: EngineName = "paddleocr"

    def __init__(self) -> None:
        from paddleocr import PaddleOCR

        self._ocr = PaddleOCR(use_angle_cls=True, lang="latin", show_log=False)

    def extract_from_image_bytes(self, image_bytes: bytes) -> str:
        import numpy as np

        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        arr = np.array(image)
        result = self._ocr.ocr(arr, cls=True)
        if not result or not result[0]:
            return ""
        parts: list[str] = []
        for item in result[0]:
            if item and len(item) >= 2:
                text = item[1][0] if isinstance(item[1], (list, tuple)) else str(item[1])
                if text:
                    parts.append(str(text).strip())
        return "\n".join(parts)


class EasyOcrEngine(OcrEngine):
    name: EngineName = "easyocr"

    def __init__(self) -> None:
        import easyocr

        # Turkce + Ingilizce fatura metni
        self._reader = easyocr.Reader(["tr", "en"], gpu=False, verbose=False)

    def extract_from_image_bytes(self, image_bytes: bytes) -> str:
        import numpy as np

        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        arr = np.array(image)
        lines = self._reader.readtext(arr, detail=0, paragraph=True)
        return "\n".join(str(line).strip() for line in lines if line)


class TesseractEngine(OcrEngine):
    name: EngineName = "tesseract"

    def __init__(self) -> None:
        import pytesseract

        if not shutil.which("tesseract"):
            raise RuntimeError(
                "tesseract bulunamadi. Kurulum: brew install tesseract tesseract-lang"
            )
        self._pytesseract = pytesseract
        try:
            langs = self._pytesseract.get_languages(config="")
            self._lang = "tur+eng" if "tur" in langs else "eng"
        except Exception:
            self._lang = "eng"

    def extract_from_image_bytes(self, image_bytes: bytes) -> str:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        try:
            return self._pytesseract.image_to_string(image, lang=self._lang).strip()
        except Exception:
            return self._pytesseract.image_to_string(image, lang="eng").strip()


_engine: OcrEngine | None = None
_engine_error: str | None = None


def _try_paddle() -> OcrEngine | None:
    try:
        import paddle  # noqa: F401
        return PaddleOcrEngine()
    except Exception as e:
        logger.info("PaddleOCR kullanilamiyor: %s", e)
        return None


def _try_easyocr() -> OcrEngine | None:
    try:
        return EasyOcrEngine()
    except Exception as e:
        logger.info("EasyOCR kullanilamiyor: %s", e)
        return None


def _try_tesseract() -> OcrEngine:
    return TesseractEngine()


def get_engine() -> OcrEngine:
    global _engine, _engine_error
    if _engine is not None:
        return _engine

    errors: list[str] = []

    paddle_engine = _try_paddle()
    if paddle_engine is not None:
        _engine = paddle_engine
        _engine_error = None
        return _engine
    errors.append("PaddleOCR/PaddlePaddle yok (Python 3.13+ icin wheel yok)")

    # Python 3.14: once Tesseract (hizli, brew ile kurulur)
    if sys.version_info >= (3, 13):
        try:
            _engine = _try_tesseract()
            _engine_error = None
            logger.info("Tesseract ile devam ediliyor (Python %s.%s).", sys.version_info.major, sys.version_info.minor)
            return _engine
        except Exception as e:
            errors.append(f"Tesseract: {e}")

    easy = _try_easyocr()
    if easy is not None:
        _engine = easy
        _engine_error = None
        logger.warning("EasyOCR ile devam ediliyor.")
        return _engine
    errors.append("EasyOCR baslatilamadi")

    try:
        _engine = _try_tesseract()
        _engine_error = None
        logger.warning("Tesseract ile devam ediliyor.")
        return _engine
    except Exception as e:
        errors.append(f"Tesseract: {e}")
        _engine_error = "; ".join(errors)
        raise RuntimeError(
            "Hicbir OCR motoru baslatilamadi.\n"
            "1) brew install tesseract tesseract-lang\n"
            "2) veya: brew install python@3.12, rm -rf paddleocr-service/.venv, tekrar ./scripts/start-paddleocr.sh\n"
            f"Detay: {_engine_error}"
        ) from e


def get_engine_name() -> str:
    if _engine is not None:
        return _engine.name
    return "not_loaded"


def get_engine_note() -> str | None:
    if _engine is None:
        return "Ilk OCR isteginde motor yuklenecek."
    if _engine.name == "easyocr":
        return (
            "PaddlePaddle bu Python surumunde yok. "
            "PaddleOCR icin: brew install python@3.12 ve .venv yenileyin."
        )
    if _engine.name == "tesseract":
        return (
            "Tesseract kullaniliyor (Paddle/EasyOCR hazir degil). "
            "Daha iyi sonuc: brew install tesseract-lang"
        )
    return None


def ocr_pdf_bytes(pdf_bytes: bytes) -> tuple[str, int]:
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    engine = get_engine()
    texts: list[str] = []
    try:
        for i, page in enumerate(doc):
            pix = page.get_pixmap(matrix=fitz.Matrix(2, 2))
            png = pix.tobytes("png")
            page_text = engine.extract_from_image_bytes(png)
            if page_text.strip():
                texts.append(f"--- Sayfa {i + 1} ---\n{page_text}")
        return "\n\n".join(texts).strip(), len(doc)
    finally:
        doc.close()


def ocr_image_bytes(image_bytes: bytes) -> str:
    return get_engine().extract_from_image_bytes(image_bytes)
