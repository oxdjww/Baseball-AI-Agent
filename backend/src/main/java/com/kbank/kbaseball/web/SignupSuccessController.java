package com.kbank.kbaseball.web;

import com.kbank.kbaseball.auth.PendingMemberData;
import com.kbank.kbaseball.auth.TelegramLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class SignupSuccessController {

    private static final String BOT_USERNAME = "baseball_ai_agent_bot";

    private final TelegramLinkService telegramLinkService;

    @GetMapping("/signup_success")
    public String signupSuccess(HttpSession session, Model model) {
        Object tokenVal = session.getAttribute("signupToken");
        if (tokenVal == null) return "redirect:/home";

        PendingMemberData pending = telegramLinkService.getPendingMember(tokenVal.toString());
        if (pending == null) {
            if (telegramLinkService.isLinked(tokenVal.toString())) {
                return "redirect:/home?activeTab=login&welcome=true";
            }
            model.addAttribute("expired", true);
            return "signup_success";
        }

        model.addAttribute("name", pending.name());
        model.addAttribute("telegramUrl", "https://t.me/" + BOT_USERNAME + "?start=" + tokenVal);
        return "signup_success";
    }
}
