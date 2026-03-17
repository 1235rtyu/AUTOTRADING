package com.autotrading.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/balances")
public class BalanceController {

    @GetMapping
    public String balances() {
        return "balances";
    }
}
