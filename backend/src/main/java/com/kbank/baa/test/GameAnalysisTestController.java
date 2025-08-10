package com.kbank.baa.test;

import com.kbank.baa.batch.tasklet.GameAnalysisTasklet;
import com.kbank.baa.sports.SportsApiClient;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import com.kbank.baa.sports.dto.ScheduledGameDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
public class GameAnalysisTestController {

    private final GameAnalysisTasklet gameAnalysisTasklet;
    private final SportsApiClient sportsApiClient;

    /**
     * 예시 호출:
     * GET /test/game-analysis?gameId=20250803LGSS02025
     */
    @GetMapping("/test/game-analysis")
    public String testGameAnalysis(@RequestParam String gameId) {
        // 1) gameId 앞 8자리(YYYYMMDD)로 경기 일자 파싱
        LocalDate gameDate = LocalDate.parse(
                gameId.substring(0, 8),
                DateTimeFormatter.BASIC_ISO_DATE
        );

        // 2) 해당 일자 스케줄에서 gameId 로 검색
        ScheduledGameDto schedule = sportsApiClient
                .fetchScheduledGames(gameDate, gameDate)
                .stream()
                .filter(g -> g.getGameId().equals(gameId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "찾을 수 없는 gameId: " + gameId + " on " + gameDate
                ));

        // 3) 실시간 정보 조회
        RealtimeGameInfoDto info = sportsApiClient.fetchGameInfo(gameId);

        // 4) Tasklet 실행
        gameAnalysisTasklet.execute(schedule, info);

        return "✅ GameAnalysisTasklet 호출 완료: " + gameId + " (" + gameDate + ")";
    }
}
