# Kbaseball

<img width="256" height="256" alt="Kbaseball" src="https://github.com/user-attachments/assets/9d4918fa-d793-4c3b-a8aa-0e4beed8b0b8" />

KBO 팬을 위한 실시간 경기 이벤트 알림 및 AI 분석 리포트 자동 발송 서비스.
텔레그램 봇을 통해 경기 중 핵심 이벤트를 즉시 수신할 수 있습니다.

---

## Features

| 기능 | 설명 |
|------|------|
| 우천 취소 감지 | 경기 3h/1h 전 기상청 API 연동, 취소 시 즉시 공지 |
| 동점/역전 알림 | 실시간 스코어 폴링으로 주요 이벤트 즉시 전송 |
| 실시간 순위 | 경기 종료 후 KBO 순위 포함 알림 발송 |
| AI 분석 리포트 | 경기 종료 1시간 후 GPT 기반 경기 분석 자동 발송 |
| 장애 관제 | AOP + Spring Event 기반 관리자 Telegram 실시간 알림 |

---

## Architecture

<img width="868" height="286" alt="System Architecture" src="https://github.com/user-attachments/assets/ffe3751d-6580-4d43-a34d-0d8eff5a39a8" />

### Tech Stack

- **Backend** — Java 17, Spring Boot 3, Spring Batch
- **Database** — PostgreSQL, Redis
- **Infra** — Docker, Nginx, Vultr VPS, GitHub Actions
- **External API** — Telegram Bot API, OpenAI API, KMA(기상청) API

---

## UML

### 우천 취소 알림

<img width="1316" height="799" alt="Rain Alert UML" src="https://github.com/user-attachments/assets/fa747c15-36b1-49e3-9655-ec7a1eb78a83" />

### 동점/역전 알림

<img width="1249" height="591" alt="Realtime Alert UML" src="https://github.com/user-attachments/assets/f4802bb8-ced4-48f4-85bb-1c21d14622ee" />

### AI 경기 분석 리포트

<img width="987" height="727" alt="AI Report UML" src="https://github.com/user-attachments/assets/b2461a1f-a69f-403d-9bbc-56951ff8708d" />

---

## Changelog

### v2 — Kbaseball (Current)

- 기간: 2026-03-14 ~
- 패키지: `com.kbank.kbaseball`
- 도메인 분리 레이어드 아키텍처, Redis 상태 관리, AOP 기반 관제 시스템
- Lazy Registration: 텔레그램 연동 완료 시점에 DB INSERT (미연동 유저 DB 오염 방지)

### v1 — Baseball AI Agent (Legacy)

- 기간: 2025-07 ~ 2026-03-13
- 패키지: `com.kbank.baa`
- 단일 batch.service 구조, 인메모리 상태 관리

---

## Daily Work Logs

| 날짜 | 내용 요약 |
|------|----------|
| [2026-03-15](docs/daily-logs/2026-03-15.md) | v2 런칭: 관제 시스템, 경기종료 알림 고도화, 13개 커밋 |
| [2026-03-14](docs/daily-logs/2026-03-14.md) | Phase 1~6 대규모 리팩터링, CLAUDE.md 추가 |
