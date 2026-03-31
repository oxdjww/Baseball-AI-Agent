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

    // --- 실제 teamId 기반 코드 검증 (2026-03-31 버그 재현) ---

    /**
     * LG(away) vs KIA(home, teamId="HT") — 오늘 실제 버그 시나리오.
     * NaverStandingsClient가 teamId를 사용하면 standings에 "HT"가 저장되고,
     * 게임 API도 homeTeamCode="HT"를 반환하므로 매칭이 성공해야 한다.
     * 이전 bugfix(52e7a07)는 teamName 기반으로 바꿔 "HT" 매칭 실패 → LG 패배 미반영.
     */
    @Test
    void legacyTeamId_htCode_lgLossApplied() {
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("HT", "KIA", 1, 2, 0, 0, 0.0),
                standing("LG", "LG",  2, 0, 0, 2, 1.5)
        ));
        // LG(away) 2:7 KIA(home) — LG 패배
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("LG", "HT", 2, 7));

        KboTeamStandingDto lg = findByCode(result, "LG");
        KboTeamStandingDto ht = findByCode(result, "HT");

        assertThat(lg.losses()).isEqualTo(3);   // 0→3패 (오늘 패배 반영)
        assertThat(lg.wins()).isEqualTo(0);
        assertThat(ht.wins()).isEqualTo(3);     // 2→3승
        assertThat(ht.losses()).isEqualTo(0);
    }

    @Test
    void legacyTeamId_obCode_obWinApplied() {
        // 두산(teamId="OB") vs 삼성(teamId="SS") — 레거시 코드 전 팀 커버리지
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("SS", "삼성", 1, 2, 0, 0, 0.0),
                standing("OB", "두산", 2, 1, 0, 1, 0.5)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("OB", "SS", 5, 3));

        assertThat(findByCode(result, "OB").wins()).isEqualTo(2);
        assertThat(findByCode(result, "SS").losses()).isEqualTo(1);
    }

    @Test
    void legacyTeamId_skCode_skLossApplied() {
        // SSG(teamId="SK") — SK 코드 정상 매칭
        KboStandingsResult raw = new KboStandingsResult("REGULAR_SEASON", List.of(
                standing("LG", "LG",  1, 3, 0, 0, 0.0),
                standing("SK", "SSG", 2, 2, 0, 1, 0.5)
        ));
        KboStandingsResult result = adjuster.applyGameResult(raw, endedGame("SK", "LG", 1, 5));

        assertThat(findByCode(result, "SK").losses()).isEqualTo(2);
        assertThat(findByCode(result, "LG").wins()).isEqualTo(4);
    }
}
