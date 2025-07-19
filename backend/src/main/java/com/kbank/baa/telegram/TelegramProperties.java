package com.kbank.baa.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
@Data
public class TelegramProperties {
    private String botToken;
    private String chatId;

    public String getApiUrl() {
        return "https://api.telegram.org/bot" + botToken + "/";
    }
}
