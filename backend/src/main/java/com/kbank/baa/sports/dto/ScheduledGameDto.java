// com/kbank/baa/sports/ScheduledGame.java
package com.kbank.baa.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScheduledGameDto {
    private String gameId;
    private String homeTeamCode;
    private String awayTeamCode;
    private String homeTeamName;
    private String awayTeamName;
    private String stadium;
    private LocalDateTime gameDateTime;
}
