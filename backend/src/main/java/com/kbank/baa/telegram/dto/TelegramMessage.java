package com.kbank.baa.telegram.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


@Value
@Builder
public class TelegramMessage {
    String chatId;
    String text;
    @Builder.Default
    ParseMode parseMode = ParseMode.HTML;
    @Builder.Default
    boolean disableWebPagePreview = false;
    @Builder.Default
    boolean disableNotification = false;

    String replyMarkupJson;

    /**
     * Telegram API(form) 요청 본문으로 변환
     */
    public MultiValueMap<String, String> toFormData() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        body.add("text", text);
        body.add("parse_mode", parseMode.name());
        body.add("disable_web_page_preview", String.valueOf(disableWebPagePreview));
        body.add("disable_notification", String.valueOf(disableNotification));
        if (replyMarkupJson != null && !replyMarkupJson.isBlank()) {
            body.add("reply_markup", replyMarkupJson);
        }
        return body;
    }
}
