package com.autotrading.service.impl;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.model.DailyProfitPoint;
import com.autotrading.model.MonitorAggregateRow;
import com.autotrading.model.MonitorSummary;
import com.autotrading.service.AutoTradingService;
import com.autotrading.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MonitorServiceImpl implements MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorServiceImpl.class);

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

        // DB 기본값 (fallback용)
        MonitorAggregateRow aggregate = autoTradingDao.findMonitorAggregate(normalizedMarket);
        double evaluationAmount = aggregate == null ? 0.0 : aggregate.getEvaluationAmount();
        double costAmount       = aggregate == null ? 0.0 : aggregate.getCostAmount();
        int    holdingCount     = aggregate == null ? 0   : aggregate.getHoldingCount();

        double totalProfitAmount = evaluationAmount - costAmount;
        double totalProfitRate   = costAmount > 0 ? (totalProfitAmount / costAmount) * 100.0 : 0.0;
        double todayBuyAmount      = 0.0;
        double todaySellAmount     = 0.0;
        double todayRealizedAmount = autoTradingDao.findTodaySignedAmount(normalizedMarket);

        if ("US".equals(normalizedMarket)) {
            try {
                String today = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                Map<String, Object> pp = kisApiClient.fetchOverseasPeriodProfit(today, today);
                if ("OK".equals(String.valueOf(pp.getOrDefault("status", "")))) {
                    todayRealizedAmount = parseAmount(pp.get("ovrsRlztPflsTotAmt"));
                    todayBuyAmount      = parseAmount(pp.get("stckBuyAmtSmtl"));
                    todaySellAmount     = parseAmount(pp.get("stckSllAmtSmtl"));
                    double evlu = parseAmount(pp.get("evluPflsSmtlAmt"));
                    if (evlu != 0.0) {
                        totalProfitAmount = evlu;
                        totalProfitRate = costAmount > 0 ? (evlu / costAmount) * 100.0 : 0.0;
                    }
                }
            } catch (Exception e) {
                logger.warn("fetchOverseasPeriodProfit failed: {}", e.getMessage());
            }
        } else if ("KR".equals(normalizedMarket)) {
            // 1순위: 주식잔고조회_실현손익 (TTTC8494R) — 수수료/세금 포함 공식 실현손익
            boolean rlzLoaded = false;
            try {
                Map<String, Object> rlz = kisApiClient.fetchDomesticRealizedPnl();
                if ("OK".equals(String.valueOf(rlz.getOrDefault("status", "")))) {
                    double rlztPfls   = parseAmount(rlz.get("rlztPfls"));
                    double totEvlu    = parseAmount(rlz.get("totEvluAmt"));
                    double evluPfls   = parseAmount(rlz.get("evluPflsSmtlAmt"));
                    double thdtBuy    = parseAmount(rlz.get("thdtBuyAmt"));
                    double thdtSll    = parseAmount(rlz.get("thdtSllAmt"));

                    todayRealizedAmount = rlztPfls;
                    todayBuyAmount      = thdtBuy;
                    todaySellAmount     = thdtSll;

                    // 평가금액/손익도 실시간 값으로 덮어씀
                    if (totEvlu > 0) {
                        evaluationAmount = totEvlu;
                        totalProfitAmount = evluPfls;
                        totalProfitRate = costAmount > 0 ? (evluPfls / costAmount) * 100.0 : 0.0;
                    }
                    rlzLoaded = true;
                }
            } catch (Exception e) {
                logger.warn("fetchDomesticRealizedPnl failed, trying ccld fallback: {}", e.getMessage());
            }

            // 2순위: CCLD FIFO 계산 (실현손익만, 수수료 미포함)
            if (!rlzLoaded) {
                try {
                    Map<String, Object> ccld = kisApiClient.fetchDomesticDailyCcldSummary();
                    if ("OK".equals(String.valueOf(ccld.getOrDefault("status", "")))) {
                        todayBuyAmount      = parseAmount(ccld.get("todayBuyAmount"));
                        todaySellAmount     = parseAmount(ccld.get("todaySellAmount"));
                        todayRealizedAmount = parseAmount(ccld.get("todayRealizedProfitAmount"));
                    }
                } catch (Exception e) {
                    logger.warn("fetchDomesticDailyCcldSummary failed: {}", e.getMessage());
                    // DB signed amount 유지
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
        summary.setHoldingCount(holdingCount);
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

    @Override
    public Map<String, Object> getExchangeRate() {
        try {
            Map<String, Object> raw = kisApiClient.fetchUsdKrwExchange();
            if (!"OK".equals(String.valueOf(raw.getOrDefault("status", "")))) {
                return Map.of("status", "ERROR", "message",
                        String.valueOf(raw.getOrDefault("message", "API error")));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) raw.getOrDefault("output", Map.of());
            // field priority: bstp_nmix_prpr > stck_prpr > price
            String rateStr = null;
            for (String key : new String[]{"bstp_nmix_prpr", "stck_prpr", "price"}) {
                Object v = output.get(key);
                if (v != null && !String.valueOf(v).isBlank()) { rateStr = String.valueOf(v); break; }
            }
            double rate = parseAmount(rateStr);
            String changeStr = null;
            for (String key : new String[]{"bstp_nmix_prdy_ctrt", "prdy_ctrt", "change"}) {
                Object v = output.get(key);
                if (v != null && !String.valueOf(v).isBlank()) { changeStr = String.valueOf(v); break; }
            }
            return Map.of("status", "OK", "rate", rate,
                    "change", parseAmount(changeStr), "rawOutput", output);
        } catch (Exception e) {
            logger.warn("getExchangeRate failed: {}", e.getMessage());
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
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
