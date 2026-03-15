# ⚾️ Kbaseball
<img width="256" height="256" alt="image" src="https://github.com/user-attachments/assets/9d4918fa-d793-4c3b-a8aa-0e4beed8b0b8" />

> **실시간 경기 알림 + AI 분석 리포트**
> "야구는 기록으로 말한다, 우리는 알림으로 본다."

---

## 핵심 가치

KBO 팬을 위한 실시간 경기 이벤트 알림 및 AI 분석 리포트 자동 발송 서비스.
경기 중 중요한 순간을 놓치지 않도록, 텔레그램으로 즉시 알림을 전달합니다.

## 주요 기능

| 기능 | 설명 |
|------|------|
| 우천 취소 감지 | 경기 3h/1h 전 기상청 API 연동, 취소 시 즉시 공지 |
| 동점/역전 알림 | 실시간 스코어 폴링으로 주요 이벤트 즉시 전송 |
| 실시간 순위 | 경기 종료 후 KBO 순위 포함 알림 발송 |
| AI 분석 리포트 | 경기 종료 1시간 후 AI 기반 경기 분석 자동 발송 |
| 장애 관제 | AOP + Spring Event → 관리자 Telegram 실시간 알림 |

---

## 프로젝트 히스토리

### v1 — Baseball AI Agent (Legacy)
- 기간: 2025-07 ~ 2026-03-13
- 패키지: `com.kbank.baa`
- 특징: 단일 batch.service 구조, 인메모리 상태 관리

### v2 — Kbaseball (Current)
- 기간: 2026-03-14 ~
- 패키지: `com.kbank.kbaseball`
- 특징: 도메인 분리 레이어드 아키텍처, Redis 상태 관리, 기능 토글, 관제 시스템

---

## 🏗 시스템 아키텍처

<img width="868" height="286" alt="image" src="https://github.com/user-attachments/assets/ffe3751d-6580-4d43-a34d-0d8eff5a39a8" />


## 🧩 UML

### ☔️ 우천 취소 알림 기능

<img width="1316" height="799" alt="image" src="https://github.com/user-attachments/assets/fa747c15-36b1-49e3-9655-ec7a1eb78a83" />

### 🔥 동점/역전 알림 기능

<img width="1249" height="591" alt="image" src="https://github.com/user-attachments/assets/f4802bb8-ced4-48f4-85bb-1c21d14622ee" />

### 🤖 AI 경기 분석 레포트 발송 기능

<img width="987" height="727" alt="image" src="https://github.com/user-attachments/assets/b2461a1f-a69f-403d-9bbc-56951ff8708d" />

---

## 🗓️ Daily Work Logs

| 날짜 | 내용 요약 |
|------|----------|
| [2026-03-15](docs/daily-logs/2026-03-15.md) | v2 런칭: 관제 시스템, 경기종료 알림 고도화, 13개 커밋 |
| [2026-03-14](docs/daily-logs/2026-03-14.md) | Phase 1~6 대규모 리팩터링, CLAUDE.md 추가 |

---

## 오늘 로그 작성 가이드

"오늘 로그 작성해줘"라고 하면 Claude Code가 아래를 자동 수행:
1. `git log --oneline --after="어제" --before="내일"` 으로 오늘 커밋 전체 조회
2. 커밋별 `--stat` + 메시지 분석
3. `docs/daily-logs/YYYY-MM-DD.md` 파일 생성
4. README.md Daily Work Logs 테이블에 링크 행 추가
5. git commit + push (dev 브랜치)
