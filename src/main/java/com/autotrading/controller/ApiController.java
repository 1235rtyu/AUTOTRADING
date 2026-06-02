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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    /* ── 입력 검증 상수 ── */

    /** KR 종목 코드: 6자리 숫자 */
    private static final Pattern KR_SYMBOL_PATTERN  = Pattern.compile("^[0-9]{6}$");
    /** US 종목 코드: 영문+숫자+일부 특수문자, 최대 10자 */
    private static final Pattern US_SYMBOL_PATTERN  = Pattern.compile("^[A-Z][A-Z0-9.\\-]{0,9}$");
    /** 계좌번호: 숫자·하이픈만 허용 */
    private static final Pattern ACCOUNT_NO_PATTERN = Pattern.compile("^[\\d\\-]{5,20}$");

    /** 허용된 거래소 코드 */
    private static final Set<String> ALLOWED_EXCHANGES = Set.of("NAS", "NYS", "AMS", "KRX");
    /** 허용된 마켓 코드 */
    private static final Set<String> ALLOWED_MARKETS   = Set.of("KR", "US");
    /** 허용된 정렬/탭 값 */
    private static final Set<String> ALLOWED_SORT_TABS = Set.of("vol", "chg", "hi");

    /** 조회 limit 상한 */
    private static final int MAX_LIMIT        = 200;
    /** buyAmount 상한 (단위: 원/달러) */
    private static final double MAX_BUY_AMOUNT = 100_000_000.0;

    private final DashboardService    dashboardService;
    private final HistoryService      historyService;
    private final WatchlistService    watchlistService;
    private final AutoTradingService  autoTradingService;
    private final MarketInsightService marketInsightService;
    private final MonitorService      monitorService;
    private final PositionService     positionService;

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

    /* ════════════════════════════════════════════
       입력 검증 헬퍼
    ════════════════════════════════════════════ */

    /**
     * 심볼 코드 검증 및 정규화.
     * market이 null이면 KR/US 둘 다 허용.
     */
    private String validateSymbol(String raw, String market) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol은 필수입니다.");
        }
        String sym = raw.trim().toUpperCase();
        if ("KR".equals(market)) {
            if (!KR_SYMBOL_PATTERN.matcher(sym).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KR 종목 코드는 6자리 숫자여야 합니다: " + sym);
            }
        } else if ("US".equals(market)) {
            if (!US_SYMBOL_PATTERN.matcher(sym).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "US 종목 코드 형식이 올바르지 않습니다: " + sym);
            }
        } else {
            // market 미지정: KR 또는 US 패턴 중 하나라도 맞으면 통과
            if (!KR_SYMBOL_PATTERN.matcher(sym).matches() && !US_SYMBOL_PATTERN.matcher(sym).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종목 코드 형식이 올바르지 않습니다: " + sym);
            }
        }
        return sym;
    }

    /** 마켓 코드 검증: "KR" 또는 "US" */
    private String validateMarket(String raw, String defaultVal) {
        if (raw == null || raw.isBlank()) return defaultVal;
        String m = raw.trim().toUpperCase();
        if (!ALLOWED_MARKETS.contains(m)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "market은 KR 또는 US만 허용됩니다.");
        }
        return m;
    }

    /** 거래소 코드 검증 */
    private String validateExchange(String raw, String defaultVal) {
        if (raw == null || raw.isBlank()) return defaultVal;
        String e = raw.trim().toUpperCase();
        if (!ALLOWED_EXCHANGES.contains(e)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 거래소 코드입니다: " + e);
        }
        return e;
    }

    /** limit 범위 검증 */
    private int validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit은 1~" + MAX_LIMIT + " 범위여야 합니다.");
        }
        return limit;
    }

    /** buyAmount 범위 검증 */
    private Double validateBuyAmount(Double amount) {
        if (amount == null) return null;
        if (!Double.isFinite(amount) || amount <= 0 || amount > MAX_BUY_AMOUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "buyAmount 범위가 올바르지 않습니다.");
        }
        return amount;
    }

    /** 연도/월 범위 검증 */
    private YearMonth validateYearMonth(Integer year, Integer month) {
        if (year == null || month == null) return YearMonth.now();
        try {
            YearMonth ym = YearMonth.of(year, month);
            // 너무 먼 미래/과거 차단
            YearMonth min = YearMonth.now().minusYears(10);
            YearMonth max = YearMonth.now().plusYears(1);
            if (ym.isBefore(min) || ym.isAfter(max)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year/month 범위가 올바르지 않습니다.");
            }
            return ym;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year/month 형식이 올바르지 않습니다.");
        }
    }

    /* ════════════════════════════════════════════
       Dashboard
    ════════════════════════════════════════════ */

    @GetMapping("/dashboard")
    public DashboardData dashboard(@RequestParam(name="limit", defaultValue="10") int limit) {
        return dashboardService.load(validateLimit(limit));
    }

    @GetMapping("/monitor/summary")
    public MonitorSummary monitorSummary(@RequestParam(name="market", defaultValue="KR") String market) {
        return monitorService.getSummary(validateMarket(market, "KR"));
    }

    @GetMapping("/monitor/calendar")
    public Map<String, Object> monitorCalendar(
            @RequestParam(name="market", defaultValue="KR") String market,
            @RequestParam(name="year",   required=false) Integer year,
            @RequestParam(name="month",  required=false) Integer month) {
        String m  = validateMarket(market, "KR");
        YearMonth ym = validateYearMonth(year, month);
        return Map.of(
            "market", m,
            "year",   ym.getYear(),
            "month",  ym.getMonthValue(),
            "data",   monitorService.getMonthlyDailyProfit(m, ym.getYear(), ym.getMonthValue())
        );
    }

    @GetMapping("/monitor/exchange-rate")
    public Map<String, Object> monitorExchangeRate() {
        return monitorService.getExchangeRate();
    }

    /* ════════════════════════════════════════════
       Orders
    ════════════════════════════════════════════ */

    @GetMapping("/orders")
    public List<OrderLog> orders(@RequestParam(name="limit", defaultValue="50") int limit) {
        return historyService.getRecentOrders(validateLimit(limit));
    }

    @GetMapping("/orders/kr")
    public List<OrderLog> ordersKr(@RequestParam(name="limit", defaultValue="50") int limit) {
        return historyService.getRecentKrOrders(validateLimit(limit));
    }

    @GetMapping("/orders/us")
    public List<OrderLog> ordersUs(@RequestParam(name="limit", defaultValue="50") int limit) {
        return historyService.getRecentUsOrders(validateLimit(limit));
    }

    @GetMapping("/pnl/recent")
    public List<Map<String, Object>> recentPnl(
            @RequestParam(name="market", defaultValue="KR") String market,
            @RequestParam(name="days",   defaultValue="3")  int days) {
        String safeMarket = ALLOWED_MARKETS.contains(market.toUpperCase()) ? market.toUpperCase() : "KR";
        int safeDays = Math.max(1, Math.min(days, 30));

        java.time.LocalDate today = java.time.LocalDate.now();
        YearMonth curMonth  = YearMonth.now();
        YearMonth prevMonth = curMonth.minusMonths(1);

        java.util.function.BiFunction<Integer,Integer,List<com.autotrading.model.DailyProfitPoint>> fetch =
            (y, m) -> {
                try { return monitorService.getMonthlyDailyProfit(safeMarket, y, m); }
                catch (Exception e) { return List.of(); }
            };

        Map<String, Double> pnlByDate = new LinkedHashMap<>();
        fetch.apply(prevMonth.getYear(), prevMonth.getMonthValue())
             .forEach(p -> pnlByDate.put(p.getTradeDate(), p.getProfitAmount()));
        fetch.apply(curMonth.getYear(), curMonth.getMonthValue())
             .forEach(p -> pnlByDate.put(p.getTradeDate(), p.getProfitAmount()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = safeDays - 1; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            String ds = d.toString();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", ds);
            item.put("pnl",  pnlByDate.getOrDefault(ds, 0.0));
            result.add(item);
        }
        return result;
    }

    /* ════════════════════════════════════════════
       Control
    ════════════════════════════════════════════ */

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
            @RequestParam(name="symbol",    defaultValue="005930") String symbol,
            @RequestParam(name="exchange",  required=false)        String exchange,
            @RequestParam(name="buyAmount", required=false)        Double buyAmount) {
        String sym = validateSymbol(symbol, null);
        String exch = exchange != null ? validateExchange(exchange, "NAS") : null;
        Double amt  = validateBuyAmount(buyAmount);
        String result = autoTradingService.start(sym, exch, amt);
        return Map.of("status", autoTradingService.status(), "message", result);
    }

    @PostMapping("/control/stop")
    public Map<String, Object> controlStop() {
        String result = autoTradingService.stop();
        return Map.of("status", autoTradingService.status(), "message", result);
    }

    @PostMapping("/control/toggle")
    public Map<String, Object> toggleSymbol(
            @RequestParam("symbol")                         String  symbol,
            @RequestParam("enable")                         boolean enable,
            @RequestParam(name="exchange",  required=false) String  exchange,
            @RequestParam(name="buyAmount", required=false) Double  buyAmount) {
        String sym  = validateSymbol(symbol, null);
        String exch = exchange != null ? validateExchange(exchange, "NAS") : null;
        Double amt  = validateBuyAmount(buyAmount);
        String result = enable
            ? autoTradingService.start(sym, exch, amt)
            : autoTradingService.stopSymbol(sym);
        return Map.of("status", autoTradingService.status(), "message", result, "symbol", sym, "enabled", enable);
    }

    /* ════════════════════════════════════════════
       Watchlist
    ════════════════════════════════════════════ */

    @GetMapping("/watchlist")
    public List<WatchlistItem> watchlist() {
        return watchlistService.getWatchlist();
    }

    @GetMapping("/watchlist/search")
    public Map<String, Object> watchlistSearch(
            @RequestParam(name="q", defaultValue="") String q,
            @RequestParam(name="limit", defaultValue="50") int limit) {
        String query = (q == null ? "" : q.trim()).toLowerCase();
        if (query.isEmpty()) {
            return Map.of("status", "OK", "query", "", "data", List.of());
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        
        List<WatchlistItem> allItems = watchlistService.getWatchlist();
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (WatchlistItem item : allItems) {
            String symbol = text(item.getSymbol()).toUpperCase();
            String exchange = text(item.getExchange());
            String folder = text(item.getFolder());
            
            // Search by symbol or try to get name from position
            boolean matchesSymbol = symbol.toLowerCase().contains(query);
            String symbolName = "";
            boolean matchesName = false;
            
            if (!matchesSymbol) {
                AutoPosition pos = positionService.getPosition(symbol);
                if (pos != null && pos.getSymbolName() != null) {
                    symbolName = pos.getSymbolName();
                    matchesName = symbolName.toLowerCase().contains(query);
                }
            } else {
                // Still fetch name for display
                AutoPosition pos = positionService.getPosition(symbol);
                if (pos != null && pos.getSymbolName() != null) {
                    symbolName = pos.getSymbolName();
                }
            }
            
            // Include if matches symbol or name
            if (matchesSymbol || matchesName) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", item.getId());
                row.put("symbol", symbol);
                row.put("exchange", exchange);
                row.put("folder", folder);
                row.put("name", symbolName);
                row.put("createdAt", item.getCreatedAt());
                results.add(row);
                if (results.size() >= safeLimit) break;
            }
        }
        
        return Map.of("status", "OK", "query", query, "data", results);
    }

    @GetMapping("/watchlist/name")
    public Map<String, Object> watchlistName(@RequestParam("symbol") String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Map.of("symbol", "", "name", "", "symbolName", "");
        }
        String target = validateSymbol(symbol, null);
        AutoPosition position = positionService.getPosition(target);
        String symbolName = (position != null && position.getSymbolName() != null) ? position.getSymbolName() : "";
        return Map.of("symbol", target, "name", symbolName, "symbolName", symbolName);
    }

    @PostMapping("/watchlist")
    public Map<String, Object> addWatchlist(
            @RequestParam("symbol")                         String symbol,
            @RequestParam(name="exchange", required=false)  String exchange,
            @RequestParam(name="folder", required=false)    String folder) {
        String sym  = validateSymbol(symbol, null);
        String exch = exchange != null ? validateExchange(exchange, "KRX") : null;
        String folderName = (folder != null && !folder.isBlank()) ? folder.trim() : null;
        watchlistService.addSymbol(sym, exch, folderName);
        return Map.of("status", "OK");
    }

    @PostMapping("/watchlist/folder")
    public Map<String, Object> setWatchlistFolder(
            @RequestParam("id") int id,
            @RequestParam(name="folder", required=false) String folder) {
        watchlistService.setFolder(id, folder);
        return Map.of("status", "OK");
    }

    @PostMapping("/watchlist/folder/clear")
    public Map<String, Object> clearWatchlistFolder(@RequestParam("folder") String folder) {
        watchlistService.clearFolder(folder);
        return Map.of("status", "OK");
    }

    @PostMapping("/watchlist/delete")
    public Map<String, Object> deleteWatchlist(@RequestParam("id") int id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id는 양수여야 합니다.");
        }
        watchlistService.remove(id);
        return Map.of("status", "OK");
    }

    @PostMapping("/watchlist/add-top")
    public Map<String, Object> addTopVolume(
            @RequestParam(name="n",       defaultValue="5")   int    n,
            @RequestParam(name="minRate", defaultValue="0")   double minRate,
            @RequestParam(name="market",  defaultValue="KR")  String market,
            @RequestParam(name="exch",    defaultValue="NAS") String exchange) {
        // 입력 검증
        if (n < 1 || n > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "n은 1~80 범위여야 합니다.");
        }
        if (!Double.isFinite(minRate) || minRate < 0 || minRate > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRate는 0~100 범위여야 합니다.");
        }
        String mkt  = validateMarket(market, "KR");
        String exch = validateExchange(exchange, "NAS");

        try {
            Map<String, Object> ranking = marketInsightService.getRanking(mkt, exch);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output = (List<Map<String, Object>>) ranking.get("data");
            if (output == null || output.isEmpty()) {
                return Map.of("status", "ERROR", "message", "랭킹 데이터가 없습니다.", "added", 0);
            }
            int added = 0;
            for (int i = 0; i < Math.min(n, output.size()); i++) {
                Map<String, Object> item = output.get(i);
                String symbol = text(item.getOrDefault("symbol", item.getOrDefault("mksc_shrn_iscd", "")));
                if (symbol.isBlank() || symbol.equals("null")) continue;
                // 심볼 형식 검증
                try { symbol = validateSymbol(symbol, mkt); } catch (Exception e) { continue; }
                if (minRate > 0) {
                    try {
                        double rate = Math.abs(Double.parseDouble(text(item.getOrDefault("prdy_ctrt", "0"))));
                        if (rate < minRate) continue;
                    } catch (NumberFormatException ignored) {}
                }
                try {
                    watchlistService.addSymbol(symbol, "US".equals(mkt) ? exch : "KRX");
                    added++;
                } catch (Exception ignored) {}
            }
            return Map.of("status", "OK", "added", added, "message", added + "개 종목 추가 완료");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("addTopVolume 오류", e);
            return Map.of("status", "ERROR", "message", "처리 중 오류가 발생했습니다.", "added", 0);
        }
    }

    @PostMapping("/control/start-top")
    public Map<String, Object> startTopVolume(
            @RequestParam(name="n",         defaultValue="3")   int    n,
            @RequestParam(name="minRate",   defaultValue="0")   double minRate,
            @RequestParam(name="market",    defaultValue="KR")  String market,
            @RequestParam(name="exch",      defaultValue="NAS") String exchange,
            @RequestParam(name="buyAmount", required=false)     Double buyAmount) {
        if (n < 1 || n > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "n은 1~80 범위여야 합니다.");
        }
        if (!Double.isFinite(minRate) || minRate < 0 || minRate > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minRate는 0~100 범위여야 합니다.");
        }
        String mkt  = validateMarket(market, "KR");
        String exch = validateExchange(exchange, "NAS");
        Double amt  = validateBuyAmount(buyAmount);

        try {
            Map<String, Object> ranking = marketInsightService.getRanking(mkt, exch);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output = (List<Map<String, Object>>) ranking.get("data");
            if (output == null || output.isEmpty()) {
                return Map.of("status", "ERROR", "message", "랭킹 데이터가 없습니다.");
            }

            int added = 0;
            String firstSymbol = "";
            for (int i = 0; i < output.size() && added < n; i++) {
                Map<String, Object> item = output.get(i);
                String symbol = text(item.getOrDefault("symbol", item.getOrDefault("mksc_shrn_iscd", "")));
                if (symbol.isBlank() || symbol.equals("null")) continue;
                try { symbol = validateSymbol(symbol, mkt); } catch (Exception e) { continue; }

                if (minRate > 0) {
                    try {
                        String rateStr = text(item.getOrDefault("prdy_ctrt", item.getOrDefault("diff_rate","0")));
                        if (Math.abs(Double.parseDouble(rateStr)) < minRate) continue;
                    } catch (NumberFormatException ignored) {}
                }
                try { watchlistService.addSymbol(symbol, "US".equals(mkt) ? exch : "KRX"); } catch (Exception ignored) {}
                if (added == 0) firstSymbol = symbol;
                added++;
            }

            if (firstSymbol.isBlank()) {
                return Map.of("status", "ERROR", "message", "조건에 맞는 종목이 없습니다.");
            }
            String result = autoTradingService.start(firstSymbol, "US".equals(mkt) ? exch : null, amt);
            return Map.of(
                "status",      autoTradingService.status(),
                "message",     added + "개 종목 등록 후 자동매매 시작: " + result,
                "added",       added,
                "firstSymbol", firstSymbol
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("startTopVolume 오류", e);
            return Map.of("status", "ERROR", "message", "처리 중 오류가 발생했습니다.");
        }
    }

    /* ════════════════════════════════════════════
       Market
    ════════════════════════════════════════════ */

    @GetMapping("/market/ranking/hts")
    public Map<String, Object> htsTopView() {
        return marketInsightService.getHtsTopView();
    }

    @GetMapping("/market/chart/time")
    public Map<String, Object> timeChart(
            @RequestParam(name="symbol", defaultValue="005930") String symbol,
            @RequestParam(name="from",   defaultValue="090000") String fromTime) {
        String sym = validateSymbol(symbol, null);
        // fromTime: HHMMSS 6자리 숫자만
        if (!fromTime.matches("^[0-9]{6}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromTime은 HHMMSS 6자리 숫자여야 합니다.");
        }
        return marketInsightService.getIntradayChart(sym, fromTime);
    }

    @GetMapping("/market/ranking")
    public Map<String, Object> ranking(
            @RequestParam(name="market", defaultValue="KR")  String market,
            @RequestParam(name="exch",   defaultValue="NAS") String exchange) {
        return marketInsightService.getRanking(validateMarket(market, "KR"), validateExchange(exchange, "NAS"));
    }

    @GetMapping("/market/symbol-suggest")
    public Map<String, Object> symbolSuggest(
            @RequestParam(name="q",      defaultValue="")    String q,
            @RequestParam(name="market", defaultValue="US")  String market,
            @RequestParam(name="exch",   defaultValue="NAS") String exchange,
            @RequestParam(name="limit",  defaultValue="10")  int limit) {

        String query      = q == null ? "" : q.trim();
        String safeMarket = validateMarket(market, "US");
        String safeExch   = validateExchange(exchange, "NAS");
        int safeLimit     = Math.max(1, Math.min(limit, 20));

        // 검색어 길이 제한 및 기본 문자 필터
        if (query.length() > 20) {
            return Map.of("status", "OK", "market", safeMarket, "query", query, "data", List.of());
        }
        if (query.isEmpty()) {
            return Map.of("status", "OK", "market", safeMarket, "query", "", "data", List.of());
        }
        String queryLower = query.toLowerCase();

        LinkedHashMap<String, Map<String, Object>> dedup = new LinkedHashMap<>();

        // 1) 랭킹 API
        try {
            Map<String, Object> ranking = marketInsightService.getRanking(safeMarket, safeExch);
            Object dataObj = ranking.get("data");
            if (dataObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) dataObj;
                for (Map<String, Object> row : rows) {
                    String symbol = text(row.get("symbol")).toUpperCase();
                    String name   = text(row.get("name"));
                    if (symbol.isEmpty()) continue;
                    if (!matchesQuery(queryLower, symbol, name)) continue;
                    putCandidate(dedup, symbol, name, safeMarket, safeExch, "ranking");
                    if (dedup.size() >= safeLimit) break;
                }
            }
        } catch (Exception ignored) {}

        // 2) 워치리스트
        if (dedup.size() < safeLimit) {
            for (WatchlistItem item : watchlistService.getWatchlist()) {
                String symbol = text(item.getSymbol()).toUpperCase();
                if (symbol.isEmpty()) continue;
                boolean isUs = Character.isLetter(symbol.charAt(0));
                if ("US".equals(safeMarket) != isUs) continue;
                if (!matchesQuery(queryLower, symbol, "")) continue;
                putCandidate(dedup, symbol, "", safeMarket, safeExch, "watchlist");
                if (dedup.size() >= safeLimit) break;
            }
        }

        // 3) 최근 주문 이력
        if (dedup.size() < safeLimit) {
            List<OrderLog> recent = "US".equals(safeMarket)
                ? historyService.getRecentUsOrders(200)
                : historyService.getRecentKrOrders(200);
            for (OrderLog row : recent) {
                String symbol = text(row.getSymbol()).toUpperCase();
                String name   = text(row.getSymbolName());
                if (symbol.isEmpty()) continue;
                if (!matchesQuery(queryLower, symbol, name)) continue;
                putCandidate(dedup, symbol, name, safeMarket, safeExch, "orders");
                if (dedup.size() >= safeLimit) break;
            }
        }

        // 4) 직접 입력 심볼 pass-through
        String typedUpper = query.toUpperCase();
        boolean typedAllowed = ("US".equals(safeMarket) && US_SYMBOL_PATTERN.matcher(typedUpper).matches())
                            || ("KR".equals(safeMarket) && KR_SYMBOL_PATTERN.matcher(typedUpper).matches());
        if (typedAllowed && !dedup.containsKey(typedUpper)) {
            putCandidate(dedup, typedUpper, "Typed symbol", safeMarket, safeExch, "typed");
        }

        List<Map<String, Object>> data = new ArrayList<>(dedup.values());
        if (data.size() > safeLimit) data = data.subList(0, safeLimit);
        return Map.of("status", "OK", "market", safeMarket, "query", query, "data", data);
    }

    @GetMapping("/market/chart")
    public Map<String, Object> chart(
            @RequestParam(name="market", defaultValue="KR")     String market,
            @RequestParam(name="symbol", defaultValue="005930") String symbol,
            @RequestParam(name="tf",     defaultValue="1m")     String timeframe,
            @RequestParam(name="exch",   defaultValue="NAS")    String exchange,
            @RequestParam(name="date",   required=false)        String date) {

        String mkt  = validateMarket(market, "KR");
        String sym  = validateSymbol(symbol, mkt);
        String exch = validateExchange(exchange, "NAS");

        // timeframe 허용 목록 검증
        Set<String> allowedTf = Set.of("1m","3m","5m","10m","15m","30m","60m","1d","1w","1mo");
        String tf = timeframe == null ? "1d" : timeframe.trim();
        if (!allowedTf.contains(tf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 timeframe입니다: " + tf);
        }

        // date 파라미터: 분봉 과거 조회용 (YYYYMMDD 형식)
        // KIS API: inquire-time-dailychartprice (FHKST03010230) — 과거 날짜 분봉
        // KIS API: inquire-time-itemchartprice  (FHKST03010200) — 당일 분봉
        String safeDate = null;
        if (date != null && !date.isBlank()) {
            String d = date.trim().replaceAll("[^0-9]", "");
            if (!d.matches("^[0-9]{8}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date는 YYYYMMDD 형식이어야 합니다.");
            }
            // 날짜 범위 검증: 오늘 이전이어야 함
            try {
                java.time.LocalDate ld = java.time.LocalDate.parse(
                    d.substring(0,4)+"-"+d.substring(4,6)+"-"+d.substring(6,8)
                );
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate oneYearAgo = today.minusYears(1);
                if (ld.isAfter(today) || ld.isBefore(oneYearAgo)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date는 오늘~1년 전 범위여야 합니다.");
                }
                safeDate = d;
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date 형식이 올바르지 않습니다.");
            }
        }

        return marketInsightService.getChart(mkt, sym, tf, exch, safeDate);
    }

    @GetMapping("/market/index")
    public Map<String, Object> marketIndex() {
        return marketInsightService.getMarketIndex();
    }

    /* ════════════════════════════════════════════
       Account / Balance
    ════════════════════════════════════════════ */

    @GetMapping("/account/balance/kr")
    public Map<String, Object> balanceKr() {
        return marketInsightService.getDomesticBalance();
    }

    @GetMapping("/account/balance/us")
    public Map<String, Object> balanceUs(
            @RequestParam(name="exch",     defaultValue="NASD") String exchange,
            @RequestParam(name="currency", defaultValue="USD")  String currency) {
        // currency 허용 목록
        Set<String> allowedCurrency = Set.of("USD", "KRW", "JPY", "HKD", "CNY");
        String cur = currency == null ? "USD" : currency.trim().toUpperCase();
        if (!allowedCurrency.contains(cur)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 currency입니다: " + cur);
        }
        // exchange: NASD, NYSE, AMEX 등 4자리
        String exch = exchange == null ? "NASD" : exchange.trim().toUpperCase();
        if (!exch.matches("^[A-Z]{3,5}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래소 코드 형식이 올바르지 않습니다.");
        }
        return marketInsightService.getOverseasBalance(exch, cur);
    }

    @GetMapping("/account/cash/us")
    public Map<String, Object> cashUs(
            @RequestParam(name="currency", defaultValue="USD") String currency) {
        Set<String> allowedCurrency = Set.of("USD", "KRW", "JPY", "HKD", "CNY");
        String cur = currency == null ? "USD" : currency.trim().toUpperCase();
        if (!allowedCurrency.contains(cur)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 currency입니다: " + cur);
        }
        return marketInsightService.getOverseasCash(cur);
    }

    @GetMapping("/account/cash/kr")
    public Map<String, Object> cashKr() {
        Map<String, Object> bal = marketInsightService.getDomesticBalance();
        if (!"OK".equals(bal.getOrDefault("status", "ERROR"))) {
            return Map.of(
                "status",  bal.getOrDefault("status", "ERROR"),
                "message", bal.getOrDefault("message", "잔고 조회 실패")
            );
        }
        Object out2 = bal.get("output2");
        Map<String, Object> data = null;
        if (out2 instanceof List && !((List<?>) out2).isEmpty()) {
            Object first = ((List<?>) out2).get(0);
            if (first instanceof Map) { @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)first; data=m; }
        } else if (out2 instanceof Map) {
            @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)out2; data=m;
        }
        return Map.of(
            "status",  "OK",
            "message", bal.getOrDefault("message", ""),
            "cash",    pickString(data, "dnca_tot_amt", "ord_psbl_amt", "ord_psbl_cash", "cash"),
            "data",    data == null ? Map.of() : data
        );
    }

    /* ════════════════════════════════════════════
       내부 유틸
    ════════════════════════════════════════════ */

    private String pickString(Map<String, Object> row, String... keys) {
        if (row == null) return "";
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v);
        }
        return "";
    }

    private String text(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private boolean matchesQuery(String queryLower, String symbol, String name) {
        if (queryLower == null || queryLower.isEmpty()) return true;
        String s = symbol == null ? "" : symbol.toLowerCase();
        String n = name   == null ? "" : name.toLowerCase();
        return s.contains(queryLower) || n.contains(queryLower);
    }

    private void putCandidate(LinkedHashMap<String, Map<String, Object>> dedup,
                              String symbol, String name, String market, String exchange, String source) {
        if (symbol == null || symbol.isBlank()) return;
        dedup.computeIfAbsent(symbol, key -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("symbol",   symbol);
            row.put("name",     name == null ? "" : name);
            row.put("market",   market);
            row.put("exchange", exchange);
            row.put("source",   source);
            return row;
        });
    }
}
