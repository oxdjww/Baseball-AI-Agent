package com.kbank.baa.web;
import com.kbank.baa.domain.team.Team;
import lombok.Data;

@Data
public class LoginForm {
    private String name;
    private Team supportTeam;
}