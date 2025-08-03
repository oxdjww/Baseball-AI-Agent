package com.kbank.baa.weather.service;

import com.kbank.baa.admin.Team;
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
public class RainfallService {
    private static final String API_URL = "https://apihub.kma.go.kr/api/typ01/url/kma_sfctm2.php";
    private static final String COLUMN_RN = "RN";

    private final RestTemplate rest;

    @Value("${kma.api.key}")
    private String apiKey;

    public double getRainfallByTeam(String teamCode) {
        Team team = Team.of(teamCode);
        String raw = fetchRawCsv(team.getStn());
        log.info("Received: {}", raw);

        String[] header = null;
        String[] data = null;

        for (String line : raw.split("\\r?\\n")) {
            String t = line.stripLeading();
            // 1) 헤더 라인: "# YYMMDDHHMI STN ... RN RN RN"
            if (t.startsWith("#") && t.contains("YYMMDDHHMI")) {
                header = splitCsv(t.substring(1).trim());
            }
            // 2) 데이터 라인: 숫자로 시작하는 실제 관측값
            else if (!t.isEmpty() && Character.isDigit(t.charAt(0))) {
                data = splitCsv(t.trim());
            }
        }

        if (header == null || data == null) {
            log.warn("파싱 실패: header={} data={}", header, data);
            return 0.0;
        }

        // RN 컬럼 위치 전부 찾아서 최대값 추출
        double maxRain = -1.0;
        for (int i = 0; i < header.length; i++) {
            if (COLUMN_RN.equals(header[i]) && i < data.length) {
                double v = parseDouble(data[i], -1.0);
                if (v >= 0 && v > maxRain) {
                    maxRain = v;
                }
            }
        }

        if (maxRain < 0) {
            log.warn("RN 파싱 실패: header={} / data={}", String.join(" ", header), String.join(" ", data));
            return 0.0;
        }
        return maxRain;
    }

    private String formatTmToHour() {
        return LocalDateTime.now()
                .withMinute(0)
                .withSecond(0)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private String fetchRawCsv(int stn) {
        String tm = formatTmToHour();  // 매 시 정각으로 고정
        String url = UriComponentsBuilder
                .fromHttpUrl(API_URL)
                .queryParam("stn", stn)
                .queryParam("tm", tm)
                .queryParam("help", 0)
                .queryParam("authKey", apiKey)
                .toUriString();

        log.debug("KMA 요청 URL: {}", url);
        return rest.getForObject(url, String.class);
    }

    private String[] splitCsv(String line) {
        return line.split("\\s+");
    }

    private double parseDouble(String s, double fallback) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            log.warn("숫자 파싱 실패 '{}'", s);
            return fallback;
        }
    }
}
