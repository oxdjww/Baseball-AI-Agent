package com.kbank.kbaseball.external.naver;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.kbaseball.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.kbaseball.external.naver.dto.ScheduledGameDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class NaverSportsClient {
    private final RestTemplate restTemplate;

    // 당일의 진행 경기 정보를 가져오는 함수
    @Retryable(retryFor = RestClientException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000))
    public List<ScheduledGameDto> fetchScheduledGames(LocalDate from, LocalDate to) {
        String url = UriComponentsBuilder
                .fromUriString("https://api-gw.sports.naver.com/schedule/games")
                .queryParam("fields", "basic,schedule,baseball")
                .queryParam("upperCategoryId", "kbaseball")
                .queryParam("categoryId", "kbo")
                .queryParam("fromDate", from)
                .queryParam("toDate", to)
                .toUriString();

        log.info("[NaverSportsClient][fetchScheduledGames] Generated Url: {}", url);

        ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode root = Optional.ofNullable(resp.getBody())
                .orElseThrow(() -> new RestClientException("########## API Fetch Failure: " + url));
        JsonNode gamesNode = root.path("result").path("games");


        List<ScheduledGameDto> list = new ArrayList<>();
        for (JsonNode g : gamesNode) {
            list.add(ScheduledGameDto.builder()
                    .gameId(g.path("gameId").asText())
                    .homeTeamCode(g.path("homeTeamCode").asText())
                    .awayTeamCode(g.path("awayTeamCode").asText())
                    .homeTeamName(g.path("homeTeamName").asText())
                    .awayTeamName(g.path("awayTeamName").asText())
                    .stadium(g.path("stadium").asText())
                    .gameDateTime(LocalDateTime.parse(g.path("gameDateTime").asText()))
                    .build());
        }
        return list;
    }


    // 게임ID를 기반으로 경기 정보 단건 조회
    @Retryable(retryFor = RestClientException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000))
    public RealtimeGameInfoDto fetchGameInfo(String gameId) {
        String url = "https://api-gw.sports.naver.com/schedule/games/" + gameId;
        ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode root = Optional.ofNullable(resp.getBody())
                .orElseThrow(() -> new RestClientException("########## API Fetch Failure: " + url));
        JsonNode g = root.path("result").path("game");


        return RealtimeGameInfoDto.builder()
                .gameId(g.path("gameId").asText())
                .gameDateTime(LocalDateTime.parse(g.path("gameDateTime").asText()))
                .statusCode(g.path("statusCode").asText())
                .stadium(g.path("stadium").asText())
                .homeTeamCode(g.path("homeTeamCode").asText())
                .awayTeamCode(g.path("awayTeamCode").asText())
                .homeTeamName(g.path("homeTeamName").asText())
                .awayTeamName(g.path("awayTeamName").asText())
                .winner(g.path("winner").asText())
                .inning(g.path("currentInning").asText())
                .homeScore(g.path("homeTeamScore").asInt())
                .awayScore(g.path("awayTeamScore").asInt())
                .isCanceled(g.path("cancel").asBoolean())
                .build();
    }

    // 게임 ID를 기반으로 경기의 취소 여부만 조회
    @Retryable(retryFor = RestClientException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000))
    public boolean fetchCancelInfoFromGameInfo(String gameId) {
        String url = "https://api-gw.sports.naver.com/schedule/games/" + gameId;
        ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
        JsonNode root = Optional.ofNullable(resp.getBody())
                .orElseThrow(() -> new RestClientException("########## API Fetch Failure: " + url));
        String cancelInfo = root.path("result").path("game").path("cancel").asText();
        return cancelInfo.equals("true");
    }

    // 재시도시 recover하는 함수
    @SuppressWarnings("unused")
    @Recover
    public List<ScheduledGameDto> recover(RestClientException ex, LocalDate from, LocalDate to) {
        return List.of();
    }

    @SuppressWarnings("unused")
    @Recover
    public RealtimeGameInfoDto recoverGameInfo(RestClientException ex, String gameId) {
        log.error("[NaverSportsClient] fetchGameInfo 재시도 소진, gameId={}", gameId, ex);
        return null;
    }

    @SuppressWarnings("unused")
    @Recover
    public boolean recoverCancelInfo(RestClientException ex, String gameId) {
        log.error("[NaverSportsClient] fetchCancelInfoFromGameInfo 재시도 소진, gameId={}", gameId, ex);
        return false;
    }
}
