package com.autotrading.service.impl;

import com.autotrading.scheduler.SchedulerService;
import com.autotrading.service.AutoTradingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AutoTradingServiceImpl implements AutoTradingService {
    private final SchedulerService schedulerService;

    public AutoTradingServiceImpl(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public String start(String symbol) {
        return schedulerService.start(symbol);
    }

    @Override
    public String start(String symbol, String exchange) {
        return schedulerService.start(symbol, exchange);
    }

    @Override
    public String start(String symbol, String exchange, Double buyAmount) {
        return schedulerService.start(symbol, exchange, buyAmount);
    }

    @Override
    public String stop() {
        return schedulerService.stop();
    }

    @Override
    public String stopSymbol(String symbol) {
        return schedulerService.stopSymbol(symbol);
    }

    @Override
    public String status() {
        return schedulerService.status();
    }

    @Override
    public List<Map<String, String>> runningSymbols() {
        return schedulerService.runningSymbols();
    }
}
