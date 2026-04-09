package com.kbank.kbaseball.batch.tasklet;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.kma.KmaWeatherClient;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RainAlertTasklet 우천 알림 분기 로직 단위 테스트
 * KmaWeatherClient, MemberService, TelegramService, NotificationHistoryService 완전 격리
 * 취소 알림 로직은 RainCancelPollingScheduler가 담당 (SSOT) — tasklet은 강수량 전달만 수행
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RainAlertTaskletTest {

    @Mock KmaWeatherClient rainfallService;
    @Mock MemberService memberService;
    @Mock TelegramService telegramService;
    @Mock NaverSportsClient sportsApiClient;  // 테스트 환경 호환성 유지 (실제 주입 안 됨)
    @Mock FeatureToggleService featureToggleService;
    @Mock NotificationHistoryService notificationHistoryService;

    @InjectMocks
    RainAlertTasklet tasklet;

    ScheduledGameDto lgVsLt;
    Member homeFan, awayFan;
    LocalDateTime alertTime;

    @BeforeEach
    void setUp() {
        lgVsLt = ScheduledGameDto.builder()
                .gameId("20260315LTLG02026")
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .stadium("잠실")
                .build();

        homeFan = Member.builder().id(1L).name("Alice").supportTeam(Team.LG).telegramId("t1").build();
        awayFan = Member.builder().id(2L).name("Bob").supportTeam(Team.LT).telegramId("t2").build();

        alertTime = LocalDateTime.of(2026, 3, 15, 17, 0);

        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.LG)).thenReturn(List.of(homeFan));
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.LT)).thenReturn(List.of(awayFan));
        when(featureToggleService.isEnabled(FeatureToggleService.RAIN_ALERT)).thenReturn(true);
        // 기본적으로 취소 이력 없음
        when(notificationHistoryService.isCancelAlreadySent(lgVsLt.getGameId())).thenReturn(false);
    }

    // ── 취소 이력 DB 체크 ─────────────────────────────────────────────────

    @Test
    void 강수량_알림_발송_전_DB_취소이력_있으면_스킵() {
        when(notificationHistoryService.isCancelAlreadySent(lgVsLt.getGameId())).thenReturn(true);
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 3, 10.0);

        verify(telegramService, never()).sendPersonalMessage(any(), any(), any());
    }

    @Test
    void 강수량_알림_발송_전_DB_취소이력_없으면_정상_발송() {
        when(notificationHistoryService.isCancelAlreadySent(lgVsLt.getGameId())).thenReturn(false);
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 3, 10.0);

        verify(telegramService, atLeastOnce()).sendPersonalMessage(anyString(), anyString(), anyString());
    }

    // ── 메시지 분기 테스트 ────────────────────────────────────────────────

    @Test
    void 고척_실내경기장_실내메시지_전송() {
        ScheduledGameDto gocheck = ScheduledGameDto.builder()
                .gameId("GAME-GOCHECK")
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .stadium("고척")
                .build();
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(15.0);
        when(notificationHistoryService.isCancelAlreadySent(gocheck.getGameId())).thenReturn(false);

        tasklet.executeForGame(gocheck, alertTime, 3, 10.0);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), textCaptor.capture());
        assertThat(textCaptor.getValue()).contains("고척 실내 경기장");
        assertThat(textCaptor.getValue()).doesNotContain("우천취소");
    }

    @Test
    void 강수량_임계값_미만_관전권장_메시지_전송() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 3, 10.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        String text = captor.getValue();
        assertThat(text).contains("비 걱정 없어요");
        assertThat(text).contains("2.0mm");
        assertThat(text).doesNotContain("우천취소");
    }

    @Test
    void 강수량_임계값_이상_취소_가능성_메시지_전송() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(12.0);

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).contains("우천취소 가능성 있어요");
    }

    @Test
    void 메시지에_경기정보_포함() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).contains("롯데").contains("LG");
    }

    // ── 전송 대상 검증 ────────────────────────────────────────────────────

    @Test
    void 홈팬_어웨이팬_모두에게_전송() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        verify(telegramService, times(1)).sendPersonalMessage(eq("t1"), eq("Alice"), anyString());
        verify(telegramService, times(1)).sendPersonalMessage(eq("t2"), eq("Bob"), anyString());
    }

    @Test
    void 홈팬만_있을때_홈팬만_전송() {
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.LT)).thenReturn(List.of());
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        verify(telegramService, times(1)).sendPersonalMessage(eq("t1"), eq("Alice"), anyString());
        verify(telegramService, never()).sendPersonalMessage(eq("t2"), anyString(), anyString());
    }

    @Test
    void 멤버없으면_telegram_미호출() {
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.LG)).thenReturn(List.of());
        when(memberService.findBySupportTeamAndNotifyRainAlertTrue(Team.LT)).thenReturn(List.of());
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        verify(telegramService, never()).sendPersonalMessage(any(), any(), any());
    }

    // ── 예외 격리 테스트 ──────────────────────────────────────────────────

    @Test
    void telegram_예외_발생시_다른_팬_영향_없음() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);
        doThrow(new RuntimeException("텔레그램 전송 실패"))
                .when(telegramService).sendPersonalMessage(eq("t1"), eq("Alice"), anyString());

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        verify(telegramService, times(1)).sendPersonalMessage(eq("t2"), eq("Bob"), anyString());
    }

    // ── 응원 링크 포함 여부 검증 ──────────────────────────────────────────

    @Test
    void 고척_경기장_응원링크_포함() {
        ScheduledGameDto gocheck = ScheduledGameDto.builder()
                .gameId("GAME-GOCHECK")
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .stadium("고척")
                .build();
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(15.0);
        when(notificationHistoryService.isCancelAlreadySent(gocheck.getGameId())).thenReturn(false);

        tasklet.executeForGame(gocheck, alertTime, 3, 10.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).contains("https://m.sports.naver.com/game/GAME-GOCHECK/cheer");
        assertThat(captor.getValue()).contains("경기 응원하러 가기!");
    }

    @Test
    void 비_걱정없음_응원링크_포함() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 3, 10.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).contains("https://m.sports.naver.com/game/20260315LTLG02026/cheer");
    }

    @Test
    void 우천취소_가능성_응원링크_포함() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(12.0);

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).contains("https://m.sports.naver.com/game/20260315LTLG02026/cheer");
    }

    // ── hoursBefore 파라미터 전달 검증 ────────────────────────────────────

    @Test
    void hoursBefore_3_메시지에_3시간_포함() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 3, 10.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).contains("3시간");
    }

    @Test
    void hoursBefore_1_메시지에_1시간_포함() {
        when(rainfallService.getRainfallByTeam(eq("LG"), any())).thenReturn(2.0);

        tasklet.executeForGame(lgVsLt, alertTime, 1, 5.0);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramService, atLeastOnce())
                .sendPersonalMessage(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).contains("1시간");
    }
}
