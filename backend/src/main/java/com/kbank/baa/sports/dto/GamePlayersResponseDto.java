package com.kbank.baa.sports.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GamePlayersResponseDto {
    private int code;
    private boolean success;
    private Result result;

    @Data
    public static class Result {
        private String gameId;
        private String homeTeamCode;
        private String homeTeamName;
        private List<Player> homeCandidates;
        private String awayTeamCode;
        private String awayTeamName;
        private List<Player> awayCandidates;

        /**
         * 주어진 팀명(teamName)에 해당하는 선수들만 반환
         */
        public List<Player> getPlayersByTeam(String teamName) {
            List<Player> players = new ArrayList<>();
            if (homeTeamName != null && homeTeamName.equals(teamName) && homeCandidates != null) {
                players.addAll(homeCandidates);
            }
            if (awayTeamName != null && awayTeamName.equals(teamName) && awayCandidates != null) {
                players.addAll(awayCandidates);
            }
            return players;
        }
    }

    @Data
    public static class Player {
        private String playerId;
        private String playerName;
        private String positionName;
        private String teamCode;
        private String teamName;
        private int totalCount;
        private int ratioPercent;
        private boolean selected;
        private int ranking;
        private int order;
    }
}
