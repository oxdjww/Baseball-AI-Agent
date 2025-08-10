package com.kbank.baa.web;

import com.kbank.baa.admin.Member;
import com.kbank.baa.admin.MemberRepository;
import com.kbank.baa.admin.Team;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Getter
@Setter
class LoginForm {
    private String name;
    private String supportTeam;
    private String telegramId;
}

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MemberRepository memberRepository;

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("teams", List.of(Team.values()));
        if (!model.containsAttribute("signup")) model.addAttribute("signup", new MemberForm());
        if (!model.containsAttribute("login"))  model.addAttribute("login", new LoginForm());
        if (!model.containsAttribute("activeTab")) model.addAttribute("activeTab", "signup"); // ✅ 기본값
        return "home";
    }


    @PostMapping("/home/signup")
    public String signup(@ModelAttribute("signup") MemberForm form, RedirectAttributes ra) {
        memberRepository.save(
                Member.builder()
                        .name(form.getName())
                        .supportTeam(Team.of(form.getSupportTeam()))
                        .telegramId(form.getTelegramId())
                        .notifyGameAnalysis(Boolean.TRUE.equals(form.getNotifyGameAnalysis()))
                        .notifyRainAlert(Boolean.TRUE.equals(form.getNotifyRainAlert()))
                        .notifyRealTimeAlert(Boolean.TRUE.equals(form.getNotifyRealTimeAlert()))
                        .build()
        );
        ra.addFlashAttribute("toast", "가입이 완료되었습니다!");
        return "redirect:/signup_success";
    }

    @PostMapping("/home/login")
    public String login(@ModelAttribute("login") LoginForm form, RedirectAttributes ra) {
        // ✅ 로그인 검증: 이름 + 응원팀 + 텔레그램ID 모두 일치해야 성공
        return memberRepository.findByNameAndSupportTeamAndTelegramId(
                        form.getName(),
                        Team.of(form.getSupportTeam()),
                        form.getTelegramId()
                )
                .map(found -> {
                    ra.addAttribute("id", found.getId());
                    return "redirect:/user/preferences";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("loginError", "일치하는 회원을 찾을 수 없습니다.");
                    ra.addFlashAttribute("activeTab", "login"); // 홈에서 로그인 탭 활성화용(선택)
                    // 입력값 유지
                    ra.addFlashAttribute("login", form);
                    return "redirect:/home";
                });
    }

    @GetMapping("/user/preferences")
    public String preferences(@RequestParam Long id, Model model) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        MemberForm pref = new MemberForm();
        pref.setId(member.getId());
        pref.setName(member.getName());
        pref.setSupportTeam(member.getSupportTeam().name());
        pref.setTelegramId(member.getTelegramId());
        pref.setNotifyGameAnalysis(member.isNotifyGameAnalysis());
        pref.setNotifyRainAlert(member.isNotifyRainAlert());
        pref.setNotifyRealTimeAlert(member.isNotifyRealTimeAlert());

        model.addAttribute("pref", pref);
        model.addAttribute("teams", List.of(Team.values())); // ✅ 응원팀 수정용
        return "user/preferences";
    }

    @PostMapping("/user/preferences")
    public String savePreferences(@ModelAttribute("pref") MemberForm form,
                                  RedirectAttributes ra) {
        // ✅ 저장 로직: 응원팀 포함 알림 설정 업데이트
        Member member = memberRepository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        member.setSupportTeam(Team.of(form.getSupportTeam())); // ✅ 응원팀 변경 반영
        member.setNotifyGameAnalysis(Boolean.TRUE.equals(form.getNotifyGameAnalysis()));
        member.setNotifyRainAlert(Boolean.TRUE.equals(form.getNotifyRainAlert()));
        member.setNotifyRealTimeAlert(Boolean.TRUE.equals(form.getNotifyRealTimeAlert()));

        memberRepository.save(member);

        ra.addFlashAttribute("toast", "알림 설정이 저장됐습니다.");
        return "redirect:/home";
    }

    @GetMapping("/signup_success")
    public String signupSuccess() {
        return "signup_success";
    }
}
