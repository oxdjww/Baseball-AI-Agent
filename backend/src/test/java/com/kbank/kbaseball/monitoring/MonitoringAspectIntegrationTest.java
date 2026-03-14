package com.kbank.kbaseball.monitoring;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import com.kbank.kbaseball.notification.telegram.TelegramNotificationClient;
import com.kbank.kbaseball.notification.telegram.dto.TelegramMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@SpringBootTest
class MonitoringAspectIntegrationTest {

    @Autowired
    FeatureToggleService featureToggleService;

    @MockBean
    TelegramNotificationClient telegramNotificationClient;

    @MockBean
    StringRedisTemplate stringRedisTemplate;

    @Test
    void AOP_서비스예외_이벤트발행_관리자알림_전송() {
        assertThatThrownBy(() -> featureToggleService.toggle("UNKNOWN_KEY"))
                .isInstanceOf(IllegalArgumentException.class);

        ArgumentCaptor<TelegramMessage> captor = ArgumentCaptor.forClass(TelegramMessage.class);
        verify(telegramNotificationClient).sendMessage(captor.capture());

        TelegramMessage sent = captor.getValue();
        assertThat(sent.getChatId()).isEqualTo("000000000");
        assertThat(sent.getText()).contains("[🚨 SYSTEM CRITICAL]");
        assertThat(sent.getText()).contains("FeatureToggleService");
        assertThat(sent.getText()).contains("UNKNOWN_KEY");
    }
}
