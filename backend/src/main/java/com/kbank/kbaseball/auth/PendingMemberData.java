package com.kbank.kbaseball.auth;

import com.kbank.kbaseball.domain.team.Team;

public record PendingMemberData(
        String name,
        Team supportTeam,
        boolean notifyGameAnalysis,
        boolean notifyRainAlert,
        boolean notifyRealTimeAlert
) {}
