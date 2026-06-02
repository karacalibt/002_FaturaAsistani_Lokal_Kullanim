#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../backend"
echo "Backend: http://localhost:8080"
exec mvn spring-boot:run
