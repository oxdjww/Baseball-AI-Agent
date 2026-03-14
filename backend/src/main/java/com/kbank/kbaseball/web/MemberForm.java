package com.kbank.kbaseball.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberForm {
    private Long id;
    private String name;
    private String supportTeam;
    private String telegramId;
    private Boolean notifyGameAnalysis = false;
    private Boolean notifyRainAlert = false;
    private Boolean notifyRealTimeAlert = false;
}