package com.kbank.baa.batch.tasklet;

import com.fasterxml.jackson.databind.JsonNode;
import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
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

        // 2) AI 프롬프트 생성 및 호출
        JsonNode recordData = root.path("result").path("recordData");
        String prompt = String.format(
                "%s) %s 경기 데이터... etcRecords, todayKeyStats, pitchingResult 포함", dateLabel, schedule.getStadium()
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
            String[] parts = analysis.split("\n\n", 3);
            String part1 = parts.length > 0 ? parts[0] : analysis;
            String part2 = parts.length > 1 ? parts[1] : "";
            String part3 = parts.length > 2 ? parts[2] : "";
            String winTeam = info.getStatusCode().equals("4") ? schedule.getHomeTeamName() : schedule.getAwayTeamName();
            String loseTeam = info.getStatusCode().equals("4") ? schedule.getAwayTeamName() : schedule.getHomeTeamName();
            String formatted = String.format(
                    "오늘 경기 요약이 도착했어요! \n\n🏆 <b>1. 승리팀(%s) 요인</b>\n%s\n\n" +
                            "💔 <b>2. 패패팀(%s) 요인</b>\n%s\n\n" +
                            "🔧 <b>3. 보완사항</b>\n%s",
                    winTeam, part1,
                    loseTeam, part2,
                    part3
            );
            log.info("[GameAnalysis] sendMessage 호출 직전 → chatId={}, preview=[{}]...",
                    m.getTelegramId(), formatted.substring(0, Math.min(40, formatted.length())));
            telegramService.sendMessage(m.getTelegramId(), m.getName(), formatted);
            log.info("[GameAnalysis] sendMessage 완료 → chatId={}", m.getTelegramId());
        }

        log.info("##### GameAnalysis END → gameId={} #####", gameId);
    }
}
