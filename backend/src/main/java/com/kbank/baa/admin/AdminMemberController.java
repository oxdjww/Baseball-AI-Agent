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
        var members = memberService.findAllSorted();
        long linkedCount = members.stream().filter(m -> m.getTelegramId() != null).count();
        model.addAttribute("members", members);
        model.addAttribute("totalCount", members.size());
        model.addAttribute("linkedCount", linkedCount);
        model.addAttribute("unlinkedCount", members.size() - linkedCount);
        return "admin/member";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("member", new Member());
        model.addAttribute("teams", Team.values());
        model.addAttribute("formAction", "/admin/members");
        return "admin/member_form";
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
        model.addAttribute("formAction", "/admin/members/" + id + "/edit");
        return "admin/member_form";
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
