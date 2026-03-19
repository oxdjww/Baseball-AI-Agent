package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.KboTeamStandingDto;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StandingsAdjuster {

    /**
     * 외부 API가 아직 반영하지 못한 금일 경기 결과를 순위표에 직접 적용한다.
     * rawStandings 객체는 변경하지 않으며 새 KboStandingsResult를 반환한다.
     */
    public KboStandingsResult applyGameResult(KboStandingsResult rawStandings, RealtimeGameInfoDto info) {
        if (rawStandings == null) {
            log.warn("[StandingsAdjuster] rawStandings가 null — 보정 없이 null 반환");
            return null;
        }
        if (Boolean.TRUE.equals(info.getIsCanceled())) {
            log.info("[StandingsAdjuster] 경기 취소 — 순위 보정 스킵 (gameId={})", info.getGameId());
            return rawStandings;
        }

        String awayCode = info.getAwayTeamCode();
        String homeCode = info.getHomeTeamCode();
        List<KboTeamStandingDto> original = rawStandings.standings();

        boolean awayFound = original.stream().anyMatch(t -> awayCode.equals(t.teamCode()));
        boolean homeFound = original.stream().anyMatch(t -> homeCode.equals(t.teamCode()));
        if (!awayFound || !homeFound) {
            log.warn("[StandingsAdjuster] 팀 코드 매핑 실패 — away={} (found={}), home={} (found={}) — 원본 반환",
                    awayCode, awayFound, homeCode, homeFound);
            return rawStandings;
        }

        List<KboTeamStandingDto> adjusted = original.stream()
                .map(team -> applyResultToTeam(team, awayCode, homeCode,
                        info.getAwayScore(), info.getHomeScore()))
                .collect(Collectors.toCollection(ArrayList::new));

        List<KboTeamStandingDto> ranked = sortAndRank(adjusted);

        log.info("[StandingsAdjuster] 순위 보정 완료 — gameId={}, {}:{} vs {}:{}",
                info.getGameId(), awayCode, info.getAwayScore(), homeCode, info.getHomeScore());

        return new KboStandingsResult(rawStandings.gameType(), ranked);
    }

    private KboTeamStandingDto applyResultToTeam(
            KboTeamStandingDto team,
            String awayCode, String homeCode,
            int awayScore, int homeScore) {

        boolean isAway = awayCode.equals(team.teamCode());
        boolean isHome = homeCode.equals(team.teamCode());
        if (!isAway && !isHome) return team;

        int wins = team.wins();
        int draws = team.draws();
        int losses = team.losses();

        if (awayScore == homeScore) {
            draws += 1;
        } else {
            boolean thisTeamWon = (isAway && awayScore > homeScore) || (isHome && homeScore > awayScore);
            if (thisTeamWon) wins += 1;
            else losses += 1;
        }

        // gameBehind는 sortAndRank에서 전체 재계산하므로 0.0으로 초기화
        return new KboTeamStandingDto(team.teamCode(), team.teamName(), team.rank(), wins, draws, losses, 0.0);
    }

    /**
     * 승률 내림차순 → 승수 내림차순 정렬 후 rank와 gameBehind를 재계산한다.
     * KBO 승차 공식: GB = ((1위승 - 팀승) + (팀패 - 1위패)) / 2
     */
    private List<KboTeamStandingDto> sortAndRank(List<KboTeamStandingDto> teams) {
        teams.sort((a, b) -> {
            int cmp = Double.compare(winningPct(b), winningPct(a)); // 승률 내림차순
            if (cmp != 0) return cmp;
            return Integer.compare(b.wins(), a.wins());             // 동률 시 승수 내림차순
        });

        KboTeamStandingDto leader = teams.get(0);
        List<KboTeamStandingDto> result = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            KboTeamStandingDto t = teams.get(i);
            double gb = Math.max(0.0,
                    ((leader.wins() - t.wins()) + (t.losses() - leader.losses())) / 2.0);
            result.add(new KboTeamStandingDto(t.teamCode(), t.teamName(), i + 1, t.wins(), t.draws(), t.losses(), gb));
        }
        return result;
    }

    private static double winningPct(KboTeamStandingDto t) {
        return (double) t.wins() / Math.max(1, t.wins() + t.losses());
    }
}
