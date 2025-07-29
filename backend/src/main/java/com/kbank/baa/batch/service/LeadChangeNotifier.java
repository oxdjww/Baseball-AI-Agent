// LeadChangeNotifier.java
package com.kbank.baa.batch.service;

import com.kbank.baa.admin.Member;
import com.kbank.baa.sports.GameMessageFormatter;
import com.kbank.baa.sports.RealtimeGameInfo;
import com.kbank.baa.sports.ScheduledGame;
import com.kbank.baa.telegram.TelegramProperties;
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
    private final TelegramProperties telegramProperties;
    private final TelegramService telegram;
    private final Map<String, String> leaderMap = new ConcurrentHashMap<>();

    public void notify(ScheduledGame schedule, List<Member> members, RealtimeGameInfo info) {
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
        log.info("########## 리더 변경 체크 → gameId={}, {} → {} ##########",
                gameId, prevLeader, currLeader);

        // 실제 변경이 있을 때만 알림 전송
        if (!currLeader.equals(prevLeader)) {
            members.stream()
                    .filter(m -> currLeader.equals(m.getSupportTeam().name()))
                    .forEach(m -> {
                        try {
                            String text = formatter.formatLeadChange(m, info, prevLeader, currLeader);
//                            telegram.sendMessage(m.getTelegramId(), text);
                            // 20250729 TEST
                            telegram.sendMessageWithMention(telegramProperties.getGroupChatId(), m.getTelegramId(), m.getName(), text);
                            log.info("########## 역전알림 전송 → member={} gameId={} ##########",
                                    m.getName(), gameId);
                        } catch (Exception e) {
                            log.error("########## 역전알림 에러 → member={} : {} ##########",
                                    m.getName(), e.getMessage(), e);
                        }
                    });
            log.info("########## 역전알림 완료 → gameId={} ##########", gameId);
        } else {
            log.info("########## 리더 변경 없음 → gameId={} (leader={}) ##########",
                    gameId, currLeader);
        }
    }
}
