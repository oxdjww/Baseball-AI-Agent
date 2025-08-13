package com.kbank.baa.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RootController {
    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "health";
    }

    @GetMapping("/health-check")
    @ResponseBody
    public String health_check() {
        return "health-check";
    }

    @GetMapping("/healthcheck")
    @ResponseBody
    public String healthcheck() {
        return "healthcheck";
    }
}
