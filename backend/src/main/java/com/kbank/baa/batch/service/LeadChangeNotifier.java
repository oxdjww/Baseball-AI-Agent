// LeadChangeNotifier.java
package com.kbank.baa.batch.service;

import com.kbank.baa.member.Member;
import com.kbank.baa.sports.GameMessageFormatter;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import com.kbank.baa.sports.dto.ScheduledGameDto;
import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@RequiredArgsConstructor
public class LeadChangeNotifier {
    private final GameMessageFormatter formatter;
    private final TelegramService telegram;
    private final Map<String, String> leaderMap = new ConcurrentHashMap<>();

    public void notify(ScheduledGameDto schedule, List<Member> members, RealtimeGameInfoDto info) {
        String gameId = schedule.getGameId();
        String currLeader = SupportUtils.calculateLeader(info);

        // AtomicReference에 이전 리더를 캡처
        AtomicReference<String> prevRef = new AtomicReference<>("NONE");

        // compute로 이전 값(oldVal)과 새로운 값(currLeader)을 한 번에 처리
        leaderMap.compute(gameId, (id, oldVal) -> {
            String previous = (oldVal == null ? "NONE" : oldVal);
            prevRef.set(previous);
            return currLeader;  // 맵에는 무조건 새로운 리더를 저장
        });

        String prevLeader = prevRef.get();
        log.info("[LeadChangeNotifier][notify] 리더 변경 체크 → gameId={}, {} → {}",
                gameId, prevLeader, currLeader);

        // 리더가 바뀌었을 때만 알림
        if (!currLeader.equals(prevLeader)) {
            members.forEach(m -> {
                try {
                    // 내팀, 상대팀 역전시 양팀 팬에게 모두 알림
                    String text = formatter.formatLeadChange(m, info, prevLeader, currLeader);
                    if (m.getTelegramId() != null) {
                        telegram.sendPersonalMessage(m.getTelegramId(), m.getName(), text);
                        log.info("[LeadChangeNotifier][notify] 역전알림 전송 → member={} gameId={}",
                                m.getName(), gameId);
                    } else {
                        log.info("LeadChangeNotifier][notify] Telegram ID is NULL. Message not sent. member={}", m.getName());
                    }
                } catch (Exception e) {
                    log.error("[LeadChangeNotifier][notify] 역전알림 에러 → member={} : {}",
                            m.getName(), e.getMessage(), e);
                }
            });
            log.info("[LeadChangeNotifier][notify] 역전알림 완료 → gameId={}", gameId);
        } else {
            log.info("[LeadChangeNotifier][notify] 리더 변경 없음 → gameId={} (leader={})",
                    gameId, currLeader);
        }
    }
}
