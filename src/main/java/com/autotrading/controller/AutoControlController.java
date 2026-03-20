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
        return controlKr(message, model);
    }

    @GetMapping("/kr")
    public String controlKr(@RequestParam(name = "message", required = false) String message, Model model) {
        model.addAttribute("status", autoTradingService.status());
        model.addAttribute("message", message);
        model.addAttribute("market", "KR");
        return "control_kr";
    }

    @GetMapping("/us")
    public String controlUs(@RequestParam(name = "message", required = false) String message, Model model) {
        model.addAttribute("status", autoTradingService.status());
        model.addAttribute("message", message);
        model.addAttribute("market", "US");
        return "control_us";
    }

    @PostMapping("/start")
    public String start(@RequestParam(name = "symbol", defaultValue = "005930") String symbol,
                        @RequestParam(name = "exchange", required = false) String exchange,
                        @RequestParam(name = "buyAmount", required = false) Double buyAmount,
                        @RequestParam(name = "market", defaultValue = "KR") String market) {
        String result = autoTradingService.start(symbol, exchange, buyAmount);
        return "redirect:" + ("US".equalsIgnoreCase(market) ? "/control/us" : "/control/kr")
                + "?message=" + URLEncoder.encode(result, StandardCharsets.UTF_8);
    }

    @PostMapping("/stop")
    public String stop(@RequestParam(name = "market", defaultValue = "KR") String market) {
        String result = autoTradingService.stop();
        return "redirect:" + ("US".equalsIgnoreCase(market) ? "/control/us" : "/control/kr")
                + "?message=" + URLEncoder.encode(result, StandardCharsets.UTF_8);
    }
}
