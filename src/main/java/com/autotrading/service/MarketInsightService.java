package com.autotrading.service;

import java.util.Map;

public interface MarketInsightService {
    Map<String, Object> getHtsTopView();

    Map<String, Object> getIntradayChart(String symbol, String fromTime);

    Map<String, Object> getRanking(String market, String exchange);

    Map<String, Object> getChart(String market, String symbol, String timeframe, String exchange);

    Map<String, Object> getMarketIndex();

    Map<String, Object> getDomesticBalance();

    Map<String, Object> getDomesticHoldings();

    Map<String, Object> getDomesticDailyCcld(String startDate,
                                             String endDate,
                                             String sideCode,
                                             String ccldDvsn,
                                             String exchangeId);

    Map<String, Object> getOverseasBalance(String exchange, String currency);

    Map<String, Object> getOverseasHoldings(String exchange, String currency);

    Map<String, Object> getOverseasCash(String currency);
}
