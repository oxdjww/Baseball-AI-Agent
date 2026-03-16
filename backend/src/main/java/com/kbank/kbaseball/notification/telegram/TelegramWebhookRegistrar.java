package com.kbank.kbaseball.notification.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramWebhookRegistrar {

    private final TelegramProperties telegramProperties;
    private final RestTemplateBuilder restTemplateBuilder;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        String webhookUrl = telegramProperties.getWebhookUrl();

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[TelegramWebhookRegistrar] webhook-url 미설정 — webhook 등록 건너뜀");
            return;
        }

        String apiUrl = telegramProperties.getApiUrl() + "setWebhook";
        log.info("[TelegramWebhookRegistrar] → setWebhook: {}", webhookUrl);
        try {
            RestTemplate rt = restTemplateBuilder.build();
            ResponseEntity<String> resp = rt.postForEntity(apiUrl, Map.of("url", webhookUrl), String.class);
            log.info("[TelegramWebhookRegistrar] ← ok: status={}, body={}", resp.getStatusCode(), resp.getBody());
        } catch (Exception e) {
            log.error("[TelegramWebhookRegistrar] ✖ setWebhook 실패: {}", e.getMessage(), e);
        }
    }
}
