package com.autotrading.controller;

import com.autotrading.service.AutoTradingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/control")
public class AutoControlController {
    private final AutoTradingService autoTradingService;

    public AutoControlController(AutoTradingService autoTradingService) {
        this.autoTradingService = autoTradingService;
    }

    @GetMapping
    public String control(@RequestParam(name = "message", required = false) String message, Model model) {
        model.addAttribute("status", autoTradingService.status());
        model.addAttribute("message", message);
        model.addAttribute("market", "KR"); // 기본은 국내
        return "control";
    }

    @GetMapping("/kr")
    public String controlKr(@RequestParam(name = "message", required = false) String message, Model model) {
        model.addAttribute("status", autoTradingService.status());
        model.addAttribute("message", message);
        model.addAttribute("market", "KR");
        return "control";
    }

    @GetMapping("/us")
    public String controlUs(@RequestParam(name = "message", required = false) String message, Model model) {
        model.addAttribute("status", autoTradingService.status());
        model.addAttribute("message", message);
        model.addAttribute("market", "US");
        return "control";
    }

    @PostMapping("/start")
    public String start(@RequestParam(name = "symbol", defaultValue = "005930") String symbol) {
        String result = autoTradingService.start(symbol);
        return "redirect:/control?message=" + URLEncoder.encode(result, StandardCharsets.UTF_8);
    }

    @PostMapping("/stop")
    public String stop() {
        String result = autoTradingService.stop();
        return "redirect:/control?message=" + URLEncoder.encode(result, StandardCharsets.UTF_8);
    }
}
