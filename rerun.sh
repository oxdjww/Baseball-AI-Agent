#!/usr/bin/env bash
# =============================================================================
# rerun.sh — 로컬 개발 재기동 스크립트
#
# 사용법:
#   ./rerun.sh          # 빌드 + 재기동 (테스트 제외)
#   ./rerun.sh --test   # 테스트까지 실행 후 재기동
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
BACKEND_DIR="$SCRIPT_DIR/backend"
RUN_TESTS=false

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[rerun]${NC} $1"; }
warn()    { echo -e "${YELLOW}[rerun]${NC} $1"; }
err_exit(){ echo -e "${RED}[rerun] ERROR:${NC} $1"; exit 1; }

# 인수 파싱
for arg in "$@"; do
  case $arg in
    --test) RUN_TESTS=true ;;
    *) echo "사용법: $0 [--test]"; exit 1 ;;
  esac
done

# ── 1. .env 로드 ──────────────────────────────────────────────────────────────
[ -f "$ENV_FILE" ] || err_exit ".env 파일을 찾을 수 없습니다: $ENV_FILE"
set -o allexport
# shellcheck disable=SC1090
source "$ENV_FILE"
set +o allexport
info ".env 로드 완료"

# 로컬 실행 시 Redis는 Docker 포트 매핑(6379)으로 접근하므로 host를 localhost로 오버라이드
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379

# ── 2. 기존 프로세스 종료 ────────────────────────────────────────────────────
warn "포트 8080 기존 프로세스 종료 중..."
OLD_PID=$(lsof -ti:8080 2>/dev/null || true)
if [ -n "$OLD_PID" ]; then
  kill -9 "$OLD_PID"
  info "기존 프로세스 종료됨 (PID: $OLD_PID)"
else
  info "실행 중인 프로세스 없음"
fi

# ── 3. 테스트 (옵션) ─────────────────────────────────────────────────────────
if [ "$RUN_TESTS" = true ]; then
  warn "테스트 실행 중..."
  cd "$BACKEND_DIR"
  ./gradlew test || err_exit "테스트 실패 — 재기동 중단"
  info "테스트 통과"
fi

# ── 4. 빌드 ──────────────────────────────────────────────────────────────────
warn "빌드 중..."
cd "$BACKEND_DIR"
./gradlew clean build -x test --quiet
info "빌드 완료"

# ── 5. 기동 ──────────────────────────────────────────────────────────────────
info "앱 시작 (bootRun) — Ctrl+C 로 종료"
echo ""
./gradlew bootRun
