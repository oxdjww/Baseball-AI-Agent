// GameAlertService.java
package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.member.MemberRepository;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameAlertService {
    private final NaverSportsClient apiClient;
    private final MemberRepository memberRepo;
    private final GameProcessor gameProcessor;
    private final FeatureToggleService featureToggleService;

    public void processAlertsFor(LocalDate date) {
        if (!featureToggleService.isEnabled(FeatureToggleService.REVERSAL_DETECTION)) {
            log.info("[GameAlertService][processAlertsFor] REVERSAL_DETECTION feature is disabled. Skipping.");
            return;
        }
        var schedules = apiClient.fetchScheduledGames(date, date);
        var members = memberRepo.findByNotifyRealTimeAlertTrue();
        schedules.forEach(game -> gameProcessor.process(game, members));
    }
}
