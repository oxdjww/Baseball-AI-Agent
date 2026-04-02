package com.kbank.kbaseball.notification.telegram;

/** @deprecated TelegramNotificationClient가 TelegramSendResult를 반환하도록 변경되어 더 이상 throw되지 않음. */
@Deprecated
public class BotBlockedException extends RuntimeException {

    private final String chatId;

    public BotBlockedException(String chatId, String message) {
        super(message);
        this.chatId = chatId;
    }

    public String getChatId() {
        return chatId;
    }
}
