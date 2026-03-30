package com.kbank.kbaseball.notification.telegram;

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
