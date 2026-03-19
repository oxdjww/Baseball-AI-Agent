package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.KboTeamStandingDto;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandingsAdjusterTest {

    private StandingsAdjuster adjuster;

    @BeforeEach
    void setUp() {
        adjuster = new StandingsAdjuster();
    }

    // --- helpers ---

    private KboTeamStandingDto standing(String code, String name, int rank, int w, int d, int l, double gb) {
        return new KboTeamStandingDto(code, name, rank, w, d, l, gb);
    }

    private RealtimeGameInfoDto endedGame(String away, String home, int awayScore, int homeScore) {
        return RealtimeGameInfoDto.builder()
                .gameId("G001")
                .awayTeamCode(away)
                .homeTeamCode(home)
                .awayScore(awayScore)
                .homeScore(homeScore)
                .isCanceled(false)
                .build();
    }

    private RealtimeGameInfoDto canceledGame(String away, String home) {
        return RealtimeGameInfoDto.builder()
                .gameId("G002")
                .awayTeamCode(away)
                .homeTeamCode(home)
                .awayScore(0)
                .homeScore(0)
                .isCanceled(true)
                .build();
    }

    private KboTeamStandingDto findByCode(KboStandingsResult result, String code) {
        return result.standings().stream()
                .filter(t -> code.equals(t.teamCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("팀 코드 없음: " + code));
    }

    // --- null guard ---

    @Test
    void nullStandings_returnsNull() {
        assertThat(adjuster.applyGameResult(null, endedGame("LG", "SS", 5, 3))).isNull();
    }

    // --- canceled game guard ---

    @Test
    void canceledGame_returnsOriginalUnchanged() {
        KboStandingsResult original = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("LG", "LG", 1, 10, 0, 5, 0.0)
        ));
        assertThat(adjuster.applyGameResult(original, canceledGame("LG", "SS"))).isSameAs(original);
    }

    // --- away wins ---

    @Test
    void awayWins_awayGetsOneMoreWin() {
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("LG", "LG", 1, 10, 0, 5, 0.0),
                standing("SS", "삼성", 2, 9, 0, 6, 0.5)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "SS", 5, 3));

        assertThat(findByCode(result, "LG").wins()).isEqualTo(11);
        assertThat(findByCode(result, "LG").losses()).isEqualTo(5);
        assertThat(findByCode(result, "SS").wins()).isEqualTo(9);
        assertThat(findByCode(result, "SS").losses()).isEqualTo(7);
    }

    // --- home wins ---

    @Test
    void homeWins_homeGetsOneMoreWin() {
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("LG", "LG", 1, 10, 0, 5, 0.0),
                standing("SS", "삼성", 2, 9, 0, 6, 0.5)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "SS", 2, 5));

        assertThat(findByCode(result, "SS").wins()).isEqualTo(10);
        assertThat(findByCode(result, "SS").losses()).isEqualTo(6);
        assertThat(findByCode(result, "LG").wins()).isEqualTo(10);
        assertThat(findByCode(result, "LG").losses()).isEqualTo(6);
    }

    // --- draw ---

    @Test
    void draw_bothTeamsGetOneDraw() {
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("LG", "LG", 1, 10, 0, 5, 0.0),
                standing("SS", "삼성", 2, 9, 0, 6, 0.5)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "SS", 3, 3));

        assertThat(findByCode(result, "LG").draws()).isEqualTo(1);
        assertThat(findByCode(result, "SS").draws()).isEqualTo(1);
        assertThat(findByCode(result, "LG").wins()).isEqualTo(10);
        assertThat(findByCode(result, "SS").wins()).isEqualTo(9);
    }

    // --- unknown team code guard ---

    @Test
    void unknownTeamCode_returnsOriginalUnchanged() {
        KboStandingsResult original = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("LG", "LG", 1, 10, 0, 5, 0.0)
        ));
        assertThat(adjuster.applyGameResult(original, endedGame("LG", "XX", 5, 3))).isSameAs(original);
    }

    // --- re-ranking ---

    @Test
    void winnerOvertakesLeader_rankUpdated() {
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("SS", "삼성", 1, 10, 0, 5, 0.0),
                standing("LG", "LG",  2, 10, 0, 6, 0.5)
        ));
        // LG(away) 승리 → LG: 11W 6L (pct=0.647), SS: 10W 6L (pct=0.625) → LG 1위
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "SS", 5, 3));

        assertThat(findByCode(result, "LG").rank()).isEqualTo(1);
        assertThat(findByCode(result, "SS").rank()).isEqualTo(2);
    }

    // --- gameBehind ---

    @Test
    void leaderGameBehindIsZero() {
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("SS", "삼성", 1, 10, 0, 5, 0.0),
                standing("LG", "LG",  2, 8,  0, 7, 2.0)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "SS", 5, 3));

        assertThat(result.standings().get(0).gameBehind()).isEqualTo(0.0);
    }

    @Test
    void gameBehindRecalculatedCorrectly() {
        // LG beats KT → LG: 9W 7L, KT: 7W 9L, SS: 10W 5L (unchanged)
        // SS rank1 GB=0, LG: ((10-9)+(7-5))/2=1.5, KT: ((10-7)+(9-5))/2=3.5
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("SS", "삼성", 1, 10, 0, 5, 0.0),
                standing("LG", "LG",  2, 8,  0, 7, 2.0),
                standing("KT", "KT",  3, 7,  0, 8, 2.5)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "KT", 5, 3));

        assertThat(findByCode(result, "SS").rank()).isEqualTo(1);
        assertThat(findByCode(result, "LG").rank()).isEqualTo(2);
        assertThat(findByCode(result, "KT").rank()).isEqualTo(3);
        assertThat(findByCode(result, "LG").gameBehind()).isEqualTo(1.5);
        assertThat(findByCode(result, "KT").gameBehind()).isEqualTo(3.5);
    }

    // --- gameType preserved ---

    @Test
    void preservesGameType() {
        KboStandingsResult raw = new KboStandingsResult("PRESEASON", List.of(
                standing("LG", "LG", 1, 5, 0, 2, 0.0),
                standing("SS", "삼성", 2, 4, 0, 3, 0.5)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "SS", 3, 1));

        assertThat(result.gameType()).isEqualTo("PRESEASON");
    }

    // --- unrelated teams unchanged ---

    @Test
    void unrelatedTeamsUnchanged() {
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("SS", "삼성", 1, 10, 0, 5, 0.0),
                standing("LG", "LG",  2, 9,  0, 6, 0.5),
                standing("KT", "KT",  3, 8,  0, 7, 1.0)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "SS", 5, 3));

        KboTeamStandingDto kt = findByCode(result, "KT");
        assertThat(kt.wins()).isEqualTo(8);
        assertThat(kt.losses()).isEqualTo(7);
        assertThat(kt.draws()).isEqualTo(0);
    }
}
