package com.kbank.baa.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public void sendMessage(String chatId, String name, String text) {
        String url = props.getApiUrl() + "sendMessage";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        String textWithName = String.format("%s님, %s", name, text);
        body.add("chat_id", chatId);
        body.add("text", textWithName);
        body.add("parse_mode", "HTML");
        // 전송 시도 로깅
        log.info("→ [TelegramService] sendMessage 호출 시작: chatId={}, name={}, textPreview={}...",
                chatId, name,
                textWithName.length() > 50 ? textWithName.substring(0, 50) : textWithName);

        try {
            ResponseEntity<String> resp = rt.postForEntity(url, body, String.class);
            // 응답 로깅
            log.info("← [TelegramService] sendMessage 응답: {} 님께 메시지 전송 완료. statusCode={}",
                    name, resp.getStatusCodeValue());
        } catch (Exception ex) {
            // 예외 로깅
            log.error("✖ [TelegramService] sendMessage 실패: chatId={}, error={}", chatId, ex.getMessage(), ex);
        }
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
