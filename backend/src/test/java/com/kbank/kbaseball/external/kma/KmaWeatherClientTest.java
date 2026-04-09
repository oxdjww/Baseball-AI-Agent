package com.kbank.kbaseball.external.kma;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * KmaWeatherClient CSV 파싱 로직 단위 테스트
 * RestTemplate을 Mock하여 외부 API 호출 없이 파싱 분기 검증
 */
@ExtendWith(MockitoExtension.class)
class KmaWeatherClientTest {

    @Mock
    RestTemplate rest;

    @InjectMocks
    KmaWeatherClient client;

    @BeforeEach
    void injectApiKey() {
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
    }

    // -- CSV 헬퍼: 실제 KMA 응답 포맷 모사 ----------------------------------
    private static String csv(String headerCols, String dataValues) {
        return "# YYMMDDHHMI " + headerCols + "\n"
                + "2603150900 " + dataValues + "\n";
    }

    // -----------------------------------------------------------------------

    @Test
    void 정상_강수량_파싱() {
        // RN = 2.5mm
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(csv("STN WD WS RN", "109 270 3.5 2.5"));

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(2.5);
    }

    @Test
    void 강수량_0mm_정상반환() {
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(csv("STN WD WS RN", "109 270 3.5 0.0"));

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(0.0);
    }

    @Test
    void RN이_마이너스9_관측값없음_0반환() {
        // -9 는 기상청 결측값 코드 → 0.0 처리
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(csv("STN WD WS RN", "109 270 3.5 -9.0"));

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(0.0);
    }

    @Test
    void RN_컬럼_없는_헤더_0반환() {
        // RN 컬럼이 아예 없는 경우
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(csv("STN WD WS PA TA", "109 270 3.5 1013.2 15.0"));

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(0.0);
    }

    @Test
    void 헤더_없는_응답_0반환() {
        // YYMMDDHHMI 포함 헤더 라인이 아예 없는 경우
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn("# Some other header\n2603150900 109 270 3.5 2.5\n");

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(0.0);
    }

    @Test
    void 데이터_라인_없는_응답_0반환() {
        // 헤더는 있지만 숫자로 시작하는 데이터 라인 없음
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn("# YYMMDDHHMI STN WD WS RN\n# comment only\n");

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(0.0);
    }

    @Test
    void null_응답_0반환() {
        when(rest.getForObject(anyString(), eq(String.class))).thenReturn(null);

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(0.0);
    }

    @Test
    void 여러_RN_컬럼_중_최댓값_반환() {
        // 헤더에 RN 이 두 번 나타나는 경우 (기상청 포맷 일부 구간에서 발생 가능)
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(csv("STN RN WS RN", "109 1.2 3.5 5.8"));

        double rain = client.getRainfallByTeam("LG", LocalDateTime.now());

        assertThat(rain).isEqualTo(5.8); // max of [1.2, 5.8]
    }

    @Test
    void 부산_팀코드_LT_정상조회() {
        // 팀 코드 → 기상청 지점 변환이 올바른지 확인 (STN=159)
        when(rest.getForObject(anyString(), eq(String.class)))
                .thenReturn(csv("STN WD WS RN", "159 180 2.0 3.0"));

        double rain = client.getRainfallByTeam("LT", LocalDateTime.now());

        assertThat(rain).isEqualTo(3.0);
    }
}
