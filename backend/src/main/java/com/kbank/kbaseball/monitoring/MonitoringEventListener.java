package com.kbank.kbaseball.monitoring;

import com.kbank.kbaseball.notification.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringEventListener {

    private final TelegramService telegramService;

    @Value("${telegram.admin-id}")
    private String telegramAdminId;

    @EventListener
    public void handleMonitoringError(MonitoringErrorEvent event) {
        log.warn("[MonitoringEventListener] 관제 이벤트 수신: feature={}", event.getFeatureName());

        String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String message = String.format(
            "<b>[🚨 SYSTEM CRITICAL]</b>\n" +
            "📛 기능: <code>%s</code>\n" +
            "💬 오류: %s\n" +
            "%s" +
            "🔍 스택:\n<pre>%s</pre>\n" +
            "⏰ %s",
            event.getFeatureName(),
            event.getErrorMessage(),
            event.getUserId() != null ? "👤 유저: " + event.getUserId() + "\n" : "",
            event.getStackTrace(),
            timestamp
        );

        telegramService.sendPlainMessage(telegramAdminId, message);
    }
}
