package com.kbank.baa.weather.service;

import com.kbank.baa.admin.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RainfallService {
    private static final String API_URL = "https://apihub.kma.go.kr/api/typ01/url/kma_sfctm2.php";
    private static final String COLUMN_RN = "RN";

    private final RestTemplate rest;

    @Value("${kma.api.key}")
    private String apiKey;

    /**
     * 팀 코드로 최근 강수량 조회 (STN 기반)
     */
    public double getRainfallByTeam(String teamCode) {
        Team team = Team.of(teamCode);
        String raw = fetchRawCsv(team.getStn());

        List<String> lines = List.of(raw.split("\\r?\\n")).stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                .toList();

        if (lines.size() < 2) {
            log.warn("강수량 데이터 없음 (team={}, resp={})", teamCode, raw);
            return 0.0;
        }

        String[] header = splitCsv(lines.get(0));
        String[] data = splitCsv(lines.get(1));
        int idx = findIndex(header, COLUMN_RN);

        if (idx < 0 || idx >= data.length) {
            log.error("RN 컬럼 누락 또는 인덱스 범위 초과 (idx={}, header={})", idx, lines.get(0));
            return 0.0;
        }

        return parseDouble(data[idx].trim(), 0.0);
    }

    private String fetchRawCsv(int stn) {
        return rest.getForObject(
                UriComponentsBuilder
                        .fromHttpUrl(API_URL)
                        .queryParam("stn", stn)
                        .queryParam("help", 0)
                        .queryParam("authKey", apiKey)
                        .toUriString(),
                String.class
        );
    }

    private String[] splitCsv(String line) {
        return line.split("\\s*,\\s*");
    }

    private int findIndex(String[] arr, String key) {
        for (int i = 0; i < arr.length; i++) {
            if (key.equals(arr[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private double parseDouble(String s, double fallback) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            log.warn("숫자 파싱 실패 '{}', default={}", s, fallback);
            return fallback;
        }
    }
}
