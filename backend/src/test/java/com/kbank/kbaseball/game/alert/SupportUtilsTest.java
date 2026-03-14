package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportUtilsTest {

    private static RealtimeGameInfoDto gameInfo(int awayScore, int homeScore) {
        return RealtimeGameInfoDto.builder()
                .awayTeamCode("LT")
                .homeTeamCode("LG")
                .awayScore(awayScore)
                .homeScore(homeScore)
                .build();
    }

    @Test
    void awayLeads_returnsAwayTeamCode() {
        assertThat(SupportUtils.calculateLeader(gameInfo(3, 2))).isEqualTo("LT");
    }

    @Test
    void homeLeads_returnsHomeTeamCode() {
        assertThat(SupportUtils.calculateLeader(gameInfo(2, 3))).isEqualTo("LG");
    }

    @Test
    void tiedScore_returnsNone() {
        assertThat(SupportUtils.calculateLeader(gameInfo(2, 2))).isEqualTo("NONE");
    }

    @Test
    void zeroZero_returnsNone() {
        assertThat(SupportUtils.calculateLeader(gameInfo(0, 0))).isEqualTo("NONE");
    }

    @Test
    void largeScores_awayLeads() {
        assertThat(SupportUtils.calculateLeader(gameInfo(15, 14))).isEqualTo("LT");
    }
}
