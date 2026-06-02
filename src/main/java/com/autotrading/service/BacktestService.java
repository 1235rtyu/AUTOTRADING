package com.autotrading.service;

import java.util.Map;

public interface BacktestService {
    /** Start async bar collection job; returns {"status":"STARTED","jobId":"..."} */
    Map<String, Object> startCollect(String market, String symbol, String startDate, String endDate);

    /** Poll progress of a collect job */
    Map<String, Object> getCollectStatus(String jobId);

    /** Run backtest synchronously on stored bars; returns full results */
    Map<String, Object> runBacktest(String market, String symbol, String startDate, String endDate,
                                    double buyAmount, com.autotrading.model.BacktestConfig config);
}
