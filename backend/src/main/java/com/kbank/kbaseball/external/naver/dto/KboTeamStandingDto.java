package com.kbank.kbaseball.external.naver.dto;

public record KboTeamStandingDto(
        String teamCode,
        String teamName,
        int rank,
        int wins,
        int draws,
        int losses,
        double gameBehind
) {}
