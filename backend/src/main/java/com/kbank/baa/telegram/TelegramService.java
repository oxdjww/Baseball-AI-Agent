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

    public void sendMessage(String text) {
        String url = props.getApiUrl() + "sendMessage";
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", props.getChatId());
        body.add("text", text);
        rt.postForEntity(url, body, String.class);
    }
}
