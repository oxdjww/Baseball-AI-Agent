// GameAlertService.java
package com.kbank.baa.game.alert;

import com.kbank.baa.member.MemberRepository;
import com.kbank.baa.external.naver.NaverSportsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GameAlertService {
    private final NaverSportsClient apiClient;
    private final MemberRepository memberRepo;
    private final GameProcessor gameProcessor;

    public void processAlertsFor(LocalDate date) {
        var schedules = apiClient.fetchScheduledGames(date, date);
        var members = memberRepo.findByNotifyRealTimeAlertTrue();
        schedules.forEach(game -> gameProcessor.process(game, members));
    }
}
