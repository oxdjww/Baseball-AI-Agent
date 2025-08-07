package com.kbank.baa.sports.dto;

import lombok.Data;

import java.util.List;

@Data
public class GameRoster {
    private List<GamePlayersResponse.Player> homePlayers;
    private List<GamePlayersResponse.Player> awayPlayers;
}
