package com.kbank.baa.sports;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.Team;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import org.springframework.stereotype.Component;

@Component
public class GameMessageFormatter {

    public String format(Member member, RealtimeGameInfoDto g) {
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
                                   RealtimeGameInfoDto info,
                                   String prevLeader,
                                   String currLeader) {
        int awayTeamScore = info.getAwayScore();
        int homeTeamScore = info.getHomeScore();

        String awayTeamName = info.getAwayTeamName();
        String homeTeamName = info.getHomeTeamName();

        if ("NONE".equals(currLeader)) {
            return String.format(
                    "[<b>%s</b> VS <b>%s</b>] 경기 상황에 변동이 있어요!\n" +
                            "경기가 <b>%d : %d</b> 동점이 되었습니다! 🔥",
                    awayTeamName,
                    homeTeamName,
                    awayTeamScore,
                    homeTeamScore
            );
        } else {
            return String.format(
                    "[<b>%s</b> VS <b>%s</b>] 경기 상황에 변동이 있어요!\n" +
                            "이제 <b>%s팀</b>이 <b>%d:%d</b>로 리드합니다! 🚀",
                    awayTeamName,
                    homeTeamName,
                    Team.getDisplayNameByCode(currLeader),
                    awayTeamScore,
                    homeTeamScore
            );
        }
    }
}
