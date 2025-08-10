package com.kbank.baa.telegram;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.admin.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramService {
    private final TelegramProperties props;
    private final RestTemplate rt = new RestTemplate();
    private final MemberRepository memberRepository;

    // 멤버별 chatId로 전송
    public void sendMessage(String chatId, String name, String text) {
        String url = props.getApiUrl() + "sendMessage";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        String textWithName = String.format("%s님, %s", name, text);
        body.add("chat_id", chatId);
        body.add("text", textWithName);
        body.add("parse_mode", "HTML");
        // 전송 시도 로깅
        log.info("→ [TelegramService] sendMessage 호출 시작: chatId={}, name={}, textPreview={}...",
                chatId, name,
                textWithName.length() > 50 ? textWithName.substring(0, 50) : textWithName);

        try {
            ResponseEntity<String> resp = rt.postForEntity(url, body, String.class);
            // 응답 로깅
            log.info("← [TelegramService] sendMessage 응답: {} 님께 메시지 전송 완료. statusCode={}",
                    name, resp.getStatusCodeValue());
        } catch (Exception ex) {
            // 예외 로깅
            log.error("✖ [TelegramService] sendMessage 실패: chatId={}, error={}", chatId, ex.getMessage(), ex);
        }
    }

    public void sendMessageToTeam(String teamCode, String text) {
        Team team;
        try {
            team = Team.valueOf(teamCode);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid teamCode='{}'로 팀 조회 불가", teamCode);
            return;
        }

        List<Member> supporters = memberRepository.findAllBySupportTeam(team);
        if (supporters.isEmpty()) {
            log.info("→ [TelegramService] 팀 {} 팬이 없어 메시지 스킵", team);
            return;
        }

        for (Member member : supporters) {
            sendMessage(
                    member.getTelegramId(),
                    member.getName(),
                    text
            );
        }
    }

    public void sendMessageToAllMembers(String message) {
        List<Member> members = memberRepository.findAll();
        String fullText = String.format("<b>새로운 공지사항이 있어요!</b>\n\n%s\n\n감사합니다.", message);

        for (Member member : members) {
            sendMessage(
                    member.getTelegramId(),
                    member.getName(),
                    fullText
            );
        }
    }
}
