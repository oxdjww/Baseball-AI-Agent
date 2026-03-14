package com.kbank.baa.admin;

import com.kbank.baa.domain.team.Team;
import com.kbank.baa.member.Member;
import com.kbank.baa.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", memberService.findAllSorted());
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
        memberService.save(member);
        return "redirect:/admin/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("member", memberService.findByIdOrThrow(id));
        model.addAttribute("teams", Team.values());
        return "member/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Member member) {
        member.setId(id);
        memberService.save(member);
        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        memberService.deleteById(id);
        return "redirect:/admin/members";
    }
}
