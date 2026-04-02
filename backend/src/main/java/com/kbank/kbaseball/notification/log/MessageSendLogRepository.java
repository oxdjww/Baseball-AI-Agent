package com.kbank.kbaseball.notification.log;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageSendLogRepository extends JpaRepository<MessageSendLog, Long> {
}
