package com.autotrading.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/monitor")
public class MonitorController {

    @GetMapping
    public String monitor() {
        return "monitor";
    }
}
