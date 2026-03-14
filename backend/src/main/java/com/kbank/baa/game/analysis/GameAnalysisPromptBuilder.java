package com.kbank.baa.game.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.baa.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.baa.external.naver.dto.ScheduledGameDto;
import org.springframework.stereotype.Component;

@Component
public class GameAnalysisPromptBuilder {

    public String resolveWinner(RealtimeGameInfoDto info) {
        if (info.getAwayScore() > info.getHomeScore()) return info.getAwayTeamName();
        if (info.getHomeScore() > info.getAwayScore()) return info.getHomeTeamName();
        return "무승부";
    }

    public String resolveLoser(RealtimeGameInfoDto info) {
        if (info.getAwayScore() > info.getHomeScore()) return info.getHomeTeamName();
        if (info.getHomeScore() > info.getAwayScore()) return info.getAwayTeamName();
        return "무승부";
    }

    public String build(ScheduledGameDto schedule, RealtimeGameInfoDto info,
                        JsonNode recordData, String awayPlayers, String homePlayers) {
        String winner = resolveWinner(info);
        String loser = resolveLoser(info);
        int awayScore = info.getAwayScore();
        int homeScore = info.getHomeScore();

        String etcRecords = recordData.path("etcRecords").toString();
        String todayKeyStats = recordData.path("todayKeyStats").toString();
        String pitchingResult = recordData.path("pitchingResult").toString();
        String scoreBoard = recordData.path("scoreBoard").toString();
        String battersBoxscore = recordData.path("battersBoxscore").toString();
        String pitchersBoxscore = recordData.path("pitchersBoxscore").toString();
        String teamPitchingBoxscore = recordData.path("teamPitchingBoxscore").toString();
        String homeStandings = recordData.path("homeStandings").toString();
        String awayStandings = recordData.path("awayStandings").toString();
        String gameInfo = recordData.path("gameInfo").toString();

        if (!"무승부".equals(winner)) {
            return buildWinnerPrompt(schedule, info, awayScore, homeScore,
                    winner, loser, awayPlayers, homePlayers,
                    etcRecords, todayKeyStats, pitchingResult, scoreBoard,
                    battersBoxscore, pitchersBoxscore, teamPitchingBoxscore,
                    homeStandings, awayStandings, gameInfo);
        } else {
            return buildDrawPrompt(schedule, info, awayScore, homeScore,
                    awayPlayers, homePlayers,
                    etcRecords, todayKeyStats, pitchingResult, scoreBoard,
                    battersBoxscore, pitchersBoxscore, teamPitchingBoxscore,
                    homeStandings, awayStandings, gameInfo);
        }
    }

    private String buildWinnerPrompt(ScheduledGameDto schedule, RealtimeGameInfoDto info,
                                     int awayScore, int homeScore,
                                     String winner, String loser,
                                     String awayPlayers, String homePlayers,
                                     String etcRecords, String todayKeyStats, String pitchingResult,
                                     String scoreBoard, String battersBoxscore, String pitchersBoxscore,
                                     String teamPitchingBoxscore, String homeStandings,
                                     String awayStandings, String gameInfo) {
        return String.format(
                "※ **응답에서는 'etcRecords', 'todayKeyStats', 'pitchingResult' 등 같은 변수명이나 JSON 필드를 일절 언급하지 말고**, 자연스럽고 깔끔한 한국어 문장으로만 요약해 주세요.\n\n" +
                        "%s 경기의 상세 JSON 데이터입니다.\n" +
                        "최종 스코어: %s %d : %d %s, 승리팀은 %s, 패배팀은 %s입니다.\n\n" +
                        "아래 JSON 필드를 참고하여, 승리팀과 패배팀의 주요 요인(결정적 사건, 핵심 지표, 결정 투수 성과 등)을 자유롭게 파악한 뒤\n" +
                        "다음 형식으로 간결히 요약·정리해 주세요. 요인 개수에 제한은 없습니다.\n" +
                        "※ **중요**: 선수의 소속팀을 신중히 고려하여 요인을 작성해 주세요.\n" +
                        "※ **중요**: JSON 외의 정보는 절대 임의로 생성하지 마세요..\n" +
                        "1) 승리팀의 주요 승리 요인\n" +
                        "- …\n\n" +
                        "2) 패배팀의 주요 패배 요인\n" +
                        "- …\n\n" +
                        "etcRecords: %s\n\n" +
                        "todayKeyStats: %s\n\n" +
                        "pitchingResult: %s\n\n" +
                        "scoreBoard: %s\n\n" +
                        "battersBoxscore: %s\n\n" +
                        "pitchersBoxscore: %s\n\n" +
                        "teamPitchingBoxscore: %s\n\n" +
                        "homeStandings: %s\n\n" +
                        "awayStandings: %s\n\n" +
                        "gameInfo: %s\n\n" +
                        "%s team members: %s\n\n" +
                        "%s team members: %s\n\n",
                schedule.getStadium(),
                info.getAwayTeamName(), awayScore, homeScore, info.getHomeTeamName(),
                winner, loser,
                etcRecords, todayKeyStats, pitchingResult, scoreBoard,
                battersBoxscore, pitchersBoxscore, teamPitchingBoxscore,
                homeStandings, awayStandings, gameInfo,
                schedule.getAwayTeamName(), awayPlayers,
                schedule.getHomeTeamName(), homePlayers
        );
    }

    private String buildDrawPrompt(ScheduledGameDto schedule, RealtimeGameInfoDto info,
                                   int awayScore, int homeScore,
                                   String awayPlayers, String homePlayers,
                                   String etcRecords, String todayKeyStats, String pitchingResult,
                                   String scoreBoard, String battersBoxscore, String pitchersBoxscore,
                                   String teamPitchingBoxscore, String homeStandings,
                                   String awayStandings, String gameInfo) {
        return String.format(
                "※ **응답에서는 변수명이나 JSON 필드를 언급하지 말고**, 자연스럽고 깔끔한 한국어로 요약해 주세요.\n\n" +
                        "%s 경기의 상세 JSON 데이터입니다.\n" +
                        "최종 스코어: %s %d : %d %s, 결과는 무승부입니다.\n\n" +
                        "아래 JSON 필드를 참고하여, 양 팀의 주요 활약 포인트를 자유롭게 정리해 주세요.\n" +
                        "※ **중요**: 선수의 소속팀을 신중히 고려하여 요인을 작성해 주세요.\n" +
                        "※ **중요**: JSON 외의 정보는 절대 임의로 생성하지 마세요..\n" +
                        "etcRecords: %s\n\n" +
                        "todayKeyStats: %s\n\n" +
                        "pitchingResult: %s\n\n" +
                        "scoreBoard: %s\n\n" +
                        "battersBoxscore: %s\n\n" +
                        "pitchersBoxscore: %s\n\n" +
                        "teamPitchingBoxscore: %s\n\n" +
                        "homeStandings: %s\n\n" +
                        "awayStandings: %s\n\n" +
                        "gameInfo: %s\n\n" +
                        "%s team members: %s\n\n" +
                        "%s team members: %s\n\n",
                schedule.getStadium(),
                info.getAwayTeamName(), awayScore, homeScore, info.getHomeTeamName(),
                etcRecords, todayKeyStats, pitchingResult, scoreBoard,
                battersBoxscore, pitchersBoxscore, teamPitchingBoxscore,
                homeStandings, awayStandings, gameInfo,
                schedule.getAwayTeamName(), awayPlayers,
                schedule.getHomeTeamName(), homePlayers
        );
    }
}
