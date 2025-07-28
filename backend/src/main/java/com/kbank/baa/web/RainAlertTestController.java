package com.kbank.baa.web;

import com.kbank.baa.batch.tasklet.RainAlertTasklet;
import com.kbank.baa.sports.ScheduledGame;
import com.kbank.baa.sports.SportsApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class RainAlertTestController {

    private final SportsApiClient apiClient;
    private final RainAlertTasklet rainTasklet;

    /**
     * today(yyyy-MM-dd) 날짜의 특정 gameId에 대해
     * 수동으로 1h/3h 전 우천 알림 로직을 바로 실행해 봅니다.
     * <p>
     * e.g. GET /test/rain-alert?date=2025-07-23&gameId=20250723LGHT02025&hoursBefore=1&threshold=5
     */
    @GetMapping("/rain-alert")
    public ResponseEntity<String> testRainAlert(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String gameId,
            @RequestParam int hoursBefore,
            @RequestParam double threshold
    ) {
        Optional<ScheduledGame> maybe = apiClient.fetchScheduledGames(date, date).stream()
                .filter(g -> g.getGameId().equals(gameId))
                .findFirst();

        if (maybe.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("해당 날짜·gameId에 대한 경기를 찾을 수 없습니다.");
        }

        rainTasklet.executeForGame(maybe.get(), hoursBefore, threshold);
        return ResponseEntity.ok("✔ 수동 알림 로직 실행 완료 for game="
                + gameId + ", " + hoursBefore + "h, threshold=" + threshold + "mm");
    }

    @GetMapping("/test/rain-alert")
    public String testAlert() {
        // 오늘 첫 번째 경기 하나를 불러와서
        List<ScheduledGame> scheduledGames = apiClient.fetchScheduledGames(LocalDate.now().plusDays(1), LocalDate.now().plusDays(1));
        // “1시간 전” 기준으로 즉시 실행
        for (ScheduledGame scheduledGame : scheduledGames) {
            rainTasklet.executeForGame(scheduledGame, 1, /*thresholdMm=*/5);
        }
        return "Rain alert sent (check logs)";
    }
}
