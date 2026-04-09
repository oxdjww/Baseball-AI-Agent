package com.kbank.kbaseball.scheduler;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.naver.NaverSportsClient;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.member.MemberService;
import com.kbank.kbaseball.notification.history.NotificationHistoryService;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RainCancelPollingScheduler {

    private final NaverSportsClient apiClient;
    private final NotificationHistoryService notificationHistoryService;
    private final MemberService memberService;
    private final TelegramService telegramService;
    private final FeatureToggleService featureToggleService;

    @Value("${telegram.admin-id}")
    private String telegramAdminId;

    @Scheduled(cron = "0 0/5 10-20 * * *", zone = "Asia/Seoul")
    public void pollCancelStatus() {
        if (!featureToggleService.isEnabled(FeatureToggleService.RAIN_ALERT)) {
            log.info("[RainCancelPollingScheduler] RAIN_ALERT disabled. Skipping.");
            return;
        }

        List<ScheduledGameDto> candidates;
        try {
            candidates = fetchCandidateGames();
        } catch (Exception e) {
            log.error("[RainCancelPollingScheduler] 경기 목록 조회 실패: {}", e.getMessage(), e);
            telegramService.sendPlainMessage(telegramAdminId,
                "<b>[배치오류] 우천취소 폴링 실패</b>\n사유: " + e.getMessage());
            return;
        }

        log.info("[RainCancelPollingScheduler] 폴링 대상 경기 {}건", candidates.size());
        for (ScheduledGameDto game : candidates) {
            try {
                processGame(game);
            } catch (Exception e) {
                log.error("[RainCancelPollingScheduler] gameId={} 처리 실패: {}",
                    game.getGameId(), e.getMessage(), e);
            }
        }
    }

    private List<ScheduledGameDto> fetchCandidateGames() {
        LocalDateTime now = LocalDateTime.now();
        return apiClient.fetchScheduledGames(LocalDate.now(), LocalDate.now()).stream()
            .filter(g -> g.getGameDateTime().isAfter(now))
            .filter(g -> !g.getStadium().equals("고척"))
            .filter(g -> !notificationHistoryService.isCancelAlreadySent(g.getGameId()))
            .collect(Collectors.toList());
    }

    private void processGame(ScheduledGameDto game) {
        boolean canceled = apiClient.fetchCancelInfoFromGameInfo(game.getGameId());
        if (!canceled) return;

        boolean firstToSend = notificationHistoryService.tryMarkCancelSent(game.getGameId());
        if (!firstToSend) {
            log.info("[RainCancelPollingScheduler] 중복 발송 방지 스킵: gameId={}", game.getGameId());
            return;
        }

        log.info("[RainCancelPollingScheduler] 취소 감지, 알림 발송: gameId={}", game.getGameId());
        sendCancelAlert(game);
    }

    private void sendCancelAlert(ScheduledGameDto game) {
        Team homeTeam = Team.of(game.getHomeTeamCode());
        Team awayTeam = Team.of(game.getAwayTeamCode());
        String vs = String.format("[%s vs %s]", awayTeam.getDisplayName(), homeTeam.getDisplayName());

        String cancelText = String.format("<b>%s\n경기가 취소되었어요!</b> ☔️", vs);

        List<Member> homeMembers = memberService.findBySupportTeamAndNotifyRainAlertTrue(homeTeam);
        List<Member> awayMembers = memberService.findBySupportTeamAndNotifyRainAlertTrue(awayTeam);

        homeMembers.forEach(m -> safeSend(m, cancelText, "홈"));
        awayMembers.forEach(m -> safeSend(m, cancelText, "어웨이"));
    }

    private void safeSend(Member m, String text, String role) {
        try {
            telegramService.sendPersonalMessage(m.getTelegramId(), m.getName(), text);
            log.info("[RainCancelPollingScheduler] → 취소 알림({}) sent to {} ({})",
                role, m.getName(), m.getTelegramId());
        } catch (Exception e) {
            log.error("[RainCancelPollingScheduler] → {}님({}) 전송 실패: {}",
                m.getName(), role, e.getMessage(), e);
        }
    }
}
