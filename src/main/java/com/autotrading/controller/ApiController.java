package com.autotrading.controller;

import com.autotrading.model.DashboardData;
import com.autotrading.model.OrderLog;
import com.autotrading.model.WatchlistItem;
import com.autotrading.service.AutoTradingService;
import com.autotrading.service.DashboardService;
import com.autotrading.service.HistoryService;
import com.autotrading.service.MarketInsightService;
import com.autotrading.service.WatchlistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DashboardService dashboardService;
    private final HistoryService historyService;
    private final WatchlistService watchlistService;
    private final AutoTradingService autoTradingService;
    private final MarketInsightService marketInsightService;

    public ApiController(DashboardService dashboardService,
                         HistoryService historyService,
                         WatchlistService watchlistService,
                         AutoTradingService autoTradingService,
                         MarketInsightService marketInsightService) {
        this.dashboardService    = dashboardService;
        this.historyService      = historyService;
        this.watchlistService    = watchlistService;
        this.autoTradingService  = autoTradingService;
        this.marketInsightService= marketInsightService;
    }

    /* ── Dashboard ── */
    @GetMapping("/dashboard")
    public DashboardData dashboard(@RequestParam(name="limit", defaultValue="10") int limit) {
        return dashboardService.load(limit);
    }

    /* ── Orders ── */
    @GetMapping("/orders")
    public List<OrderLog> orders(@RequestParam(name="limit", defaultValue="50") int limit) {
        return historyService.getRecentOrders(limit);
    }

    /* ── Control ── */
    @GetMapping("/control/status")
    public Map<String, Object> controlStatus() {
        return Map.of("status", autoTradingService.status());
    }

    @PostMapping("/control/start")
    public Map<String, Object> controlStart(
            @RequestParam(name="symbol", defaultValue="005930") String symbol) {
        String result = autoTradingService.start(symbol);
        return Map.of("status", autoTradingService.status(), "message", result);
    }

    @PostMapping("/control/stop")
    public Map<String, Object> controlStop() {
        String result = autoTradingService.stop();
        return Map.of("status", autoTradingService.status(), "message", result);
    }

    /* ── Watchlist ── */
    @GetMapping("/watchlist")
    public List<WatchlistItem> watchlist() {
        return watchlistService.getWatchlist();
    }

    @PostMapping("/watchlist")
    public Map<String, Object> addWatchlist(@RequestParam("symbol") String symbol) {
        watchlistService.addSymbol(symbol);
        return Map.of("status", "OK");
    }

    @PostMapping("/watchlist/delete")
    public Map<String, Object> deleteWatchlist(@RequestParam("id") int id) {
        watchlistService.remove(id);
        return Map.of("status", "OK");
    }

    /* ──────────────────────────────────────────────
       종목 자동매매 ON/OFF 토글
       watchlist에 있는 특정 종목의 자동매매를 개별 제어
       ────────────────────────────────────────────── */
    @PostMapping("/control/toggle")
    public Map<String, Object> toggleSymbol(
            @RequestParam("symbol") String symbol,
            @RequestParam("enable") boolean enable) {
        String result = enable
                ? autoTradingService.start(symbol)
                : autoTradingService.stop();
        return Map.of(
                "status",  autoTradingService.status(),
                "message", result,
                "symbol",  symbol,
                "enabled", enable
        );
    }

    /* ──────────────────────────────────────────────
       거래량 TOP N 자동 watchlist 등록
       KIS 거래량순위 상위 N개를 watchlist에 일괄 추가
       ────────────────────────────────────────────── */
    @PostMapping("/watchlist/add-top")
    public Map<String, Object> addTopVolume(
            @RequestParam(name="n",       defaultValue="5") int    n,
            @RequestParam(name="minRate", defaultValue="0") double minRate,
            @RequestParam(name="market",  defaultValue="KR") String market,
            @RequestParam(name="exch",    defaultValue="NAS") String exchange) {
        try {
            Map<String, Object> ranking = marketInsightService.getRanking(market, exchange);

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> output =
                    (java.util.List<Map<String, Object>>) ranking.get("data");

            if (output == null || output.isEmpty()) {
                return Map.of("status", "ERROR", "message", "랭킹 데이터 없음", "added", 0);
            }

            int added = 0;
            for (int i = 0; i < Math.min(n, output.size()); i++) {
                Map<String, Object> item = output.get(i);

                // getRanking()이 정규화한 "symbol" 필드 사용
                String symbol = String.valueOf(
                        item.getOrDefault("symbol",
                        item.getOrDefault("mksc_shrn_iscd", "")));
                if (symbol.isBlank() || symbol.equals("null")) continue;

                // 등락률 필터
                if (minRate > 0) {
                    double rate = 0;
                    try {
                        rate = Double.parseDouble(
                                String.valueOf(item.getOrDefault("prdy_ctrt", "0")));
                    } catch (NumberFormatException ignored) {}
                    if (Math.abs(rate) < minRate) continue;
                }

                try {
                    watchlistService.addSymbol(symbol);
                    added++;
                } catch (Exception ignored) {} // 중복 무시
            }
            return Map.of("status", "OK", "added", added, "message", added + "개 종목 추가 완료");
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage(), "added", 0);
        }
    }

    /* ──────────────────────────────────────────────
       거래량 TOP 조건 자동매매 시작
       랭킹에서 조건에 맞는 종목을 watchlist에 추가 후 엔진 시작
       ────────────────────────────────────────────── */
    @PostMapping("/control/start-top")
    public Map<String, Object> startTopVolume(
            @RequestParam(name="n",       defaultValue="3")  int    n,
            @RequestParam(name="minRate", defaultValue="0")  double minRate,
            @RequestParam(name="market",  defaultValue="KR") String market,
            @RequestParam(name="exch",    defaultValue="NAS") String exchange) {
        try {
            Map<String, Object> ranking = marketInsightService.getRanking(market, exchange);

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> output =
                    (java.util.List<Map<String, Object>>) ranking.get("data"); // ★ "data"

            if (output == null || output.isEmpty()) {
                return Map.of("status", "ERROR", "message", "랭킹 데이터 없음");
            }

            int added = 0;
            String firstSymbol = "";

            for (int i = 0; i < output.size() && added < n; i++) {
                Map<String, Object> item = output.get(i);

                // getRanking()이 정규화한 "symbol" 필드 사용
                String symbol = String.valueOf(
                        item.getOrDefault("symbol",
                        item.getOrDefault("mksc_shrn_iscd", "")));
                if (symbol.isBlank() || symbol.equals("null")) continue;

                // 등락률 필터
                if (minRate > 0) {
                    double rate = 0;
                    try {
                        rate = Double.parseDouble(
                                String.valueOf(item.getOrDefault("prdy_ctrt",
                                item.getOrDefault("diff_rate", "0"))));
                    } catch (NumberFormatException ignored) {}
                    if (Math.abs(rate) < minRate) continue;
                }

                try { watchlistService.addSymbol(symbol); } catch (Exception ignored) {}
                if (added == 0) firstSymbol = symbol;
                added++;
            }

            if (firstSymbol.isBlank()) {
                return Map.of("status", "ERROR", "message", "조건에 맞는 종목 없음");
            }

            String result = autoTradingService.start(firstSymbol);
            return Map.of(
                    "status",      autoTradingService.status(),
                    "message",     added + "개 종목 등록 후 자동매매 시작: " + result,
                    "added",       added,
                    "firstSymbol", firstSymbol
            );
        } catch (Exception e) {
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    /* ── Market ── */
    @GetMapping("/market/ranking/hts")
    public Map<String, Object> htsTopView() {
        return marketInsightService.getHtsTopView();
    }

    @GetMapping("/market/chart/time")
    public Map<String, Object> timeChart(
            @RequestParam(name="symbol", defaultValue="005930") String symbol,
            @RequestParam(name="from",   defaultValue="090000") String fromTime) {
        return marketInsightService.getIntradayChart(symbol, fromTime);
    }

    @GetMapping("/market/ranking")
    public Map<String, Object> ranking(
            @RequestParam(name="market", defaultValue="KR")  String market,
            @RequestParam(name="exch",   defaultValue="NAS") String exchange) {
        return marketInsightService.getRanking(market, exchange);
    }

    @GetMapping("/market/chart")
    public Map<String, Object> chart(
            @RequestParam(name="market", defaultValue="KR")    String market,
            @RequestParam(name="symbol", defaultValue="005930") String symbol,
            @RequestParam(name="tf",     defaultValue="1m")     String timeframe,
            @RequestParam(name="exch",   defaultValue="NAS")    String exchange) {
        return marketInsightService.getChart(market, symbol, timeframe, exchange);
    }

    /* ===== Account / Balance ===== */
    @GetMapping("/account/balance/kr")
    public Map<String, Object> balanceKr() {
        return marketInsightService.getDomesticBalance();
    }

    @GetMapping("/account/balance/us")
    public Map<String, Object> balanceUs(
            @RequestParam(name="exch", defaultValue="NASD") String exchange,
            @RequestParam(name="currency", defaultValue="USD") String currency) {
        return marketInsightService.getOverseasBalance(exchange, currency);
    }

    @GetMapping("/account/cash/us")
    public Map<String, Object> cashUs(
            @RequestParam(name="currency", defaultValue="USD") String currency) {
        return marketInsightService.getOverseasCash(currency);
    }
}
