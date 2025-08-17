package com.kbank.baa.batch.tasklet;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.admin.Team;
import com.kbank.baa.sports.GameMessageFormatter;
import com.kbank.baa.sports.GameRosterClient;
import com.kbank.baa.sports.dto.RealtimeGameInfoDto;
import com.kbank.baa.sports.dto.ScheduledGameDto;
import com.kbank.baa.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameAnalysisTasklet {
    private final RestTemplate restTemplate;
    private final OpenAiChatModel chatModel;
    private final MemberRepository memberRepo;
    private final TelegramService telegramService;
    private final GameRosterClient gameRosterClient;
    private final GameMessageFormatter gameMessageFormatter;

    public void execute(ScheduledGameDto schedule, RealtimeGameInfoDto info) {
        String gameId = schedule.getGameId();
        String dateLabel = schedule.getGameDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("[GameAnalysis][execute] GameAnalysis START → gameId={} #####", gameId);
        log.info("[GameAnalysis][execute] Tasklet 시작 → 경기일={}, 경기장={}", dateLabel, schedule.getStadium());

        // 1) 네이버 스포츠 기록 API 호출
        String url = "https://api-gw.sports.naver.com/schedule/games/" + gameId + "/record";
        log.info("[GameAnalysis][execute] 기록 API 호출 URL={} for gameId={}", url, gameId);
        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        if (root == null || !root.has("result")) {
            log.error("[GameAnalysis][execute] recordData 없음 → gameId={}", gameId);
            return;
        }
        // root record
        JsonNode recordData = root.path("result").path("recordData");

        // child records
        JsonNode etcRecords = recordData.path("etcRecords");
        JsonNode todayKeyStats = recordData.path("todayKeyStats");
        JsonNode pitchingResult = recordData.path("pitchingResult");
        JsonNode scoreBoard = recordData.path("scoreBoard");
        JsonNode battersBoxscore = recordData.path("battersBoxscore");
        JsonNode pitchersBoxscore = recordData.path("pitchersBoxscore");
        JsonNode teamPitchingBoxscore = recordData.path("teamPitchingBoxscore");
        JsonNode homeStandings = recordData.path("homeStandings");
        JsonNode awayStandings = recordData.path("awayStandings");
        JsonNode gameInfo = recordData.path("gameInfo");

        String formattedScoreBoard = gameMessageFormatter.formatScoreboard(scoreBoard);

        String winner, loser;
        int awayScore = info.getAwayScore();
        int homeScore = info.getHomeScore();

        // 2) 승/패/무 결정
        if (awayScore > homeScore) {
            winner = info.getAwayTeamName();
            loser = info.getHomeTeamName();
        } else if (homeScore > awayScore) {
            winner = info.getHomeTeamName();
            loser = info.getAwayTeamName();
        } else {
            winner = "무승부";
            loser = "무승부";
        }

        List<String> awayTeamPlayers = gameRosterClient.fetchPlayerNamesByTeam(gameId, schedule.getAwayTeamName());
        List<String> homeTeamPlayers = gameRosterClient.fetchPlayerNamesByTeam(gameId, schedule.getHomeTeamName());
        String awayList = String.join(", ", awayTeamPlayers);
        String homeList = String.join(", ", homeTeamPlayers);

        // 3) AI 프롬프트 생성 (모든 필드 포함)
        String prompt;
        if (!"무승부".equals(winner)) {
            prompt = String.format(
                    "※ **응답에서는 ‘etcRecords’, ‘todayKeyStats’, ‘pitchingResult’ 등 같은 변수명이나 JSON 필드를 일절 언급하지 말고**, 자연스럽고 깔끔한 한국어 문장으로만 요약해 주세요.\n\n" +
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
                            "%s team members: %s\n\n"
                    ,
                    schedule.getStadium(),
                    info.getAwayTeamName(), awayScore,
                    homeScore, info.getHomeTeamName(),
                    winner, loser,
                    etcRecords.toString(),
                    todayKeyStats.toString(),
                    pitchingResult.toString(),
                    scoreBoard.toString(),
                    battersBoxscore.toString(),
                    pitchersBoxscore.toString(),
                    teamPitchingBoxscore.toString(),
                    homeStandings.toString(),
                    awayStandings.toString(),
                    gameInfo.toString(),
                    schedule.getAwayTeamName(), awayList,
                    schedule.getHomeTeamName(), homeList
            );
        } else {
            prompt = String.format(
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
                            "%s team members: %s\n\n"
                    ,
                    schedule.getStadium(),
                    info.getAwayTeamName(), awayScore,
                    homeScore, info.getHomeTeamName(),
                    etcRecords.toString(),
                    todayKeyStats.toString(),
                    pitchingResult.toString(),
                    scoreBoard.toString(),
                    battersBoxscore.toString(),
                    pitchersBoxscore.toString(),
                    teamPitchingBoxscore.toString(),
                    homeStandings.toString(),
                    awayStandings.toString(),
                    gameInfo.toString(),
                    schedule.getAwayTeamName(), awayList,
                    schedule.getHomeTeamName(), homeList
            );
        }

        // 4) AI 호출 & 메시지 전송 (기존 로직 유지)
        log.info("[GameAnalysis][execute] Prompt: {}", prompt);
        String analysis = "fake analysis\n";
        try {
            analysis = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("[GameAnalysis][execute] AI 호출 실패", e);
            analysis = "⚠️ AI 분석 중 오류 발생";
        }


        // 5) 대상 멤버 필터링 및 전송
        List<Member> targets = memberRepo.findByNotifyGameAnalysisTrue();
        log.info("[GameAnalysis][execute] 대상 멤버 수: {}명", targets.size());
        for (Member m : targets) {
            log.info("[GameAnalysis][execute] 회원 검토 → 이름={}, 응원팀={}", m.getName(), m.getSupportTeam());
            boolean isHome = String.valueOf(m.getSupportTeam()).equals(schedule.getHomeTeamCode());
            boolean isAway = String.valueOf(m.getSupportTeam()).equals(schedule.getAwayTeamCode());
            log.info("[GameAnalysis] filter 결과 → isHome={}, isAway={}", isHome, isAway);
            if (!(isHome || isAway)) continue;

            // 한 줄 요약(3번째 파트)은 빼고, 승/패팀 요인만 취급
            String[] parts = analysis.split("\n\n", 2);
            String winFactors = parts[0];
            String loseFactors = parts.length > 1 ? parts[1] : "";

            String formatted = String.format(
                    "오늘 경기 요약이 도착했어요!\n" +
                            "⚾️ <b>%s %d : %d %s</b>\n" +
                            "AWAY: %s, HOME: %s\n\n" +
                            "%s\n\n" +
                            "🏆 <b>1. 승리팀(%s) 요인</b>\n%s\n\n" +
                            "💔 <b>2. 패배팀(%s) 요인</b>\n%s",
                    Team.getDisplayNameByCode(schedule.getAwayTeamCode()), awayScore, // 어웨이팀 정보
                    homeScore, Team.getDisplayNameByCode(schedule.getHomeTeamCode()),  // 홈팀 정보
                    Team.getDisplayNameByCode(schedule.getAwayTeamCode()), Team.getDisplayNameByCode(schedule.getHomeTeamCode()),
                    formattedScoreBoard,
                    winner, winFactors,
                    loser, loseFactors
            );

            log.info("[GameAnalysis][execute] 경기 분석 전송 시도 → gameId={}, member={}, preview=[{}]...",
                    gameId, m.getName(), formatted.substring(0, Math.min(40, formatted.length())));
            telegramService.sendPersonalMessage(m.getTelegramId(), m.getName(), formatted);
            log.info("[GameAnalysis][execute] 경기 분석 전송 완료 → chatId={}", m.getTelegramId());
        }


        log.info("[GameAnalysis][execute] GameAnalysis END → gameId={} #####", gameId);
    }

}
