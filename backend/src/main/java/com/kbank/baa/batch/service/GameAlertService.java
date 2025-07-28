// GameAlertService.java
package com.kbank.baa.batch.service;

import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.sports.SportsApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GameAlertService {
    private final SportsApiClient apiClient;
    private final MemberRepository memberRepo;
    private final GameProcessor gameProcessor;

    public void processAlertsFor(LocalDate date) {
        var schedules = apiClient.fetchScheduledGames(date, date);
        var members = memberRepo.findByNotifyRealTimeAlertTrue();
        schedules.forEach(game -> gameProcessor.process(game, members));
    }
}