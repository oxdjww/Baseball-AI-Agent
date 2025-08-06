package com.kbank.baa.batch.service;

import com.kbank.baa.admin.Member;
import com.kbank.baa.batch.tasklet.GameAnalysisTasklet;
import com.kbank.baa.sports.RealtimeGameInfo;
import com.kbank.baa.sports.ScheduledGame;
import com.kbank.baa.sports.SportsApiClient;
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
    private final GameStatusLogger statusLogger;
    private final LeadChangeNotifier leadNotifier;
    private final Set<String> gameEndChecker = new HashSet<>();
    private final TaskScheduler taskScheduler;
    private final GameAnalysisTasklet gameAnalysisTasklet;

    public void process(ScheduledGame schedule, List<Member> members) {
        var gameId = schedule.getGameId();
        log.info("########## processGame 시작 → gameId={} ##########", gameId);

        // 🔹 여기서 필터링: 홈/어웨이 팀을 응원하는 멤버만 members에 재할당
        members = members.stream()
                .filter(m ->
                        m.getSupportTeam().name().equals(schedule.getHomeTeamCode()) ||
                                m.getSupportTeam().name().equals(schedule.getAwayTeamCode())
                )
                .collect(Collectors.toList());

        RealtimeGameInfo info;
        try {
            info = apiClient.fetchGameInfo(gameId);
        } catch (Exception e) {
            log.error("########## 게임 정보 조회 실패 → {} : {} ##########", gameId, e.getMessage());
            return;
        }

        if (!"STARTED".equals(info.getStatusCode())) {
            statusLogger.log(schedule, info);
            if ("ENDED".equals(info.getStatusCode()) && !gameEndChecker.contains(info.getGameId())) {
                gameEndChecker.add(info.getGameId());
                LocalDateTime analysisTime = LocalDateTime.now().plusHours(1);
                Date when = Date.from(analysisTime.atZone(ZoneId.systemDefault()).toInstant());

                taskScheduler.schedule(
                        () -> gameAnalysisTasklet.execute(schedule, info),
                        when
                );

                log.info("##### → scheduled game analysis for {} at {} (1h after end)",
                        gameId, analysisTime);
            }
        } else {
            leadNotifier.notify(schedule, members, info);
        }

        log.info("########## processGame 완료 → gameId={} ##########", gameId);
    }
}
