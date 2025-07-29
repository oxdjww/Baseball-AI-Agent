package com.kbank.baa.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramService {
    private final TelegramProperties props;
    private final RestTemplate rt = new RestTemplate();

    // 멤버별 chatId로 전송
    public void sendMessage(String chatId, String text) {
        String url = props.getApiUrl() + "sendMessage";
        log.info("##### Telegram으로 메시지 전송 시도: url={}, chatId={}, text={}", url, chatId, text);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("text", text);
        body.add("parse_mode", "HTML");

        rt.postForEntity(url, body, String.class);
    }

    // 단체방 태그 전송
    public void sendMessageWithMention(String chatId, String mentionId, String name, String text) {
        String url = props.getApiUrl() + "sendMessage";

        // mentionId가 null이거나 비어있지 않다면 멘션 처리
        String formattedText;
        if (mentionId != null && !mentionId.isBlank()) {
            formattedText = String.format("👤 <a href=\"tg://user?id=%s\">%s</a> %s", mentionId, name, text);
        } else {
            formattedText = text; // 멘션이 없으면 그대로 전송
        }

        log.info("########## Telegram으로 메시지 전송 시도: chatId={}, mentionId={}, text={}", chatId, mentionId, formattedText);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("text", formattedText);
        body.add("parse_mode", "HTML");

        rt.postForEntity(url, body, String.class);
    }
}
