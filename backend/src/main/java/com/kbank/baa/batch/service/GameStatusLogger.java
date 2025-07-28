// GameStatusLogger.java
package com.kbank.baa.batch.service;

import com.kbank.baa.sports.RealtimeGameInfo;
import com.kbank.baa.sports.ScheduledGame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameStatusLogger {
    public void log(ScheduledGame s, RealtimeGameInfo info) {
        log.info("########## STATUS={} [{}] {} vs {} ##########",
                info.getStatusCode(),
                info.getGameId(),
                info.getAwayTeamCode(),
                info.getHomeTeamCode());
    }
}