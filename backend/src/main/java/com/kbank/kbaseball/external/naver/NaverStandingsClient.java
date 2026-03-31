package com.kbank.kbaseball.external.naver;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.kbaseball.external.naver.dto.KboStandingsResult;
import com.kbank.kbaseball.external.naver.dto.KboTeamStandingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class NaverStandingsClient {

    private final RestTemplate restTemplate;

    private String buildStandingsUrl() {
        int year = LocalDate.now().getYear();
        return "https://api-gw.sports.naver.com/statistics/categories/kbo/seasons/" + year + "/teams";
    }

    @Retryable(retryFor = RestClientException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000))
    public KboStandingsResult fetchStandings() {
        log.info("[NaverStandingsClient][fetchStandings] Fetching KBO standings");

        String url = buildStandingsUrl();
        ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode root = Optional.ofNullable(resp.getBody())
                .orElseThrow(() -> new RestClientException("API Fetch Failure: " + url));

        JsonNode result = root.path("result");
        String gameType = result.path("gameType").asText("REGULAR_SEASON");
        JsonNode teams = result.path("seasonTeamStats");

        List<KboTeamStandingDto> standings = new ArrayList<>();
        for (JsonNode t : teams) {
            standings.add(new KboTeamStandingDto(
                    t.path("teamId").asText(),
                    t.path("teamName").asText(),
                    t.path("ranking").asInt(),
                    t.path("winGameCount").asInt(),
                    t.path("drawnGameCount").asInt(),
                    t.path("loseGameCount").asInt(),
                    t.path("gameBehind").asDouble()
            ));
        }

        log.info("[NaverStandingsClient][fetchStandings] teamCodes: {}",
                standings.stream().map(KboTeamStandingDto::teamCode).toList());

        log.info("[NaverStandingsClient][fetchStandings] Fetched {} teams, gameType={}", standings.size(), gameType);
        return new KboStandingsResult(gameType, standings);
    }

    @SuppressWarnings("unused")
    @Recover
    public KboStandingsResult recover(RestClientException ex) {
        log.error("[NaverStandingsClient] fetchStandings 재시도 소진", ex);
        return null;
    }
}
