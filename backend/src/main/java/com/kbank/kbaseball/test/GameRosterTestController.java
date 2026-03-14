package com.kbank.kbaseball.test;

import com.kbank.kbaseball.external.naver.NaverRosterClient;
import com.kbank.kbaseball.external.naver.dto.GamePlayersResponseDto.Player;
import com.kbank.kbaseball.external.naver.dto.GameRosterResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class GameRosterTestController {
    private final NaverRosterClient gameRosterClient;

    /**
     * 전체 로스터 테스트
     */
    @GetMapping("/roster")
    public List<Player> getGameRoster(@RequestParam String gameId) {
        return gameRosterClient.fetchGameRoster(gameId);
    }

    /**
     * 팀별 로스터 테스트
     */
    @GetMapping("/playersByTeam")
    public List<Player> getPlayersByTeam(
            @RequestParam String gameId,
            @RequestParam String teamName) {
        return gameRosterClient.fetchPlayersByTeam(gameId, teamName);
    }

    /**
     * 홈/어웨이 묶음 로스터 테스트
     */
    @GetMapping("/rosterDetail")
    public GameRosterResponseDto getGameRosterDetail(@RequestParam String gameId) {
        return gameRosterClient.fetchGameRosterDetail(gameId);
    }
}
