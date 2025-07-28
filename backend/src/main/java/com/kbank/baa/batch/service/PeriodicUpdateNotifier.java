// PeriodicUpdateNotifier.java
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

@Component
@Slf4j
@RequiredArgsConstructor
public class PeriodicUpdateNotifier {
    private final GameMessageFormatter formatter;
    private final TelegramProperties telegramProperties;
    private final TelegramService telegram;

    public void notify(ScheduledGame schedule, List<Member> members, RealtimeGameInfo info) {
        var gameId = schedule.getGameId();
        log.info("########## 주기알림 시작 → gameId={} ##########", gameId);

        members.stream()
                .filter(m -> SupportUtils.isSupporting(m, schedule))
                .forEach(m -> {
                    try {
                        var text = formatter.format(m, info);
//                        telegram.sendMessage(m.getTelegramId(), text);
                        // 20250729 TEST
                        telegram.sendMessage(telegramProperties.getGroupChatId(), m.getTelegramId(), m.getName(), text);
                    } catch (Exception e) {
                        log.error("########## 주기알림 에러 → member={} : {} ##########", m.getName(), e.getMessage());
                    }
                });

        log.info("########## 주기알림 완료 → gameId={} ##########", gameId);
    }
}