package com.kbank.baa.batch.service;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.Team;
import com.kbank.baa.batch.tasklet.GameAnalysisTasklet;
import com.kbank.baa.sports.SportsApiClient;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import com.kbank.baa.sports.dto.ScheduledGameDto;
import com.kbank.baa.telegram.TelegramService;
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

    public void process(ScheduledGameDto schedule, List<Member> members) {
        var gameId = schedule.getGameId();
        log.info("########## processGame 시작 → gameId={} ##########", gameId);

        // 게임에 해당되는 (홈/어웨이) 팀을 응원하는 멤버만 재초기화
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
            log.error("########## 게임 정보 조회 실패 → {} : {} ##########", gameId, e.getMessage());
            return;
        }

        if (!"STARTED".equals(info.getStatusCode())) {
            log.info("########## STATUS={} [{}] {} vs {} ##########",
                    info.getStatusCode(),
                    info.getGameId(),
                    info.getAwayTeamCode(),
                    info.getHomeTeamCode());
            if (("ENDED".equals(info.getStatusCode()) || "RESULT".equals(info.getStatusCode())) && !gameEndChecker.contains(info.getGameId())) {
                gameEndChecker.add(info.getGameId());
                LocalDateTime analysisTime = LocalDateTime.now().plusHours(1);
                Date when = Date.from(analysisTime.atZone(ZoneId.systemDefault()).toInstant());

                taskScheduler.schedule(
                        () -> gameAnalysisTasklet.execute(schedule, info),
                        when
                );

                // 경기 종료 알림
                String awayTeamCode = info.getAwayTeamCode();
                String homeTeamCode = info.getHomeTeamCode();
                String awayTeamName = Team.getDisplayNameByCode(awayTeamCode);
                String homeTeamName = Team.getDisplayNameByCode(homeTeamCode);
                String gameEndMessageAway = String.format("금일 %s와의 경기가 종료되었습니다.\n\n한 시간 뒤, Ai 게임 분석 레포트가 전송됩니다!\n\n감사합니다.", homeTeamName);
                String gameEndMessageHome = String.format("금일 %s와의 경기가 종료되었습니다.\n\n한 시간 뒤, Ai 게임 분석 레포트가 전송됩니다!\n\n감사합니다.", awayTeamName);
                telegramService.sendMessageToTeam(awayTeamCode, gameEndMessageAway);
                telegramService.sendMessageToTeam(homeTeamCode, gameEndMessageHome);

                log.info("##### → scheduled game analysis for {} at {} (1h after end)",
                        gameId, analysisTime);
            }
        } else {
            leadNotifier.notify(schedule, members, info);
        }

        log.info("########## processGame 완료 → gameId={} ##########", gameId);
    }
}
