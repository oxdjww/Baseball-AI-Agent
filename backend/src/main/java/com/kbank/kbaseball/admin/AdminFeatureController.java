package com.kbank.kbaseball.admin;

import com.kbank.kbaseball.config.featuretoggle.FeatureToggleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/features")
@RequiredArgsConstructor
public class AdminFeatureController {

    private final FeatureToggleService featureToggleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("settings", featureToggleService.findAll());
        return "admin/feature";
    }

    @PostMapping("/{key}/toggle")
    public String toggle(@PathVariable String key) {
        featureToggleService.toggle(key);
        return "redirect:/admin/features";
    }
}
