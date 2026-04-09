package com.kbank.kbaseball.scheduler;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.member.MemberService;
import com.kbank.kbaseball.notification.history.NotificationHistoryService;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RainCancelPollingSchedulerTest {

    @Mock NaverSportsClient apiClient;
    @Mock NotificationHistoryService notificationHistoryService;
    @Mock MemberService memberService;
    @Mock TelegramService telegramService;
    @Mock FeatureToggleService featureToggleService;

    @InjectMocks
    RainCancelPollingScheduler scheduler;

    ScheduledGameDto upcomingGame;
    Member homeFan, awayFan;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "telegramAdminId", "admin-id");

        upcomingGame = ScheduledGameDto.builder()
                .gameId("20260409HTLG02026")
                .homeTeamCode("LG")
                .awayTeamCode("HT")
                .stadium("잠실")
                .gameDateTime(LocalDateTime.now().plusHours(2))
                .build();

        homeFan = Member.builder().id(1L).name("Alice").supportTeam(Team.LG).telegramId("t1").build();
        awayFan = Member.builder().id(2L).name("Bob").supportTeam(Team.HT).telegramId("t2").build();

        when(featureToggleService.isEnabled(FeatureToggleService.RAIN_ALERT)).thenReturn(true);
        when(apiClient.fetchScheduledGames(any(), any())).thenReturn(List.of(upcomingGame));
        when(notificationHistoryService.isCancelAlreadySent(upcomingGame.getGameId())).thenReturn(false);
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.LG)).thenReturn(List.of(homeFan));
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.HT)).thenReturn(List.of(awayFan));
    }

    // ── Feature Toggle ────────────────────────────────────────────────────

    @Test
    void feature_disabled_시_폴링_스킵() {
        when(featureToggleService.isEnabled(FeatureToggleService.RAIN_ALERT)).thenReturn(false);

        scheduler.pollCancelStatus();

        verify(apiClient, never()).fetchScheduledGames(any(), any());
    }

    // ── 경기 필터링 ───────────────────────────────────────────────────────

    @Test
    void 고척_경기장_제외() {
        ScheduledGameDto gochuk = ScheduledGameDto.builder()
                .gameId("GOCHECK-GAME")
                .homeTeamCode("LG").awayTeamCode("HT")
                .stadium("고척")
                .gameDateTime(LocalDateTime.now().plusHours(2))
                .build();
        when(apiClient.fetchScheduledGames(any(), any())).thenReturn(List.of(gochuk));

        scheduler.pollCancelStatus();

        verify(apiClient, never()).fetchCancelInfoFromGameInfo(gochuk.getGameId());
    }

    @Test
    void 이미_시작한_경기_제외() {
        ScheduledGameDto pastGame = ScheduledGameDto.builder()
                .gameId("PAST-GAME")
                .homeTeamCode("LG").awayTeamCode("HT")
                .stadium("잠실")
                .gameDateTime(LocalDateTime.now().minusMinutes(30))
                .build();
        when(apiClient.fetchScheduledGames(any(), any())).thenReturn(List.of(pastGame));

        scheduler.pollCancelStatus();

        verify(apiClient, never()).fetchCancelInfoFromGameInfo(pastGame.getGameId());
    }

    @Test
    void DB_이력_있는_경기_중복_발송_안됨() {
        when(notificationHistoryService.isCancelAlreadySent(upcomingGame.getGameId())).thenReturn(true);

        scheduler.pollCancelStatus();

        verify(apiClient, never()).fetchCancelInfoFromGameInfo(upcomingGame.getGameId());
        verify(telegramService, never()).sendPersonalMessage(any(), any(), any());
    }

    // ── 취소 감지 및 알림 ─────────────────────────────────────────────────

    @Test
    void 취소_미결정_경기_알림_미발송() {
        when(apiClient.fetchCancelInfoFromGameInfo(upcomingGame.getGameId())).thenReturn(false);

        scheduler.pollCancelStatus();

        verify(telegramService, never()).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void 취소_확정_tryMarkCancelSent_true_반환시_알림_발송() {
        when(apiClient.fetchCancelInfoFromGameInfo(upcomingGame.getGameId())).thenReturn(true);
        when(notificationHistoryService.tryMarkCancelSent(upcomingGame.getGameId())).thenReturn(true);

        scheduler.pollCancelStatus();

        verify(telegramService, atLeastOnce()).sendPersonalMessage(anyString(), anyString(), anyString());
    }

    @Test
    void 취소_확정_tryMarkCancelSent_false_반환시_중복_방지_발송_안됨() {
        when(apiClient.fetchCancelInfoFromGameInfo(upcomingGame.getGameId())).thenReturn(true);
        when(notificationHistoryService.tryMarkCancelSent(upcomingGame.getGameId())).thenReturn(false);

        scheduler.pollCancelStatus();

        verify(telegramService, never()).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void 취소_확정_홈팬_어웨이팬_모두_발송() {
        when(apiClient.fetchCancelInfoFromGameInfo(upcomingGame.getGameId())).thenReturn(true);
        when(notificationHistoryService.tryMarkCancelSent(upcomingGame.getGameId())).thenReturn(true);

        scheduler.pollCancelStatus();

        verify(telegramService, times(1)).sendPersonalMessage(eq("t1"), eq("Alice"), anyString());
        verify(telegramService, times(1)).sendPersonalMessage(eq("t2"), eq("Bob"), anyString());
    }

    @Test
    void 취소_메시지_강수량_정보_미포함() {
        when(apiClient.fetchCancelInfoFromGameInfo(upcomingGame.getGameId())).thenReturn(true);
        when(notificationHistoryService.tryMarkCancelSent(upcomingGame.getGameId())).thenReturn(true);

        scheduler.pollCancelStatus();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).doesNotContain("시간 전 강수량");
        assertThat(captor.getValue()).contains("경기가 취소되었어요");
    }

    // ── 예외 처리 ─────────────────────────────────────────────────────────

    @Test
    void 게임목록_조회_실패시_관리자_알림_발송() {
        when(apiClient.fetchScheduledGames(any(), any()))
                .thenThrow(new RuntimeException("Naver API 오류"));

        scheduler.pollCancelStatus();

        verify(telegramService, times(1)).sendPlainMessage(eq("admin-id"), anyString());
        verify(telegramService, never()).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void 개별_경기_실패시_다른_경기_계속_처리() {
        ScheduledGameDto secondGame = ScheduledGameDto.builder()
                .gameId("SECOND-GAME")
                .homeTeamCode("OB").awayTeamCode("SS")
                .stadium("잠실")
                .gameDateTime(LocalDateTime.now().plusHours(1))
                .build();
        Member obFan = Member.builder().id(3L).name("Charlie").supportTeam(Team.OB).telegramId("t3").build();

        when(apiClient.fetchScheduledGames(any(), any())).thenReturn(List.of(upcomingGame, secondGame));
        when(notificationHistoryService.isCancelAlreadySent(secondGame.getGameId())).thenReturn(false);
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.OB)).thenReturn(List.of(obFan));
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.SS)).thenReturn(List.of());

        // 첫 번째 경기 처리 중 예외 발생
        when(apiClient.fetchCancelInfoFromGameInfo(upcomingGame.getGameId()))
                .thenThrow(new RuntimeException("첫 번째 경기 API 오류"));
        when(apiClient.fetchCancelInfoFromGameInfo(secondGame.getGameId())).thenReturn(true);
        when(notificationHistoryService.tryMarkCancelSent(secondGame.getGameId())).thenReturn(true);

        scheduler.pollCancelStatus();

        // 두 번째 경기는 정상 처리됨
        verify(telegramService, times(1)).sendPersonalMessage(eq("t3"), eq("Charlie"), anyString());
    }
}
