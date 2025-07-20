package com.kbank.baa.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class TelegramService {
    private final TelegramProperties props;
    private final RestTemplate rt = new RestTemplate();

    // 멤버별 chatId로 전송
    public void sendMessage(String chatId, String text) {
        String url = props.getApiUrl() + "sendMessage";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("text", text);
        rt.postForEntity(url, body, String.class);
    }

    // 기존 기본 chatId 호환용
    public void sendMessage(String text) {
        sendMessage(props.getChatId(), text);
    }
}
