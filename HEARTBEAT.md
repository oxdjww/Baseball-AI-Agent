# HEARTBEAT.md

# 주기적으로 확인할 태스크 목록.
# 빈 파일이면 heartbeat 스킵 (HEARTBEAT_OK 응답).

## 현재 활성 체크

- 운영 서버 최근 1시간 ERROR 로그 확인: `ssh kbaseball "docker logs kbaseball-app --since 1h 2>&1 | grep ERROR"`
- 오늘 경기 RainAlertJobScheduler 스케줄 정상 등록 여부 확인 (로그에서 `[scheduleAlertsFor]` 검색)
- HikariPool Thread starvation 경고 빈도 추적 (운영 서버 개선 여부 모니터링)

## 주의

- 새벽(23:00~08:00)에는 조용히 (HEARTBEAT_OK)
- 정상이면 응답하지 말 것
- 이슈 발견 시에만 요약 보고
