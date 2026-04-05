// LeadChangeNotifier.java
package com.kbank.kbaseball.game.alert;

import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.game.message.GameMessageFormatter;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import com.kbank.kbaseball.notification.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class LeadChangeNotifier {
    private static final String GAME_LEADER_KEY_PREFIX = "game:leader:";

    private final GameMessageFormatter formatter;
    private final TelegramService telegram;
    private final StringRedisTemplate redis;

    public void notify(ScheduledGameDto schedule, List<Member> members, RealtimeGameInfoDto info) {
        String gameId = schedule.getGameId();
        String currLeader = SupportUtils.calculateLeader(info);

        // GETSET: 이전 리더 읽기 + 새 리더 쓰기를 원자적으로 처리
        String leaderKey = GAME_LEADER_KEY_PREFIX + gameId;
        String rawPrev = redis.opsForValue().getAndSet(leaderKey, currLeader);
        redis.expire(leaderKey, Duration.ofHours(24)); // 하루 지난 키 자동 정리

        if (rawPrev == null) {
            // 최초 폴링: 역전이 아닌 선취점이므로 별도 알림 발송
            if (!"NONE".equals(currLeader)) {
                log.info("[LeadChangeNotifier][notify] 선취점 감지 → gameId={}, leader={}", gameId, currLeader);
                members.forEach(m -> {
                    try {
                        if (m.getTelegramId() != null) {
                            String text = formatter.formatFirstScore(m, info, currLeader);
                            telegram.sendPersonalMessage(m.getTelegramId(), m.getName(), text);
                            log.info("[LeadChangeNotifier][notify] 선취점 알림 전송 → member={} gameId={}",
                                    m.getName(), gameId);
                        } else {
                            log.info("[LeadChangeNotifier][notify] Telegram ID is NULL. Message not sent. member={}", m.getName());
                        }
                    } catch (Exception e) {
                        log.error("[LeadChangeNotifier][notify] 선취점 알림 에러 → member={} : {}",
                                m.getName(), e.getMessage(), e);
                    }
                });
            }
            return;
        }

        String prevLeader = rawPrev;
        log.info("[LeadChangeNotifier][notify] 리더 변경 체크 → gameId={}, {} → {}",
                gameId, prevLeader, currLeader);

        // 리더가 바뀌었을 때만 알림
        if (!currLeader.equals(prevLeader)) {
            boolean isFirstScore = "NONE".equals(prevLeader)
                    && Math.min(info.getHomeScore(), info.getAwayScore()) == 0;

            members.forEach(m -> {
                try {
                    String text;
                    if (isFirstScore) {
                        // 0:0에서 첫 득점 = 선취점
                        text = formatter.formatFirstScore(m, info, currLeader);
                    } else {
                        // 실제 역전 또는 동점
                        text = formatter.formatLeadChange(m, info, prevLeader, currLeader);
                    }
                    if (m.getTelegramId() != null) {
                        telegram.sendPersonalMessage(m.getTelegramId(), m.getName(), text);
                        log.info("[LeadChangeNotifier][notify] {}알림 전송 → member={} gameId={}",
                                isFirstScore ? "선취점" : "역전", m.getName(), gameId);
                    } else {
                        log.info("[LeadChangeNotifier][notify] Telegram ID is NULL. Message not sent. member={}", m.getName());
                    }
                } catch (Exception e) {
                    log.error("[LeadChangeNotifier][notify] {}알림 에러 → member={} : {}",
                            isFirstScore ? "선취점" : "역전", m.getName(), e.getMessage(), e);
                }
            });
            log.info("[LeadChangeNotifier][notify] {}알림 완료 → gameId={}",
                    isFirstScore ? "선취점" : "역전", gameId);
        } else {
            log.info("[LeadChangeNotifier][notify] 리더 변경 없음 → gameId={} (leader={})",
                    gameId, currLeader);
        }
    }
}
