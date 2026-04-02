package com.kbank.kbaseball.notification.log;

import com.kbank.kbaseball.notification.telegram.dto.TelegramSendResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "telegram_send_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MessageSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tsl_seq")
    @SequenceGenerator(name = "tsl_seq", sequenceName = "telegram_send_log_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "telegram_id", nullable = false, length = 64)
    private String telegramId;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "bot_blocked", nullable = false)
    private boolean botBlocked;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    public static MessageSendLog of(String telegramId, String messageText, TelegramSendResult result) {
        return MessageSendLog.builder()
                .telegramId(telegramId)
                .messageText(messageText)
                .httpStatus(result.statusCode())
                .errorMessage(truncate(result.errorMessage(), 512))
                .botBlocked(result.botBlocked())
                .sentAt(Instant.now())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
