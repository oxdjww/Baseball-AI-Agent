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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealTimeAlertTasklet implements Tasklet {
    private final SportsApiClient apiClient;
    private final MemberRepository memberRepo;
    private final GameMessageFormatter formatter;
    private final TelegramService telegram;

    // gameId → 현재 리드 중인 팀코드 ("NONE"은 비김/초기상태)
    private final Map<String, String> leaderMap = new ConcurrentHashMap<>();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("########## RealTimeAlertTasklet.execute 시작 ##########");
        LocalDate today = LocalDate.now();
        List<ScheduledGame> schedules = apiClient.fetchScheduledGames(today, today);
        List<Member> members = memberRepo.findByNotifyRealTimeAlertTrue();
        log.info("########## 조회된 오늘 경기 수={}, 알림 대상 멤버 수={} ##########",
                schedules.size(), members.size());

        for (ScheduledGame schedule : schedules) {
            processGame(schedule, members);
        }

        log.info("########## RealTimeAlertTasklet.execute 완료 ##########");
        return RepeatStatus.FINISHED;
    }

    private void processGame(ScheduledGame schedule, List<Member> members) {
        String gameId = schedule.getGameId();
        log.info("########## processGame 시작 → gameId={} ##########", gameId);

        RealtimeGameInfo info;
        try {
            info = apiClient.fetchGameInfo(gameId);
            log.info("########## 게임 정보 조회 성공 → gameId={}, status={} ##########",
                    gameId, info.getStatusCode());
        } catch (Exception e) {
            log.error("########## 게임 정보 조회 실패 → {} : {} ##########",
                    gameId, e.getMessage(), e);
            return;
        }

        String status = info.getStatusCode();
        if (!"STARTED".equals(status)) {
            logGameStatus(schedule, info);
        } else {
            notifyPeriodicUpdates(schedule, members, info);
            checkAndNotifyLeadChange(schedule, members, info);
        }

        log.info("########## processGame 완료 → gameId={} ##########", gameId);
    }

    private void logGameStatus(ScheduledGame s, RealtimeGameInfo info) {
        log.info("########## STATUS={} [{}] {} vs {} ##########",
                info.getStatusCode(),
                info.getGameId(),
                info.getAwayTeamCode(),
                info.getHomeTeamCode()
        );
        // 필요시 READY/ENDED 등 세부 로그 추가
    }

    private void notifyPeriodicUpdates(ScheduledGame schedule, List<Member> members, RealtimeGameInfo info) {
        String gameId = schedule.getGameId();
        log.info("########## notifyPeriodicUpdates 시작 → gameId={} ##########", gameId);

        members.stream()
                .filter(m -> isSupportingGame(m, schedule))
                .forEach(m -> {
                    try {
                        String text = formatter.format(m, info);
                        telegram.sendMessage(m.getTelegramId(), text);
                        log.info("########## 주기알림 전송 → member={} gameId={} ##########",
                                m.getName(), gameId);
                    } catch (Exception e) {
                        log.error("########## 주기알림 에러 → member={} : {} ##########",
                                m.getName(), e.getMessage(), e);
                    }
                });

        log.info("########## notifyPeriodicUpdates 완료 → gameId={} ##########", gameId);
    }

    private void checkAndNotifyLeadChange(ScheduledGame schedule, List<Member> members, RealtimeGameInfo info) {
        String gameId = schedule.getGameId();
        String prevLeader = leaderMap.getOrDefault(gameId, "NONE");
        String currLeader = calculateLeader(info);

        log.info("########## checkAndNotifyLeadChange 시작 → gameId={}, prevLeader={}, currLeader={} ##########",
                gameId, prevLeader, currLeader);

        if (!currLeader.equals(prevLeader)) {
            log.info("########## 리더 변경 감지 → gameId={}, {} → {} ##########",
                    gameId, prevLeader, currLeader);

            leaderMap.put(gameId, currLeader);

            members.stream()
                    .filter(m -> currLeader.equals(m.getSupportTeam().name()))
                    .forEach(m -> {
                        try {
                            String text = formatter.formatLeadChange(m, info, prevLeader, currLeader);
                            telegram.sendMessage(m.getTelegramId(), text);
                            log.info("########## 역전/동점 알림 전송 → member={} gameId={} ##########",
                                    m.getName(), gameId);
                        } catch (Exception e) {
                            log.error("########## 역전알림 에러 → member={} : {} ##########",
                                    m.getName(), e.getMessage(), e);
                        }
                    });

            log.info("########## checkAndNotifyLeadChange 완료 (알림전송) → gameId={} ##########", gameId);
        } else {
            log.info("########## 리더 변경 없음 → gameId={} (leader={}) ##########", gameId, currLeader);
        }
    }

    private boolean isSupportingGame(Member m, ScheduledGame s) {
        String t = m.getSupportTeam().name();
        return t.equals(s.getHomeTeamCode()) || t.equals(s.getAwayTeamCode());
    }

    private String calculateLeader(RealtimeGameInfo info) {
        int away = info.getAwayScore();
        int home = info.getHomeScore();
        if (away > home) return info.getAwayTeamCode();
        else if (home > away) return info.getHomeTeamCode();
        else return "NONE";
    }
}
