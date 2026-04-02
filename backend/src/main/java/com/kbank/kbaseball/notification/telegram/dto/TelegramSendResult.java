package com.kbank.kbaseball.notification.telegram.dto;

/**
 * TelegramNotificationClient.sendMessage() 호출 결과를 담는 값 객체.
 *
 * 성공:           statusCode=200, errorMessage=null, botBlocked=false
 * 봇 차단:        statusCode=403, errorMessage=원본 메시지, botBlocked=true
 * 기타 4xx:       statusCode=해당 코드, errorMessage=원본 메시지, botBlocked=false
 * 네트워크 예외:  statusCode=null, errorMessage=예외 메시지, botBlocked=false
 */
public record TelegramSendResult(
        Integer statusCode,
        String  errorMessage,
        boolean botBlocked
) {
    public static TelegramSendResult success(int statusCode) {
        return new TelegramSendResult(statusCode, null, false);
    }

    public static TelegramSendResult botBlocked(int statusCode, String errorMessage) {
        return new TelegramSendResult(statusCode, errorMessage, true);
    }

    public static TelegramSendResult failure(int statusCode, String errorMessage) {
        return new TelegramSendResult(statusCode, errorMessage, false);
    }

    public static TelegramSendResult exception(String errorMessage) {
        return new TelegramSendResult(null, errorMessage, false);
    }
}
