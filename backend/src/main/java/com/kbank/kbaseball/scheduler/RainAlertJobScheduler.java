package com.kbank.kbaseball.scheduler;

import com.kbank.kbaseball.batch.tasklet.RainAlertTasklet;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RainAlertJobScheduler {

    private final NaverSportsClient apiClient;
    private final TaskScheduler scheduler;
    private final RainAlertTasklet rainTasklet;
    private final TelegramService telegramService;

    @Value("${telegram.admin-id}")
    private String telegramAdminId;

    /**
     * 앱 시작 직후 오늘 일정 스케줄링
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        scheduleAlertsFor(LocalDate.now());
    }

    /**
     * 매일 자정에 다시 스케줄 갱신
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void onMidnight() {
        scheduleAlertsFor(LocalDate.now());
    }

    private void scheduleAlertsFor(LocalDate date) {
        log.info("[RainAlertJobScheduler][scheduleAlertsFor] Scheduling rain alerts for date {}", date);
        try {
            List<ScheduledGameDto> games = apiClient.fetchScheduledGames(date, date);
            for (ScheduledGameDto game : games) {
                scheduleForGame(game, 3, 10);
                scheduleForGame(game, 1, 5);
            }
        } catch (Exception e) {
            log.error("[RainAlertJobScheduler] 스케줄링 실패: {}", e.getMessage(), e);
            telegramService.sendPlainMessage(telegramAdminId,
                    "<b>[배치오류] 우천알림 스케줄링 실패</b>\n사유: " + e.getMessage());
        }
    }

    private void scheduleForGame(ScheduledGameDto game,
                                 int hoursBefore,
                                 double thresholdMm) {
        LocalDateTime alertTime = game.getGameDateTime().minusHours(hoursBefore);
        if (alertTime.isAfter(LocalDateTime.now())) {
            Date when = Date.from(alertTime.atZone(ZoneId.systemDefault()).toInstant());
            scheduler.schedule(
                    () -> rainTasklet.executeForGame(game, alertTime, hoursBefore, thresholdMm),
                    when
            );
            log.info("[RainAlertJobScheduler][scheduleForGame] → scheduled alert for game {} at {} ({}h before)",
                    game.getGameId(), alertTime, hoursBefore);
        } else {
            log.info("[RainAlertJobScheduler][scheduleForGame] → skipping past alert time {} for game {}", alertTime, game.getGameId());
        }
    }
}
