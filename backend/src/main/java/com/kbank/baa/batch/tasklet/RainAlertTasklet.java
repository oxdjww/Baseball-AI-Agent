// RainAlertTasklet.java
package com.kbank.baa.batch.tasklet;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.admin.Team;
import com.kbank.baa.sports.SportsApiClient;
import com.kbank.baa.sports.dto.ScheduledGameDto;
import com.kbank.baa.telegram.TelegramService;
import com.kbank.baa.weather.service.RainfallService;
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
    private final RainfallService rainfallService;
    private final MemberRepository memberRepo;
    private final TelegramService telegram;
    private final SportsApiClient sportsApiClient;

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
        // 1) enum에서 팀 정보 조회
        Team homeTeam = Team.of(game.getHomeTeamCode());
        Team awayTeam = Team.of(game.getAwayTeamCode());

        // 2) 기상청에서 해당 경기장의 강수량 조회 (홈팀 스테이션 사용)
        double rain = rainfallService.getRainfallByTeam(homeTeam.name(), alertTime);
        log.info("##### checkRain: game={} {}h before, rain={}mm, threshold={}mm",
                game.getGameId(), hoursBefore, rain, thresholdMm);

        // 3) 홈/어웨이팀 모두 알림 켠 멤버 조회
        List<Member> homeMembers = memberRepo.findBySupportTeamAndNotifyRainAlertTrue(homeTeam);
        List<Member> awayMembers = memberRepo.findBySupportTeamAndNotifyRainAlertTrue(awayTeam);
        log.info("→ {} 멤버 (홈: {}) + {} 멤버 (어웨이: {})",
                homeMembers.size(), homeTeam.getDisplayName(),
                awayMembers.size(), awayTeam.getDisplayName());

        // 4) 메시지 텍스트 생성
        String vs = String.format("[%s vs %s]", awayTeam.getDisplayName(), homeTeam.getDisplayName());
        String text;

        // 고척(실내)경기장 이거나, 기준치 미달일 경우
        if (game.getStadium().equals("고척")) {
            log.info("고척 경기장(실내) 관전 권장 메시지 생성");
            text = String.format(
                    " %s 오늘은 고척 실내 경기장!\n비 걱정 없어요! 즐겁게 관전하세요! ⚾",
                    vs
            );
        } else if (rain < thresholdMm) {
            // 강수량이 우천 취소 기준치 미달
            log.info("rain < thresholdMm ({} < {}), 관전 권장 메시지 생성", rain, thresholdMm);
            text = String.format(
                    " %s %d시간 전 강수량 %.1fmm\n비 걱정 없어요! 즐겁게 관전하세요! ⚾",
                    vs, hoursBefore, rain
            );
        } else {
            // 강수량이 우천 취소 기준치에 해당될 경우
            if (sportsApiClient.fetchCancelInfoFromGameInfo(game.getGameId())) {
                // 강수량도 기준치 이상이고, 실제로 게임도 취소된 경우
                log.info("rain >= thresholdMm ({} ≥ {}), 우천취소 확정 메시지 생성", rain, thresholdMm);
                text = String.format(
                        "<b>%s %d시간 전 강수량 %.1fmm\n경기가 우천취소 되었어요!</b> ☔️",
                        vs, hoursBefore, rain
                );
            } else {
                // 강수량은 기준치 이상이나, 실제로 게임은 아직 취소되지 않은 경우
                log.info("rain >= thresholdMm ({} ≥ {}), 우천취소 가능성 메시지 생성", rain, thresholdMm);
                text = String.format(
                        "<b>%s %d시간 전 강수량 %.1fmm\n우천취소 가능성 있어요!</b> ☔️",
                        vs, hoursBefore, rain
                );
            }
        }
        log.debug("##### Generated text: {}", text);

        // 5) 홈팀 멤버에게 전송
        homeMembers.forEach(m -> {
            try {
//                telegram.sendMessage(m.getTelegramId(), text);
                // 20250729 TEST
                telegram.sendMessage(m.getTelegramId(), m.getName(), text);
                log.info("→ 우천 알림(홈) sent to {} ({})", m.getName(), m.getTelegramId());
            } catch (Exception e) {
                log.error("→ {}님(홈)에게 우천 알림 전송 실패: {}", m.getName(), e.getMessage(), e);
            }
        });

        // 6) 어웨이팀 멤버에게도 전송
        awayMembers.forEach(m -> {
            try {
                telegram.sendMessage(m.getTelegramId(), m.getName(), text);
                log.info("→  우천 알림(어웨이) sent to {} ({})", m.getName(), m.getTelegramId());
            } catch (Exception e) {
                log.error("→ {}님(어웨이)에게 우천 알림 전송 실패: {}", m.getName(), e.getMessage(), e);
            }
        });
    }

}
