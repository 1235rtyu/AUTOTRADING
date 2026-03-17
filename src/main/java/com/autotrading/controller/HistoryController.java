package com.autotrading.controller;

import com.autotrading.service.HistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/history")
public class HistoryController {
    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", historyService.getRecentOrders(50));
        return "orders";
    }
}
