package com.kbank.baa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {
    @Bean
    public org.springframework.data.auditing.DateTimeProvider auditingDateTimeProvider() {
        return () -> java.util.Optional.of(java.time.Instant.now());
    }
}
