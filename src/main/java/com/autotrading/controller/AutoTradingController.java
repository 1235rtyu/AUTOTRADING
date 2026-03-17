package com.autotrading.controller;

import com.autotrading.service.AutoTradingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/auto")
public class AutoTradingController {
    private final AutoTradingService autoTradingService;

    public AutoTradingController(AutoTradingService autoTradingService) {
        this.autoTradingService = autoTradingService;
    }

    @GetMapping("/start")
    @ResponseBody
    public String start(@RequestParam(name = "symbol", defaultValue = "005930") String symbol) {
        return autoTradingService.start(symbol);
    }

    @GetMapping("/stop")
    @ResponseBody
    public String stop() {
        return autoTradingService.stop();
    }

    @GetMapping("/status")
    @ResponseBody
    public String status() {
        return autoTradingService.status();
    }
}
