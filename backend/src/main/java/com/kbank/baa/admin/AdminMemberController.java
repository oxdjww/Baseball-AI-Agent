package com.kbank.baa.admin;

import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", memberRepository.findAll().stream()
                .sorted(Comparator.comparing(Member::getId))
                .toList());
        return "member/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("member", new Member());
        model.addAttribute("teams", Team.values());
        return "member/form";
    }

    @PostMapping
    public String create(@ModelAttribute Member member) {
        memberRepository.save(member);
        return "redirect:/admin/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        model.addAttribute("member", member);
        model.addAttribute("teams", Team.values());
        return "member/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Member member) {
        member.setId(id);
        memberRepository.save(member);
        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        memberRepository.deleteById(id);
        return "redirect:/admin/members";
    }
}
