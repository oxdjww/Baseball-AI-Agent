# StandingsAdjuster — 경기 종료 순위 즉시 보정 로직

## 왜 만들었나

경기가 끝난 직후 Naver Sports API는 순위표를 즉시 업데이트하지 않는다.
팬에게 경기 종료 알림을 보낼 때 `fetchStandings()`를 호출해도 방금 끝난 경기가 반영되지 않은 **이전 순위**가 돌아온다.

```
실제 상황 (LG 12:7 SSG 승리 직후):

Naver API 반환값:  LG 3승 1무 3패  ← stale
팬이 받아야 할 값: LG 4승 1무 3패  ← 정확
```

추가 API 호출이나 재시도 없이, **이미 알고 있는 경기 결과를 순위표에 직접 적용**해 해결한다.

---

## 보정 흐름

```
[Naver fetchStandings() → stale KboStandingsResult]
              ↓
  StandingsAdjuster.applyGameResult(rawStandings, info)
              ↓
   Step 1. 오늘 경기 참여 2팀의 승/패/무 수치 갱신
              ↓
   Step 2. 전체 10팀 승률 기준 재정렬
              ↓
   Step 3. 1위 기준 각 팀 승차(GB) 재계산
              ↓
  [보정된 KboStandingsResult → GameEndNotificationBuilder]
```

나머지 8팀(오늘 경기 미참여)은 수치를 건드리지 않는다.

---

## Step 1 — 승/패/무 반영

`RealtimeGameInfoDto`에는 방금 끝난 경기의 최종 스코어가 담겨있다.

```java
if (awayScore == homeScore) {
    // 무승부: 양 팀 모두 draws + 1
    draws += 1;
} else {
    boolean thisTeamWon = (isAway && awayScore > homeScore)
                       || (isHome && homeScore > awayScore);
    if (thisTeamWon) wins   += 1;
    else             losses += 1;
}
```

| 상황 | 처리 |
|---|---|
| 원정팀 승리 | 원정 wins+1, 홈 losses+1 |
| 홈팀 승리 | 홈 wins+1, 원정 losses+1 |
| 무승부 | 양 팀 draws+1 |
| 취소 (`isCanceled=true`) | 수치 변경 없음, 원본 반환 |

---

## Step 2 — 승률 기준 재정렬

KBO 공식 순위 기준은 **승률**이다. 무승부는 분모에서 제외한다.

```
승률 = wins / (wins + losses)
```

동률일 경우 승수가 더 많은 팀이 상위:

```java
teams.sort((a, b) -> {
    int cmp = Double.compare(winningPct(b), winningPct(a)); // 승률 내림차순
    if (cmp != 0) return cmp;
    return Integer.compare(b.wins(), a.wins());             // 동률 시 승수 내림차순
});
```

> 시즌 초 0경기 팀은 `Math.max(1, wins + losses)` 로 0 나누기를 방지한다.

---

## Step 3 — 승차(GB) 재계산

KBO 표준 승차 공식:

```
GB = ((1위 승수 - 팀 승수) + (팀 패수 - 1위 패수)) / 2
```

1위 팀은 항상 GB = 0.

예시 (LG가 오늘 승리한 직후):

```
1위 삼성: 10승 5패
2위 LG:   4승  3패  (오늘 win 반영)
3위 KT:   8승  8패

LG GB = ((10 - 4) + (3 - 5)) / 2 = (6 - 2) / 2 = 2.0
KT GB = ((10 - 8) + (8 - 5)) / 2 = (2 + 3) / 2 = 2.5
```

---

## 엣지 케이스

| 상황 | 처리 |
|---|---|
| `rawStandings == null` | null 반환 → 기존 "순위 조회 실패" 처리 흐름 유지 |
| 취소 경기 | 원본 반환 (스탯 변경 없음) |
| 팀 코드 미매핑 | 원본 반환 + WARN 로그 (half-apply 방지) |
| 1위 팀 GB 부동소수점 오차 | `Math.max(0.0, gb)` 로 `-0.0` 방지 |

---

## 관련 파일

| 파일 | 역할 |
|---|---|
| `game/alert/StandingsAdjuster.java` | 보정 로직 본체 |
| `game/alert/GameProcessor.java` | `fetchStandings()` 직후 `applyGameResult()` 호출 |
| `game/alert/GameEndNotificationBuilder.java` | 보정된 순위표를 받아 메시지 포맷팅 |
| `test/.../StandingsAdjusterTest.java` | 11개 단위 케이스 |
| `test/.../VerificationTest.java` | Spring 컨텍스트 E2E 검증 |
