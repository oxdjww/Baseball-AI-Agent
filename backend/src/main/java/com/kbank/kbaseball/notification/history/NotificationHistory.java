package com.kbank.kbaseball.notification.history;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "notification_history",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_notification_history_game_type",
        columnNames = {"game_id", "notification_type"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false, length = 64)
    private String gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private NotificationType notificationType;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    public static NotificationHistory of(String gameId, NotificationType type) {
        NotificationHistory h = new NotificationHistory();
        h.gameId = gameId;
        h.notificationType = type;
        h.sentAt = Instant.now();
        return h;
    }
}
