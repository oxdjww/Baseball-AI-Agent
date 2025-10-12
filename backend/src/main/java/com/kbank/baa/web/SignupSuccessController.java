package com.kbank.baa.web;

import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class SignupSuccessController {

    private final MemberRepository memberRepository;

    @GetMapping("/signup_success")
    public String signupSuccess(HttpSession session, Model model) {
        Object val = session.getAttribute("memberId");
        if (val == null) return "redirect:/home"; // 보호

        Long memberId;
        try { memberId = Long.valueOf(val.toString()); } catch (Exception e) { return "redirect:/home"; }

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return "redirect:/home";

        model.addAttribute("member", member);
        return "signup_success"; // 네 템플릿 파일명
    }
}
