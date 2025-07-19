package com.kbank.baa.sports;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SportsApiClient {
    private final RestTemplate restTemplate;

    /**
     * 주어진 gameId로 API 호출 후 GameInfo로 변환
     */
    @Retryable(
            value = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
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

    @Recover
    public GameInfo recover(RestClientException ex, String gameId) {
        // 3회 재시도 후에도 실패했을 때 호출되는 메서드
        // 폴백으로 빈 데이터를 넘기거나 null 리턴
        return GameInfo.builder()
                .inning("정보 없음")
                .homeTeam("–")
                .awayTeam("–")
                .homeScore(0)
                .awayScore(0)
                .build();
    }
}
