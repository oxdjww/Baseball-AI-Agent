package com.kbank.baa.batch.tasklet;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.admin.Team;
import com.kbank.baa.sports.RealtimeGameInfo;
import com.kbank.baa.sports.ScheduledGame;
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

    public void execute(ScheduledGame schedule, RealtimeGameInfo info) {
        String gameId = schedule.getGameId();
        String dateLabel = schedule.getGameDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("##### GameAnalysis START → gameId={} #####", gameId);
        log.info("[GameAnalysis] Tasklet 시작 → 경기일={}, 경기장={}", dateLabel, schedule.getStadium());

        // 1) 네이버 스포츠 기록 API 호출
        String url = "https://api-gw.sports.naver.com/schedule/games/" + gameId + "/record";
        log.info("[GameAnalysis] 기록 API 호출 URL={} for gameId={}", url, gameId);
        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        if (root == null || !root.has("result")) {
            log.error("[GameAnalysis] recordData 없음 → gameId={}", gameId);
            return;
        }
        JsonNode recordData = root.path("result").path("recordData");
        JsonNode etcRecords = recordData.path("etcRecords");
        JsonNode todayKeyStats = recordData.path("todayKeyStats");
        JsonNode pitchingResult = recordData.path("pitchingResult");

        // 2) AI 프롬프트 생성 및 호출
        String prompt = String.format(
                "※ **응답에서는 ‘etcRecords’, ‘todayKeyStats’, ‘pitchingResult’ 같은 변수명이나 JSON 필드를 일절 언급하지 말고**, 자연스럽고 깔끔한 한국어 문장으로만 요약해 주세요.\n\n" +
                        "%s) %s 경기의 상세 JSON 데이터입니다.\n" +
                        "최종 스코어: %s팀 %d : %d %s팀 이며,\n" +
                        "etcRecords, todayKeyStats, pitchingResult만 추출했습니다.\n\n" +
                        "아래 JSON 필드를 참고하여, 승리팀과 패배팀의 주요 요인(결정적 사건, 핵심 지표, 결정 투수 성과 등)을 자유롭게 파악한 뒤\n" +
                        "다음 형식으로 간결히 요약·정리해 주세요. 요인 개수에 제한은 없습니다.\n\n" +
                        "1) 승리팀의 주요 승리 요인\n" +
                        "- 요인 A\n" +
                        "- 요인 B\n" +
                        "- …\n\n" +
                        "2) 패배팀의 주요 패배 요인\n" +
                        "- 요인 A\n" +
                        "- 요인 B\n" +
                        "- …\n\n" +
                        "etcRecords: %s\n\n" +
                        "todayKeyStats: %s\n\n" +
                        "pitchingResult: %s",
                dateLabel,
                schedule.getStadium(),
                // 어웨이-홈 팀 이름
                info.getAwayTeamName(), info.getAwayScore(),
                info.getHomeScore(), info.getHomeTeamName(),
                etcRecords.toString(),
                todayKeyStats.toString(),
                pitchingResult.toString()
        );

        log.info("[GameAnalysis] AI 프롬프트 생성 → length={} chars", prompt.length());
        String analysis;
        try {
            analysis = chatModel.call(prompt);
            log.info("[GameAnalysis] AI 응답 수신 → length={} chars", analysis.length());
        } catch (Exception e) {
            log.error("[GameAnalysis] AI 호출 실패: {}", e.getMessage(), e);
            analysis = "⚠️ AI 분석 중 오류 발생";
        }

        // 3) 대상 멤버 필터링 및 전송
        List<Member> targets = memberRepo.findByNotifyGameAnalysisTrue();
        log.info("[GameAnalysis] 대상 멤버 수: {}명", targets.size());
        for (Member m : targets) {
            log.info("[GameAnalysis] 회원 검토 → 이름={}, 응원팀={}", m.getName(), m.getSupportTeam());
            boolean isHome = String.valueOf(m.getSupportTeam()).equals(schedule.getHomeTeamCode());
            boolean isAway = String.valueOf(m.getSupportTeam()).equals(schedule.getAwayTeamCode());
            log.info("[GameAnalysis] filter 결과 → isHome={}, isAway={}", isHome, isAway);
            if (!(isHome || isAway)) continue;

            // 분석 응답 포맷팅
            // 분석 응답 포맷팅
// 한 줄 요약(3번째 파트)은 빼고, 승/패팀 요인만 취급
            String[] parts = analysis.split("\n\n", 2);
            String winFactors = parts[0];
            String loseFactors = parts.length > 1 ? parts[1] : "";

            String winTeam = info.getStatusCode().equals("4") ? schedule.getHomeTeamName() : schedule.getAwayTeamName();
            String loseTeam = info.getStatusCode().equals("4") ? schedule.getAwayTeamName() : schedule.getHomeTeamName();

            // 최종 스코어 추출 (예시: awayScore, homeScore 변수로 가정)
            int awayScore = info.getAwayScore();
            int homeScore = info.getHomeScore();

            String formatted = String.format(
                    "오늘 경기 요약이 도착했어요!\n" +
                            "최종 스코어: %s %d : %d %s\n\n" +
                            "🏆 <b>1. 승리팀(%s) 요인</b>\n%s\n\n" +
                            "💔 <b>2. 패배팀(%s) 요인</b>\n%s",
                    Team.getDisplayNameByCode(schedule.getAwayTeamCode()),  // 원정팀 이름
                    awayScore,                                            // 원정팀 점수
                    homeScore,                                            // 홈팀 점수
                    Team.getDisplayNameByCode(schedule.getHomeTeamCode()),// 홈팀 이름
                    winTeam, winFactors,
                    loseTeam, loseFactors
            );

            log.info("[GameAnalysis] sendMessage 호출 직전 → chatId={}, preview=[{}]...",
                    m.getTelegramId(), formatted.substring(0, Math.min(40, formatted.length())));
            telegramService.sendMessage(m.getTelegramId(), m.getName(), formatted);
            log.info("[GameAnalysis] sendMessage 완료 → chatId={}", m.getTelegramId());
        }

        log.info("##### GameAnalysis END → gameId={} #####", gameId);
    }
}
