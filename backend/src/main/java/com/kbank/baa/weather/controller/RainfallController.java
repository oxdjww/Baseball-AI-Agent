// src/main/java/com/kbank/baa/weather/controller/TestRainfallController.java
package com.kbank.baa.weather.controller;

import com.kbank.baa.weather.service.RainfallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rainfall")
@RequiredArgsConstructor
public class RainfallController {

    private final RainfallService rainfallService;


    @GetMapping("/{teamCode}")
    public ResponseEntity<?> getRainfall(@PathVariable String teamCode) {
        double mm = rainfallService.getRainfallByTeam(teamCode);
        return ResponseEntity.ok(Map.of(
                "teamCode", teamCode.toUpperCase(),
                "rainfall_mm", mm
        ));
    }
}
