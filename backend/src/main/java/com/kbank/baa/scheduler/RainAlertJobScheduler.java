package com.kbank.baa.scheduler;

import com.kbank.baa.batch.tasklet.RainAlertTasklet;
import com.kbank.baa.sports.ScheduledGame;
import com.kbank.baa.sports.SportsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final SportsApiClient apiClient;
    private final TaskScheduler scheduler;
    private final RainAlertTasklet rainTasklet;

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
        log.info("##### Scheduling rain alerts for date {}", date);
        List<ScheduledGame> games = apiClient.fetchScheduledGames(date, date);
        for (ScheduledGame game : games) {
            scheduleForGame(game, 3, 10);
            scheduleForGame(game, 1, 5);
        }
    }

    private void scheduleForGame(ScheduledGame game,
                                 int hoursBefore,
                                 double thresholdMm) {
        LocalDateTime alertTime = game.getGameDateTime().minusHours(hoursBefore);
        if (alertTime.isAfter(LocalDateTime.now())) {
            Date when = Date.from(alertTime.atZone(ZoneId.systemDefault()).toInstant());
            scheduler.schedule(
                    () -> rainTasklet.executeForGame(game, hoursBefore, thresholdMm),
                    when
            );
            log.info("##### → scheduled alert for game {} at {} ({}h before)",
                    game.getGameId(), alertTime, hoursBefore);
        } else {
            log.debug("##### → skipping past alert time {} for game {}", alertTime, game.getGameId());
        }
    }
}
