package com.kbank.kbaseball;

import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.game.alert.LeadChangeNotifier;
import com.kbank.kbaseball.game.message.GameMessageFormatter;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LeadChangeNotifierTest {

    @Mock
    GameMessageFormatter formatter;
    @Mock
    TelegramService telegram;
    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOps;
    @InjectMocks
    LeadChangeNotifier notifier;

    ScheduledGameDto schedule;
    Member homeFan, awayFan;

    @BeforeEach
    void setUp() {
        schedule = ScheduledGameDto.builder()
                .gameId("GAME123")
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .build();

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

        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.getAndSet(anyString(), anyString())).thenReturn(null);

        when(formatter.formatLeadChange(eq(homeFan), any(), anyString(), anyString()))
                .thenReturn("[홈팀 역전] Alice 메시지");
        when(formatter.formatLeadChange(eq(awayFan), any(), anyString(), anyString()))
                .thenReturn("[어웨이팀 역전] Bob 메시지");
    }

    @Test
    void notify_whenLeaderChanges_sendsToAllSupporters() {
        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .statusCode("STARTED")
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(3)
                .awayScore(2)
                .build();

        notifier.notify(schedule, List.of(homeFan, awayFan), info);

        verify(telegram, times(1))
                .sendPersonalMessage("t1", "Alice", "[홈팀 역전] Alice 메시지");
        verify(telegram, times(1))
                .sendPersonalMessage("t2", "Bob", "[어웨이팀 역전] Bob 메시지");
    }

    @Test
    void notify_whenNoLeaderChange_sendsNoMessage() {
        // Redis에 이미 "LG"가 저장되어 있고, 현재도 LG가 리드
        when(valueOps.getAndSet(anyString(), anyString())).thenReturn("LG");

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(3)
                .awayScore(2)
                .build();

        notifier.notify(schedule, List.of(homeFan, awayFan), info);

        verify(telegram, never()).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void notify_whenTiedAfterLead_sendsTiedMessage() {
        // 이전에 LG가 리드했지만 현재 동점 → currLeader="NONE"으로 변경
        when(valueOps.getAndSet(anyString(), anyString())).thenReturn("LG");

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(2)
                .awayScore(2)
                .build();

        notifier.notify(schedule, List.of(homeFan, awayFan), info);

        // 리더가 "LG" → "NONE"으로 바뀌었으므로 formatter 호출
        verify(formatter, times(1)).formatLeadChange(eq(homeFan), any(), eq("LG"), eq("NONE"));
        verify(formatter, times(1)).formatLeadChange(eq(awayFan), any(), eq("LG"), eq("NONE"));
    }

    @Test
    void notify_firstCallTiedScore_sendsNoMessage() {
        // 첫 호출(Redis=null) + 동점(0-0) → prevLeader="NONE", currLeader="NONE" → 변화 없음
        when(valueOps.getAndSet(anyString(), anyString())).thenReturn(null);

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(0)
                .awayScore(0)
                .build();

        notifier.notify(schedule, List.of(homeFan, awayFan), info);

        verify(telegram, never()).sendPersonalMessage(any(), any(), any());
        verify(formatter, never()).formatLeadChange(any(), any(), any(), any());
    }

    @Test
    void notify_memberWithNullTelegramId_skipsTelegram() {
        Member nullTelegramFan = Member.builder()
                .id(3L)
                .name("Carol")
                .supportTeam(Team.LG)
                .telegramId(null)
                .build();

        when(formatter.formatLeadChange(eq(nullTelegramFan), any(), anyString(), anyString()))
                .thenReturn("[홈팀 역전] Carol 메시지");

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(3)
                .awayScore(2)
                .build();

        // 첫 호출: prevLeader="NONE", currLeader="LG" → 변경 감지
        notifier.notify(schedule, List.of(nullTelegramFan), info);

        // formatter는 호출되지만 telegram은 호출되지 않아야 함
        verify(formatter, times(1)).formatLeadChange(eq(nullTelegramFan), any(), anyString(), anyString());
        verify(telegram, never()).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void notify_exceptionInFormatter_otherMemberStillNotified() {
        // homeFan의 formatter 호출 시 예외 발생
        when(formatter.formatLeadChange(eq(homeFan), any(), anyString(), anyString()))
                .thenThrow(new RuntimeException("포맷 실패"));

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(3)
                .awayScore(2)
                .build();

        notifier.notify(schedule, List.of(homeFan, awayFan), info);

        // homeFan은 예외로 인해 telegram 미발송, awayFan은 정상 발송
        verify(telegram, never()).sendPersonalMessage(eq("t1"), any(), any());
        verify(telegram, times(1)).sendPersonalMessage("t2", "Bob", "[어웨이팀 역전] Bob 메시지");
    }

    @Test
    void notify_emptyMemberList_noInteraction() {
        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(3)
                .awayScore(2)
                .build();

        notifier.notify(schedule, List.of(), info);

        verify(telegram, never()).sendPersonalMessage(any(), any(), any());
        verify(formatter, never()).formatLeadChange(any(), any(), any(), any());
    }

    @Test
    void notify_prevAwayToHome_sendsReversal() {
        // 이전에 LT(어웨이)가 리드 → 현재 LG(홈)가 역전
        when(valueOps.getAndSet(anyString(), anyString())).thenReturn("LT");

        RealtimeGameInfoDto info = RealtimeGameInfoDto.builder()
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .homeScore(5)
                .awayScore(4)
                .build();

        notifier.notify(schedule, List.of(homeFan, awayFan), info);

        // 양팀 팬 모두에게 알림 전송
        verify(telegram, times(1)).sendPersonalMessage("t1", "Alice", "[홈팀 역전] Alice 메시지");
        verify(telegram, times(1)).sendPersonalMessage("t2", "Bob", "[어웨이팀 역전] Bob 메시지");
    }
}
