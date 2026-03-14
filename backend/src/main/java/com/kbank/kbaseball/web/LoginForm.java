package com.kbank.kbaseball.web;
import com.kbank.kbaseball.domain.team.Team;
import lombok.Data;

@Data
public class LoginForm {
    private String name;
    private Team supportTeam;
}