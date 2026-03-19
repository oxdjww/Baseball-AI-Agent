package com.kbank.kbaseball.test;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import com.kbank.kbaseball.external.naver.NaverStandingsClient;
import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.game.alert.GameEndNotificationBuilder;
import com.kbank.kbaseball.game.alert.StandingsAdjuster;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameEndNotificationTestController {

    private final NaverSportsClient sportsApiClient;
    private final NaverStandingsClient standingsClient;
    private final StandingsAdjuster standingsAdjuster;
    private final GameEndNotificationBuilder notificationBuilder;
    private final TelegramService telegramService;
    private final FeatureToggleService featureToggleService;

    @Value("${telegram.admin-id}")
    private String adminId;

    /**
     * 예시 호출:
     * GET /test/game-end-notification?gameId=20260315LGSS02026&teamCode=LG
     */
    @GetMapping("/test/game-end-notification")
    public String testGameEndNotification(
            @RequestParam String gameId,
            @RequestParam String teamCode,
            @RequestParam(required = false) Boolean aiEnabled) {

        LocalDate gameDate = LocalDate.parse(
                gameId.substring(0, 8),
                DateTimeFormatter.BASIC_ISO_DATE
        );

        ScheduledGameDto schedule = sportsApiClient
                .fetchScheduledGames(gameDate, gameDate)
                .stream()
                .filter(g -> g.getGameId().equals(gameId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("찾을 수 없는 gameId: " + gameId));

        RealtimeGameInfoDto info = sportsApiClient.fetchGameInfo(gameId);
        KboStandingsResult standings = standingsAdjuster.applyGameResult(
                standingsClient.fetchStandings(), info);

        LocalDate today = LocalDate.now();
        List<ScheduledGameDto> upcoming = sportsApiClient.fetchScheduledGames(
                today.plusDays(1), today.plusDays(7));
        ScheduledGameDto nextGame = upcoming.stream()
                .filter(g -> g.getHomeTeamCode().equals(teamCode) || g.getAwayTeamCode().equals(teamCode))
                .findFirst()
                .orElse(null);

        boolean effectiveAiEnabled = (aiEnabled != null)
                ? aiEnabled
                : featureToggleService.isEnabled(FeatureToggleService.AI_ANALYSIS);
        String message = notificationBuilder.build(teamCode, info, standings, nextGame, effectiveAiEnabled);

        telegramService.sendPlainMessage(adminId, message);
        return message;
    }
}
