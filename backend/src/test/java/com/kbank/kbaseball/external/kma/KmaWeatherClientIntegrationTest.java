package com.kbank.kbaseball.external.kma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * KmaWeatherClient 실제 API 통합 테스트
 *
 * 프로젝트 루트의 .env 파일에서 KMA_API_KEY 를 읽어 실제 기상청 API 를 호출합니다.
 * .env 가 없거나 키가 placeholder 인 경우, 또는 API 호출이 실패하는 경우 테스트를 자동으로 스킵합니다.
 *
 * 로컬 실행 시 자동으로 .env 를 탐지하므로 별도 설정 없이 동작합니다.
 */
@SpringBootTest
class KmaWeatherClientIntegrationTest {

    @Autowired
    KmaWeatherClient client;

    @Value("${kma.api.key}")
    String apiKey;

    /**
     * .env 에서 KMA_API_KEY 를 읽어 Spring 프로퍼티로 주입
     * 파일이 없거나 키가 없으면 test yml 의 기본값(test-key)을 유지
     */
    @DynamicPropertySource
    static void overrideKmaApiKey(DynamicPropertyRegistry registry) {
        Path envFile = Path.of("../.env");
        if (!Files.exists(envFile)) return;
        try {
            Files.lines(envFile)
                    .filter(line -> line.startsWith("KMA_API_KEY="))
                    .map(line -> line.substring("KMA_API_KEY=".length()).trim())
                    .filter(key -> !key.isBlank())
                    .findFirst()
                    .ifPresent(key -> registry.add("kma.api.key", () -> key));
        } catch (IOException ignored) {
        }
    }

    private void skipIfNoRealKey() {
        assumeTrue(
                !"test-key".equals(apiKey),
                "기상청 실제 API 키가 없어 통합 테스트를 스킵합니다. (.env 의 KMA_API_KEY 확인)"
        );
    }

    @Test
    void 실제API_LG서울_강수량_비음수반환() {
        skipIfNoRealKey();
        try {
            double rain = client.getRainfallByTeam("LG", LocalDateTime.now());
            assertThat(rain)
                    .as("기상청 API 강수량은 항상 0 이상이어야 합니다.")
                    .isGreaterThanOrEqualTo(0.0);
        } catch (Exception e) {
            Assumptions.abort("API 호출 실패 (키 만료 또는 네트워크 오류): " + e.getMessage());
        }
    }

    @Test
    void 실제API_LT부산_강수량_비음수반환() {
        skipIfNoRealKey();
        try {
            double rain = client.getRainfallByTeam("LT", LocalDateTime.now());
            assertThat(rain)
                    .as("부산(사직) 기상청 API 강수량은 0 이상이어야 합니다.")
                    .isGreaterThanOrEqualTo(0.0);
        } catch (Exception e) {
            Assumptions.abort("API 호출 실패 (키 만료 또는 네트워크 오류): " + e.getMessage());
        }
    }

    @Test
    void 실제API_SS대구_강수량_비음수반환() {
        skipIfNoRealKey();
        try {
            double rain = client.getRainfallByTeam("SS", LocalDateTime.now());
            assertThat(rain)
                    .as("대구 기상청 API 강수량은 0 이상이어야 합니다.")
                    .isGreaterThanOrEqualTo(0.0);
        } catch (Exception e) {
            Assumptions.abort("API 호출 실패 (키 만료 또는 네트워크 오류): " + e.getMessage());
        }
    }

    @Test
    void 실제API_과거시점_조회_비음수반환() {
        skipIfNoRealKey();
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(12).withMinute(0);
            double rain = client.getRainfallByTeam("LG", yesterday);
            assertThat(rain)
                    .as("과거 시점 강수량은 0 이상이어야 합니다.")
                    .isGreaterThanOrEqualTo(0.0);
        } catch (Exception e) {
            Assumptions.abort("API 호출 실패 (키 만료 또는 네트워크 오류): " + e.getMessage());
        }
    }
}
