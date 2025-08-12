package com.kbank.baa.web;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.admin.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {

    private final MemberRepository memberRepository;

    // GET /home?activeTab=login | signup
    @GetMapping
    public String home(@RequestParam(defaultValue = "signup") String activeTab,
                       @RequestParam(required = false) String loginError,
                       HttpSession session,
                       Model model) {
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("teams", Team.values());
        model.addAttribute("signup", new SignupForm());
        model.addAttribute("login", new LoginForm());

        // 세션에서 memberId 꺼내 모델에 member 올리기 (없으면 null)
        Member member = null;
        Object val = session.getAttribute("memberId");
        if (val != null) {
            try {
                Long memberId = (val instanceof Long) ? (Long) val : Long.valueOf(val.toString());
                member = memberRepository.findById(memberId).orElse(null);
            } catch (Exception ignore) {}
        }
        model.addAttribute("member", member);

        if (loginError != null) model.addAttribute("loginError", loginError);
        return "home";
    }

    // POST /home/signup
    @PostMapping("/signup")
    public String signup(@ModelAttribute("signup") SignupForm form,
                         HttpSession session) {
        Member saved = memberRepository.save(
                Member.builder()
                        .name(form.getName())
                        .supportTeam(form.getSupportTeam())
                        // .telegramId(null)  // 연동 전이므로 비워둠
                        .notifyGameAnalysis(form.isNotifyGameAnalysis())
                        .notifyRainAlert(form.isNotifyRainAlert())
                        .notifyRealTimeAlert(form.isNotifyRealTimeAlert())
                        .build()
        );
        session.setAttribute("memberId", saved.getId());
        return "redirect:/signup_success";
    }

    // POST /home/login
    @PostMapping("/login")
    public String login(@ModelAttribute("login") LoginForm form,
                        HttpSession session) {
        return memberRepository.findByNameAndSupportTeam(form.getName(), form.getSupportTeam())
                .map(m -> {
                    session.setAttribute("memberId", m.getId());
                    return "redirect:/home?activeTab=login";
                })
                .orElse("redirect:/home?activeTab=login&loginError=일치하는%20회원이%20없습니다.");
    }
}
