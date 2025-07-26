// RealTimeAlertTasklet.java
package com.kbank.baa.batch.tasklet;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.sports.*;
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
    private final ScoreTracker tracker;
    private final TelegramService telegram;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("===== RealTimeAlertTasklet 실행 시작 =====");

        LocalDate today = LocalDate.now();  // 오늘 날짜
        // 1) 오늘 경기 일정 조회
        List<ScheduledGame> schedules = apiClient.fetchScheduledGames(today, today);
        log.debug("오늘 경기 일정 개수: {}", schedules.size());

        // 2) 알림 대상 멤버 조회
        List<Member> members = memberRepo.findByNotifyRealTimeAlertTrue();
        log.debug("실시간 알림 대상 멤버 수: {}", members.size());

        // 3) 멤버별 반복 처리
        for (Member m : members) {
            String teamCode = m.getSupportTeam().name();
            boolean supportHome = teamCode.equalsIgnoreCase("HOME"); // 실제 코드에 맞게 수정
            log.info(">> 멤버 처리: {} (응원팀: {})", m.getName(), teamCode);

            // 4) 경기별 반복 처리
            for (ScheduledGame s : schedules) {
                // 응원팀과 관련 없는 경기는 스킵
                if (!(teamCode.equals(s.getHomeTeamCode()) || teamCode.equals(s.getAwayTeamCode()))) {
                    log.trace("경기 제외: {} 은(는) 응원팀 경기가 아님", s.getGameId());
                    continue;
                }
                log.debug(">>> 경기 조회: {} ({} vs {})", s.getGameId(), s.getAwayTeamCode(), s.getHomeTeamCode());

                try {
                    // API 호출로 실시간 정보 조회
                    RealtimeGameInfo info = apiClient.fetchGameInfo(s.getGameId());
                    String status = info.getStatusCode();
                    log.debug(">>> 경기 상태: {}", status);

                    // 경기 중이 아닐 땐 무시
                    if (!"STARTED".equals(status)) {
                        log.info(">>> 경기 미진행 상태, 스킵: {}", status);
                        continue;
                    }

                    // 현재/이전 스코어 비교
                    int currHome = info.getHomeScore();
                    int currAway = info.getAwayScore();
                    Integer prevHome = tracker.getPrevHome(s.getGameId());
                    Integer prevAway = tracker.getPrevAway(s.getGameId());
                    log.debug(">>> 이전 스코어: {}:{}  →  현재 스코어: {}:{}", prevHome, prevAway, currHome, currAway);

                    if (prevHome != null && prevAway != null) {
                        boolean wasLosing = supportHome ? prevHome < prevAway : prevAway < prevHome;
                        boolean wasTied = prevHome.equals(prevAway);
                        boolean nowLeading = supportHome ? currHome > currAway : currAway > currHome;
                        log.debug(">>> wasLosing={}, wasTied={}, nowLeading={}", wasLosing, wasTied, nowLeading);

                        // ‘지고 있다→역전’ 또는 ‘동점→역전’ 감지 시
                        if ((wasLosing || wasTied) && nowLeading) {
                            log.info("+++ 역전 감지! 메시지 전송 시작 +++");
                            String msg = formatter.formatReversal(m, info);
                            telegram.sendMessage(m.getTelegramId(), msg);
                            log.info("+++ 메시지 전송 완료 to {} +++", m.getTelegramId());
                        }
                    }

                    // 5) 상태 업데이트
                    tracker.update(s.getGameId(), currHome, currAway);

                } catch (Exception e) {
                    log.error("##### 멤버={} 처리 중 오류 발생: {}", m.getName(), e.getMessage(), e);
                }
            }
        }

        log.info("===== RealTimeAlertTasklet 실행 종료 =====");
        return RepeatStatus.FINISHED;
    }
}
