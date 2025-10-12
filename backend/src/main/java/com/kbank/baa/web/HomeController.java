package com.kbank.baa.web;

import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberRepository;
import com.kbank.baa.admin.Team;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {

    private final MemberRepository memberRepository;

    // DTO들
    @Data
    public static class SignupForm {
        private String name;
        private Team supportTeam;
        private boolean notifyGameAnalysis;
        private boolean notifyRainAlert;
        private boolean notifyRealTimeAlert;
    }

    @Data
    public static class LoginForm {
        private String name;
        private Team supportTeam;
    }

    @Data
    public static class PrefForm {
        private Long id;
        private Team supportTeam;
        private boolean notifyGameAnalysis;
        private boolean notifyRainAlert;
        private boolean notifyRealTimeAlert;
    }

    // GET /home
    @GetMapping
    public String home(@RequestParam(defaultValue = "signup") String activeTab,
                       HttpSession session,
                       Model model) {
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("teams", Team.values());
        if (!model.containsAttribute("signup")) model.addAttribute("signup", new SignupForm());
        if (!model.containsAttribute("login")) model.addAttribute("login", new LoginForm());

        // 세션에서 로그인 멤버 로드
        Member member = null;
        Object val = session.getAttribute("memberId");
        if (val != null) {
            try {
                Long memberId = Long.valueOf(val.toString());
                member = memberRepository.findById(memberId).orElse(null);
            } catch (Exception ignored) {
            }
        }
        model.addAttribute("member", member);
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
                        .notifyGameAnalysis(form.isNotifyGameAnalysis())
                        .notifyRainAlert(form.isNotifyRainAlert())
                        .notifyRealTimeAlert(form.isNotifyRealTimeAlert())
                        .build()
        );
        session.setAttribute("memberId", saved.getId());
        return "redirect:/signup_success";
    }

    // ✅ POST /home/login — 실패 시 토스트, 성공 시 로그인 탭으로
    @PostMapping("/login")
    public String login(@ModelAttribute("login") LoginForm form,
                        HttpSession session,
                        RedirectAttributes ra) {
        return memberRepository.findByNameAndSupportTeam(form.getName(), form.getSupportTeam())
                .map(m -> {
                    session.setAttribute("memberId", m.getId());
                    return "redirect:/home?activeTab=login"; // 로그인 탭으로
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("loginError", "일치하는 회원을 찾을 수 없습니다.");
                    ra.addFlashAttribute("activeTab", "login");
                    ra.addFlashAttribute("login", form); // 입력값 유지(선택)
                    return "redirect:/home?activeTab=login";
                });
    }

    // 로그아웃
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/home?activeTab=login";
    }

    // ✅ 응원팀/알림 설정 화면
    @GetMapping("/preferences")
    public String preferences(HttpSession session, Model model) {
        Object val = session.getAttribute("memberId");
        if (val == null) return "redirect:/home?activeTab=login";
        Member member = memberRepository.findById(Long.valueOf(val.toString()))
                .orElse(null);
        if (member == null) return "redirect:/home?activeTab=login";

        PrefForm pref = new PrefForm();
        pref.setId(member.getId());
        pref.setSupportTeam(member.getSupportTeam());
        pref.setNotifyGameAnalysis(member.isNotifyGameAnalysis());
        pref.setNotifyRainAlert(member.isNotifyRainAlert());
        pref.setNotifyRealTimeAlert(member.isNotifyRealTimeAlert());

        model.addAttribute("pref", pref);
        model.addAttribute("teams", Team.values());
        return "user/preferences";
    }

    // ✅ 응원팀/알림 저장
    @PostMapping("/preferences")
    public String savePreferences(@ModelAttribute("pref") PrefForm form,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        Object val = session.getAttribute("memberId");
        if (val == null || !val.toString().equals(String.valueOf(form.getId()))) {
            ra.addFlashAttribute("loginError", "다시 로그인 해주세요.");
            ra.addFlashAttribute("activeTab", "login");
            return "redirect:/home?activeTab=login";
        }

        Member m = memberRepository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        m.setSupportTeam(form.getSupportTeam());
        m.setNotifyGameAnalysis(form.isNotifyGameAnalysis());
        m.setNotifyRainAlert(form.isNotifyRainAlert());
        m.setNotifyRealTimeAlert(form.isNotifyRealTimeAlert());
        memberRepository.save(m);

        // 저장 후 홈으로 (로그인 탭에 로그인됨 배너가 보임)
        return "redirect:/home?activeTab=login";
    }
}
