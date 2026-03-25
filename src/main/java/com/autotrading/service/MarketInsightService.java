package com.autotrading.service;

import java.util.Map;

public interface MarketInsightService {

    Map<String, Object> getHtsTopView();

    Map<String, Object> getIntradayChart(String symbol, String fromTime);

    Map<String, Object> getRanking(String market, String exchange);

    /**
     * 차트 데이터 조회.
     *
     * @param market    "KR" 또는 "US"
     * @param symbol    종목 코드
     * @param timeframe 타임프레임 (1m, 5m, 15m, 30m, 60m, 1d, 1w, 1mo)
     * @param exchange  거래소 코드
     * @param date      분봉 과거 조회용 날짜 (YYYYMMDD). null이면 당일/최신
     */
    Map<String, Object> getChart(String market,
                                 String symbol,
                                 String timeframe,
                                 String exchange,
                                 String date);

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