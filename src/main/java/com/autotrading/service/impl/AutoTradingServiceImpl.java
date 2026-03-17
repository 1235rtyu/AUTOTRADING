package com.autotrading.service.impl;

import com.autotrading.scheduler.SchedulerService;
import com.autotrading.service.AutoTradingService;
import org.springframework.stereotype.Service;

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
    public String stop() {
        return schedulerService.stop();
    }

    @Override
    public String status() {
        return schedulerService.status();
    }
}
