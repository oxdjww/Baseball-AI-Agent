package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameAlertServiceTest {

    @Mock
    NaverSportsClient apiClient;

    @Mock
    MemberRepository memberRepo;

    @Mock
    GameProcessor gameProcessor;

    @Mock
    FeatureToggleService featureToggleService;

    @InjectMocks
    GameAlertService gameAlertService;

    @BeforeEach
    void setUp() {
        when(featureToggleService.isEnabled(FeatureToggleService.REVERSAL_DETECTION)).thenReturn(true);
    }

    private static ScheduledGameDto game(String gameId) {
        return ScheduledGameDto.builder()
                .gameId(gameId)
                .homeTeamCode("LG")
                .awayTeamCode("LT")
                .build();
    }

    @Test
    void processAlertsFor_noGames_processorNeverCalled() {
        LocalDate date = LocalDate.of(2024, 3, 14);
        when(apiClient.fetchScheduledGames(date, date)).thenReturn(List.of());
        when(memberRepo.findByNotifyRealTimeAlertTrue()).thenReturn(List.of());

        gameAlertService.processAlertsFor(date);

        verify(gameProcessor, never()).process(any(), any());
    }

    @Test
    void processAlertsFor_twoGames_processorCalledTwice() {
        LocalDate date = LocalDate.of(2024, 3, 14);
        when(apiClient.fetchScheduledGames(date, date)).thenReturn(List.of(game("G1"), game("G2")));
        when(memberRepo.findByNotifyRealTimeAlertTrue()).thenReturn(List.of());

        gameAlertService.processAlertsFor(date);

        verify(gameProcessor, times(2)).process(any(), any());
    }

    @Test
    void processAlertsFor_noInterestedMembers_processCalledWithEmptyList() {
        LocalDate date = LocalDate.of(2024, 3, 14);
        when(apiClient.fetchScheduledGames(date, date)).thenReturn(List.of(game("G1")));
        when(memberRepo.findByNotifyRealTimeAlertTrue()).thenReturn(List.of());

        gameAlertService.processAlertsFor(date);

        verify(gameProcessor, times(1)).process(any(), eq(List.of()));
    }

    @Test
    void processAlertsFor_passesTodayDate() {
        LocalDate today = LocalDate.of(2024, 3, 14);
        when(apiClient.fetchScheduledGames(today, today)).thenReturn(List.of());
        when(memberRepo.findByNotifyRealTimeAlertTrue()).thenReturn(List.of());

        gameAlertService.processAlertsFor(today);

        verify(apiClient).fetchScheduledGames(today, today);
    }
}
