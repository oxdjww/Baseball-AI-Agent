# AOP 관제 시스템 — 설계 결정사항

## 설계 목표

서비스 계층에서 발생하는 모든 예외를 **코드 변경 없이** 관리자 Telegram으로 실시간 전달한다.
각 서비스 메서드에 try-catch를 추가하지 않고, AOP로 전체 계층을 일괄 감시한다.

---

## Pointcut 설계

```java
// MonitoringAspect.java
@Pointcut("execution(public * com.kbank.kbaseball..*Service.*(..)) " +
          "&& !within(com.kbank.kbaseball.notification..*)")
public void serviceMethod() {}
```

**`notification` 패키지 제외 이유**

`TelegramService`가 예외를 던졌을 때 MonitoringAspect가 이를 가로채
다시 `TelegramService`를 호출해 관제 메시지를 보내려 하면 무한 재귀가 발생한다.

```
TelegramService 예외
  → MonitoringAspect 감지
  → TelegramService.sendPlainMessage() 호출 (관제 메시지 발송)
  → TelegramService 또 예외 → MonitoringAspect 감지 → ...
```

`!within(com.kbank.kbaseball.notification..*)`으로 TelegramService를 포함한
notification 패키지 전체를 Pointcut에서 제외해 이 루프를 차단한다.

---

## CGLIB 이중 발화 방지

### 문제

`@EnableRetry`와 `@Transactional`이 함께 적용된 서비스 메서드에서
Spring이 CGLIB 프록시를 두 겹으로 감싼다.

```
외부 CGLIB 프록시(@Retryable)
  → 내부 CGLIB 프록시(@Transactional)
    → 실제 서비스 메서드
```

`@Around`가 각 프록시 호출마다 실행되므로, 메서드 하나가 예외를 던지면
MonitoringAspect의 catch 블록이 두 번 실행된다 → 관제 메시지 이중 발송.

### 해결 — ThreadLocal callDepth

```java
private static final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

@Around("serviceMethod()")
public Object aroundService(ProceedingJoinPoint pjp) throws Throwable {
    callDepth.set(callDepth.get() + 1);
    try {
        return pjp.proceed();
    } catch (Exception ex) {
        if (callDepth.get() == 1) {
            // 최외곽 프록시(depth==1)에서만 이벤트 발행
            eventPublisher.publishEvent(new MonitoringErrorEvent(...));
        }
        throw ex;
    } finally {
        int d = callDepth.get() - 1;
        callDepth.set(d);
        if (d == 0) callDepth.remove(); // ThreadLocal 메모리 누수 방지
    }
}
```

`callDepth`는 현재 스레드의 AOP 중첩 깊이를 추적한다.
외부 프록시 진입 시 depth=1, 내부 프록시 진입 시 depth=2가 된다.
예외 발생 시 depth==1인 최외곽에서만 이벤트를 발행하므로 이중 발송이 없다.

`finally`에서 depth를 반드시 복원하고, 0이 되면 `remove()`로 ThreadLocal을 정리한다.
스레드 풀 환경에서 ThreadLocal을 정리하지 않으면 이전 요청의 depth 값이 다음 요청에 남아
잘못된 분기가 발생할 수 있다.

---

## GlobalExceptionHandler 보완

AOP는 `@Service` 계층만 감시하므로 `@RestController`에서 직접 던진 예외는 포착하지 못한다.

```java
// GlobalExceptionHandler.java
@ExceptionHandler(Exception.class)
public ResponseEntity<...> handleServerError(Exception ex, HttpServletRequest request) {
    if (isAlreadyHandledByMonitoringAspect(ex)) {
        // 이미 AOP가 처리한 예외 → 재발행 안 함
        return buildErrorResponse(...);
    }
    eventPublisher.publishEvent(new MonitoringErrorEvent(...));
    return buildErrorResponse(...);
}

boolean isAlreadyHandledByMonitoringAspect(Throwable ex) {
    return Arrays.stream(ex.getStackTrace())
            .anyMatch(frame ->
                    MonitoringAspect.class.getName().equals(frame.getClassName()));
}
```

**이중 발화 방지 방식**

서비스 메서드 예외는 AOP가 먼저 잡아 이벤트를 발행하고 예외를 다시 던진다.
이 예외는 컨트롤러까지 전파되어 GlobalExceptionHandler에도 도달한다.
스택트레이스에 `MonitoringAspect`가 있으면 AOP가 이미 처리했다는 신호이므로
GlobalExceptionHandler는 이벤트를 재발행하지 않는다.

---

## 이벤트 파이프라인

```
서비스 예외 발생
  → MonitoringAspect (callDepth==1 확인)
  → ApplicationEventPublisher.publishEvent(MonitoringErrorEvent)
  → MonitoringEventListener (@EventListener)
  → HtmlUtils.htmlEscape (Telegram HTML 이스케이프)
  → TelegramService.sendPlainMessage(adminId, html)
  → Telegram Bot API → 관리자 DM
```

Spring Event를 통해 AOP와 Telegram 발송을 디커플링했다.
AOP가 Telegram 서비스를 직접 호출하면 notification 패키지를 Pointcut에서 제외하는 것만으로는
순환 의존 리스크를 완전히 차단하기 어렵다.
이벤트 발행으로 분리함으로써 관제 채널을 Telegram에서 다른 수단(이메일, Slack 등)으로
교체하더라도 MonitoringAspect를 수정할 필요가 없다.

---

## 관제 메시지 포맷

```
[🚨 SYSTEM CRITICAL]
📛 기능: GameAlertService.processAlertsFor
💬 오류: Read timed out executing GET https://...
📋 인자: {arg0=2026-05-10}
🔍 스택:
  NaverSportsClient.fetchScheduledGames(NaverSportsClient.java:58)
  GameAlertService.processAlertsFor(GameAlertService.java:42)
  ...
⏰ 2026-05-10 18:03:21 KST
```

스택 5프레임만 포함해 Telegram 메시지 길이 제한(4,096자)을 지킨다.
메서드 인자(`contextData`)를 포함하면 발생 시점의 입력값을 알 수 있어 재현에 도움이 된다.

---

## 관련 파일

| 파일 | 역할 |
|---|---|
| `monitoring/MonitoringAspect.java` | Pointcut + callDepth 이중 발화 방지 |
| `monitoring/GlobalExceptionHandler.java` | 컨트롤러 계층 보완 감시 |
| `monitoring/MonitoringErrorEvent.java` | 이벤트 DTO |
| `monitoring/MonitoringEventListener.java` | 이벤트 수신 → Telegram 발송 |
