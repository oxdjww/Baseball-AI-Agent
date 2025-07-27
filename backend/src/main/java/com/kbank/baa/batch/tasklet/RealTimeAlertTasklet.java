package com.kbank.baa.batch.tasklet;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.sports.GameMessageFormatter;
import com.kbank.baa.sports.RealtimeGameInfo;
import com.kbank.baa.sports.ScheduledGame;
import com.kbank.baa.sports.SportsApiClient;
import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealTimeAlertTasklet implements Tasklet {
    private final SportsApiClient apiClient;
    private final MemberRepository memberRepo;
    private final GameMessageFormatter formatter;
    private final TelegramService telegram;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate today = LocalDate.now();

        // 1) 오늘 일정 조회
        List<ScheduledGame> schedules = apiClient.fetchScheduledGames(today, today);

        // 2) 알림 대상 멤버 조회
        List<Member> members = memberRepo.findByNotifyRealTimeAlertTrue();

        // 3) 멤버별로 처리
        for (Member m : members) {
            String team = m.getSupportTeam().name();

            schedules.stream()
                    .filter(s -> team.equals(s.getHomeTeamCode()) || team.equals(s.getAwayTeamCode()))
                    .forEach(s -> {
                        try {
                            RealtimeGameInfo info = apiClient.fetchGameInfo(s.getGameId());
                            String status = info.getStatusCode();

                            // 경기 시작 전/후 상태 로깅
                            if (!"STARTED".equals(status)) {
                                log.info("##### STATUS: {}", status);
                                log.info("##### [{}]{} vs {} #####",
                                        info.getGameId(), info.getAwayTeamCode(), info.getHomeTeamCode());
                                if ("BEFORE".equals(status) || "READY".equals(status)) {
                                    log.info("##### 경기 시작 예정: {} #####", info.getGameDateTime());
                                } else if ("ENDED".equals(status) || "RESULT".equals(status)) {
                                    log.info("##### 경기 종료: WINNER {} #####", info.getWinner());
                                }
                                return;
                            }

                            log.info("##### [{}]{} vs {} 경기중! 메시지 전송 #####",
                                    info.getGameId(), info.getAwayTeamCode(), info.getHomeTeamCode());

                            String text = formatter.format(m, info);
                            telegram.sendMessage(m.getTelegramId(), text);

                        } catch (Exception e) {
                            // 개별 경기 또는 메시지 전송 실패 시 로깅 후 계속 진행
                            log.error("##### 멤버={}({}) 처리 중 오류 발생: {}",
                                    m.getName(), m.getTelegramId(), e.getMessage(), e);
                        }
                    });
        }

        return RepeatStatus.FINISHED;
    }
}