package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.KboTeamStandingDto;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Component
public class GameEndNotificationBuilder {

    public String build(
            String teamCode,
            RealtimeGameInfoDto info,
            KboStandingsResult standingsResult,
            ScheduledGameDto nextGame,
            boolean aiEnabled
    ) {
        boolean isAway = teamCode.equals(info.getAwayTeamCode());
        String myTeamName = Team.getDisplayNameByCode(teamCode);
        String oppTeamName = Team.getDisplayNameByCode(
                isAway ? info.getHomeTeamCode() : info.getAwayTeamCode());
        int myScore = isAway ? info.getAwayScore() : info.getHomeScore();
        int oppScore = isAway ? info.getHomeScore() : info.getAwayScore();

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("<b>[경기 종료] %s %d:%d %s</b>",
                myTeamName, myScore, oppScore, oppTeamName));
        sb.append("\n\n오늘 경기도 수고하셨습니다! ⚾️");

        if (standingsResult == null || standingsResult.standings().isEmpty()) {
            sb.append("\n\n📊 [순위 정보 조회 실패]");
        } else {
            String rankingTypeLabel = "PRESEASON".equals(standingsResult.gameType())
                    ? "시범경기 순위" : "정규시즌 순위";
            sb.append(String.format("\n\n📊 <b>실시간 %s</b>\n", rankingTypeLabel));
            sb.append("<i>※ 금일 경기 결과를 즉시 반영한 순위로, 네이버 등 공식 집계와 일시적으로 다를 수 있습니다.</i>\n");

            List<KboTeamStandingDto> standings = standingsResult.standings();
            int myIndex = -1;
            for (int i = 0; i < standings.size(); i++) {
                if (teamCode.equals(standings.get(i).teamCode())) {
                    myIndex = i;
                    break;
                }
            }

            if (myIndex < 0) {
                sb.append(String.format("[순위 정보 조회 실패 — teamCode '%s' 매핑 없음]\n", teamCode));
            } else {
                KboTeamStandingDto myTeam = standings.get(myIndex);

                if (myIndex > 0) {
                    KboTeamStandingDto above = standings.get(myIndex - 1);
                    String gap = formatGap(Math.abs(myTeam.gameBehind() - above.gameBehind()));
                    sb.append(String.format("• %d위 %s (%s ↑)\n", above.rank(), Team.getDisplayNameByCode(above.teamCode()), gap));
                }

                sb.append(String.format("• <b>%d위 %s (%d승 %d무 %d패)</b>\n",
                        myTeam.rank(), myTeamName,
                        myTeam.wins(), myTeam.draws(), myTeam.losses()));

                if (myIndex < standings.size() - 1) {
                    KboTeamStandingDto below = standings.get(myIndex + 1);
                    String gap = formatGap(Math.abs(myTeam.gameBehind() - below.gameBehind()));
                    sb.append(String.format("• %d위 %s (%s ↓)\n", below.rank(), Team.getDisplayNameByCode(below.teamCode()), gap));
                }
            }
        }

        if (nextGame != null) {
            String oppCode = teamCode.equals(nextGame.getHomeTeamCode())
                    ? nextGame.getAwayTeamCode()
                    : nextGame.getHomeTeamCode();
            String nextOppName = Team.getDisplayNameByCode(oppCode);
            String dateStr = nextGame.getGameDateTime().toLocalDate()
                    .format(DateTimeFormatter.ofPattern("M월 d일"));
            String dayOfWeek = nextGame.getGameDateTime().getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.KOREAN);
            String timeStr = nextGame.getGameDateTime().toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            sb.append(String.format("\n🗓️ <b>다음 경기 예고</b> %s(%s) %s vs %s (%s)\n",
                    dateStr, dayOfWeek, timeStr, nextOppName, nextGame.getStadium()));
        }

        if (aiEnabled) {
            sb.append("\n⚾️ 1시간 뒤, AI 게임 분석 레포트가 전송됩니다!");
        }

        return sb.toString();
    }

    private String formatGap(double gap) {
        if (gap == 0.0) return "동률";
        if (gap % 1.0 == 0.0) return String.valueOf((int) gap) + "G";
        return String.valueOf(gap) + "G";
    }
}
