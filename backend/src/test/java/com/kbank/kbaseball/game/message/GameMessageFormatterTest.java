package com.kbank.kbaseball.game.message;

import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.member.Member;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameMessageFormatterTest {

    private final GameMessageFormatter formatter = new GameMessageFormatter();

    @Test
    void formatFirstScore_myTeamLeading_containsCheerMessage() {
        Member lgFan = Member.builder()
                .id(1L)
                .name("Alice")
                .supportTeam(Team.LG)
                .telegramId("t1")
                .build();

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeTeamName("LG트윈스")
                .awayTeamName("롯데자이언츠")
                .homeScore(1)
                .awayScore(0)
                .inning("1회말")
                .build();

        String result = formatter.formatFirstScore(lgFan, info, "LG");

        assertThat(result).contains("선취점");
        assertThat(result).contains("먼저 득점");
        assertThat(result).doesNotContain("역전을 기원");
    }

    @Test
    void formatFirstScore_opponentLeading_containsReverseMessage() {
        Member lgFan = Member.builder()
                .id(1L)
                .name("Alice")
                .supportTeam(Team.LG)
                .telegramId("t1")
                .build();

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeTeamName("LG트윈스")
                .awayTeamName("롯데자이언츠")
                .homeScore(0)
                .awayScore(1)
                .inning("1회초")
                .build();

        String result = formatter.formatFirstScore(lgFan, info, "LT");

        assertThat(result).contains("상대팀이 선취점");
        assertThat(result).contains("역전을 기원");
        assertThat(result).doesNotContain("선취점! 🎉");
    }
}
