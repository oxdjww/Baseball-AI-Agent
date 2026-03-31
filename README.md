# Kbaseball

<img width="128" height="128" alt="Kbaseball" src="https://github.com/user-attachments/assets/9d4918fa-d793-4c3b-a8aa-0e4beed8b0b8" />

KBO 팬을 위한 실시간 경기 이벤트 알림 및 AI 분석 리포트 자동 발송 서비스.
텔레그램 봇을 통해 경기 중 핵심 이벤트(역전·동점·우천취소)를 즉시 수신하고, 경기 종료 후 GPT 기반 분석 리포트를 제공합니다.

---

## Features

| 기능 | 설명 |
|------|------|
| 우천 취소 감지 | 경기 3h/1h 전 기상청 API 연동, 취소 여부 즉시 공지 |
| 동점/역전 알림 | 3분 간격 스코어 폴링으로 리드 변경 이벤트 즉시 전송 |
| 경기 종료 알림 | 최종 스코어 + KBO 순위(경기 결과 즉시 보정) + 다음 경기 일정 포함 |
| AI 분석 리포트 | 경기 종료 1시간 후 Perplexity(sonar-pro) 기반 분석 자동 발송 |
| 장애 관제 | AOP + Spring Event 기반 서비스 계층 오류 실시간 알림 |
| Feature Toggle | 관리자 UI에서 기능별(AI분석/역전감지/우천알림) 런타임 토글 |

---

## System Architecture

```mermaid
graph TB
    subgraph Users["사용자"]
        BROWSER[Web Browser]
        TG_USER[Telegram User]
    end

    subgraph Infra["Infrastructure · Vultr VPS"]
        NGINX[Nginx\nReverse Proxy]
        subgraph Docker["Docker Compose"]
            APP[Spring Boot 3\nJava 17 · JVM 400m]
            PG[(PostgreSQL 15\n256MB)]
            REDIS[(Redis Alpine\n50MB · LRU)]
        end
    end

    subgraph External["External APIs"]
        TG_API[Telegram Bot API]
        NAVER[Naver Sports API\n경기일정·실시간스코어·순위·기록]
        KMA[KMA 기상청 API\n강수량 조회]
        PERPLEXITY[Perplexity API\nsonar-pro]
    end

    BROWSER -->|HTTPS| NGINX
    TG_USER <-->|Bot| TG_API
    TG_API -->|Webhook POST| NGINX
    NGINX -->|/services/kbaseball| APP

    APP <-->|JPA| PG
    APP <-->|Spring Data Redis| REDIS
    APP -->|sendMessage| TG_API
    APP -->|GET 경기 데이터| NAVER
    APP -->|GET 강수량| KMA
    APP -->|Chat Completions| PERPLEXITY
```

---

## Data Flow

### 1. 회원가입 · Telegram 연동 (Lazy Registration)

DB에는 Telegram 연동이 확정된 회원만 저장됩니다. 폼 제출 시점에는 Redis에만 임시 데이터를 기록하고, 봇이 `/start` 명령을 수신하는 순간 `telegramId`를 포함한 레코드를 삽입합니다.

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Web as HomeController
    participant Redis as Redis<br/>pending:signup:{token} TTL 15m
    participant Success as SignupSuccessController
    participant TG as Telegram Bot
    participant Webhook as TelegramWebhookService
    participant DB as PostgreSQL

    User->>Web: POST /home/signup (name, team, 알림설정)
    Web->>Redis: SET pending:signup:{token} → PendingMemberData (JSON)
    Web->>User: 302 redirect /signup_success (session: signupToken)

    User->>Success: GET /signup_success
    Success->>Redis: GET pending:signup:{token}
    Redis-->>Success: PendingMemberData
    Success-->>User: 가입 완료 페이지<br/>+ Telegram 딥링크 (t.me/bot?start={token})

    User->>TG: 딥링크 클릭 → /start {token}
    TG->>Webhook: Webhook POST (text="/start {token}", telegramUserId)
    Webhook->>Redis: GET pending:signup:{token}
    Redis-->>Webhook: PendingMemberData (JSON)
    Webhook->>DB: INSERT member (name, team, telegramId, 알림설정)
    Note over Webhook,DB: @Transactional — 커밋 완료 후 Redis 키 삭제
    Webhook->>DB: commit
    Webhook->>Redis: DEL pending:signup:{token}
    Webhook->>TG: "텔레그램 연동이 완료되었습니다 ✅"
    TG->>User: 연동 완료 메시지
```

---

### 2. 우천 취소 알림

```mermaid
sequenceDiagram
    participant Sched as RainAlertJobScheduler<br/>@Scheduled 매일 자정·앱 시작시
    participant Naver as NaverSportsClient
    participant Task as TaskScheduler
    participant Tasklet as RainAlertTasklet
    participant KMA as KmaWeatherClient
    participant Member as MemberService
    participant TG as TelegramService

    Sched->>Naver: fetchScheduledGames(today)
    Naver-->>Sched: List<ScheduledGameDto>

    loop 경기별 (3h전 · 1h전)
        Sched->>Task: schedule(RainAlertTasklet, 경기시작 - N시간)
    end

    Note over Task,Tasklet: 예약 시각 도달
    Task->>Tasklet: executeForGame(game, thresholdMm)
    Tasklet->>KMA: getRainfallByTeam(teamCode, alertTime)
    KMA-->>Tasklet: 강수량(mm)

    alt 실내 구장 (고척)
        Tasklet->>TG: "실내 경기장, 비 걱정 없어요!"
    else 강수량 < threshold
        Tasklet->>TG: "쾌청! 즐거운 관전 되세요"
    else 강수량 ≥ threshold
        Tasklet->>Naver: fetchCancelInfoFromGameInfo(gameId)
        Naver-->>Tasklet: isCanceled: true/false
        alt 취소 확정
            Tasklet->>TG: GAME_CANCELED 템플릿
        else 취소 가능성
            Tasklet->>TG: 우천취소 주의 메시지
        end
    end

    Tasklet->>Member: findBySupportTeamAndNotifyRainAlertTrue(team)
    Member-->>Tasklet: List<Member>
    loop 대상 회원
        Tasklet->>TG: sendPersonalMessage(telegramId, name, text)
    end
```

---

### 3. 동점 · 역전 실시간 알림

```mermaid
sequenceDiagram
    participant Cron as RealtimeJobScheduler<br/>cron: 0 0/3 13-23 * * *
    participant Batch as Spring Batch<br/>realTimeAlertJob
    participant Service as GameAlertService
    participant Naver as NaverSportsClient
    participant Proc as GameProcessor
    participant Redis as Redis<br/>game:leader:{gameId}
    participant Notifier as LeadChangeNotifier
    participant Adjuster as StandingsAdjuster
    participant TG as TelegramService

    Cron->>Batch: JobLauncher.run(realTimeAlertJob)
    Batch->>Service: processAlertsFor(today)
    Service->>Naver: fetchScheduledGames(today)
    Naver-->>Service: List<ScheduledGameDto>

    loop 진행 중 경기
        Service->>Proc: process(game, members)
        Proc->>Naver: fetchGameInfo(gameId)
        Naver-->>Proc: RealtimeGameInfoDto {statusCode, homeScore, awayScore}

        alt statusCode == STARTED
            Proc->>Notifier: notify(game, members, info)
            Notifier->>Notifier: calculateLeader(info)
            Notifier->>Redis: GETSET game:leader:{gameId} → {currLeader}
            Redis-->>Notifier: prevLeader

            alt currLeader ≠ prevLeader (역전·동점 감지)
                loop 홈팀·어웨이팀 팬
                    Notifier->>TG: sendPersonalMessage(역전/동점 메시지)
                end
            end

        else statusCode == ENDED
            Proc->>Redis: SETNX game:ended:{gameId} TTL 24h
            Note over Proc,Redis: 중복 발송 방지 — 최초 1회만 통과

            Proc->>Naver: fetchStandings()
            Naver-->>Proc: KboStandingsResult (stale — 경기 결과 미반영)
            Proc->>Adjuster: applyGameResult(rawStandings, info)
            Note over Adjuster: 승/패/무 적용 → 승률 재정렬 → rank·GB 재계산
            Adjuster-->>Proc: 보정된 KboStandingsResult
            Proc->>Naver: fetchScheduledGames(today+1, today+7)
            Naver-->>Proc: 다음 경기 일정

            loop 홈팀·어웨이팀 팬
                Proc->>TG: 경기종료 + 보정된 순위 + 다음경기 알림
            end

            Note over Proc: AI 분석 활성화 시 TaskScheduler로 1h 후 예약
        end
    end
```

---

### 4. AI 경기 분석 리포트

```mermaid
sequenceDiagram
    participant Proc as GameProcessor
    participant Task as TaskScheduler
    participant Tasklet as GameAnalysisTasklet
    participant NaverRecord as NaverGameRecordClient
    participant NaverRoster as NaverRosterClient
    participant Prompt as GameAnalysisPromptBuilder
    participant AI as Perplexity API<br/>sonar-pro
    participant Fmt as GameMessageFormatter
    participant TG as TelegramService

    Note over Proc: 경기 종료 감지 (statusCode == ENDED)
    Proc->>Task: schedule(GameAnalysisTasklet, now + 1h)

    Note over Task,Tasklet: 1시간 후 실행
    Task->>Tasklet: execute(schedule, info)
    Tasklet->>NaverRecord: fetchRecordData(gameId)
    NaverRecord-->>Tasklet: 스코어보드 · 타자·투수 박스스코어

    Tasklet->>NaverRoster: fetchPlayerNamesByTeam(gameId, awayTeam)
    NaverRoster-->>Tasklet: 어웨이팀 선수 명단

    Tasklet->>NaverRoster: fetchPlayerNamesByTeam(gameId, homeTeam)
    NaverRoster-->>Tasklet: 홈팀 선수 명단

    Tasklet->>Prompt: build(schedule, info, recordData, rosters)
    Prompt-->>Tasklet: 분석 프롬프트 (승리·패배 요인 요청)

    Tasklet->>AI: ChatCompletion (sonar-pro, temperature=0.7)
    AI-->>Tasklet: 한국어 경기 분석 텍스트

    Tasklet->>Fmt: formatScoreboard(scoreBoard)
    Fmt-->>Tasklet: ASCII 스코어보드 (<pre> 포맷)

    loop notifyGameAnalysis=true 회원 중 해당 팀 팬
        Tasklet->>TG: sendPersonalMessage(스코어보드 + AI 분석)
    end
```

---

### 5. 장애 관제 (AOP + Spring Event + Telegram)

서비스 계층 전체에 AOP를 적용해 예외를 가로채고, Spring Event 파이프라인을 통해 관리자 Telegram으로 실시간 관제 메시지를 발송합니다.
`@RestController` 계층 예외는 `GlobalExceptionHandler`가 별도로 커버하여 누락 없이 포착합니다.

```mermaid
flowchart TD
    subgraph AppLayer["애플리케이션 계층"]
        SVC["@Service 메서드\nGameAlertService · TelegramLinkService\nMemberService · RainAlertTasklet ..."]
        CTL["@RestController 메서드\nGlobalExceptionHandler 커버"]
    end

    subgraph AOPLayer["AOP 관제 계층"]
        ASPECT["MonitoringAspect\n@Around — 전체 Service 계층\n(notification 패키지 제외 — 무한루프 방지)"]
        DEPTH{"callDepth == 1?\nCGLIB 중첩 방지"}
    end

    subgraph EventLayer["Spring Event 파이프라인"]
        EVT["MonitoringErrorEvent\nfeatureName: ClassName.method\nerrorMessage · stackTrace 5 frames\ncontextData: 메서드 인자"]
        LISTENER["MonitoringEventListener\n@EventListener"]
    end

    subgraph TelegramLayer["Telegram 관제 채널"]
        MSG["🚨 SYSTEM CRITICAL\n📛 기능: {className.method}\n💬 오류: {escaped message}\n📋 인자: {arg0, arg1, ...}\n🔍 스택: {top 5 frames}\n⏰ {KST timestamp}"]
        TG["TelegramService\nsendPlainMessage(adminId, html)"]
        API["Telegram Bot API\n→ 관리자 DM"]
    end

    SVC -->|"RuntimeException 발생"| ASPECT
    ASPECT --> DEPTH
    DEPTH -->|"Yes — 이벤트 발행"| EVT
    DEPTH -->|"No — rethrow"| SVC
    CTL -->|"미처리 예외"| GEXH["GlobalExceptionHandler\nAOP 미처리 여부 스택 검사"]
    GEXH -->|"AOP 미처리 시"| EVT
    EVT --> LISTENER
    LISTENER -->|"HtmlUtils.htmlEscape\n특수문자 이스케이프"| MSG
    MSG --> TG
    TG --> API
```

#### 관제 메시지 포맷 예시

```
[🚨 SYSTEM CRITICAL]
📛 기능: GameAlertService.processAlertsFor
💬 오류: Read timed out executing GET https://api-gw.sports.naver.com/...
📋 인자: {arg0=2026-05-10}
🔍 스택:
  NaverSportsClient.fetchScheduledGames(NaverSportsClient.java:58)
  GameAlertService.processAlertsFor(GameAlertService.java:42)
  ...
⏰ 2026-05-10 18:03:21 KST
```

---

### 6. OpenClaw 기반 장애탐지 체계

**OpenClaw**는 이 프로젝트와 함께 운영되는 Claude Code AI 에이전트 워크스페이스입니다.
Spring AOP가 장애를 자동 감지해 Telegram으로 관제톡을 발송하면,
개발자가 해당 내용을 Telegram 연결 OpenClaw에게 전달하는 것만으로
코드 탐색·원인 분석·패치·커밋·Notion 기록까지 AI가 자율 수행합니다.

```mermaid
flowchart TD
    subgraph SpringBoot["Spring Boot (자동)"]
        EX["서비스 레이어 예외 발생"]
        AOP["MonitoringAspect\nAOP 감지 — depth=1 중복방지"]
        EVT["MonitoringErrorEvent publish\nfeatureName · errorMessage · stackTrace"]
        TG_SEND["TelegramService\n관제톡 자동 발송"]
    end

    subgraph Human["개발자 (1회 개입)"]
        DEV["관제톡 확인 후\nOpenClaw Telegram 채팅에 전달"]
    end

    subgraph OpenClaw["OpenClaw AI Agent (Claude Code)"]
        direction TB
        RECV["관제톡 내용 파악\nfeatureName · args 파싱"]
        ANALYSIS["원인 분석\n코드 탐색 / SSH 로그 조회"]
        JUDGE{"심각도 판단"}
        HOTFIX["코드 패치 → commit → push\nCI/CD 자동 배포 트리거"]
        DEFER["이슈 등록 및 문서화"]
    end

    subgraph Tools["OpenClaw 활용 수단"]
        NOTION["Notion MCP\n장애 로그 자동 기록\n(TroubleShooting DB)"]
        SSH["SSH — netcat.kr\n운영 서버 로그 점검"]
        CODE["코드베이스 탐색\nGrep · Read · Edit"]
        CICD["GitHub Actions\npush 트리거 → Docker Hub → Vultr 배포"]
    end

    EX --> AOP --> EVT --> TG_SEND
    TG_SEND -->|"🚨 SYSTEM CRITICAL"| DEV
    DEV -->|"관제톡 내용 전달"| RECV
    RECV --> ANALYSIS
    ANALYSIS --> SSH
    ANALYSIS --> CODE
    ANALYSIS --> JUDGE
    JUDGE -->|"Critical — 즉각 대응"| HOTFIX
    JUDGE -->|"Low — 추후 처리"| DEFER
    HOTFIX --> CODE
    HOTFIX --> CICD
    HOTFIX --> NOTION
    DEFER --> NOTION
```

#### OpenClaw 워크스페이스 파일 구조

| 파일 | 역할 |
|------|------|
| `AGENTS.md` | 에이전트 세션 동작 규칙 — 메모리 관리, 그룹챗 예절, Heartbeat 활용법 |
| `SOUL.md` | 에이전트 핵심 가치관 및 행동 원칙 |
| `IDENTITY.md` | 에이전트 이름 · 캐릭터 · 시그니처 정의 |
| `USER.md` | 사용자 프로필 — 이름, 타임존, 선호 스타일 |
| `HEARTBEAT.md` | 주기적 점검 태스크 — 서버 상태, 이슈 배치 점검 |
| `TOOLS.md` | 로컬 환경 도구 메모 — SSH 호스트, 디바이스 정보 |

#### 활성 통합

| 통합 | 용도 |
|------|------|
| **Notion MCP** | 장애 로그·개발 기록 자동 작성 |
| **GitHub MCP** | 이슈·PR 조회 및 코멘트 |
| **OpenClaw Heartbeat** | 주기적 서버 관제·로그 점검 |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 17, Spring Boot 3.x |
| Persistence | Spring Data JPA, PostgreSQL 15 |
| Batch | Spring Batch (Tasklet 기반) |
| Cache | Spring Data Redis |
| Scheduling | `@Scheduled` + `TaskScheduler` (ThreadPool size 4) |
| AOP | Spring AOP (`@Aspect`, `@Around`) |
| External API | RestTemplate + `@Retryable` (max 5, backoff 2s) |
| AI | Spring AI → Perplexity API (sonar-pro) |
| Notification | Telegram Bot API (Webhook 방식) |
| Infra | Docker Compose, Nginx, Vultr VPS, GitHub Actions CI/CD |

---

## Redis Key Strategy

| Key Pattern | TTL | Purpose |
|-------------|-----|---------|
| `pending:signup:{token}` | 15m | 임시 회원 데이터 (Lazy Registration) |
| `linked:signup:{token}` | 10m | Telegram 연동 완료 마커 (만료 페이지 오판 방지) |
| `game:leader:{gameId}` | 24h | 현재 리더 추적 (역전 감지용 이전값 비교) |
| `game:ended:{gameId}` | 24h | 경기 종료 처리 중복 방지 (SETNX) |

---

## Changelog

### v2 — Kbaseball · 2026-03-14 ~

- 도메인 분리 레이어드 아키텍처 (`com.kbank.kbaseball`)
- Redis 기반 상태 관리 (리더 추적, 중복 방지)
- Lazy Registration: 텔레그램 연동 완료 시점에만 DB INSERT
- AOP + Spring Event 기반 실시간 관제 시스템
- Feature Toggle (런타임 기능별 on/off)
- 경기 종료 알림 고도화 (실시간 순위 + 다음 경기 포함)
- StandingsAdjuster: 경기 종료 시 순위/승차 즉시 보정 (Naver API 지연 문제 해결)

### v1 — Baseball AI Agent · 2025-07 ~ 2026-03-13

- 단일 batch.service 구조 (`com.kbank.baa`)
- 인메모리 상태 관리

---

## Technical Docs

| 문서 | 내용 |
|------|------|
| [StandingsAdjuster](docs/standings-adjuster.md) | 경기 종료 순위 즉시 보정 로직 — 승차 공식, 재정렬 기준, 엣지 케이스 |
| [Lazy Registration & Bot Defense](docs/lazy-registration.md) | 봇 공격 대응 3층 방어 — Nginx rate limit, Honeypot, Redis TTL 기반 지연 DB 삽입 |
| [Redis State Design](docs/redis-state-design.md) | GETSET/SETNX 원자성 활용 — 역전 감지, 경기 종료 중복 방지, 서버 재시작 내성 |
| [Schema Evolution](docs/schema-evolution.md) | Flyway V1~V5 스키마 진화 — 복합 PK 실수 교정, soft delete, partial unique index 전환 |
| [Monitoring AOP Design](docs/monitoring-aop.md) | AOP 관제 설계 결정사항 — CGLIB callDepth 이중 발화 방지, notification 패키지 제외 이유 |

---

## Daily Work Logs

| 날짜 | 내용 요약 |
|------|----------|
| [2026-03-22](docs/daily-logs/2026-03-22.md) | v1 운영 전 점검, 회원탈퇴(soft delete + partial unique index), 관제 구조도 README 상세화, Thymeleaf section 밖 요소 버림 버그 2회 수정 |
| [2026-03-21](docs/daily-logs/2026-03-21.md) | Home/Preferences UI 모바일 최적화, Member 복합키→단일키 리팩터링, 텔레그램 연동 환영 메시지, signup_success UI 개편 |
| [2026-03-19](docs/daily-logs/2026-03-19.md) | StandingsAdjuster 도입 — 경기 종료 순위 즉시 보정, 승차 재계산, 단위·통합 테스트 81개 통과 |
| [2026-03-16](docs/daily-logs/2026-03-16.md) | HikariCP keepalive 설정, TaskScheduler 타임아웃 개선, TelegramWebhookRegistrar NPE 수정, OpenClaw 워크스페이스 초기화 |
| [2026-03-15](docs/daily-logs/2026-03-15.md) | v2 런칭: 관제 시스템, 경기종료 알림 고도화, Vultr 배포, Lazy Registration, 23개 커밋 |
| [2026-03-14](docs/daily-logs/2026-03-14.md) | Phase 1~6 대규모 리팩터링, CLAUDE.md 추가 |
