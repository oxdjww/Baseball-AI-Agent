package com.kbank.kbaseball.batch.tasklet;

import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.member.MemberService;
import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.domain.team.Team;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.notification.history.NotificationHistoryService;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import com.kbank.kbaseball.external.kma.KmaWeatherClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RainAlertTasklet implements Tasklet {
    private final KmaWeatherClient rainfallService;
    private final MemberService memberService;
    private final TelegramService telegramService;
    private final FeatureToggleService featureToggleService;
    private final NotificationHistoryService notificationHistoryService;

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution,
                                @NonNull ChunkContext chunkContext) {
        return RepeatStatus.FINISHED;
    }

    /**
     * @param game        알림 대상 경기
     * @param hoursBefore 경기 시작 몇 시간 전인지 (1 또는 3)
     * @param thresholdMm 시간당 강수량 기준(mm)
     */
    public void executeForGame(ScheduledGameDto game,
                               LocalDateTime alertTime,
                               int hoursBefore,
                               double thresholdMm) {
        if (!featureToggleService.isEnabled(FeatureToggleService.RAIN_ALERT)) {
            log.info("[RainAlertTasklet][executeForGame] RAIN_ALERT feature is disabled. Skipping.");
            return;
        }
        // 1) enum에서 팀 정보 조회
        Team homeTeam = Team.of(game.getHomeTeamCode());
        Team awayTeam = Team.of(game.getAwayTeamCode());

        // 2) 기상청에서 해당 경기장의 강수량 조회 (홈팀 스테이션 사용)
        double rain = rainfallService.getRainfallByTeam(homeTeam.name(), alertTime);
        log.info("[RainAlertTasklet][executeForGame] checkRain: game={} {}h before, rain={}mm, threshold={}mm",
                game.getGameId(), hoursBefore, rain, thresholdMm);

        // 3) 홈/어웨이팀 모두 알림 켠 멤버 조회
        List<Member> homeMembers = memberService.findBySupportTeamAndNotifyRainAlertTrue(homeTeam);
        List<Member> awayMembers = memberService.findBySupportTeamAndNotifyRainAlertTrue(awayTeam);
        log.info("[RainAlertTasklet][executeForGame]→ {} 멤버 (홈: {}) + {} 멤버 (어웨이: {})",
                homeMembers.size(), homeTeam.getDisplayName(),
                awayMembers.size(), awayTeam.getDisplayName());

        // 4) 발송 전 취소 이력 DB 확인 (폴링 스케줄러가 이미 취소 알림을 보냈으면 강수량 알림 생략)
        if (notificationHistoryService.isCancelAlreadySent(game.getGameId())) {
            log.info("[RainAlertTasklet][executeForGame] 취소된 경기, 강수량 알림 생략: gameId={}", game.getGameId());
            return;
        }

        // 5) 메시지 텍스트 생성 (강수량 정보 전달에만 집중, 취소 판단은 폴링 스케줄러가 담당)
        String vs = String.format("[%s vs %s]", awayTeam.getDisplayName(), homeTeam.getDisplayName());
        String text;

        if (game.getStadium().equals("고척")) {
            log.info("[RainAlertTasklet][executeForGame] 고척 경기장(실내) 관전 권장 메시지 생성");
            text = String.format(
                    " %s 오늘은 고척 실내 경기장!\n비 걱정 없어요! 즐겁게 관전하세요! ⚾",
                    vs
            );
        } else if (rain < thresholdMm) {
            log.info("[RainAlertTasklet][executeForGame] rain < thresholdMm ({} < {}), 관전 권장 메시지 생성", rain, thresholdMm);
            text = String.format(
                    " %s %d시간 전 강수량 %.1fmm\n비 걱정 없어요! 즐겁게 관전하세요! ⚾",
                    vs, hoursBefore, rain
            );
        } else {
            log.info("[RainAlertTasklet][executeForGame] rain >= thresholdMm ({} ≥ {}), 우천취소 가능성 메시지 생성", rain, thresholdMm);
            text = String.format(
                    "<b>%s %d시간 전 강수량 %.1fmm\n우천취소 가능성 있어요!</b> ☔️",
                    vs, hoursBefore, rain
            );
        }

        final String finalText = text + "\n\n경기 응원하러 가기! ⬇️\nhttps://m.sports.naver.com/game/" + game.getGameId() + "/cheer";
        log.info("[RainAlertTasklet][executeForGame] Generated text: {}", finalText);

        // 6) 홈팀 멤버에게 전송
        homeMembers.forEach(m -> {
            try {
                telegramService.sendPersonalMessage(m.getTelegramId(), m.getName(), finalText);
                log.info("[RainAlertTasklet][executeForGame] → 우천 알림(홈) sent to {} ({})", m.getName(), m.getTelegramId());
            } catch (Exception e) {
                log.error("[RainAlertTasklet][executeForGame] → {}님(홈)에게 우천 알림 전송 실패: {}", m.getName(), e.getMessage(), e);
            }
        });

        // 7) 어웨이팀 멤버에게도 전송
        awayMembers.forEach(m -> {
            try {
                telegramService.sendPersonalMessage(m.getTelegramId(), m.getName(), finalText);
                log.info("[RainAlertTasklet][executeForGame] →  우천 알림(어웨이) sent to {} ({})", m.getName(), m.getTelegramId());
            } catch (Exception e) {
                log.error("[RainAlertTasklet][executeForGame] → {}님(어웨이)에게 우천 알림 전송 실패: {}", m.getName(), e.getMessage(), e);
            }
        });
    }
}
