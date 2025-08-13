package com.kbank.baa.telegram;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.telegram.template.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/telegram")
@Slf4j
public class TelegramWebhookController {

    private final StringRedisTemplate redis;
    private final MemberRepository memberRepository;
    private final TelegramService telegramService;

    @PostMapping("/webhook")
    @Transactional
    public void onUpdate(@RequestBody Map<String, Object> update) {
        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return;

        Map<String, Object> from = (Map<String, Object>) message.get("from");
        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        String text = (String) message.getOrDefault("text", "");

        log.info("[TelegramWebhookController][onUpdate] text={}", text);

        Long telegramUserId = ((Number) from.get("id")).longValue(); // 고유 사용자 ID
        Long chatId = ((Number) chat.get("id")).longValue();

        if (text.startsWith("/start ")) {
            String token = text.substring(7).trim();
            String key = "tg:link:" + token;
            String memberIdStr = redis.opsForValue().get(key);

            log.info("[TelegramWebhookController][onUpdate] token={} -> memberId={}", token, memberIdStr);

            if (memberIdStr != null) {
                Long memberId = Long.valueOf(memberIdStr);
                Optional<Member> opt = memberRepository.findById(memberId);

                if (opt.isPresent()) {
                    Member m = opt.get();

                    m.setTelegramId(String.valueOf(telegramUserId));
                    memberRepository.save(m);

                    redis.delete(key);

                    telegramService.sendTemplateMessage(
                            String.valueOf(chatId),
                            m.getName(),
                            NotificationTemplate.LINK_SUCCESS
                    );
                } else {
                    telegramService.sendTemplateMessage(
                            String.valueOf(chatId),
                            "회원",
                            NotificationTemplate.ACCOUNT_NOT_FOUND
                    );
                }
            } else {
                telegramService.sendTemplateMessage(
                        String.valueOf(chatId),
                        "회원",
                        NotificationTemplate.TOKEN_EXPIRED
                );
            }
            return;
        }

        // 기타 텍스트 → 가이드 (선택)
        telegramService.sendTemplateMessage(
                String.valueOf(chatId),
                "회원",
                NotificationTemplate.WELCOME_GUIDE,
                "https://t.me/lIllllIIllllI"
        );
    }
}
