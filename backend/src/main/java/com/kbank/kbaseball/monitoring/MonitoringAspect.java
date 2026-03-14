package com.kbank.kbaseball.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringAspect {

    private final ApplicationEventPublisher eventPublisher;

    // @EnableRetry + @Transactional 조합 시 CGLIB 프록시가 2중 래핑되어
    // 동일 서비스 메서드에 aroundService가 중첩 호출될 수 있음.
    // 깊이가 1인 최외곽 호출에서만 이벤트를 발행해 이중 발화를 방지한다.
    private static final ThreadLocal<Integer> callDepth = ThreadLocal.withInitial(() -> 0);

    // *Service 클래스 전체, notification 패키지 제외 (TelegramService 무한루프 방지)
    @Pointcut("execution(public * com.kbank.kbaseball..*Service.*(..)) " +
              "&& !within(com.kbank.kbaseball.notification..*)")
    public void serviceMethod() {}

    @Around("serviceMethod()")
    public Object aroundService(ProceedingJoinPoint pjp) throws Throwable {
        callDepth.set(callDepth.get() + 1);
        try {
            return pjp.proceed();
        } catch (Exception ex) {
            if (callDepth.get() == 1) {
                // CGLIB 이중 프록시가 있어도 최외곽(depth==1)에서만 발행
                String featureName = AopUtils.getTargetClass(pjp.getTarget()).getSimpleName()
                        + "." + pjp.getSignature().getName();

                String stackTrace = Arrays.stream(ex.getStackTrace())
                        .limit(5)
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n"));

                Map<String, String> contextData = buildContextData(pjp.getArgs());
                String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

                log.warn("[MonitoringAspect] 서비스 예외 감지: feature={}, error={}", featureName, errorMessage);

                eventPublisher.publishEvent(new MonitoringErrorEvent(
                        this, featureName, errorMessage, stackTrace, contextData, null));
            }
            throw ex;
        } finally {
            int d = callDepth.get() - 1;
            callDepth.set(d);
            if (d == 0) callDepth.remove();
        }
    }

    private Map<String, String> buildContextData(Object[] args) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            map.put("arg" + i, args[i] != null ? args[i].toString() : "null");
        }
        return map;
    }
}
