package com.kbank.kbaseball.monitoring;

import com.kbank.kbaseball.notification.telegram.TelegramService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonitoringEventListenerTest {

    @Mock TelegramService telegramService;
    @InjectMocks MonitoringEventListener listener;

    private static final String ADMIN_ID = "000000000";

    @Test
    void 이벤트_수신시_telegramService_sendPlainMessage_호출() {
        ReflectionTestUtils.setField(listener, "telegramAdminId", ADMIN_ID);
        MonitoringErrorEvent event = new MonitoringErrorEvent(
                this,
                "FeatureToggleService.toggle",
                "Unknown feature key: FAKE",
                "at com.kbank.kbaseball...",
                Map.of("arg0", "FAKE"),
                null
        );

        listener.handleMonitoringError(event);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramService).sendPlainMessage(eq(ADMIN_ID), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("[🚨 SYSTEM CRITICAL]");
        assertThat(msgCaptor.getValue()).contains("FeatureToggleService.toggle");
        assertThat(msgCaptor.getValue()).contains("Unknown feature key: FAKE");
        assertThat(msgCaptor.getValue()).contains("📋 인자:");
        assertThat(msgCaptor.getValue()).contains("FAKE");
    }

    @Test
    void userId_null이면_유저정보_미포함() {
        ReflectionTestUtils.setField(listener, "telegramAdminId", ADMIN_ID);
        MonitoringErrorEvent event = new MonitoringErrorEvent(
                this, "SomeService.method", "err", "stack", Map.of(), null);

        listener.handleMonitoringError(event);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService).sendPlainMessage(eq(ADMIN_ID), captor.capture());
        assertThat(captor.getValue()).doesNotContain("👤 유저:");
    }

    @Test
    void contextData_비어있으면_인자정보_미포함() {
        ReflectionTestUtils.setField(listener, "telegramAdminId", ADMIN_ID);
        MonitoringErrorEvent event = new MonitoringErrorEvent(
                this, "SomeService.method", "err", "stack", Map.of(), null);

        listener.handleMonitoringError(event);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService).sendPlainMessage(eq(ADMIN_ID), captor.capture());
        assertThat(captor.getValue()).doesNotContain("📋 인자:");
    }

    @Test
    void HTML_특수문자_이스케이프() {
        ReflectionTestUtils.setField(listener, "telegramAdminId", ADMIN_ID);
        MonitoringErrorEvent event = new MonitoringErrorEvent(
                this,
                "SomeService.method",
                "Error: value <null> is not allowed",
                "at com.example.Foo.bar<T>(Foo.java:10)",
                Map.of(),
                null
        );

        listener.handleMonitoringError(event);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService).sendPlainMessage(eq(ADMIN_ID), captor.capture());
        assertThat(captor.getValue()).contains("&lt;null&gt;");
        assertThat(captor.getValue()).contains("&lt;T&gt;");
        assertThat(captor.getValue()).doesNotContain("<null>");
    }
}
