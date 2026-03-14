package com.kbank.kbaseball.monitoring;

import com.kbank.kbaseball.notification.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

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

        // 스택 트레이스, 오류 메시지에 <, > 등 HTML 특수문자가 포함될 수 있어 이스케이프 필수
        // (Telegram HTML 모드에서 파싱 오류 방지)
        String escapedError = HtmlUtils.htmlEscape(event.getErrorMessage());
        String escapedStack = HtmlUtils.htmlEscape(event.getStackTrace());

        String contextInfo = (event.getContextData() != null && !event.getContextData().isEmpty())
                ? "📋 인자: <code>" + HtmlUtils.htmlEscape(event.getContextData().toString()) + "</code>\n"
                : "";

        String message = String.format(
            "<b>[🚨 SYSTEM CRITICAL]</b>\n" +
            "📛 기능: <code>%s</code>\n" +
            "💬 오류: %s\n" +
            "%s" +
            "%s" +
            "🔍 스택:\n<pre>%s</pre>\n" +
            "⏰ %s",
            event.getFeatureName(),
            escapedError,
            event.getUserId() != null ? "👤 유저: " + event.getUserId() + "\n" : "",
            contextInfo,
            escapedStack,
            timestamp
        );

        telegramService.sendPlainMessage(telegramAdminId, message);
    }
}
