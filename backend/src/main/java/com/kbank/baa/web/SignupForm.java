// SignupForm.java
package com.kbank.baa.web;

import com.kbank.baa.admin.Team;
import lombok.Data;

@Data
public class SignupForm {
    private String name;
    private Team supportTeam;
    private boolean notifyGameAnalysis;
    private boolean notifyRainAlert;
    private boolean notifyRealTimeAlert;
}

