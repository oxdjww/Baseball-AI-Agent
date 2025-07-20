// com/kbank/baa/batch/tasklet/RealTimeAlertTasklet.java
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

        // 3) 멤버별로 해당 경기 상세 조회 → 메시지 생성 → 전송
        for (Member m : members) {
            String team = m.getSupportTeam().name();
            schedules.stream()
                    .filter(s -> team.equals(s.getHomeTeamCode()) || team.equals(s.getAwayTeamCode()))
                    .forEach(s -> {
                        RealtimeGameInfo info = apiClient.fetchGameInfo(s.getGameId());

                        String status = info.getStatusCode();
                        // 경기 시작 전일때는 보내지 않음
                        if (!status.equals("STARTED")) { // Case: BEFORE, READY, STARTED, ENDED
                            log.info("##### STATUS: {}", status);
                            log.info("##### [{}]{} vs {} #####", info.getGameId(), info.getAwayTeamCode(), info.getHomeTeamCode());
                            if (status.equals("BEFORE") | status.equals("READY")) {
                                log.info("##### 경기 시작 예정: {} #####", info.getGameDateTime());
                            } else if (status.equals("ENDED") | status.equals("RESULT")) {
                                log.info("##### 경기 종료: WINNER {} #####", info.getWinner());
                            }
                            return;
                        }

                        log.info("##### [{}]{} vs {} 경기중! 메시지 전송 #####", info.getGameId(), info.getAwayTeamCode(), info.getHomeTeamCode());

                        String text = formatter.format(m, info);
                        telegram.sendMessage(m.getTelegramId(), text);
                    });
        }

        return RepeatStatus.FINISHED;
    }
}
