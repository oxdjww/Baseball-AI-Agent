package com.kbank.kbaseball.notification.log;

import com.kbank.kbaseball.notification.telegram.dto.TelegramSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageLogService {

    private final MessageSendLogRepository repository;

    /**
     * 전송 이력을 저장한다.
     * REQUIRES_NEW: 호출자 트랜잭션과 독립 실행.
     * 이력 저장 실패가 봇 차단 처리 등 핵심 흐름을 중단시키지 않도록 예외를 swallow한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String telegramId, String messageText, TelegramSendResult result) {
        try {
            repository.save(MessageSendLog.of(telegramId, messageText, result));
        } catch (Exception e) {
            log.error("[MessageLogService] 전송 이력 저장 실패: telegramId={}, error={}",
                    telegramId, e.getMessage(), e);
        }
    }
}
