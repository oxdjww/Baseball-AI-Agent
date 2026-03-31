# Redis 상태 관리 설계 — GETSET / SETNX 원자성 활용

## 왜 만들었나

v1은 역전 감지를 위한 "현재 리더" 정보를 JVM 힙 내 `Map<String, String>`으로 관리했다.
서버를 재시작하면 이 Map이 초기화되고, 재시작 직후 실행된 폴링 사이클에서
이미 발생한 역전 이벤트를 "새 이벤트"로 판단해 알림을 재발송했다.

경기 중에 배포가 발생하면 팬 전체에게 중복 알림이 쏟아지는 버그가 반복됐다.

---

## 원자성이 필요한 이유

역전 감지와 경기 종료 중복 방지는 각각 다른 원자성 요건을 갖는다.

| 요건 | 역전 감지 | 경기 종료 중복 방지 |
|---|---|---|
| 이전 값 읽기 | 필요 (비교 대상) | 불필요 |
| 새 값 쓰기 | 필요 (현재 리더 갱신) | 필요 (처리 완료 마킹) |
| 원자성 보장 | read-then-write를 분리하면 race condition | write가 1회만 성공해야 함 |
| Redis 명령 | `GETSET` (= `getAndSet`) | `SETNX` (= `setIfAbsent`) |

---

## GETSET — 역전 감지

```java
// LeadChangeNotifier.java
String rawPrev = redis.opsForValue().getAndSet(leaderKey, currLeader);
redis.expire(leaderKey, Duration.ofHours(24));
String prevLeader = (rawPrev == null) ? "NONE" : rawPrev;

if (!currLeader.equals(prevLeader)) {
    // 알림 발송
}
```

`GETSET`은 이전 값을 반환하면서 새 값을 원자적으로 저장한다.
별도의 GET + SET 두 번의 왕복으로 분리하면 두 폴링 스레드가 동시에 같은 키를 읽고
둘 다 "리더 변경"으로 판단해 이중 발송할 수 있다.

**TTL 24h 근거:** KBO 경기는 하루를 넘기지 않는다.
24시간이 지나면 키가 자동 만료되어 다음 날 동일 경기 ID가 재사용되어도 충돌하지 않는다.

**rawPrev == null 처리:** 경기 시작 후 첫 번째 폴링에서는 키가 없으므로 null이 반환된다.
이를 `"NONE"`으로 치환해 첫 폴링에서는 항상 알림이 발송되지 않도록 한다.
(초기 리더 감지 시 알림이 나가는 것을 방지)

---

## SETNX — 경기 종료 중복 방지

```java
// GameProcessor.java (개념 코드)
Boolean isFirst = redis.opsForValue()
        .setIfAbsent("game:ended:" + gameId, "1", Duration.ofHours(24));

if (Boolean.TRUE.equals(isFirst)) {
    // 경기 종료 처리 (순위 보정 + 알림 발송)
}
```

`SETNX`(Set if Not eXists)는 키가 없을 때만 쓰기에 성공하고 `true`를 반환한다.
이미 키가 있으면 쓰기를 건너뛰고 `false`를 반환한다.

경기 종료 직후 여러 폴링 사이클이 동시에 `ENDED` 상태를 감지하더라도
첫 번째 스레드만 `true`를 받아 처리를 진행하고 나머지는 건너뛴다.

**TTL 24h 근거:** 역전 감지 키와 동일 이유. 경기가 끝난 다음 날 동일 `gameId`가 재사용되는 상황을 방어한다.

---

## Redis Key 전체 목록

| 키 패턴 | 명령 | TTL | 용도 |
|---|---|---|---|
| `game:leader:{gameId}` | GETSET | 24h | 역전 감지 — 이전 리더 vs 현재 리더 비교 |
| `game:ended:{gameId}` | SETNX | 24h | 경기 종료 처리 멱등성 보장 |
| `pending:signup:{token}` | SET | 15m | Lazy Registration 임시 데이터 |
| `linked:signup:{token}` | SET | 10m | Telegram 연동 완료 마커 |

**TTL 15m / 10m 근거**

- `pending:signup` 15m: 폼 제출 → Telegram 딥링크 클릭까지 사용자 예상 소요 시간.
  만료 시 토큰 자동 무효화, 별도 정리 코드 불필요.
- `linked:signup` 10m: 연동 완료 후 `/signup_success` 재방문 시 "연동됨" vs "만료됨"을 구분하기 위한 마커.
  10분이면 가입 직후 페이지 이탈 사이클을 충분히 커버한다.

---

## 인메모리 대비 Redis의 이점

| 항목 | 인메모리 (v1) | Redis (v2) |
|---|---|---|
| 서버 재시작 | 상태 소멸 → 중복 알림 | 상태 유지 → 재시작 내성 |
| 다중 인스턴스 | 인스턴스별 상태 분리 | 공유 상태 |
| TTL 기반 만료 | 수동 정리 필요 | 자동 만료 |
| 원자성 | 별도 동기화 필요 | Redis 명령 수준 원자성 |

---

## 관련 파일

| 파일 | 역할 |
|---|---|
| `game/alert/LeadChangeNotifier.java` | GETSET으로 역전 감지 |
| `game/alert/GameProcessor.java` | SETNX로 경기 종료 중복 방지 |
| `auth/TelegramLinkService.java` | pending / linked 키 관리 |
