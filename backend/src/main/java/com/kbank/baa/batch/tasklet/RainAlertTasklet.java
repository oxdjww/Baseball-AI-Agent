package com.kbank.baa.batch.tasklet;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.admin.Team;
import com.kbank.baa.sports.ScheduledGame;
import com.kbank.baa.telegram.TelegramService;
import com.kbank.baa.weather.service.RainfallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RainAlertTasklet implements Tasklet {
    private final RainfallService rainfallService;
    private final MemberRepository memberRepo;
    private final TelegramService telegram;

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        return RepeatStatus.FINISHED;
    }

    /**
     * @param game        알림 대상 경기
     * @param hoursBefore 경기 시작 몇 시간 전인지 (1 또는 3)
     * @param thresholdMm 시간당 강수량 기준(mm)
     */
    public void executeForGame(ScheduledGame game,
                               int hoursBefore,
                               double thresholdMm) {
        String teamCode = game.getHomeTeamCode();
        double rain = rainfallService.getRainfallByTeam(teamCode);

        log.info("##### checkRain: game={} {}h before, rain={}mm, threshold={}mm",
                game.getGameId(), hoursBefore, rain, thresholdMm);

        // 구단 서포터 조회
        List<Member> members = memberRepo.findBySupportTeam(Team.valueOf(teamCode));

        if (rain >= thresholdMm) {
            // 비가 올 가능성
            String text = String.format(
                    "[%s] %s vs %s: %dh 전 강수량 %.1fmm → 우천취소 가능성 있어요! ☔",
                    game.getGameDateTime().toLocalTime(),
                    game.getAwayTeamCode(), game.getHomeTeamCode(),
                    hoursBefore, rain
            );
            members.forEach(m -> {
                telegram.sendMessage(m.getTelegramId(), text);
                log.info("##### → rain alert sent to {} ({})", m.getName(), m.getTelegramId());
            });
        } else {
            // 비 걱정 없으니 야구 즐기세요!
            String text = String.format(
                    "[%s] %s vs %s: %dh 전 강수량 %.1fmm → 비 걱정 없어요! 즐겁게 관전하세요! ⚾",
                    game.getGameDateTime().toLocalTime(),
                    game.getAwayTeamCode(), game.getHomeTeamCode(),
                    hoursBefore, rain
            );
            members.forEach(m -> {
                telegram.sendMessage(m.getTelegramId(), text);
                log.info("##### → no-rain message sent to {} ({})", m.getName(), m.getTelegramId());
            });
        }
    }
}
