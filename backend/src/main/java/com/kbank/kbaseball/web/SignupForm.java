// SignupForm.java
package com.kbank.kbaseball.web;

import com.kbank.kbaseball.domain.team.Team;
import lombok.Data;

@Data
public class SignupForm {
    private String name;
    private Team supportTeam;
    private boolean notifyGameAnalysis;
    private boolean notifyRainAlert;
    private boolean notifyRealTimeAlert;
}

