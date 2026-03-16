// src/main/java/com/kbank/kbaseball/test/RainfallTestController.java
package com.kbank.kbaseball.test;

import com.kbank.kbaseball.external.kma.KmaWeatherClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/rainfall")
@RequiredArgsConstructor
public class RainfallTestController {

    private final KmaWeatherClient rainfallService;


    @GetMapping("/{teamCode}")
    public ResponseEntity<?> getRainfall(@PathVariable String teamCode) {
        double mm = rainfallService.getRainfallByTeam(teamCode, LocalDateTime.now());
        return ResponseEntity.ok(Map.of(
                "teamCode", teamCode.toUpperCase(),
                "rainfall_mm", mm
        ));
    }
}
