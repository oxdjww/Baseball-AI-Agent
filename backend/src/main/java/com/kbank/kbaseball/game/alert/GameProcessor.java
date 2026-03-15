package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.batch.tasklet.GameAnalysisTasklet;
import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import com.kbank.kbaseball.external.naver.NaverStandingsClient;
import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.game.message.GameMessageFormatter;
import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import com.kbank.kbaseball.template.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class GameProcessor {
    private static final String GAME_ENDED_KEY_PREFIX = "game:ended:";

    private final NaverSportsClient apiClient;
    private final NaverStandingsClient standingsClient;
    private final LeadChangeNotifier leadNotifier;
    private final StringRedisTemplate redis;
    private final TaskScheduler taskScheduler;
    private final GameAnalysisTasklet gameAnalysisTasklet;
    private final TelegramService telegramService;
    private final GameMessageFormatter gameMessageFormatter;
    private final GameEndNotificationBuilder notificationBuilder;
    private final FeatureToggleService featureToggleService;

    public void process(ScheduledGameDto schedule, List<Member> members) {
        var gameId = schedule.getGameId();
        log.info("[GameProcessor][process] processGame 시작 → gameId={}", gameId);

        // 홈/어웨이 팀 팬만 필터링
        members = members.stream()
                .filter(m ->
                        m.getSupportTeam().name().equals(schedule.getHomeTeamCode()) ||
                                m.getSupportTeam().name().equals(schedule.getAwayTeamCode())
                )
                .collect(Collectors.toList());

        RealtimeGameInfoDto info;
        try {
            info = apiClient.fetchGameInfo(gameId);
        } catch (Exception e) {
            log.error("[GameProcessor][process] 게임 정보 조회 실패 → {} : {}", gameId, e.getMessage());
            return;
        }

        if ("STARTED".equals(info.getStatusCode())) {
            // 진행 중: 역전 알림 처리
            leadNotifier.notify(schedule, members, info);
        } else {
            // 게임 진행 중 X
            log.info("[GameProcessor][process] STATUS={} [{}] {} vs {}",
                    info.getStatusCode(),
                    info.getGameId(),
                    info.getAwayTeamCode(),
                    info.getHomeTeamCode());

            if (("ENDED".equals(info.getStatusCode()) || "RESULT".equals(info.getStatusCode()))
                    && Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                            GAME_ENDED_KEY_PREFIX + info.getGameId(), "1", Duration.ofHours(24)))) {

                log.info("[GameProcessor][process] {} game ended", info.getGameId());

                String awayTeamCode = info.getAwayTeamCode();
                String homeTeamCode = info.getHomeTeamCode();
                String awayTeamName = Team.getDisplayNameByCode(awayTeamCode);
                String homeTeamName = Team.getDisplayNameByCode(homeTeamCode);

                if (info.getIsCanceled()) {
                    // 우천취소: 각 팀 팬에게 알림
                    String oppForAwayFans = gameMessageFormatter.withParticle(homeTeamName, "과", "와");
                    String oppForHomeFans = gameMessageFormatter.withParticle(awayTeamName, "과", "와");

                    telegramService.sendMessageToTeam(
                            awayTeamCode,
                            NotificationTemplate.GAME_CANCELED,
                            oppForAwayFans,
                            homeTeamName
                    );
                    telegramService.sendMessageToTeam(
                            homeTeamCode,
                            NotificationTemplate.GAME_CANCELED,
                            oppForHomeFans,
                            awayTeamName
                    );
                }

                boolean aiEnabled = featureToggleService.isEnabled(FeatureToggleService.AI_ANALYSIS);

                // 순위 조회
                KboStandingsResult standings = standingsClient.fetchStandings();

                // 다음 경기 조회 (today+1 ~ today+7)
                LocalDate today = LocalDate.now();
                List<ScheduledGameDto> upcoming = apiClient.fetchScheduledGames(
                        today.plusDays(1), today.plusDays(7));

                ScheduledGameDto nextGameForAway = upcoming.stream()
                        .filter(g -> g.getHomeTeamCode().equals(awayTeamCode)
                                || g.getAwayTeamCode().equals(awayTeamCode))
                        .findFirst()
                        .orElse(null);

                ScheduledGameDto nextGameForHome = upcoming.stream()
                        .filter(g -> g.getHomeTeamCode().equals(homeTeamCode)
                                || g.getAwayTeamCode().equals(homeTeamCode))
                        .findFirst()
                        .orElse(null);

                // 어웨이/홈 각각 메시지 빌드 & 발송
                String awayMsg = notificationBuilder.build(awayTeamCode, info, standings, nextGameForAway, aiEnabled);
                telegramService.sendMessageToTeam(awayTeamCode, NotificationTemplate.GENERIC, awayMsg);

                String homeMsg = notificationBuilder.build(homeTeamCode, info, standings, nextGameForHome, aiEnabled);
                telegramService.sendMessageToTeam(homeTeamCode, NotificationTemplate.GENERIC, homeMsg);

                // AI 분석 예약 — aiEnabled일 때만
                if (aiEnabled) {
                    LocalDateTime analysisTime = LocalDateTime.now().plusHours(1);
                    Date when = Date.from(analysisTime.atZone(ZoneId.systemDefault()).toInstant());
                    taskScheduler.schedule(
                            () -> gameAnalysisTasklet.execute(schedule, info),
                            when
                    );
                    log.info("[GameProcessor][process] → scheduled game analysis for {} at {} (1h after end)", gameId, analysisTime);
                }
            }
        }

        log.info("[GameProcessor][process] processGame 완료 → gameId={}", gameId);
    }
}
