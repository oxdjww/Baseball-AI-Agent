#!/bin/bash
set -euo pipefail

echo "======================================"
echo " Kbaseball v2 — 서버 기초 공사 시작"
echo "======================================"

# ── 1. Swap 4GB 생성 ──────────────────────
echo "[1/5] Swap 설정..."
if swapon --show | grep -q /swapfile; then
  echo "  → /swapfile 이미 존재, 건너뜀"
else
  fallocate -l 4G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "  → 4GB Swap 생성 완료"
fi
sysctl vm.swappiness=10
echo 'vm.swappiness=10' >> /etc/sysctl.conf
free -h

# ── 2. Docker 설치 ────────────────────────
echo "[2/5] Docker 설치..."
if command -v docker &>/dev/null; then
  echo "  → Docker 이미 설치됨: $(docker --version)"
else
  apt-get update -qq
  apt-get install -y -qq ca-certificates curl gnupg lsb-release

  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list

  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin

  systemctl enable docker
  systemctl start docker
  echo "  → Docker 설치 완료: $(docker --version)"
  echo "  → Docker Compose: $(docker compose version)"
fi

# ── 3. Timezone 설정 ──────────────────────
echo "[3/5] Timezone → Asia/Seoul..."
timedatectl set-timezone Asia/Seoul
echo "  → $(timedatectl | grep 'Time zone')"

# ── 4. UFW 방화벽 ─────────────────────────
echo "[4/5] UFW 방화벽 설정..."
if ! command -v ufw &>/dev/null; then
  apt-get install -y -qq ufw
fi
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp   comment 'SSH'
ufw allow 80/tcp   comment 'HTTP'
ufw allow 443/tcp  comment 'HTTPS'
ufw allow 8080/tcp comment 'Spring Boot'
ufw --force enable
ufw status verbose

# ── 5. 앱 디렉터리 준비 ───────────────────
echo "[5/5] 앱 디렉터리 준비..."
mkdir -p /root/kbaseball
if [ ! -f /root/kbaseball/.env ]; then
  cat > /root/kbaseball/.env.template << 'EOF'
# PostgreSQL
POSTGRES_USER=kbaseball
POSTGRES_PASSWORD=CHANGE_ME
POSTGRES_DB=kbaseball

# Spring Boot
ADMIN_PASSWORD=CHANGE_ME
SECRET_KEY=CHANGE_ME

# Telegram
TELEGRAM_BOT_TOKEN=CHANGE_ME
TELEGRAM_CHAT_ID=CHANGE_ME
TELEGRAM_ADMIN_ID=CHANGE_ME

# External APIs
OPENAI_API_KEY=CHANGE_ME
KMA_API_KEY=CHANGE_ME
EOF
  echo "  → .env.template 생성됨"
  echo "  !! /root/kbaseball/.env 파일을 직접 생성하세요:"
  echo "     cp /root/kbaseball/.env.template /root/kbaseball/.env"
  echo "     nano /root/kbaseball/.env"
else
  echo "  → .env 이미 존재"
fi

echo ""
echo "======================================"
echo " 기초 공사 완료!"
echo "======================================"
echo ""
echo "다음 단계:"
echo "  1. /root/kbaseball/.env 파일 생성 및 값 입력"
echo "  2. docker-compose.prod.yml 복사:"
echo "     scp docker-compose.prod.yml root@$(hostname -I | awk '{print $1}'):/root/kbaseball/"
echo "  3. 컨테이너 실행:"
echo "     cd /root/kbaseball && docker compose -f docker-compose.prod.yml up -d"
echo ""
