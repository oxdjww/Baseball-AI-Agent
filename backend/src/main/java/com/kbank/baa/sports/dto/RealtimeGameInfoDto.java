package com.kbank.baa.sports.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RealtimeGameInfoDto {
    // — 스케줄·메타데이터
    private String gameId;               // ex. "20250720LTLG02025"
    private String memberName;           // ex. "홍길동"
    private LocalDateTime gameDateTime;  // ex. 2025-07-20T18:00:00
    private String statusCode;           // ex. "경기전", "진행중"
    private String stadium;              // ex. "잠실"
    private String homeTeamCode;         // ex. "LG"
    private String awayTeamCode;         // ex. "LT"
    private String homeTeamName;         // ex. "LG"
    private String awayTeamName;         // ex. "롯데"
    private String winner;

    // — 실시간 스코어
    private String inning;               // ex. "9회말"
    private int homeScore;               // ex. 0
    private int awayScore;               // ex. 1
}
