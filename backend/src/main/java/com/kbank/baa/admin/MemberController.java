package com.kbank.baa.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", memberRepository.findAll());
        return "member/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("member", new Member());
        // ★ teams enum 값 추가
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
        // ★ teams enum 값 추가
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
