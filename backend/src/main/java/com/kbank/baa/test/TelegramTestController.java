package com.kbank.baa.test;

import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TelegramTestController {

    private final TelegramService telegramService;

    /**
     * 테스트용 메시지 전송 엔드포인트
     * 예시 호출: /test/telegram?chatId=-1001234567890&text=Hello+from+Spring!
     */
    @GetMapping("/test/telegram")
    public String testSendTelegram(
            @RequestParam String chatId,
            @RequestParam String text
    ) {
        telegramService.sendMessage(chatId, text);
        return "✅ 메시지 전송 완료: chatId = " + chatId;
    }
}
