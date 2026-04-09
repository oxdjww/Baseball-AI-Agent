package com.kbank.kbaseball.notification.history;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    boolean existsByGameIdAndNotificationType(String gameId, NotificationType type);
}
