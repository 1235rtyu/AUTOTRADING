package com.autotrading.service.impl;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.model.DailyProfitPoint;
import com.autotrading.model.MonitorAggregateRow;
import com.autotrading.model.MonitorSummary;
import com.autotrading.service.AutoTradingService;
import com.autotrading.service.MonitorService;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MonitorServiceImpl implements MonitorService {
    private final AutoTradingDao autoTradingDao;
    private final AutoTradingService autoTradingService;
    private final KoreaInvestmentApiClient kisApiClient;

    public MonitorServiceImpl(AutoTradingDao autoTradingDao,
                              AutoTradingService autoTradingService,
                              KoreaInvestmentApiClient kisApiClient) {
        this.autoTradingDao = autoTradingDao;
        this.autoTradingService = autoTradingService;
        this.kisApiClient = kisApiClient;
    }

    @Override
    public MonitorSummary getSummary(String market) {
        String normalizedMarket = normalizeMarket(market);
        MonitorAggregateRow aggregate = autoTradingDao.findMonitorAggregate(normalizedMarket);

        double evaluationAmount = aggregate == null ? 0.0 : aggregate.getEvaluationAmount();
        double costAmount = aggregate == null ? 0.0 : aggregate.getCostAmount();
        double totalProfitAmount = evaluationAmount - costAmount;
        double totalProfitRate = costAmount > 0 ? (totalProfitAmount / costAmount) * 100.0 : 0.0;
        double todayBuyAmount = 0.0;
        double todaySellAmount = 0.0;
        double todaySignedAmount = autoTradingDao.findTodaySignedAmount(normalizedMarket);
        double todayRealizedAmount = todaySignedAmount;

        // Prefer broker-side ccld summary for KR so realized pnl matches actual fills.
        if ("KR".equals(normalizedMarket)) {
            boolean ccldLoaded = false;
            try {
                Map<String, Object> ccld = kisApiClient.fetchDomesticDailyCcldSummary();
                if ("OK".equals(String.valueOf(ccld.getOrDefault("status", "")))) {
                    double ccldBuy = parseAmount(ccld.get("todayBuyAmount"));
                    double ccldSell = parseAmount(ccld.get("todaySellAmount"));
                    double ccldRealized = parseAmount(ccld.get("todayRealizedProfitAmount"));
                    if (ccldBuy > 0 || ccldSell > 0 || Math.abs(ccldRealized) > 0) {
                        todayBuyAmount = ccldBuy;
                        todaySellAmount = ccldSell;
                        todayRealizedAmount = ccldRealized;
                        ccldLoaded = true;
                    }
                }
            } catch (Exception ignored) {
                // Fallback path below.
            }

            // Fallback: if ccld summary is unavailable, use balance snapshot totals.
            if (!ccldLoaded) {
                try {
                    Map<String, Object> balance = kisApiClient.fetchDomesticBalance();
                    if ("OK".equals(String.valueOf(balance.getOrDefault("status", "")))) {
                        Map<String, Object> summaryRow = firstRow(balance.get("output2"));
                        todayBuyAmount = parseAmount(summaryRow.get("thdt_buy_amt"));
                        todaySellAmount = parseAmount(summaryRow.get("thdt_sll_amt"));
                        if (todayBuyAmount > 0 || todaySellAmount > 0) {
                            todayRealizedAmount = todaySellAmount - todayBuyAmount;
                        }
                    }
                } catch (Exception ignored) {
                    // Final fallback keeps DB signed amount.
                }
            }
        }

        MonitorSummary summary = new MonitorSummary();
        summary.setMarket(normalizedMarket);
        summary.setTotalEvaluationAmount(evaluationAmount);
        summary.setTotalProfitAmount(totalProfitAmount);
        summary.setTotalProfitRate(totalProfitRate);
        summary.setTodayProfitAmount(todayRealizedAmount);
        summary.setTodayRealizedProfitAmount(todayRealizedAmount);
        summary.setTodayBuyAmount(todayBuyAmount);
        summary.setTodaySellAmount(todaySellAmount);
        summary.setHoldingCount(aggregate == null ? 0 : aggregate.getHoldingCount());
        summary.setRunningStrategyCount(countRunningStrategies(normalizedMarket));
        summary.setUpdatedAt(LocalDateTime.now());
        return summary;
    }

    @Override
    public List<DailyProfitPoint> getMonthlyDailyProfit(String market, int year, int month) {
        String normalizedMarket = normalizeMarket(market);
        YearMonth targetMonth = safeYearMonth(year, month);
        LocalDateTime startAt = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = targetMonth.plusMonths(1).atDay(1).atStartOfDay();
        return autoTradingDao.findDailySignedAmounts(normalizedMarket, startAt, endAt);
    }

    private int countRunningStrategies(String market) {
        int count = 0;
        List<Map<String, String>> running = autoTradingService.runningSymbols();
        for (Map<String, String> row : running) {
            String symbol = row.get("symbol");
            String exchange = row.get("exchange");
            boolean usSymbol = isUsSymbol(symbol, exchange);
            if ("US".equals(market) && usSymbol) {
                count++;
            }
            if ("KR".equals(market) && !usSymbol) {
                count++;
            }
        }
        return count;
    }

    private boolean isUsSymbol(String symbol, String exchange) {
        if (exchange != null && !exchange.isBlank()) {
            String ex = exchange.trim().toUpperCase(Locale.ROOT);
            return !("KRX".equals(ex) || "KR".equals(ex) || "KOSPI".equals(ex) || "KOSDAQ".equals(ex));
        }
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        return Character.isLetter(symbol.charAt(0));
    }

    private String normalizeMarket(String market) {
        return "US".equalsIgnoreCase(market) ? "US" : "KR";
    }

    private YearMonth safeYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException ignored) {
            return YearMonth.now();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstRow(Object output2) {
        if (output2 instanceof List && !((List<?>) output2).isEmpty()) {
            Object first = ((List<?>) output2).get(0);
            if (first instanceof Map) {
                return (Map<String, Object>) first;
            }
        }
        if (output2 instanceof Map) {
            return (Map<String, Object>) output2;
        }
        return Collections.emptyMap();
    }

    private double parseAmount(Object value) {
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value).replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }
}
