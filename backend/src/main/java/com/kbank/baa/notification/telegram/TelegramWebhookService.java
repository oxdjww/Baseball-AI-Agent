package com.kbank.baa.notification.telegram;

import com.kbank.baa.auth.LinkResult;
import com.kbank.baa.auth.TelegramLinkService;
import com.kbank.baa.template.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookService {

    private final TelegramLinkService telegramLinkService;
    private final TelegramService telegramService;

    public void handle(String text, Long telegramUserId, Long chatId) {
        if (text.startsWith("/start ")) {
            handleStartCommand(text.substring(7).trim(), telegramUserId, chatId);
            return;
        }
        sendWelcomeGuide(chatId);
    }

    // @Transactional 밖에서 실행 — DB 커밋 완료 후 Telegram API 호출
    private void handleStartCommand(String token, Long telegramUserId, Long chatId) {
        LinkResult result = telegramLinkService.linkAccount(token, telegramUserId);

        if (result instanceof LinkResult.Linked linked) {
            telegramService.sendTemplateMessage(String.valueOf(chatId), linked.memberName(), NotificationTemplate.LINK_SUCCESS);
        } else if (result instanceof LinkResult.MemberNotFound) {
            telegramService.sendTemplateMessage(String.valueOf(chatId), "회원", NotificationTemplate.ACCOUNT_NOT_FOUND);
        } else if (result instanceof LinkResult.TokenExpired) {
            telegramService.sendTemplateMessage(String.valueOf(chatId), "회원", NotificationTemplate.TOKEN_EXPIRED);
        }
    }

    private void sendWelcomeGuide(Long chatId) {
        telegramService.sendTemplateMessage(
                String.valueOf(chatId),
                "회원",
                NotificationTemplate.WELCOME_GUIDE,
                "https://t.me/lIllllIIllllI"
        );
    }
}
