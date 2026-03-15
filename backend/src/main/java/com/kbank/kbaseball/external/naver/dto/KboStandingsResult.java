package com.kbank.kbaseball.external.naver.dto;

import java.util.List;

public record KboStandingsResult(
        String gameType,
        List<KboTeamStandingDto> standings
) {}
