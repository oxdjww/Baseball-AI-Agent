package com.kbank.kbaseball.web;

import com.kbank.kbaseball.member.Member;
import com.kbank.kbaseball.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class SignupSuccessController {

    private final MemberService memberService;

    @GetMapping("/signup_success")
    public String signupSuccess(HttpSession session, Model model) {
        Object val = session.getAttribute("memberId");
        if (val == null) return "redirect:/home"; // 보호

        Long memberId;
        try { memberId = Long.valueOf(val.toString()); } catch (Exception e) { return "redirect:/home"; }

        Member member;
        try {
            member = memberService.findByIdOrThrow(memberId);
        } catch (IllegalArgumentException e) {
            return "redirect:/home";
        }

        model.addAttribute("member", member);
        return "signup_success"; // 네 템플릿 파일명
    }
}
