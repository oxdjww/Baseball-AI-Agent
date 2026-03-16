# USER.md

- **Name:** oxdjww
- **What to call them:** 그냥 반말로 (엉, 해줘, 알겠어 스타일)
- **Timezone:** Asia/Seoul (UTC+9)
- **GitHub:** oxdjww

---

## 개발 스타일

- Java 17 / Spring Boot 3.x 백엔드 개발자
- 레이어드 아키텍처(Controller → Service → Repository) 철저히 준수
- Record 클래스로 DTO, JPA Entity 분리 필수
- 테스트는 통합/유닛 혼용. 테스트 코드도 실제 코드처럼 엄격하게
- 커밋 메시지 한국어, `Feat:` / `Fix:` / `Refactor:` / `Docs:` 접두어 사용
- 과도한 엔지니어링 싫어함 — 지금 필요한 것만, 미래 대비 추상화 지양

## 소통 스타일

- 짧고 직접적인 답변 선호 (긴 전제 설명 싫어함)
- 코드 보여줄 때 diff 위주, 전체 파일 재출력 최소화
- "~할까요?" 식 물음 최소화 → 판단해서 바로 해
- 잘못된 분석엔 직접 교정 ("단단히 잘못 알고 있는데" 스타일)

## 현재 프로젝트

- **Kbaseball** — KBO 실시간 알림 + AI 분석 서비스
- 운영 서버: netcat.kr (Vultr VPS, Docker Compose)
- 로컬에서는 `./gradlew test`, `./gradlew bootRun` 으로 개발
- CI/CD: GitHub Actions → Docker Hub → Vultr 자동 배포

## 자주 쓰는 명령

```bash
./gradlew test              # 테스트 실행
./gradlew bootRun           # 로컬 실행
./gradlew clean build       # 전체 빌드
ssh kbaseball               # 운영 서버 접속
docker logs kbaseball-app --since 1h  # 최근 1시간 로그
```

## 기억할 것

- 운영 서버는 **netcat.kr** — MacBook 로컬이 아님
- `backend/logs/`는 로컬 테스트/과거 로그 (운영 로그 아님)
- `.env`, `.mcp.json`, `application-secret.yml` 등 절대 커밋 금지
- HikariCP keepalive, TaskScheduler await-termination 설정 이미 완료 (2026-03-16)
