package com.kbank.baa.batch.tasklet;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberService;
import com.kbank.baa.domain.team.Team;
import com.kbank.baa.game.analysis.GameAnalysisPromptBuilder;
import com.kbank.baa.game.message.GameMessageFormatter;
import com.kbank.baa.external.naver.NaverRosterClient;
import com.kbank.baa.external.naver.NaverGameRecordClient;
import com.kbank.baa.external.naver.dto.RealtimeGameInfoDto;
import com.kbank.baa.external.naver.dto.ScheduledGameDto;
import com.kbank.baa.notification.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameAnalysisTasklet {
    private final OpenAiChatModel chatModel;
    private final MemberService memberService;
    private final TelegramService telegramService;
    private final NaverRosterClient gameRosterClient;
    private final NaverGameRecordClient gameRecordClient;
    private final GameAnalysisPromptBuilder promptBuilder;
    private final GameMessageFormatter gameMessageFormatter;

    public void execute(ScheduledGameDto schedule, RealtimeGameInfoDto info) {
        String gameId = schedule.getGameId();
        String dateLabel = schedule.getGameDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("[GameAnalysis][execute] GameAnalysis START → gameId={} #####", gameId);
        log.info("[GameAnalysis][execute] Tasklet 시작 → 경기일={}, 경기장={}", dateLabel, schedule.getStadium());

        // 1) 기록 API 호출
        JsonNode recordData;
        try {
            recordData = gameRecordClient.fetchRecordData(gameId);
        } catch (IllegalStateException e) {
            log.error("[GameAnalysis][execute] recordData 없음 → gameId={}", gameId);
            return;
        }

        // 2) 선수 명단 조회
        String awayPlayers = String.join(", ", gameRosterClient.fetchPlayerNamesByTeam(gameId, schedule.getAwayTeamName()));
        String homePlayers = String.join(", ", gameRosterClient.fetchPlayerNamesByTeam(gameId, schedule.getHomeTeamName()));

        // 3) AI 프롬프트 생성 및 호출
        String prompt = promptBuilder.build(schedule, info, recordData, awayPlayers, homePlayers);
        log.info("[GameAnalysis][execute] Prompt: {}", prompt);

        String analysis = "fake analysis\n";
        try {
            analysis = chatModel.call(prompt);
        } catch (Exception e) {
            log.error("[GameAnalysis][execute] AI 호출 실패", e);
            analysis = "⚠️ AI 분석 중 오류 발생";
        }

        // 4) 대상 멤버 필터링 및 전송
        String winner = promptBuilder.resolveWinner(info);
        String loser = promptBuilder.resolveLoser(info);
        String formattedScoreBoard = gameMessageFormatter.formatScoreboard(recordData.path("scoreBoard"));
        int awayScore = info.getAwayScore();
        int homeScore = info.getHomeScore();

        List<Member> targets = memberService.findByNotifyGameAnalysisTrue();
        log.info("[GameAnalysis][execute] 대상 멤버 수: {}명", targets.size());

        for (Member m : targets) {
            log.info("[GameAnalysis][execute] 회원 검토 → 이름={}, 응원팀={}", m.getName(), m.getSupportTeam());
            boolean isHome = String.valueOf(m.getSupportTeam()).equals(schedule.getHomeTeamCode());
            boolean isAway = String.valueOf(m.getSupportTeam()).equals(schedule.getAwayTeamCode());
            log.info("[GameAnalysis] filter 결과 → isHome={}, isAway={}", isHome, isAway);
            if (!(isHome || isAway)) continue;

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
                    Team.getDisplayNameByCode(schedule.getAwayTeamCode()), awayScore,
                    homeScore, Team.getDisplayNameByCode(schedule.getHomeTeamCode()),
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
