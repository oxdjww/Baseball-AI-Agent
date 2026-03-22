#!/usr/bin/env bash
# =============================================================================
# dev.sh — 로컬 개발 환경 재기동 스크립트
#
# 사용법:
#   ./dev.sh backend   # 백엔드만 재기동 (DB·Redis 유지)
#   ./dev.sh full      # DB + Redis + 백엔드 모두 재기동
#   ./dev.sh stop      # 백엔드 프로세스만 종료
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
BACKEND_DIR="$SCRIPT_DIR/backend"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${GREEN}[dev]${NC} $1"; }
step()    { echo -e "${CYAN}[dev]${NC} $1"; }
warn()    { echo -e "${YELLOW}[dev]${NC} $1"; }
err_exit(){ echo -e "${RED}[dev] ERROR:${NC} $1"; exit 1; }

MODE="${1:-}"
[ -z "$MODE" ] && { echo -e "사용법: $0 {backend|full|stop}"; exit 1; }

# ── .env 로드 ─────────────────────────────────────────────────────────────────
[ -f "$ENV_FILE" ] || err_exit ".env 파일을 찾을 수 없습니다: $ENV_FILE"
set -o allexport
# shellcheck disable=SC1090
source "$ENV_FILE"
set +o allexport
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379

# ── 공통: 백엔드 프로세스 종료 ────────────────────────────────────────────────
kill_backend() {
  local pid
  pid=$(lsof -ti:8080 2>/dev/null || true)
  if [ -n "$pid" ]; then
    kill -9 "$pid"
    warn "기존 백엔드 종료 (PID: $pid)"
  else
    info "실행 중인 백엔드 없음"
  fi
  # app.pid 정리
  [ -f "$SCRIPT_DIR/app.pid" ] && rm -f "$SCRIPT_DIR/app.pid"
}

# ── 공통: 백엔드 기동 (bootRun, 포그라운드) ───────────────────────────────────
start_backend() {
  step "백엔드 기동 중 (Ctrl+C로 종료)..."
  cd "$BACKEND_DIR"
  exec ./gradlew bootRun
}

# ── stop ──────────────────────────────────────────────────────────────────────
if [ "$MODE" = "stop" ]; then
  kill_backend
  info "백엔드 종료 완료"
  exit 0
fi

# ── backend ───────────────────────────────────────────────────────────────────
if [ "$MODE" = "backend" ]; then
  echo ""
  step "── 백엔드만 재기동 ─────────────────────────────"
  kill_backend
  echo ""
  start_backend
  exit 0
fi

# ── full ──────────────────────────────────────────────────────────────────────
if [ "$MODE" = "full" ]; then
  echo ""
  step "── 전체 재기동 (DB + Redis + 백엔드) ───────────"

  # 1. 백엔드 프로세스 종료
  kill_backend

  # 2. DB·Redis 컨테이너 재시작
  [ -f "$COMPOSE_FILE" ] || err_exit "docker-compose.yml을 찾을 수 없습니다: $COMPOSE_FILE"

  warn "DB·Redis 컨테이너 중지 중..."
  docker compose -f "$COMPOSE_FILE" stop db redis 2>/dev/null || true

  warn "DB·Redis 컨테이너 기동 중..."
  docker compose -f "$COMPOSE_FILE" up -d db redis

  # 3. PostgreSQL ready 대기
  step "PostgreSQL 준비 대기 중..."
  for i in $(seq 1 20); do
    if PGPASSWORD="$POSTGRES_PASSWORD" psql -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c '\q' >/dev/null 2>&1; then
      info "PostgreSQL 준비 완료 (${i}초)"
      break
    fi
    if [ "$i" -eq 20 ]; then
      err_exit "PostgreSQL 20초 내 응답 없음 — 컨테이너 상태 확인 필요"
    fi
    sleep 1
  done

  # 4. Redis ready 대기
  step "Redis 준비 대기 중..."
  for i in $(seq 1 10); do
    if redis-cli -h localhost -p 6379 ping >/dev/null 2>&1; then
      info "Redis 준비 완료 (${i}초)"
      break
    fi
    if [ "$i" -eq 10 ]; then
      err_exit "Redis 10초 내 응답 없음 — 컨테이너 상태 확인 필요"
    fi
    sleep 1
  done

  echo ""
  start_backend
  exit 0
fi

echo -e "사용법: $0 {backend|full|stop}"
exit 1
