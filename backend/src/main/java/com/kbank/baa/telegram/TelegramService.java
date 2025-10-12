package com.kbank.baa.telegram;

import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberRepository;
import com.kbank.baa.admin.Team;
import com.kbank.baa.telegram.dto.ParseMode;
import com.kbank.baa.telegram.dto.TelegramMessage;
import com.kbank.baa.telegram.template.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService {

    private static final String NAME_PREFIX_PATTERN = "%s님, %s"; // 공통 개인화 패턴

    private final MemberRepository memberRepository;
    private final TelegramClient telegramClient;

    /**
     * 1) 단건 전송(개인화 포함)
     */
    public void sendPersonalMessage(String chatId, String name, String rawText) {
        String personalized = String.format(NAME_PREFIX_PATTERN, name, rawText);

        TelegramMessage msg = TelegramMessage.builder()
                .chatId(chatId)
                .text(personalized)
                .parseMode(ParseMode.HTML)
                .build();

        telegramClient.sendMessage(msg);
    }

    /**
     * 2) 템플릿 기반 단건 전송(개인화 포함)
     */
    public void sendTemplateMessage(String chatId, String name, NotificationTemplate template, Object... args) {
        String text = template.format(args);
        sendPersonalMessage(chatId, name, text);
    }

    /**
     * 3) 팀 팬 전체에게 전송
     */
    public void sendMessageToTeam(String teamCode, NotificationTemplate template, Object... args) {
        final Team team;
        try {
            team = Team.valueOf(teamCode);
        } catch (IllegalArgumentException ex) {
            log.warn("[TelegramService][sendMessageToTeam] Invalid teamCode='{}'로 팀 조회 불가", teamCode);
            return;
        }

        List<Member> supporters = memberRepository.findAllBySupportTeam(team);
        if (supporters.isEmpty()) {
            log.info("[TelegramService][sendMessageToTeam] → 팀 {} 팬이 없어 메시지 스킵", team);
            return;
        }

        String rawText = template.format(args);
        supporters.forEach(m -> sendPersonalMessage(m.getTelegramId(), m.getName(), rawText));
    }

    /**
     * 4) 전체 공지 (템플릿 공통화)
     */
    public void sendAnnouncementToAllMembers(String bodyText) {
        List<Member> members = memberRepository.findAll();
        String rawText = NotificationTemplate.ANNOUNCEMENT.format(bodyText);

        members.forEach(m -> sendPersonalMessage(m.getTelegramId(), m.getName(), rawText));
    }

    /**
     * 5) 기존 코드 레거시
     */
    @Deprecated
    public void sendMessageToAllMembers_Legacy(String message) {
        sendAnnouncementToAllMembers(message);
    }
}
