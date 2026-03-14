package com.kbank.kbaseball.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
public class TelegramLinkController {

    private final TelegramLinkService telegramLinkService;

    @GetMapping("/auth/telegram/link")
    public RedirectView link(@RequestParam Long memberId) {
        return new RedirectView(telegramLinkService.generateLinkUrl(memberId));
    }
}
