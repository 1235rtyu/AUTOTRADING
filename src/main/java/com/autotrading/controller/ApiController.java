package com.autotrading.controller;

import com.autotrading.model.DashboardData;
import com.autotrading.model.AutoPosition;
import com.autotrading.model.MonitorSummary;
import com.autotrading.model.OrderLog;
import com.autotrading.model.WatchlistItem;
import com.autotrading.position.PositionService;
import com.autotrading.service.AutoTradingService;
import com.autotrading.service.DashboardService;
import com.autotrading.service.HistoryService;
import com.autotrading.service.MarketInsightService;
import com.autotrading.service.MonitorService;
import com.autotrading.service.WatchlistService;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final MonitorService monitorService;
    private final PositionService positionService;

    public ApiController(DashboardService dashboardService,
                         HistoryService historyService,
                         WatchlistService watchlistService,
                         AutoTradingService autoTradingService,
                         MarketInsightService marketInsightService,
                         MonitorService monitorService,
                         PositionService positionService) {
        this.dashboardService    = dashboardService;
        this.historyService      = historyService;
        this.watchlistService    = watchlistService;
        this.autoTradingService  = autoTradingService;
        this.marketInsightService= marketInsightService;
        this.monitorService      = monitorService;
        this.positionService     = positionService;
    }

    /* ?? Dashboard ?? */
    @GetMapping("/dashboard")
    public DashboardData dashboard(@RequestParam(name="limit", defaultValue="10") int limit) {
        return dashboardService.load(limit);
    }

    @GetMapping("/monitor/summary")
    public MonitorSummary monitorSummary(@RequestParam(name = "market", defaultValue = "KR") String market) {
        return monitorService.getSummary(market);
    }

    @GetMapping("/monitor/calendar")
    public Map<String, Object> monitorCalendar(@RequestParam(name = "market", defaultValue = "KR") String market,
                                               @RequestParam(name = "year", required = false) Integer year,
                                               @RequestParam(name = "month", required = false) Integer month) {
        YearMonth target = YearMonth.now();
        if (year != null && month != null) {
            try { target = YearMonth.of(year, month); } catch (Exception ignored) { target = YearMonth.now(); }
        }
        String normalizedMarket = "US".equalsIgnoreCase(market) ? "US" : "KR";
        return Map.of(
                "market", normalizedMarket,
                "year",   target.getYear(),
                "month",  target.getMonthValue(),
                "data",   monitorService.getMonthlyDailyProfit(normalizedMarket, target.getYear(), target.getMonthValue())
        );
    }

    /* ?? Orders ?? */
    @GetMapping("/orders")
    public List<OrderLog> orders(@RequestParam(name="limit", defaultValue="50") int limit) {
        return historyService.getRecentOrders(limit);
    }

    @GetMapping("/orders/kr")
    public List<OrderLog> ordersKr(@RequestParam(name="limit", defaultValue="50") int limit) {
        return historyService.getRecentKrOrders(limit);
    }

    @GetMapping("/orders/us")
    public List<OrderLog> ordersUs(@RequestParam(name="limit", defaultValue="50") int limit) {
        return historyService.getRecentUsOrders(limit);
    }

    /* ?? Control ?? */
    @GetMapping("/control/status")
    public Map<String, Object> controlStatus() {
        return Map.of("status", autoTradingService.status());
    }

    @GetMapping("/control/running")
    public Map<String, Object> controlRunning() {
        List<Map<String, String>> symbols = autoTradingService.runningSymbols();
        return Map.of("status", autoTradingService.status(), "count", symbols.size(), "symbols", symbols);
    }

    @PostMapping("/control/start")
    public Map<String, Object> controlStart(
            @RequestParam(name="symbol", defaultValue="005930") String symbol,
            @RequestParam(name="exchange", required = false) String exchange,
            @RequestParam(name="buyAmount", required = false) Double buyAmount) {
        String result = autoTradingService.start(symbol, exchange, buyAmount);
        return Map.of("status", autoTradingService.status(), "message", result);
    }

    @PostMapping("/control/stop")
    public Map<String, Object> controlStop() {
        String result = autoTradingService.stop();
        return Map.of("status", autoTradingService.status(), "message", result);
    }

    /* ?? Watchlist ?? */
    @GetMapping("/watchlist")
    public List<WatchlistItem> watchlist() {
        return watchlistService.getWatchlist();
    }

    @GetMapping("/watchlist/name")
    public Map<String, Object> watchlistName(@RequestParam("symbol") String symbol) {
        String target = symbol == null ? "" : symbol.trim().toUpperCase();
        if (target.isBlank()) return Map.of("symbol", "", "name", "", "symbolName", "");
        AutoPosition position = positionService.getPosition(target);
        String symbolName = (position != null && position.getSymbolName() != null) ? position.getSymbolName() : "";
        return Map.of("symbol", target, "name", symbolName, "symbolName", symbolName);
    }

    @PostMapping("/watchlist")
    public Map<String, Object> addWatchlist(@RequestParam("symbol") String symbol,
                                            @RequestParam(name="exchange", required = false) String exchange) {
        watchlistService.addSymbol(symbol, exchange);
        return Map.of("status", "OK");
    }

    @PostMapping("/watchlist/delete")
    public Map<String, Object> deleteWatchlist(@RequestParam("id") int id) {
        watchlistService.remove(id);
        return Map.of("status", "OK");
    }

    @PostMapping("/control/toggle")
    public Map<String, Object> toggleSymbol(
            @RequestParam("symbol") String symbol,
            @RequestParam("enable") boolean enable,
            @RequestParam(name="exchange", required = false) String exchange,
            @RequestParam(name="buyAmount", required = false) Double buyAmount) {
        String result = enable
                ? autoTradingService.start(symbol, exchange, buyAmount)
                : autoTradingService.stopSymbol(symbol);
        return Map.of("status", autoTradingService.status(), "message", result, "symbol", symbol, "enabled", enable);
    }

    @PostMapping("/watchlist/add-top")
    public Map<String, Object> addTopVolume(
            @RequestParam(name="n",       defaultValue="5")  int    n,
            @RequestParam(name="minRate", defaultValue="0")  double minRate,
            @RequestParam(name="market",  defaultValue="KR") String market,
            @RequestParam(name="exch",    defaultValue="NAS") String exchange) {
        try {
            Map<String, Object> ranking = marketInsightService.getRanking(market, exchange);
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> output = (java.util.List<Map<String, Object>>) ranking.get("data");
            if (output == null || output.isEmpty()) return Map.of("status", "ERROR", "message", "??궧 ?곗씠???놁쓬", "added", 0);
            int added = 0;
            for (int i = 0; i < Math.min(n, output.size()); i++) {
                Map<String, Object> item = output.get(i);
                String symbol = String.valueOf(item.getOrDefault("symbol", item.getOrDefault("mksc_shrn_iscd", "")));
                if (symbol.isBlank() || symbol.equals("null")) continue;
                if (minRate > 0) {
                    try { if (Math.abs(Double.parseDouble(String.valueOf(item.getOrDefault("prdy_ctrt","0")))) < minRate) continue; } catch (NumberFormatException ignored) {}
                }
                try { watchlistService.addSymbol(symbol, "US".equalsIgnoreCase(market) ? exchange : "KRX"); added++; } catch (Exception ignored) {}
            }
            return Map.of("status", "OK", "added", added, "message", added + "媛?醫낅ぉ 異붽? ?꾨즺");
        } catch (Exception e) { return Map.of("status", "ERROR", "message", e.getMessage(), "added", 0); }
    }

    @PostMapping("/control/start-top")
    public Map<String, Object> startTopVolume(
            @RequestParam(name="n",       defaultValue="3")  int    n,
            @RequestParam(name="minRate", defaultValue="0")  double minRate,
            @RequestParam(name="market",  defaultValue="KR") String market,
            @RequestParam(name="exch",    defaultValue="NAS") String exchange,
            @RequestParam(name="buyAmount", required = false) Double buyAmount) {
        try {
            Map<String, Object> ranking = marketInsightService.getRanking(market, exchange);
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> output = (java.util.List<Map<String, Object>>) ranking.get("data");
            if (output == null || output.isEmpty()) return Map.of("status", "ERROR", "message", "??궧 ?곗씠???놁쓬");
            int added = 0; String firstSymbol = "";
            for (int i = 0; i < output.size() && added < n; i++) {
                Map<String, Object> item = output.get(i);
                String symbol = String.valueOf(item.getOrDefault("symbol", item.getOrDefault("mksc_shrn_iscd", "")));
                if (symbol.isBlank() || symbol.equals("null")) continue;
                if (minRate > 0) {
                    try { if (Math.abs(Double.parseDouble(String.valueOf(item.getOrDefault("prdy_ctrt", item.getOrDefault("diff_rate","0"))))) < minRate) continue; } catch (NumberFormatException ignored) {}
                }
                try { watchlistService.addSymbol(symbol, "US".equalsIgnoreCase(market) ? exchange : "KRX"); } catch (Exception ignored) {}
                if (added == 0) firstSymbol = symbol;
                added++;
            }
            if (firstSymbol.isBlank()) return Map.of("status", "ERROR", "message", "議곌굔??留욌뒗 醫낅ぉ ?놁쓬");
            String result = autoTradingService.start(firstSymbol, "US".equalsIgnoreCase(market) ? exchange : null, buyAmount);
            return Map.of("status", autoTradingService.status(), "message", added + "媛?醫낅ぉ ?깅줉 ???먮룞留ㅻℓ ?쒖옉: " + result, "added", added, "firstSymbol", firstSymbol);
        } catch (Exception e) { return Map.of("status", "ERROR", "message", e.getMessage()); }
    }

    /* ?? Market ?? */
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

    /**
     * Symbol suggestion API (market-aware).
     * - Primarily uses market ranking API (live source)
     * - Supplements with watchlist + recent orders (user context)
     * - Returns de-duplicated candidates for autocomplete dropdown
     */
    @GetMapping("/market/symbol-suggest")
    public Map<String, Object> symbolSuggest(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "market", defaultValue = "US") String market,
            @RequestParam(name = "exch", defaultValue = "NAS") String exchange,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        String query = q == null ? "" : q.trim();
        String safeMarket = "KR".equalsIgnoreCase(market) ? "KR" : "US";
        int safeLimit = Math.max(1, Math.min(limit, 20));
        if (query.isEmpty()) {
            return Map.of("status", "OK", "market", safeMarket, "query", "", "data", List.of());
        }
        String queryLower = query.toLowerCase();

        LinkedHashMap<String, Map<String, Object>> dedup = new LinkedHashMap<>();

        // 1) Live source: ranking API candidates (primary)
        try {
            Map<String, Object> ranking = marketInsightService.getRanking(safeMarket, exchange);
            Object dataObj = ranking.get("data");
            if (dataObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) dataObj;
                for (Map<String, Object> row : rows) {
                    String symbol = text(row.get("symbol")).toUpperCase();
                    String name = text(row.get("name"));
                    if (symbol.isEmpty()) continue;
                    if (!matchesQuery(queryLower, symbol, name)) continue;
                    putCandidate(dedup, symbol, name, safeMarket, exchange, "ranking");
                    if (dedup.size() >= safeLimit) break;
                }
            }
        } catch (Exception ignored) {
            // Suggestions are best-effort; keep response stable.
        }

        // 2) User context: watchlist
        if (dedup.size() < safeLimit) {
            for (WatchlistItem item : watchlistService.getWatchlist()) {
                String symbol = text(item.getSymbol()).toUpperCase();
                if (symbol.isEmpty()) continue;
                boolean isUs = Character.isLetter(symbol.charAt(0));
                if ("US".equals(safeMarket) != isUs) continue;
                if (!matchesQuery(queryLower, symbol, "")) continue;
                putCandidate(dedup, symbol, "", safeMarket, exchange, "watchlist");
                if (dedup.size() >= safeLimit) break;
            }
        }

        // 3) User context: recent orders (has symbolName)
        if (dedup.size() < safeLimit) {
            List<OrderLog> recent = "US".equals(safeMarket)
                    ? historyService.getRecentUsOrders(200)
                    : historyService.getRecentKrOrders(200);
            for (OrderLog row : recent) {
                String symbol = text(row.getSymbol()).toUpperCase();
                String name = text(row.getSymbolName());
                if (symbol.isEmpty()) continue;
                if (!matchesQuery(queryLower, symbol, name)) continue;
                putCandidate(dedup, symbol, name, safeMarket, exchange, "orders");
                if (dedup.size() >= safeLimit) break;
            }
        }

        // 4) Always allow typed symbol pass-through for unknown symbols.
        String typedUpper = query.toUpperCase();
        boolean typedAllowed = ("US".equals(safeMarket) && typedUpper.matches("^[A-Z][A-Z0-9.\\-]{0,9}$"))
                || ("KR".equals(safeMarket) && typedUpper.matches("^[0-9]{6}$"));
        if (typedAllowed && !dedup.containsKey(typedUpper)) {
            putCandidate(dedup, typedUpper, "Typed symbol", safeMarket, exchange, "typed");
        }

        List<Map<String, Object>> data = new ArrayList<>(dedup.values());
        if (data.size() > safeLimit) {
            data = data.subList(0, safeLimit);
        }
        return Map.of("status", "OK", "market", safeMarket, "query", query, "data", data);
    }

    @GetMapping("/market/chart")
    public Map<String, Object> chart(
            @RequestParam(name="market", defaultValue="KR")     String market,
            @RequestParam(name="symbol", defaultValue="005930") String symbol,
            @RequestParam(name="tf",     defaultValue="1m")     String timeframe,
            @RequestParam(name="exch",   defaultValue="NAS")    String exchange) {
        return marketInsightService.getChart(market, symbol, timeframe, exchange);
    }

    /* ?????????????????????????????????????????????????????????????????
       [NEW] /api/market/index
       ???붾㈃ 吏??移대뱶??
       KOSPI / KOSDAQ / S&P500 / NASDAQ / DOW / USD/KRW 6媛?吏??諛섑솚.

       ?묐떟 ?뺤떇:
       {
         "status": "OK",
         "data": [
           { "name":"KOSPI",   "price":2580.5,  "change":0.42,  "point": 10.8  },
           { "name":"KOSDAQ",  "price":745.2,   "change":-0.11, "point":-0.8   },
           { "name":"S&P 500", "price":5432.1,  "change":0.35,  "point": 18.9  },
           { "name":"NASDAQ",  "price":17280.3, "change":0.61,  "point":104.2  },
           { "name":"DOW",     "price":38940.2, "change":-0.08, "point":-31.4  },
           { "name":"USD/KRW", "price":1358.0,  "change":0.12,  "point":  1.6  }
         ]
       }
    ???????????????????????????????????????????????????????????????? */
    @GetMapping("/market/index")
    public Map<String, Object> marketIndex() {
        return marketInsightService.getMarketIndex();
    }

    /* ?? Account / Balance ?? */
    @GetMapping("/account/balance/kr")
    public Map<String, Object> balanceKr() {
        return marketInsightService.getDomesticBalance();
    }

    @GetMapping("/account/balance/us")
    public Map<String, Object> balanceUs(
            @RequestParam(name="exch",     defaultValue="NASD") String exchange,
            @RequestParam(name="currency", defaultValue="USD")  String currency) {
        return marketInsightService.getOverseasBalance(exchange, currency);
    }

    @GetMapping("/account/cash/us")
    public Map<String, Object> cashUs(
            @RequestParam(name="currency", defaultValue="USD") String currency) {
        return marketInsightService.getOverseasCash(currency);
    }

    @GetMapping("/account/cash/kr")
    public Map<String, Object> cashKr() {
        Map<String, Object> bal = marketInsightService.getDomesticBalance();
        if (!"OK".equals(bal.getOrDefault("status", "ERROR")))
            return Map.of("status", bal.getOrDefault("status","ERROR"), "message", bal.getOrDefault("message","Balance request failed"));
        Object out2 = bal.get("output2");
        Map<String, Object> data = null;
        if (out2 instanceof List && !((List<?>) out2).isEmpty()) {
            Object first = ((List<?>) out2).get(0);
            if (first instanceof Map) { @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)first; data=m; }
        } else if (out2 instanceof Map) { @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)out2; data=m; }
        return Map.of("status","OK","message",bal.getOrDefault("message",""),"cash",pickString(data,"dnca_tot_amt","ord_psbl_amt","ord_psbl_cash","cash"),"data",data==null?Map.of():data);
    }

    private String pickString(Map<String, Object> row, String... keys) {
        if (row == null) return "";
        for (String key : keys) { Object v = row.get(key); if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v); }
        return "";
    }

    private String text(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private boolean matchesQuery(String queryLower, String symbol, String name) {
        if (queryLower == null || queryLower.isEmpty()) return true;
        String s = symbol == null ? "" : symbol.toLowerCase();
        String n = name == null ? "" : name.toLowerCase();
        return s.contains(queryLower) || n.contains(queryLower);
    }

    private void putCandidate(LinkedHashMap<String, Map<String, Object>> dedup,
                              String symbol,
                              String name,
                              String market,
                              String exchange,
                              String source) {
        if (symbol == null || symbol.isBlank()) return;
        dedup.computeIfAbsent(symbol, key -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol", symbol);
            row.put("name", name == null ? "" : name);
            row.put("market", market);
            row.put("exchange", exchange);
            row.put("source", source);
            return row;
        });
    }


    
}

