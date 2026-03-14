// SupportUtils.java
package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;

public final class SupportUtils {
    private SupportUtils() {
    }

    public static String calculateLeader(RealtimeGameInfoDto info) {
        var away = info.getAwayScore();
        var home = info.getHomeScore();
        if (away > home) return info.getAwayTeamCode();
        if (home > away) return info.getHomeTeamCode();
        return "NONE";
    }
}
