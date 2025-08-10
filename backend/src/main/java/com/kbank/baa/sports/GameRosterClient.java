package com.kbank.baa.sports;

import com.kbank.baa.sports.dto.GamePlayersResponseDto;
import com.kbank.baa.sports.dto.GamePlayersResponseDto.Player;
import com.kbank.baa.sports.dto.GameRosterResponseDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GameRosterClient {
    private static final String URL_TEMPLATE =
            "https://api-gw.sports.naver.com/poll/bestPlayer/kbo/{gameId}/status";

    private final RestTemplate restTemplate;

    // RestTemplateBuilder 로 RestTemplate 빈을 생성
    public GameRosterClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    /**
     * 전체 홈·어웨이 로스터 반환
     */
    public List<Player> fetchGameRoster(String gameId) {
        GamePlayersResponseDto resp = callApi(gameId);
        GamePlayersResponseDto.Result result = resp.getResult();

        List<Player> roster = new ArrayList<>();
        if (result.getHomeCandidates() != null) roster.addAll(result.getHomeCandidates());
        if (result.getAwayCandidates() != null) roster.addAll(result.getAwayCandidates());
        return roster;
    }

    /**
     * 특정 팀(homeTeamName 또는 awayTeamName)에 소속된 선수만 반환
     */
    public List<Player> fetchPlayersByTeam(String gameId, String teamName) {
        GamePlayersResponseDto.Result result = callApi(gameId).getResult();
        return result.getPlayersByTeam(teamName);
    }

    /**
     * 주어진 gameId, teamName에 해당하는 선수 이름 리스트만 반환
     */
    public List<String> fetchPlayerNamesByTeam(String gameId, String teamName) {
        return fetchPlayersByTeam(gameId, teamName).stream()
                .map(Player::getPlayerName)
                .collect(Collectors.toList());
    }

    /**
     * 홈/어웨이 묶음 DTO 반환
     */
    public GameRosterResponseDto fetchGameRosterDetail(String gameId) {
        GamePlayersResponseDto.Result result = callApi(gameId).getResult();
        GameRosterResponseDto roster = new GameRosterResponseDto();
        roster.setHomePlayers(result.getHomeCandidates());
        roster.setAwayPlayers(result.getAwayCandidates());
        return roster;
    }

    /**
     * 공통 API 호출 로직
     */
    private GamePlayersResponseDto callApi(String gameId) {
        GamePlayersResponseDto resp = restTemplate
                .getForObject(URL_TEMPLATE, GamePlayersResponseDto.class, gameId);
        if (resp == null || !resp.isSuccess()) {
            throw new IllegalStateException("API 호출 실패: gameId=" + gameId);
        }
        return resp;
    }
}
