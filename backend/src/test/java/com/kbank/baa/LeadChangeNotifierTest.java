package com.kbank.baa;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.Team;
import com.kbank.baa.batch.service.LeadChangeNotifier;
import com.kbank.baa.sports.GameMessageFormatter;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import com.kbank.baa.sports.dto.ScheduledGameDto;
import com.kbank.baa.telegram.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeadChangeNotifierTest {

    @Mock
    GameMessageFormatter formatter;
    @Mock
    TelegramService telegram;
    @InjectMocks
    LeadChangeNotifier notifier;

    ScheduledGameDto schedule;
    Member homeFan, awayFan;

    @BeforeEach
    void setUp() {
        // 1) 테스트용 ScheduledGame 세팅
        schedule = ScheduledGameDto.builder()
                .gameId("GAME123")
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .build();

        // 2) 홈팀 팬과 어웨이팀 팬 두 명 준비
        homeFan = Member.builder()
                .id(1L)
                .name("Alice")
                .supportTeam(Team.LG)
                .telegramId("t1")
                .build();

        awayFan = Member.builder()
                .id(2L)
                .name("Bob")
                .supportTeam(Team.LT)
                .telegramId("t2")
                .build();

        // 3) 메시지 포맷터가 호출될 때, 멤버별로 구분된 메시지를 리턴하도록 스텁
        when(formatter.formatLeadChange(eq(homeFan), any(), anyString(), anyString()))
                .thenReturn("[홈팀 역전] Alice 메시지");
        when(formatter.formatLeadChange(eq(awayFan), any(), anyString(), anyString()))
                .thenReturn("[어웨이팀 역전] Bob 메시지");
    }

    @Test
    void notify_whenLeaderChanges_sendsToAllSupporters() {
        // — “역전” 상태로 간주하기 위해 homeScore > awayScore 로 리더 결정
        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .statusCode("STARTED")
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(3)
                .awayScore(2)
                .build();

        // 첫 호출: leaderMap 에 아무 값도 없으니, prev="NONE" → curr="LG" 로 변경 감지
        notifier.notify(schedule, List.of(homeFan, awayFan), info);

        // 검증: 두 멤버 모두에게 sendMessage가 호출되어야 한다
        verify(telegram, times(1))
                .sendMessage("t1", "Alice", "[홈팀 역전] Alice 메시지");
        verify(telegram, times(1))
                .sendMessage("t2", "Bob", "[어웨이팀 역전] Bob 메시지");
    }
}
