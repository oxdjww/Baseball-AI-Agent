package com.kbank.baa.sports.dto;

import lombok.Data;

import java.util.List;

@Data
public class GameRosterResponseDto {
    private List<GamePlayersResponseDto.Player> homePlayers;
    private List<GamePlayersResponseDto.Player> awayPlayers;
}
