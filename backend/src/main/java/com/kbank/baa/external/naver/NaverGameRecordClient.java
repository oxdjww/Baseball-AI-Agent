package com.kbank.baa.external.naver;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class NaverGameRecordClient {
    private static final String URL_TEMPLATE =
            "https://api-gw.sports.naver.com/schedule/games/{gameId}/record";

    private final RestTemplate restTemplate;

    public NaverGameRecordClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public JsonNode fetchRecordData(String gameId) {
        log.info("[NaverGameRecordClient][fetchRecordData] gameId={}", gameId);
        JsonNode root = restTemplate.getForObject(URL_TEMPLATE, JsonNode.class, gameId);
        if (root == null || !root.has("result")) {
            throw new IllegalStateException("기록 API 응답 없음: gameId=" + gameId);
        }
        return root.path("result").path("recordData");
    }
}
