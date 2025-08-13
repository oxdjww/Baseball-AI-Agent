package com.kbank.baa.test;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.telegram.TelegramService;
import com.kbank.baa.telegram.template.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TelegramTestController {

    private final TelegramService telegramService;
    private final MemberRepository memberRepository;

    private String getNameByChatId(String chatId) {
        return memberRepository.findAll().stream()
                .filter(m -> chatId.equals(m.getTelegramId()))
                .map(Member::getName)
                .findFirst()
                .orElse("회원");
    }

    // 1) 단건 메시지
    @GetMapping("/test/telegram")
    public String sendPersonal(@RequestParam String chatId, @RequestParam String text) {
        String name = getNameByChatId(chatId);
        telegramService.sendPersonalMessage(chatId, name, text);
        return "✅ 개인 메시지 전송 완료: " + name;
    }

    // 2) 템플릿 - 공지
    @GetMapping("/test/telegram/template/announcement")
    public String templateAnnouncement(@RequestParam String chatId, @RequestParam String body) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.ANNOUNCEMENT, body);
        return "✅ ANNOUNCEMENT 템플릿 전송 완료: " + name;
    }

    // 3) 템플릿 - 경기 종료
    @GetMapping("/test/telegram/template/game-ended")
    public String templateGameEnded(@RequestParam String chatId, @RequestParam String teamName) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.GAME_ENDED, teamName);
        return "✅ GAME_ENDED 템플릿 전송 완료: " + name;
    }

    // 4) 템플릿 - 스코어 포함 종료
    @GetMapping("/test/telegram/template/game-ended-score")
    public String templateGameEndedScore(
            @RequestParam String chatId,
            @RequestParam String homeTeam,
            @RequestParam int homeScore,
            @RequestParam int awayScore,
            @RequestParam String awayTeam
    ) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.GAME_ENDED_WITH_SCORE,
                homeTeam, homeTeam, homeScore, awayScore, awayTeam);
        return "✅ GAME_ENDED_WITH_SCORE 템플릿 전송 완료: " + name;
    }

    // 5) 템플릿 - 우천취소
    @GetMapping("/test/telegram/template/game-canceled")
    public String templateGameCanceled(
            @RequestParam String chatId,
            @RequestParam String teamName,
            @RequestParam String opponent
    ) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.GAME_CANCELED, teamName, opponent);
        return "✅ GAME_CANCELED 템플릿 전송 완료: " + name;
    }

    // 6) 템플릿 - 링크 성공
    @GetMapping("/test/telegram/template/link-success")
    public String templateLinkSuccess(@RequestParam String chatId) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.LINK_SUCCESS);
        return "✅ LINK_SUCCESS 템플릿 전송 완료: " + name;
    }

    // 7) 템플릿 - 계정 없음
    @GetMapping("/test/telegram/template/account-not-found")
    public String templateAccountNotFound(@RequestParam String chatId) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.ACCOUNT_NOT_FOUND);
        return "✅ ACCOUNT_NOT_FOUND 템플릿 전송 완료: " + name;
    }

    // 8) 템플릿 - 토큰 만료
    @GetMapping("/test/telegram/template/token-expired")
    public String templateTokenExpired(@RequestParam String chatId) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.TOKEN_EXPIRED);
        return "✅ TOKEN_EXPIRED 템플릿 전송 완료: " + name;
    }

    // 9) 템플릿 - 환영 가이드
    @GetMapping("/test/telegram/template/welcome-guide")
    public String templateWelcomeGuide(@RequestParam String chatId, @RequestParam String guideUrl) {
        String name = getNameByChatId(chatId);
        telegramService.sendTemplateMessage(chatId, name, NotificationTemplate.WELCOME_GUIDE, guideUrl);
        return "✅ WELCOME_GUIDE 템플릿 전송 완료: " + name;
    }
}
