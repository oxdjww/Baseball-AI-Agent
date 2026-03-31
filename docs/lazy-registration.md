# Lazy Registration & 봇 공격 3층 방어

## 왜 만들었나

v1 운영 중 가입 폼 제출 시 DB에 즉시 INSERT하는 구조를 사용했다.
봇이 폼을 자동으로 반복 제출하면서 가짜 회원 1,000건 이상이 DB에 삽입되는 오염이 발생했다.

단순 레이트 리밋만으로는 정교한 봇을 막기 어렵고,
핵심 문제는 "Telegram과 연동되지 않은 회원"이 DB에 남는 구조 자체였다.

---

## 3층 방어 구조

### Layer 1 — Nginx 레이트 리밋

```nginx
limit_req_zone $binary_remote_addr zone=signup:10m rate=5r/min;
```

동일 IP에서 분당 5건을 초과하면 요청을 차단한다.
봇의 대량 요청을 가장 앞 단에서 제거한다.

---

### Layer 2 — Honeypot 필드

```java
// HomeController.java:83
@PostMapping("/signup")
public String signup(@ModelAttribute SignupForm form,
                     @RequestParam(name = "website", required = false, defaultValue = "") String honeypot,
                     HttpSession session) {
    if (!honeypot.isEmpty()) {
        return "redirect:/home"; // 봇 감지 → 조용히 리다이렉트
    }
    ...
}
```

HTML 폼에 `name="website"` 숨김 필드를 추가한다.
사람은 보이지 않으므로 이 필드를 채우지 않는다.
봇은 모든 `<input>` 필드를 자동으로 채우므로 `website`에 값이 들어온다.

---

### Layer 3 — Lazy Registration (핵심)

DB INSERT를 폼 제출 시점이 아니라 **Telegram 연동이 완료된 시점**으로 늦춘다.

```
폼 제출
  → Redis에만 저장 (TTL 15분)
  → 사용자가 딥링크 클릭 → Telegram /start {token} 수신
  → 이 시점에 DB INSERT (telegramId 포함)
```

**왜 Telegram 연동을 두 번째 factor로 선택했는가**

Telegram Bot API는 실제 Telegram 계정을 소유한 사람만 `/start` 명령을 보낼 수 있다.
봇이 폼을 통과하더라도 유효한 Telegram 계정 없이는 DB에 도달하지 못한다.
별도 CAPTCHA나 이메일 인증 없이 Telegram 자체가 신원 확인 역할을 한다.

**TTL 15분 근거**

가입 폼 제출 → 딥링크 클릭까지 사용자가 예상하는 최대 대기 시간을 15분으로 잡았다.
이 시간이 지나면 Redis 키가 만료되어 토큰이 자동 무효화된다.
별도 만료 처리 코드가 필요 없다.

---

## 흐름

```
[폼 제출] POST /home/signup
    ↓
HomeController → UUID 토큰 생성
    ↓
Redis SET pending:signup:{token} → PendingMemberData(JSON), TTL 15m
    ↓
302 redirect /signup_success (세션에 signupToken 저장)
    ↓
[사용자] Telegram 딥링크 클릭 (t.me/bot?start={token})
    ↓
TelegramWebhookService → /start {token} 수신
    ↓
TelegramLinkService.linkAccount(token, telegramUserId)
    ↓
Redis GET pending:signup:{token} → PendingMemberData 역직렬화
    ↓
Member 저장 (@Transactional)
    ↓
afterCommit() → Redis DEL pending:signup:{token}
             → Redis SET linked:signup:{token} "1", TTL 10m
```

`linked:signup:{token}` 키는 가입 완료 후 사용자가 `/signup_success` 페이지를
새로고침할 때 "이미 연동된 토큰"을 구분하기 위한 마커다.
TTL이 만료된 `pending` 키와 이미 연동된 토큰을 혼동해 "만료됨" 안내를 잘못 표시하는 문제를 막는다.

---

## 중복 telegramId 처리

```java
// TelegramLinkService.java:74
if (memberRepository.existsByTelegramId(String.valueOf(telegramUserId))) {
    return new LinkResult.AlreadyLinked();
}
```

동일 Telegram 계정으로 중복 가입을 시도하면 DB INSERT 없이 차단한다.
V5 마이그레이션에서 `partial unique index (WHERE deleted=false)`가 적용되어
탈퇴 후 재가입은 허용하면서 활성 계정 중복은 DB 레벨에서도 방어한다.

---

## 관련 파일

| 파일 | 역할 |
|---|---|
| `web/HomeController.java` | Honeypot 검사, Redis 저장 |
| `auth/TelegramLinkService.java` | DB INSERT, afterCommit Redis 정리 |
| `notification/telegram/TelegramWebhookService.java` | `/start` 명령 파싱 → linkAccount 호출 |
| `db/init/V5__partial_unique_telegram_id.sql` | 탈퇴 회원 제외 telegram_id UNIQUE 인덱스 |
