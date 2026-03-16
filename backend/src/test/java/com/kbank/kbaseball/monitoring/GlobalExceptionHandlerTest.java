package com.kbank.kbaseball.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    ApplicationEventPublisher eventPublisher;

    GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(eventPublisher);
    }

    @Test
    void 컨트롤러_예외는_MonitoringErrorEvent_발행() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/game-analysis");
        RuntimeException ex = new RuntimeException("찾을 수 없는 gameId");

        handler.handleServerError(ex, request);

        ArgumentCaptor<MonitoringErrorEvent> captor =
                ArgumentCaptor.forClass(MonitoringErrorEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        MonitoringErrorEvent event = captor.getValue();
        assertThat(event.getFeatureName()).isEqualTo("Controller:GET /test/game-analysis");
        assertThat(event.getErrorMessage()).isEqualTo("찾을 수 없는 gameId");
    }

    @Test
    void 응답은_500_JSON_반환() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test/some-api");
        RuntimeException ex = new RuntimeException("error");

        var response = handler.handleServerError(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("path", "/test/some-api");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void AOP가_처리한_예외는_이벤트_재발행_안함() {
        // MonitoringAspect가 스택트레이스에 포함된 예외 생성
        Exception ex = buildExceptionWithMonitoringAspectInStackTrace();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/something");

        handler.handleServerError(ex, request);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void isAlreadyHandledByMonitoringAspect_스택트레이스에_없으면_false() {
        RuntimeException ex = new RuntimeException("plain error");

        assertThat(handler.isAlreadyHandledByMonitoringAspect(ex)).isFalse();
    }

    @Test
    void isAlreadyHandledByMonitoringAspect_스택트레이스에_있으면_true() {
        Exception ex = buildExceptionWithMonitoringAspectInStackTrace();

        assertThat(handler.isAlreadyHandledByMonitoringAspect(ex)).isTrue();
    }

    @Test
    void 예외_메시지_null이면_클래스명_사용() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/null-msg");
        RuntimeException ex = new RuntimeException((String) null);

        handler.handleServerError(ex, request);

        ArgumentCaptor<MonitoringErrorEvent> captor =
                ArgumentCaptor.forClass(MonitoringErrorEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("RuntimeException");
    }

    /**
     * MonitoringAspect 클래스명이 스택트레이스에 포함된 예외를 직접 조작해 생성한다.
     */
    private Exception buildExceptionWithMonitoringAspectInStackTrace() {
        RuntimeException ex = new RuntimeException("service error");
        StackTraceElement[] original = ex.getStackTrace();
        StackTraceElement[] modified = new StackTraceElement[original.length + 1];
        modified[0] = new StackTraceElement(
                MonitoringAspect.class.getName(), "aroundService", "MonitoringAspect.java", 40);
        System.arraycopy(original, 0, modified, 1, original.length);
        ex.setStackTrace(modified);
        return ex;
    }
}
