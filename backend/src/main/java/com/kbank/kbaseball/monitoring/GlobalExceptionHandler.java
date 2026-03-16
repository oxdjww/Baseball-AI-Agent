package com.kbank.kbaseball.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST 컨트롤러에서 발생한 미처리 예외를 관제 이벤트로 연결한다.
 *
 * <p>MonitoringAspect(AOP)가 *Service 클래스만 감시하기 때문에,
 * 컨트롤러 레이어에서 직접 던져진 예외(Service를 거치지 않는 경우)는
 * 여기서 보완 감시한다.
 *
 * <p>이중 발화 방지: 예외의 스택트레이스에 MonitoringAspect가 포함되어 있으면
 * AOP가 이미 처리한 것이므로 이벤트를 재발행하지 않는다.
 */
@RestControllerAdvice(annotations = RestController.class)
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleServerError(
            Exception ex, HttpServletRequest request) {

        if (isAlreadyHandledByMonitoringAspect(ex)) {
            log.debug("[GlobalExceptionHandler] AOP가 이미 처리한 예외 — 재발행 생략: {}",
                    ex.getClass().getSimpleName());
        } else {
            String featureName = "Controller:" + request.getMethod()
                    + " " + request.getRequestURI();
            String errorMessage = ex.getMessage() != null
                    ? ex.getMessage()
                    : ex.getClass().getSimpleName();
            String stackTrace = Arrays.stream(ex.getStackTrace())
                    .limit(5)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));

            log.warn("[GlobalExceptionHandler] 컨트롤러 예외 감지: uri={}, error={}",
                    request.getRequestURI(), errorMessage);

            eventPublisher.publishEvent(new MonitoringErrorEvent(
                    this, featureName, errorMessage, stackTrace, Map.of(), null));
        }

        return buildErrorResponse(request.getRequestURI());
    }

    /**
     * 예외의 스택트레이스에 MonitoringAspect 클래스가 포함되어 있으면
     * 이미 AOP 감시가 처리한 서비스 레이어 예외임을 의미한다.
     */
    boolean isAlreadyHandledByMonitoringAspect(Throwable ex) {
        return Arrays.stream(ex.getStackTrace())
                .anyMatch(frame ->
                        MonitoringAspect.class.getName().equals(frame.getClassName()));
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String path) {
        String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", timestamp);
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("path", path);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
