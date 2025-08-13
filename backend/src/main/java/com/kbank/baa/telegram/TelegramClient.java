package com.kbank.baa.telegram;

import com.kbank.baa.telegram.dto.TelegramMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramClient {

    private final TelegramProperties props;
    private final RestTemplateBuilder restTemplateBuilder;

    private RestTemplate rt() {
        // timeout, interceptor 등 공통 옵션 여기서 세팅 가능
        return restTemplateBuilder.build();
    }

    public void sendMessage(TelegramMessage message) {
        final String url = props.getApiUrl() + "sendMessage";

        String preview = message.getText().length() > 80
                ? message.getText().substring(0, 80) + "…"
                : message.getText();

        log.info("[TelegramClient][sendMessage] → sendMessage: chatId={}, preview={}", message.getChatId(), preview);
        try {
            ResponseEntity<String> resp = rt().postForEntity(url, message.toFormData(), String.class);
            log.info("[TelegramClient][sendMessage] ← ok: chatId={}, status={}", message.getChatId(), resp.getStatusCodeValue());
        } catch (Exception e) {
            log.error("[TelegramClient][sendMessage] ✖ fail: chatId={}, error={}", message.getChatId(), e.getMessage(), e);
        }
    }
}
