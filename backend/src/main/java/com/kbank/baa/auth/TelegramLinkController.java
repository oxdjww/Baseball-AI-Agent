package com.kbank.baa.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TelegramLinkController {

    private final StringRedisTemplate redis;
    private final String botUsername = "baseball_ai_agent_bot"; // @ 없이

    @GetMapping("/auth/telegram/link")
    public RedirectView link(@RequestParam Long memberId) {
        // 1) 1회용 토큰
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 2) token -> memberId (TTL 5분)
        redis.opsForValue().set("tg:link:" + token, String.valueOf(memberId), Duration.ofMinutes(5));

        // 3) 텔레그램 딥링크로 리다이렉트
        String deepLink = "https://t.me/" + botUsername + "?start=" + token;
        return new RedirectView(deepLink);
    }
}
