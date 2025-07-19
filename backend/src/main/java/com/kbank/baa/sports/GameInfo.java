package com.kbank.baa.sports;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameInfo {
    private String inning;      // ex. "9회말"
    private String homeTeam;    // ex. "키움"
    private String awayTeam;    // ex. "삼성"
    private int homeScore;      // ex. 0
    private int awayScore;      // ex. 1
}
