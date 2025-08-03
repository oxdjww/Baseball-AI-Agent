// src/main/java/com/kbank/baa/weather/controller/TestRainfallController.java
package com.kbank.baa.test;

import com.kbank.baa.weather.service.RainfallService;
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

    private final RainfallService rainfallService;


    @GetMapping("/{teamCode}")
    public ResponseEntity<?> getRainfall(@PathVariable String teamCode) {
        double mm = rainfallService.getRainfallByTeam(teamCode, LocalDateTime.now());
        return ResponseEntity.ok(Map.of(
                "teamCode", teamCode.toUpperCase(),
                "rainfall_mm", mm
        ));
    }
}
