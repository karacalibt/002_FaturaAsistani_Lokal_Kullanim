#!/usr/bin/env bash
# Demo icin hafif model (~2GB RAM). qwen3.6 yerine bunu kullanin.
set -euo pipefail
echo "Hafif model indiriliyor (qwen2.5:3b)..."
ollama pull qwen2.5:3b
echo ""
echo "Backend icin:"
echo "  export OLLAMA_MODEL=qwen2.5:3b"
echo "  cd backend && mvn spring-boot:run"
