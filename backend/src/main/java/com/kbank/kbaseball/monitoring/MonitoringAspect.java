package com.kbank.kbaseball.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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

    // *Service 클래스 전체, notification 패키지 제외 (TelegramService 무한루프 방지)
    @Pointcut("execution(public * com.kbank.kbaseball..*Service.*(..)) " +
              "&& !within(com.kbank.kbaseball.notification..*)")
    public void serviceMethod() {}

    @AfterThrowing(pointcut = "serviceMethod()", throwing = "ex")
    public void handleException(JoinPoint joinPoint, Exception ex) {
        String featureName = joinPoint.getTarget().getClass().getSimpleName()
                + "." + joinPoint.getSignature().getName();

        String stackTrace = Arrays.stream(ex.getStackTrace())
                .limit(5)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));

        Map<String, String> contextData = buildContextData(joinPoint.getArgs());

        log.warn("[MonitoringAspect] 서비스 예외 감지: feature={}, error={}", featureName, ex.getMessage());

        eventPublisher.publishEvent(new MonitoringErrorEvent(
                this,
                featureName,
                ex.getMessage(),
                stackTrace,
                contextData,
                null
        ));
    }

    private Map<String, String> buildContextData(Object[] args) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            map.put("arg" + i, args[i] != null ? args[i].toString() : "null");
        }
        return map;
    }
}
