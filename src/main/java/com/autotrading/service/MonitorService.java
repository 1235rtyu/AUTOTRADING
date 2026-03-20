package com.autotrading.service;

import com.autotrading.model.DailyProfitPoint;
import com.autotrading.model.MonitorSummary;

import java.util.List;

public interface MonitorService {
    MonitorSummary getSummary(String market);
    List<DailyProfitPoint> getMonthlyDailyProfit(String market, int year, int month);
}
