package com.kbank.baa.batch.service;

import com.kbank.baa.admin.Member;
import com.kbank.baa.sports.RealtimeGameInfo;
import com.kbank.baa.sports.ScheduledGame;
import com.kbank.baa.sports.SportsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class GameProcessor {
    private final SportsApiClient apiClient;
    private final GameStatusLogger statusLogger;
    private final PeriodicUpdateNotifier periodicNotifier;
    private final LeadChangeNotifier leadNotifier;

    public void process(ScheduledGame schedule, List<Member> members) {
        var gameId = schedule.getGameId();
        log.info("########## processGame 시작 → gameId={} ##########", gameId);
        RealtimeGameInfo info;
        try {
            info = apiClient.fetchGameInfo(gameId);
        } catch (Exception e) {
            log.error("########## 게임 정보 조회 실패 → {} : {} ##########", gameId, e.getMessage());
            return;
        }

        if (!"STARTED".equals(info.getStatusCode())) {
            statusLogger.log(schedule, info);
        } else {
//            periodicNotifier.notify(schedule, members, info);
            leadNotifier.notify(schedule, members, info);
        }

        log.info("########## processGame 완료 → gameId={} ##########", gameId);
    }
}