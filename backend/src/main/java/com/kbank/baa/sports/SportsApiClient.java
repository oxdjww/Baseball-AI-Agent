package com.kbank.baa.sports;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SportsApiClient {
    private final RestTemplate restTemplate;

    /**
     * 주어진 gameId로 API 호출 후 GameInfo로 변환
     */
    public GameInfo fetchGameInfo(String gameId) {
        String url = "https://api-gw.sports.naver.com/schedule/games/" + gameId;
        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        JsonNode game = root.path("result").path("game");
        return GameInfo.builder()
                .inning(game.path("currentInning").asText())
                .homeTeam(game.path("homeTeamName").asText())
                .awayTeam(game.path("awayTeamName").asText())
                .homeScore(game.path("homeTeamScore").asInt())
                .awayScore(game.path("awayTeamScore").asInt())
                .build();
    }
}
