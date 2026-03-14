package com.kbank.kbaseball.notification.telegram;

import com.kbank.kbaseball.notification.telegram.TelegramWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/telegram")
@Slf4j
public class TelegramWebhookController {

    private final TelegramWebhookService telegramWebhookService;

    @PostMapping("/webhook")
    public void onUpdate(@RequestBody Map<String, Object> update) {
        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return;

        Map<String, Object> from = (Map<String, Object>) message.get("from");
        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        String text = (String) message.getOrDefault("text", "");

        log.info("[TelegramWebhookController][onUpdate] text={}", text);

        Long telegramUserId = ((Number) from.get("id")).longValue();
        Long chatId = ((Number) chat.get("id")).longValue();

        telegramWebhookService.handle(text, telegramUserId, chatId);
    }
}
