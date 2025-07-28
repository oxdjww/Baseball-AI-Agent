// SupportUtils.java
package com.kbank.baa.batch.service;

import com.kbank.baa.admin.Member;
import com.kbank.baa.sports.RealtimeGameInfo;
import com.kbank.baa.sports.ScheduledGame;

public final class SupportUtils {
    private SupportUtils() {
    }

    public static boolean isSupporting(Member m, ScheduledGame s) {
        var t = m.getSupportTeam().name();
        return t.equals(s.getHomeTeamCode()) || t.equals(s.getAwayTeamCode());
    }

    public static String calculateLeader(RealtimeGameInfo info) {
        var away = info.getAwayScore();
        var home = info.getHomeScore();
        if (away > home) return info.getAwayTeamCode();
        if (home > away) return info.getHomeTeamCode();
        return "NONE";
    }
}
