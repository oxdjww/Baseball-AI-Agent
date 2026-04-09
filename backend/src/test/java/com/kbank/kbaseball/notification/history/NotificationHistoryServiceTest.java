package com.kbank.kbaseball.notification.history;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NotificationHistoryService DB 기반 중복 방지 로직 검증.
 * REQUIRES_NEW 트랜잭션은 테스트 트랜잭션 밖에서 커밋되므로,
 * 테스트 롤백 정책과의 충돌을 피하기 위해 @Transactional(NOT_SUPPORTED)로 비활성화.
 * 각 테스트 후 @AfterEach에서 DB를 직접 정리한다.
 */
@DataJpaTest
@Import(NotificationHistoryService.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationHistoryServiceTest {

    @Autowired
    NotificationHistoryService notificationHistoryService;

    @Autowired
    NotificationHistoryRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    private static final String GAME_ID = "20260409HTLG02026";

    @Test
    void tryMarkCancelSent_첫호출_true반환() {
        boolean result = notificationHistoryService.tryMarkCancelSent(GAME_ID);

        assertThat(result).isTrue();
    }

    @Test
    void tryMarkCancelSent_중복호출_false반환() {
        notificationHistoryService.tryMarkCancelSent(GAME_ID);

        boolean result = notificationHistoryService.tryMarkCancelSent(GAME_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isCancelAlreadySent_등록전_false반환() {
        assertThat(notificationHistoryService.isCancelAlreadySent(GAME_ID)).isFalse();
    }

    @Test
    void isCancelAlreadySent_등록후_true반환() {
        notificationHistoryService.tryMarkCancelSent(GAME_ID);

        assertThat(notificationHistoryService.isCancelAlreadySent(GAME_ID)).isTrue();
    }

    @Test
    void 서버_재시작_시나리오_DB_영속성_검증() {
        // 서버 기동 후 폴링 스케줄러가 취소 감지하여 첫 발송
        boolean firstCall = notificationHistoryService.tryMarkCancelSent(GAME_ID);
        assertThat(firstCall).isTrue();

        // 서버 재시작 이후에도 DB에 이력이 남아있으므로 중복 발송 방지
        assertThat(notificationHistoryService.isCancelAlreadySent(GAME_ID)).isTrue();
        boolean secondCall = notificationHistoryService.tryMarkCancelSent(GAME_ID);
        assertThat(secondCall).isFalse();
    }

    @Test
    void 다른_gameId는_독립적으로_처리됨() {
        String gameId1 = "GAME-001";
        String gameId2 = "GAME-002";

        boolean first = notificationHistoryService.tryMarkCancelSent(gameId1);
        boolean second = notificationHistoryService.tryMarkCancelSent(gameId2);

        assertThat(first).isTrue();
        assertThat(second).isTrue();
    }
}
