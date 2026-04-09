package com.kbank.kbaseball.notification.history;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHistoryService {

    private final NotificationHistoryRepository repository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 취소 알림을 아직 보내지 않은 경우 DB에 원자적으로 기록하고 true 반환.
     * 이미 기록되어 있으면 false 반환 (중복 발송 방지).
     * JdbcTemplate 직접 INSERT: JPA EntityManager를 우회하여 constraint violation 시에도
     * 트랜잭션이 rollback-only로 마킹되지 않아 REQUIRES_NEW 트랜잭션이 정상 커밋됨.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkCancelSent(String gameId) {
        try {
            jdbcTemplate.update(
                "INSERT INTO notification_history (game_id, notification_type, sent_at) VALUES (?, ?, NOW())",
                gameId, NotificationType.CANCEL_ALERT.name()
            );
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("[NotificationHistoryService] 이미 등록된 취소 알림 gameId={}", gameId);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isCancelAlreadySent(String gameId) {
        return repository.existsByGameIdAndNotificationType(gameId, NotificationType.CANCEL_ALERT);
    }
}
