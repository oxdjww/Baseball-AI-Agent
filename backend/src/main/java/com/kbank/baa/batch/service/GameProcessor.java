package com.kbank.baa.batch.service;

import com.kbank.baa.member.Member;
import com.kbank.baa.admin.Team;
import com.kbank.baa.batch.tasklet.GameAnalysisTasklet;
import com.kbank.baa.sports.GameMessageFormatter;
import com.kbank.baa.sports.SportsApiClient;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import com.kbank.baa.sports.dto.ScheduledGameDto;
import com.kbank.baa.telegram.TelegramService;
import com.kbank.baa.telegram.template.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class GameProcessor {
    private final SportsApiClient apiClient;
    private final LeadChangeNotifier leadNotifier;
    private final Set<String> gameEndChecker = new HashSet<>();
    private final TaskScheduler taskScheduler;
    private final GameAnalysisTasklet gameAnalysisTasklet;
    private final TelegramService telegramService;
    private final GameMessageFormatter gameMessageFormatter;

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
                    && !gameEndChecker.contains(info.getGameId())) {

                log.info("[GameProcessor][process] {} game ended", info.getGameId());
                gameEndChecker.add(info.getGameId());

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
                            oppForAwayFans,  // "%s의"에 들어갈 '(상대팀명+과/와)'
                            homeTeamName     // "상대: %s"에 표시할 상대팀명
                    );
                    telegramService.sendMessageToTeam(
                            homeTeamCode,
                            NotificationTemplate.GAME_CANCELED,
                            oppForHomeFans,
                            awayTeamName
                    );
                }

                // 1시간 뒤 게임 분석 예약
                LocalDateTime analysisTime = LocalDateTime.now().plusHours(1);
                Date when = Date.from(analysisTime.atZone(ZoneId.systemDefault()).toInstant());
                taskScheduler.schedule(
                        () -> gameAnalysisTasklet.execute(schedule, info),
                        when
                );

                // 경기 종료(스코어 포함) 알림: 각 팀 팬에게 포맷만 넘김
                String oppForAwayFans = gameMessageFormatter.withParticle(homeTeamName, "과", "와");
                String oppForHomeFans = gameMessageFormatter.withParticle(awayTeamName, "과", "와");

                telegramService.sendMessageToTeam(
                        awayTeamCode,
                        NotificationTemplate.GAME_ENDED_WITH_SCORE,
                        oppForAwayFans,         // "%s의" → 상대팀명+과/와
                        awayTeamName,           // 스코어 라인 왼쪽 팀명 (원정)
                        info.getAwayScore(),    // 원정 점수
                        info.getHomeScore(),    // 홈 점수
                        homeTeamName            // 스코어 라인 오른쪽 팀명 (홈)
                );
                telegramService.sendMessageToTeam(
                        homeTeamCode,
                        NotificationTemplate.GAME_ENDED_WITH_SCORE,
                        oppForHomeFans,
                        awayTeamName,
                        info.getAwayScore(),
                        info.getHomeScore(),
                        homeTeamName
                );

                log.info("[GameProcessor][process] → scheduled game analysis for {} at {} (1h after end)", gameId, analysisTime);
            }
        }

        log.info("[GameProcessor][process] processGame 완료 → gameId={}", gameId);
    }
}
