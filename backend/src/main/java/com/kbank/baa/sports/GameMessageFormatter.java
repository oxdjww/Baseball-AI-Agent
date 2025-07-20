package com.kbank.baa.sports;

import com.kbank.baa.admin.Member;
import org.springframework.stereotype.Component;

@Component
public class GameMessageFormatter {

    /**
     * RealtimeGameInfo → "[LG vs 롯데] 잠실 18:00 경기전\n9회말에 0:1" 형태의 메시지 생성
     */
    public String format(RealtimeGameInfo info) {
        return String.format(
                "%s 님, [%s vs %s] %s %s 경기 %s\n%s에 %d:%d",
                info.getMemberName(),
                info.getHomeTeamName(),
                info.getAwayTeamName(),
                info.getStadium(),
                info.getGameDateTime().toLocalTime(),
                info.getStatusCode(),
                info.getInning(),
                info.getHomeScore(),
                info.getAwayScore()
        );
    }

    public String format(Member member, RealtimeGameInfo g) {
        return String.format(
                "%s님의 응원팀 %s의 경기 현황을 알려드려요.%n" +
                        "[%s vs %s] %s %s 경기 %n" +
                        "%s | %d:%d",
                member.getName(),
                member.getSupportTeam().name(),
                g.getHomeTeamName(),
                g.getAwayTeamName(),
                g.getStadium(),
                g.getGameDateTime().toLocalTime(),
                g.getInning(),
                g.getHomeScore(),
                g.getAwayScore()
        );
    }
}
