package com.kbank.baa.sports;

import com.kbank.baa.admin.Member;
import org.springframework.stereotype.Component;

@Component
public class GameMessageFormatter {

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

    public String formatLeadChange(Member m,
                                   RealtimeGameInfo info,
                                   String prevLeader,
                                   String currLeader) {
        int away = info.getAwayScore();
        int home = info.getHomeScore();

        if ("NONE".equals(currLeader)) {
            return String.format("[%s] 경기가 %d:%d 동점이 되었습니다!",
                    info.getGameId(), away, home);
        } else {
            return String.format("[%s] %s팀이 %d:%d로 리드합니다 (이전 %s). 응원하세요! 🚀",
                    info.getGameId(),
                    currLeader, away, home,
                    "NONE".equals(prevLeader) ? "초기" : prevLeader);
        }
    }
}
