package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.KboTeamStandingDto;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GameEndNotificationBuilderTest {

    private GameEndNotificationBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new GameEndNotificationBuilder();
    }

    private RealtimeGameInfoDto gameInfo(String awayCode, String homeCode, int awayScore, int homeScore) {
        return RealtimeGameInfoDto.builder()
                .gameId("testGame")
                .awayTeamCode(awayCode)
                .homeTeamCode(homeCode)
                .awayTeamName("away")
                .homeTeamName("home")
                .awayScore(awayScore)
                .homeScore(homeScore)
                .stadium("잠실")
                .statusCode("ENDED")
                .isCanceled(false)
                .build();
    }

    private KboTeamStandingDto standing(String teamCode, String teamName, int rank, int wins, int draws, int losses, double gb) {
        return new KboTeamStandingDto(teamCode, teamName, rank, wins, draws, losses, gb);
    }

    private KboStandingsResult regularStandings(List<KboTeamStandingDto> standings) {
        return new KboStandingsResult("REGULAR_SEASON", standings);
    }

    @Test
    void build_rank1_noAboveTeam() {
        List<KboTeamStandingDto> standings = List.of(
                standing("LG", "LG", 1, 10, 0, 5, 0.0),
                standing("SS", "삼성", 2, 9, 0, 6, 0.5)
        );
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 5, 3);

        String result = builder.build("LG", info, regularStandings(standings), null, false);

        assertThat(result).contains("1위 LG");
        assertThat(result).doesNotContain("↑");
        assertThat(result).contains("↓");
        assertThat(result).contains("삼성");
    }

    @Test
    void build_rank10_noBelowTeam() {
        List<KboTeamStandingDto> standings = List.of(
                standing("LG", "LG", 9, 5, 0, 10, 4.5),
                standing("HH", "한화", 10, 4, 0, 11, 5.0)
        );
        RealtimeGameInfoDto info = gameInfo("HH", "LG", 1, 3);

        String result = builder.build("HH", info, regularStandings(standings), null, false);

        assertThat(result).contains("10위 한화");
        assertThat(result).contains("↑");
        assertThat(result).doesNotContain("↓");
    }

    @Test
    void build_tiedRank_indexBased() {
        List<KboTeamStandingDto> standings = List.of(
                standing("SS", "삼성", 2, 9, 0, 6, 0.5),
                standing("LG", "LG", 2, 9, 0, 6, 0.5),
                standing("KT", "KT", 2, 9, 0, 6, 0.5)
        );
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 2, 4);

        String result = builder.build("LG", info, regularStandings(standings), null, false);

        // LG is at index 1, so above is 삼성 (index 0), below is KT (index 2)
        assertThat(result).contains("삼성").contains("↑");
        assertThat(result).contains("KT").contains("↓");
    }

    @Test
    void build_gameBehindZero_showsTied() {
        List<KboTeamStandingDto> standings = List.of(
                standing("SS", "삼성", 1, 10, 0, 5, 0.0),
                standing("LG", "LG", 2, 10, 0, 5, 0.0)
        );
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 3, 5);

        String result = builder.build("LG", info, regularStandings(standings), null, false);

        assertThat(result).contains("동률");
    }

    @Test
    void build_gameBehindNonZero_showsG() {
        List<KboTeamStandingDto> standings = List.of(
                standing("SS", "삼성", 1, 10, 0, 5, 0.0),
                standing("LG", "LG", 2, 8, 0, 7, 1.5)
        );
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 3, 5);

        String result = builder.build("LG", info, regularStandings(standings), null, false);

        assertThat(result).contains("1.5G");
    }

    @Test
    void build_noNextGame_omitsPreviewLine() {
        List<KboTeamStandingDto> standings = List.of(
                standing("LG", "LG", 1, 10, 0, 5, 0.0)
        );
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 5, 3);

        String result = builder.build("LG", info, regularStandings(standings), null, false);

        assertThat(result).doesNotContain("다음 경기 예고");
    }

    @Test
    void build_aiEnabled_includesAiMention() {
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 5, 3);

        String result = builder.build("LG", info, null, null, true);

        assertThat(result).contains("AI 게임 분석 레포트");
    }

    @Test
    void build_aiDisabled_excludesAiMention() {
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 5, 3);

        String result = builder.build("LG", info, null, null, false);

        assertThat(result).doesNotContain("AI 게임 분석 레포트");
    }

    @Test
    void build_preseason_showsPreseasonLabel() {
        KboStandingsResult preseasonStandings = new KboStandingsResult(
                "PRESEASON",
                List.of(standing("LG", "LG", 1, 3, 0, 1, 0.0))
        );
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 5, 3);

        String result = builder.build("LG", info, preseasonStandings, null, false);

        assertThat(result).contains("시범경기 순위");
        assertThat(result).doesNotContain("정규시즌 순위");
    }

    @Test
    void build_withNextGame_includesPreviewLine() {
        RealtimeGameInfoDto info = gameInfo("LG", "SS", 5, 3);
        ScheduledGameDto nextGame = ScheduledGameDto.builder()
                .gameId("nextGame")
                .homeTeamCode("LG")
                .awayTeamCode("KT")
                .homeTeamName("LG")
                .awayTeamName("KT")
                .stadium("잠실")
                .gameDateTime(LocalDateTime.of(2026, 3, 20, 18, 30))
                .build();

        String result = builder.build("LG", info, null, nextGame, false);

        assertThat(result).contains("다음 경기 예고");
        assertThat(result).contains("KT");
        assertThat(result).contains("잠실");
    }
}
