package com.kbank.kbaseball.external.kma;

import com.kbank.kbaseball.domain.team.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class KmaWeatherClient {
    private static final String API_URL = "https://apihub.kma.go.kr/api/typ01/url/kma_sfctm2.php";
    private static final String COLUMN_RN = "RN";

    private final RestTemplate rest;

    @Value("${kma.api.key}")
    private String apiKey;

    /**
     * *알림 시점* 기준으로 강수량 조회
     */
    public double getRainfallByTeam(String teamCode, LocalDateTime baseTime) {
        Team team = Team.of(teamCode);
        String raw = fetchRawCsv(team.getStn(), baseTime);
        log.info("[KmaWeatherClient][getRainfallByTeam] Received raw CSV:\n{}", raw);

        String[] header = null;
        String[] data = null;

        for (String line : raw.split("\\r?\\n")) {
            String t = line.stripLeading();
            if (t.startsWith("#") && t.contains("YYMMDDHHMI")) {
                header = splitCsv(t.substring(1).trim());
            } else if (!t.isEmpty() && Character.isDigit(t.charAt(0))) {
                data = splitCsv(t.trim());
            }
        }

        // ◆ 관측값이 없으면 정상(0.0) 처리
        if (header == null || data == null) {
            log.info("[KmaWeatherClient][getRainfallByTeam] 강수량 데이터 없음(정상): header={} data={}", header, data);
            return 0.0;
        }

        // RN 컬럼 최대값 찾기
        double maxRain = -1.0;
        for (int i = 0; i < header.length; i++) {
            if (COLUMN_RN.equals(header[i]) && i < data.length) {
                double v = parseDouble(data[i], -1.0);
                if (v >= 0 && v > maxRain) {
                    maxRain = v;
                }
            }
        }

        // ◆ RN 컬럼이 하나도 없으면 정상(0.0) 처리
        if (maxRain < 0) {
            log.info("[KmaWeatherClient][getRainfallByTeam] RN 컬럼 값 없음(정상): header={} / data={}",
                    String.join(" ", header),
                    String.join(" ", data));
            return 0.0;
        }

        return maxRain;
    }

    /**
     * 정각 `baseTime` 을 이용해 tm 파라미터까지 지정
     */
    private String fetchRawCsv(int stn, LocalDateTime baseTime) {
        String tm = baseTime
                .withMinute(0)
                .withSecond(0)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

        String url = UriComponentsBuilder
                .fromHttpUrl(API_URL)
                .queryParam("stn", stn)
                .queryParam("tm", tm)            // ★ 여기에 tm 추가
                .queryParam("help", 0)
                .queryParam("authKey", apiKey)
                .toUriString();

        log.info("[KmaWeatherClient][fetchRawCsv] KMA 요청 URL: {}", url);
        String result = rest.getForObject(url, String.class);
        return result != null ? result : "";
    }

    private String[] splitCsv(String line) {
        return line.split("\\s+");
    }

    private double parseDouble(String s, double fallback) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            log.warn("[KmaWeatherClient][parseDouble] 숫자 파싱 실패 '{}'", s);
            return fallback;
        }
    }
}
