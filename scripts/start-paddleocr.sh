#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/paddleocr-service"

# PaddlePaddle: Python 3.10-3.12. Python 3.14'te EasyOCR otomatik kullanilir.
pick_python() {
  for cmd in python3.12 python3.11 python3.10 python3; do
    if command -v "$cmd" >/dev/null 2>&1; then
      ver=$("$cmd" -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')
      major=${ver%%.*}
      minor=${ver#*.}
      if [ "$major" -eq 3 ] && [ "$minor" -le 12 ]; then
        echo "$cmd"
        return
      fi
    fi
  done
  command -v python3
}

PYTHON=$(pick_python)
echo "Python: $($PYTHON --version) ($PYTHON)"

if [ -d ".venv" ]; then
  VENV_PY=".venv/bin/python"
  if [ -x "$VENV_PY" ]; then
    VVER=$("$VENV_PY" -c 'import sys; print(sys.version_info.minor)' 2>/dev/null || echo "0")
    CUR=$("$PYTHON" -c 'import sys; print(sys.version_info.minor)')
    if [ "$VVER" != "$CUR" ]; then
      echo "Eski venv (3.$VVER) siliniyor; 3.$CUR ile yeniden kuruluyor..."
      rm -rf .venv
    fi
  fi
fi

if [ ! -d ".venv" ]; then
  "$PYTHON" -m venv .venv
  source .venv/bin/activate
  pip install --upgrade pip
  pip install -r requirements.txt
  # Python <= 3.12 ise Paddle dene (basarisiz olursa EasyOCR zaten requirements'ta)
  PY_MINOR=$(python -c 'import sys; print(sys.version_info.minor)')
  if [ "$PY_MINOR" -le 12 ]; then
    echo "PaddleOCR deneniyor (Python 3.$PY_MINOR)..."
    pip install paddlepaddle -i https://www.paddlepaddle.org.cn/packages/stable/cpu/ 2>/dev/null \
      && pip install paddleocr==2.9.1 2>/dev/null \
      || echo "Paddle kurulamadi; EasyOCR kullanilacak."
  else
    echo "Python 3.$PY_MINOR: Paddle wheel yok, EasyOCR kullanilacak."
  fi
else
  source .venv/bin/activate
fi

if ! command -v tesseract >/dev/null 2>&1; then
  echo "Uyari: tesseract yok. Python 3.14 icin onerilir:"
  echo "  brew install tesseract tesseract-lang"
fi

pip install -q pytesseract 2>/dev/null || true

echo "OCR servisi: http://localhost:8866"
echo "Motor kontrolu: curl http://localhost:8866/health"
exec uvicorn app:app --host 0.0.0.0 --port 8866
