# Claude Code 활용 가이드 — Kbaseball 프로젝트

> 이 프로젝트에서 Claude Code를 어떻게 세팅하고 쓰는지 정리한 문서.

---

## 1. CLAUDE.md — 프로젝트 컨텍스트 주입

Claude Code가 **매 대화 시작 시 자동으로 읽는** 프로젝트 규칙 파일.
아키텍처, DB 컨벤션, 코드 스타일을 여기에 정의해두면 별도 설명 없이도 프로젝트에 맞게 동작한다.

**실제로 적용된 규칙들:**

| 규칙 | 적용 사례 |
|------|-----------|
| Record 클래스로 DTO 작성 | `GameEndNotificationBuilder`, Lazy Registration 토큰 DTO 등 전체 적용 |
| SELECT * 금지, FK 인덱스 필수 | 레포지토리 쿼리 작성 시 자동으로 지킴 |
| HikariCP 설정 명시 | `application.yml`에 keepalive, idle-timeout 등 직접 추가 (2026-03-16) |
| 새 기능 전 Plan 모드 사용 | Lazy Registration 전환, AOP 관제 시스템 설계 전 사용 |
| N+1 방지 | `@BatchSize`, JOIN FETCH 적용 |

"10만 건 이상 테이블은 EXPLAIN ANALYZE 확인" 같은 기준도 박혀 있어서
Claude가 쿼리 최적화 제안 시 이 기준을 자동으로 참조한다.

---

## 2. .claude/settings.local.json — 명령어 권한 관리

Claude가 실행할 수 있는 Bash 명령어를 **화이트리스트**로 관리.
허용 목록에 있는 명령은 팝업 없이 자동 실행, 없는 건 승인 요청이 뜬다.

```json
{
  "permissions": {
    "allow": [
      "Bash(git:*)",
      "Bash(./gradlew test:*)",
      "Bash(ssh:*)",
      "Bash(docker logs:*)",
      "Bash(scp:*)",
      "mcp__notion__API-post-page",
      "mcp__notion__API-patch-block-children",
      "WebSearch"
    ]
  }
}
```

**운용 원칙:**
- 개발 루틴 명령(git, 테스트, SSH 로그 조회)은 자동 허용 → 작업 흐름이 끊기지 않음
- DB 직접 삭제, 운영 재시작 등 위험한 작업은 의도적으로 제외 → 팝업으로 한 번 더 확인
- `grep`, `find`, `sed` 는 허용하지 않음 — Claude Code의 전용 툴(`Grep`, `Glob`, `Edit`)을 쓰게 유도
- MCP 서버 API 호출 범위도 여기서 지정

---

## 3. Plan Mode — 큰 결정 전 설계 먼저

`/plan` 명령으로 설계 모드 진입. 구현 시작 전 아키텍처와 영향도를 먼저 정리한다.

**실제로 Plan Mode를 쓴 상황들:**

- **Lazy Registration 전환**: 폼 제출 시 DB INSERT → Redis TTL 저장으로 플로우 전환.
  DB 스키마, 스케줄러, 컨트롤러 전체에 영향 → Plan에서 먼저 설계 확정 후 구현
- **AOP 관제 시스템 설계**: `MonitoringAspect` → `Spring Event` → `TelegramService` 파이프라인.
  레이어 분리 방식을 먼저 확정 후 진행
- **봇 공격 방어 3층 구조**: nginx 레이트 리밋 + purge 스케줄러 + Honeypot 조합을
  Plan 단계에서 결정 후 각 레이어 순서대로 구현

설계 승인 → 일반 모드 전환 → 구현. 이 순서 덕분에 중간에 방향을 뒤엎는 일이 없었다.

---

## 4. Auto Memory — 세션 간 컨텍스트 유지

Claude Code가 대화 사이에 기억을 유지하는 기능.
`~/.claude/projects/.../memory/` 에 마크다운 파일로 저장되고, 다음 대화 시 자동으로 로드된다.

**실제로 저장된 기억들:**

```
feedback_commit_message_style.md
  → "커밋 메시지는 git log 스타일 확인 후 한국어로 작성"
  → 한 번 교정하면 이후 대화에서 자동으로 지킴

project_deployment.md
  → "운영 서버는 netcat.kr (Vultr VPS), 로컬 MacBook이 아님"
  → 로그 확인 요청 시 로컬 logs/ 보지 않고 SSH로 바로 접근

project_monitoring_system.md
  → "AOP + Spring Event 관제 시스템 구현 완료 (dev 브랜치, 2026-03-15)"
  → 관련 작업 시 기존 구조 파악 후 접근
```

핵심은 **매번 배경 설명을 다시 안 해도 된다**는 것.
"운영 서버 로그 봐줘"라고만 해도 `netcat.kr`에 SSH로 바로 붙는다.

---

## 5. MCP 서버 연동

Claude Code에 외부 서비스를 직접 연결해서 도구처럼 쓴다.

| MCP 서버 | 실제 사용 |
|----------|-----------|
| **Notion** | 운영 이슈 해결 후 TroubleShooting DB에 직접 기록 |
| **GitHub** | PR 생성, 이슈 조회 |
| **context7** | Spring Boot, Spring Batch 최신 공식 문서 실시간 조회 |

예: 운영 장애 대응 후 Claude가 Notion 페이지에 원인/해결 내용을 직접 작성.
별도로 Notion을 열어서 정리할 필요 없음.

---

## 6. OpenClaw — Claude Code와 별개로 돌아가는 외부 감시 AI

> **Claude Code와는 완전히 다른 별개의 AI 에이전트 도구.**
> Claude Code는 개발 작업용 대화형 AI이고, OpenClaw는 운영 서버를 자동으로 감시하는 상시 실행 에이전트다.

```
[Claude Code]                      [OpenClaw]
개발 작업용 대화형 AI              MacBook에서 상시 실행
코드 작성, 디버깅, 배포       ←→  5분마다 cron 실행
설계, 테스트, 문서화               ssh kbaseball
                                   docker logs --since 5m
                                   ERROR 감지 → Telegram 알림
                                   정상이면 무음
```

**왜 이중으로 운영하나:**

| | Spring Boot MonitoringAspect | OpenClaw SSH 감시 |
|-|------------------------------|-------------------|
| 방식 | 앱 내부 AOP | 앱 외부 SSH |
| 감지 대상 | 특정 API 예외, 서비스 레이어 오류 | 앱 프로세스 자체 다운, DB 연결 완전 단절 |
| 한계 | 앱이 멈추면 감지 불가 | 5분 지연 |
| 역할 | 전술적 실시간 대응 | 전략적 최악의 상황 감지 |

두 시스템이 서로 감시 불가능한 영역을 보완하는 구조.
1GB RAM 서버에서 AI 분석 부하를 MacBook으로 분산하는 효과도 있다.

**OpenClaw 워크스페이스 파일들 (프로젝트 루트):**

| 파일 | 역할 |
|------|------|
| `SOUL.md` | 이 프로젝트에서 AI 행동 원칙 |
| `USER.md` | 개발자 프로파일 (소통 스타일, 환경) |
| `TOOLS.md` | SSH 설정, 인프라 정보, Redis 키 등 환경 메모 |
| `HEARTBEAT.md` | 주기적으로 체크할 항목 정의 |
| `AGENTS.md` | 에이전트 행동 규칙 (메모리 관리, 안전 기준) |

---

## 7. 운영 이슈 대응 패턴

Claude Code로 운영 이슈를 처리하는 고정 흐름:

```
1. SSH로 운영 서버 로그 확인
   ssh kbaseball "docker logs kbaseball-app --since 1h 2>&1 | grep -E 'ERROR|WARN'"

2. 코드 / 설정 / 인프라 중 원인 위치 특정

3. 최소 변경으로 수정 (과도한 방어 코드 추가 금지)

4. ./gradlew test 전체 통과 확인

5. git commit + push → GitHub Actions 자동 배포

6. Notion TroubleShooting DB에 MCP로 직접 기록
```

---

## 8. Git 워크플로우

Claude가 커밋을 만들 때 자동으로 따르는 규칙 (`SOUL.md` + Auto Memory에 정의):

- 커밋 전 `git log` 스타일 확인 후 한국어로 메시지 작성
- 접두어: `Feat:` / `Fix:` / `Refactor:` / `Docs:`
- 커밋 전 `./gradlew test` 통과 필수
- staged diff 확인 → `.env`, `.mcp.json` 등 민감 파일 포함 여부 체크 후 커밋

---

## 요약

| 기능 | 목적 | 효과 |
|------|------|------|
| `CLAUDE.md` | 프로젝트 규칙 자동 주입 | 매번 설명 없이도 컨벤션 준수 |
| `settings.local.json` | 명령어 권한 관리 | 개발 루틴은 자동 실행, 위험 작업은 승인 팝업 |
| Plan Mode | 설계 → 구현 순서 강제 | 큰 변경 전 방향 확정 후 진행 |
| Auto Memory | 세션 간 컨텍스트 유지 | 배경 설명 재설명 불필요 |
| MCP 연동 | 외부 서비스 직접 제어 | Notion 기록, GitHub 관리를 대화 중에 처리 |
| OpenClaw | 운영 서버 외부 감시 (Claude Code와 별개) | 앱이 죽어도 감지 가능한 2중 안전망 |
