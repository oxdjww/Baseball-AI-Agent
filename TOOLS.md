# TOOLS.md - 이 프로젝트 환경 메모

## SSH

```
Host: kbaseball (→ netcat.kr)
User: ~/.ssh/config 에 등록된 별칭
접속: ssh kbaseball
```

운영 서버 로그 확인:
```bash
ssh kbaseball "docker logs kbaseball-app --since 1h 2>&1 | grep -E 'ERROR|WARN'"
ssh kbaseball "docker ps"
ssh kbaseball "docker logs kbaseball-app --tail 100"
```

## 프로젝트 구조

```
Kbaseball/
├── backend/                  # Spring Boot 앱
│   ├── src/main/java/com/kbank/kbaseball/
│   │   ├── config/           # HikariCP, TaskScheduler, Security, Batch
│   │   ├── monitoring/       # AOP 관제, GlobalExceptionHandler, EventListener
│   │   ├── scheduler/        # RainAlertJobScheduler, RealtimeJobScheduler
│   │   ├── batch/tasklet/    # RainAlertTasklet, RealTimeAlertTasklet, GameAnalysisTasklet
│   │   ├── external/         # Naver, KMA, Perplexity API 클라이언트
│   │   ├── notification/     # TelegramService, WebhookRegistrar
│   │   └── game/             # GameProcessor, LeadChangeNotifier, GameEndNotificationBuilder
│   └── src/test/             # 유닛 + 통합 테스트
├── docs/daily-logs/          # 날짜별 개발 로그 (YYYY-MM-DD.md)
├── AGENTS.md / SOUL.md / USER.md / IDENTITY.md / TOOLS.md
└── README.md
```

## 인프라

| 구성요소 | 상세 |
|----------|------|
| 운영 서버 | Vultr VPS — netcat.kr |
| 컨테이너 | Docker Compose (app + postgres + redis) |
| 리버스 프록시 | Nginx (`/services/kbaseball`) |
| CI/CD | GitHub Actions → Docker Hub → Vultr |
| JVM 힙 | 400m |
| PostgreSQL | 256MB |
| Redis | 50MB (LRU) |

## 외부 API

| API | 용도 | 환경변수 |
|-----|------|----------|
| Telegram Bot | 알림 발송 + Webhook 수신 | `TELEGRAM_BOT_TOKEN`, `TELEGRAM_ADMIN_ID` |
| Naver Sports | 경기 일정/스코어/순위 | (별도 인증 없음) |
| KMA 기상청 | 강수량 조회 | `KMA_API_KEY` |
| Perplexity | AI 분석 (sonar-pro) | `OPENAI_API_KEY` (base-url override) |

## 주요 Redis Key

| 키 | TTL | 용도 |
|----|-----|------|
| `pending:signup:{token}` | 15m | Lazy Registration 임시 데이터 |
| `game:leader:{gameId}` | 24h | 역전 감지용 현재 리더 |
| `game:ended:{gameId}` | 24h | 경기 종료 중복 처리 방지 |

## 민감 파일 (절대 커밋 금지)

- `.env` — API 키, DB 비밀번호, Telegram 토큰
- `.mcp.json` — MCP 서버 인증 토큰
- `backend/.idea/` — IDE 설정
