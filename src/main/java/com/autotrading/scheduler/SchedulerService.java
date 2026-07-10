package com.autotrading.scheduler;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.dao.WatchlistDao;
import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.market.MarketDataService;
import com.autotrading.model.AutoPosition;
import com.autotrading.model.StockQuote;
import com.autotrading.model.WatchlistItem;
import com.autotrading.order.OrderService;
import com.autotrading.position.PositionService;
import com.autotrading.service.RiskManager;
import com.autotrading.service.TradeHistoryService;
import com.autotrading.strategy.StrategyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.beans.factory.DisposableBean;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SchedulerService implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    // ???? ?⑤벏???怨몃땾 ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    private static final long   COOLDOWN_NORMAL_SEC            = 120;
    private static final long   COOLDOWN_STOPLOSS_SEC          = 600;
    private static final long   TICK_INTERVAL_MS               = 5_000L;
    private static final int    BUY_FILL_CONFIRM_MAX_ATTEMPTS  = 5;

    private static final int    SELL_FILL_CONFIRM_MAX_ATTEMPTS = 8;
    private static final int    SELL_FILL_CONFIRM_MAX_ATTEMPTS_OVERSEAS = 25; // 미국 주식 KIS 체결 반영 지연 대응 (~25-30s)
    private static final long   BUY_FILL_CONFIRM_INTERVAL_MS   = 700L;
    private static final long   SELL_FILL_CONFIRM_INTERVAL_MS  = 350L;

    private static final int    OVERSEAS_QUOTE_FAIL_LIMIT      = 24;
    private static final double VOLUME_RESET_RATIO             = 0.5;
    private static final long   POSITION_SYNC_INTERVAL_SEC     = 60L;
    private static final long   POSITION_CACHE_TTL_MS          = 3_000L;
    private static final long   POSITION_CACHE_MAX_STALE_MS    = 10_000L;
    private static final long   REJECT_COOLDOWN_MS             = 30_000L;
    private static final long   REJECT_COOLDOWN_PRICE_MS       = 60_000L;

    // ?逾?fix2: 筌?Ŋ??揶쏄퉮????쎈솭 ???????揶쏄쑨爰?(?類ㅺ맒 TTL癰귣???筌욁룓苡?
    private static final long   POSITION_CACHE_FAIL_RETRY_MS   = 1_500L;

    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_ERROR    = "ERROR";

    // ???? ????볦퍢 ?온????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final ZoneId NY_ZONE  = ZoneId.of("America/New_York");

    private static final LocalTime KRX_OPEN_TIME            = LocalTime.of(9, 0);
    private static final LocalTime KRX_CLOSE_TIME           = LocalTime.of(15, 30);
    private static final LocalTime KRX_OPENING_BLOCK_START  = LocalTime.of(9, 0);
    private static final LocalTime KRX_OPENING_BLOCK_END    = LocalTime.of(9, 5);
    private static final LocalTime KRX_OPENING_CAUTION_END  = LocalTime.of(9, 15);

    private static final LocalTime US_OPEN_TIME             = LocalTime.of(9, 30);
    private static final LocalTime US_CLOSE_TIME            = LocalTime.of(16, 0);

    private static final String KRX_MARKET_PROXY = "229200";
    private static final String US_MARKET_PROXY  = "QQQ";

    private static final String KRX_MARKET_PROXY_EXCHANGE = "KRX";
    private static final String US_MARKET_PROXY_EXCHANGE  = "NASD";

    private static final long MARKET_SESSION_CACHE_TTL_MS   = 15_000L;

    // ???? ??뤵????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    private final MarketDataService marketDataService;
    private final StrategyEngine strategyEngine;
    private final OrderService orderService;
    private final PositionService positionService;
    private final AutoTradingDao autoTradingDao;
    private final WatchlistDao watchlistDao;
    private final RiskManager riskManager;
    private final KoreaInvestmentApiClient kisApiClient;
    private final TradeHistoryService tradeHistoryService;

    // ???? ?怨밴묶 筌???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    private final Map<String, ScheduledExecutorService> schedulers       = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>>       tickTasks        = new ConcurrentHashMap<>();
    private final Map<String, Double>                   prevVolume       = new ConcurrentHashMap<>();
    private final Map<String, Instant>                  lastSellTime     = new ConcurrentHashMap<>();
    private final Map<String, Instant>                  stopLossTime     = new ConcurrentHashMap<>();
    private final Map<String, BarAccumulator>           barAccumMap      = new ConcurrentHashMap<>();
    private final Map<String, String>                   symbolExchange   = new ConcurrentHashMap<>();
    private final Map<String, Integer>                  quoteFailCount   = new ConcurrentHashMap<>();
    private final Map<String, String>                   symbolNameCache  = new ConcurrentHashMap<>();
    private final Map<String, Long>                     rejectCooldownUntilMs = new ConcurrentHashMap<>();
    private final Map<String, String>                   lastRejectMessage     = new ConcurrentHashMap<>();
    private final Map<String, Integer>                  positionSyncFailCount = new ConcurrentHashMap<>();

    private final Map<String, ScheduledFuture<?>>       buyFillConfirmTasks  = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>>       sellFillConfirmTasks = new ConcurrentHashMap<>();
    private final Map<String, SessionStatusCacheEntry>  marketSessionCache   = new ConcurrentHashMap<>();
    private final Map<String, PositionSnapshot>         positionCache        = new ConcurrentHashMap<>();
    private final AtomicBoolean                         positionCacheRefreshing = new AtomicBoolean(false);

    private final ScheduledExecutorService buyFillConfirmExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "buy-fill-confirm");
                t.setDaemon(true);
                return t;
            });

    private final ScheduledExecutorService sellFillConfirmExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sell-fill-confirm");
                t.setDaemon(true);
                return t;
            });

    private final ScheduledExecutorService positionSyncExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "position-sync");
                t.setDaemon(true);
                return t;
            });

    private final ScheduledExecutorService dailyStatsExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "daily-stats");
                t.setDaemon(true);
                return t;
            });

    private final AtomicBoolean positionSyncStarted = new AtomicBoolean(false);
    private volatile long positionCacheUpdatedMs = 0L;
    private volatile long lastKrCacheOkMs = 0L;
    private volatile long lastUsCacheOkMs = 0L;
    private volatile long lastAccountConfigWarnMs = 0L;

    // ???? ??? ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    private static class BarAccumulator {
        double open       = 0;
        double high       = 0;
        double low        = Double.MAX_VALUE;
        double close      = 0;
        double volAccum   = 0;
        long   barStartMs = 0;

        boolean isEmpty() {
            return barStartMs == 0;
        }

        void update(double price, double deltaVol) {
            if (isEmpty()) {
                open = price;
                high = price;
                low  = price;
            } else {
                high = Math.max(high, price);
                low  = Math.min(low, price);
            }
            close    = price;
            volAccum += deltaVol;
        }

        void startNewBar(long newBarStartMs, double firstPrice, double firstDeltaVol) {
            open       = firstPrice;
            high       = firstPrice;
            low        = firstPrice;
            close      = firstPrice;
            volAccum   = firstDeltaVol;
            barStartMs = newBarStartMs;
        }

        static long bucketTs(long epochMs) {
            return (epochMs / 60_000L) * 60_000L;
        }
    }

    private static class PositionSnapshot {
        final int quantity;
        final double avgPrice;
        final boolean liveUnavailable;
        final String exchange;
        final String symbolName;

        PositionSnapshot(int quantity, double avgPrice, boolean liveUnavailable, String exchange, String symbolName) {
            this.quantity = quantity;
            this.avgPrice = avgPrice;
            this.liveUnavailable = liveUnavailable;
            this.exchange = exchange;
            this.symbolName = symbolName;
        }
    }

    private static class RealPosition {
        final int quantity;
        final double avgPrice;

        RealPosition(int quantity, double avgPrice) {
            this.quantity = quantity;
            this.avgPrice = avgPrice;
        }
    }

    private static class LivePosition {
        final String symbol;
        final String symbolName;
        final int quantity;
        final double avgPrice;
        final String exchange;

        LivePosition(String symbol, String symbolName, int quantity, double avgPrice, String exchange) {
            this.symbol = symbol;
            this.symbolName = symbolName;
            this.quantity = quantity;
            this.avgPrice = avgPrice;
            this.exchange = exchange;
        }
    }

    private static class SellFillContext {
        final String symbol;
        final String exchange;
        final int previousQty;
        final int requestedSellQty;
        final int expectedRemainingQty;
        final double avgPrice;
        final boolean defensiveExit;
        final double referencePrice;
        final String reason;
        final boolean marketOrder;

        SellFillContext(String symbol,
                        String exchange,
                        int previousQty,
                        int requestedSellQty,
                        int expectedRemainingQty,
                        double avgPrice,
                        boolean defensiveExit,
                        double referencePrice,
                        String reason,
                        boolean marketOrder) {
            this.symbol = symbol;
            this.exchange = exchange;
            this.previousQty = previousQty;
            this.requestedSellQty = requestedSellQty;
            this.expectedRemainingQty = expectedRemainingQty;
            this.avgPrice = avgPrice;
            this.defensiveExit = defensiveExit;
            this.referencePrice = referencePrice;
            this.reason = reason;
            this.marketOrder = marketOrder;
        }
    }

    private static class MarketSessionStatus {
        final boolean tradable;
        final String source;
        final String detail;

        MarketSessionStatus(boolean tradable, String source, String detail) {
            this.tradable = tradable;
            this.source = source;
            this.detail = detail;
        }
    }

    private static class SessionStatusCacheEntry {
        final MarketSessionStatus status;
        final long checkedAtMs;

        SessionStatusCacheEntry(MarketSessionStatus status, long checkedAtMs) {
            this.status = status;
            this.checkedAtMs = checkedAtMs;
        }
    }

    // ???? ??밴쉐????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    public SchedulerService(MarketDataService marketDataService,
                            StrategyEngine strategyEngine,
                            OrderService orderService,
                            PositionService positionService,
                            AutoTradingDao autoTradingDao,
                            WatchlistDao watchlistDao,
                            RiskManager riskManager,
                            KoreaInvestmentApiClient kisApiClient,
                            TradeHistoryService tradeHistoryService) {
        this.marketDataService = marketDataService;
        this.strategyEngine = strategyEngine;
        this.orderService = orderService;
        this.positionService = positionService;
        this.autoTradingDao = autoTradingDao;
        this.watchlistDao = watchlistDao;
        this.riskManager = riskManager;
        this.kisApiClient = kisApiClient;
        this.tradeHistoryService = tradeHistoryService;
    }

    @Override
    public void afterPropertiesSet() {
        scheduleDailyStatsAggregation();
        if (!ensureAccountConfigReady("startup")) {
            logger.error("KIS account config missing at startup - scheduler will remain disabled until configured.");
            return;
        }
        startPositionSyncLoop("startup");
        try {
            syncAllPositions("startup");
        } catch (Exception e) {
            logger.warn("Startup position sync failed: {}", e.getMessage());
        }
        ensureMarketProxySchedulers();
    }

    public void onAccountLogin(String caller) {
        if (!ensureAccountConfigReady("login:" + caller)) {
            logger.error("KIS account config still missing after login - sync not started.");
            return;
        }
        boolean firstSyncLoopStart = startPositionSyncLoop("login");
        try {
            syncAllPositions(firstSyncLoopStart ? "login-bootstrap" : "login");
        } catch (Exception e) {
            logger.warn("Login position sync failed: {}", e.getMessage());
        }
        ensureMarketProxySchedulers();
    }

    private boolean startPositionSyncLoop(String reason) {
        if (!positionSyncStarted.compareAndSet(false, true)) {
            return false;
        }
        logger.info("Position sync loop started ({})", reason);
        positionSyncExecutor.scheduleWithFixedDelay(
                () -> {
                    try {
                        syncAllPositions("periodic");
                    } catch (Exception e) {
                        logger.warn("Position sync failed: {}", e.getMessage());
                    }
                },
                2L,
                POSITION_SYNC_INTERVAL_SEC,
                TimeUnit.SECONDS
        );
        return true;
    }

    // ?逾?fix (??곸겫??곷뭼): ???ル굝利???筌뤴뫀諭?executor ?類ｂ봺
    public void shutdown() {
        logger.info("SchedulerService shutdown initiated");
        try { stop(); } catch (Exception e) { logger.warn("stop() on shutdown error: {}", e.getMessage()); }
        shutdownExecutor(positionSyncExecutor, "position-sync");
        shutdownExecutor(buyFillConfirmExecutor, "buy-fill-confirm");
        shutdownExecutor(sellFillConfirmExecutor, "sell-fill-confirm");
        shutdownExecutor(dailyStatsExecutor, "daily-stats");
        logger.info("SchedulerService shutdown complete");
    }

    // =========================================================================
    // 일별 트레이드 통계 자동 집계
    //   KRX : 16:05 KST (장 마감 15:30 + 35분 여유)
    //   US  : 17:00 ET  (장 마감 16:00 + 60분 여유, 체결 지연 대응)
    //   토/일 : 트레이드 없으므로 집계 스킵
    //   실행 후 다음날 같은 시각 자동 재예약 (무한 루프)
    // =========================================================================
    private void scheduleDailyStatsAggregation() {
        scheduleMarketCloseAgg("KRX", KST_ZONE, LocalTime.of(16, 5));
        scheduleMarketCloseAgg("US",  NY_ZONE,  LocalTime.of(17, 0));
    }

    private void scheduleMarketCloseAgg(String market, ZoneId zone, LocalTime runAt) {
        ZonedDateTime now  = ZonedDateTime.now(zone);
        ZonedDateTime next = now.toLocalDate().atTime(runAt).atZone(zone);
        if (!now.isBefore(next)) {
            next = next.plusDays(1);
        }
        long delayMs = next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli();

        dailyStatsExecutor.schedule(() -> {
            try {
                LocalDate targetDate = LocalDate.now(zone);
                DayOfWeek dow = targetDate.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    logger.info("DAILY_STATS_AGG [{}] skipped — weekend ({})", market, targetDate);
                } else {
                    tradeHistoryService.aggregateDailyStats(targetDate);
                    logger.info("DAILY_STATS_AGG [{}] completed for {}", market, targetDate);
                }
            } catch (Exception e) {
                logger.error("DAILY_STATS_AGG [{}] failed: {}", market, e.getMessage(), e);
            } finally {
                scheduleMarketCloseAgg(market, zone, runAt);
            }
        }, delayMs, TimeUnit.MILLISECONDS);

        logger.info("DAILY_STATS_AGG [{}] next run in {}min ({})",
                market, delayMs / 60_000, next.toLocalDateTime());
    }

    @Override
    public void destroy() {
        shutdown();
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            executor.shutdownNow();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                logger.warn("Executor {} did not terminate in 3s", name);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized String start(String symbol) {
        return start(symbol, null, null);
    }

    public synchronized String start(String symbol, String exchange) {
        return start(symbol, exchange, null);
    }

    public synchronized String start(String symbol, String exchange, Double buyAmount) {
        if (symbol == null || symbol.isBlank()) return "Invalid symbol";
        String sym = symbol.trim().toUpperCase();

        if (!ensureAccountConfigReady("start")) {
            return "KIS account config missing";
        }

        if (buyAmount != null) {
            strategyEngine.setBuyAmount(sym, buyAmount);
        }

        if (schedulers.containsKey(sym)) {
            if (buyAmount != null) {
                return "Already Running (buy amount updated): " + sym;
            }
            return "Already Running: " + sym;
        }

        String orderExchange = resolveOrderExchangeForStart(sym, exchange);
        symbolExchange.put(sym, orderExchange);

        StrategyEngine.Market market = resolveMarket(orderExchange);
        strategyEngine.setMarket(sym, market);
        logger.info("Market set for {} : exchange={} -> orderExchange={} -> {}",
                sym, exchange, orderExchange, market);

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scheduler-" + sym);
            t.setDaemon(true);
            return t;
        });

        ScheduledFuture<?> task = exec.scheduleWithFixedDelay(
                () -> {
                    try {
                        execute(sym);
                    } catch (Exception e) {
                        logger.error("Strategy error for {}", sym, e);
                    }
                },
                0L,
                TICK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );

        schedulers.put(sym, exec);
        tickTasks.put(sym, task);
        logger.info("Auto-trading scheduler started for {}", sym);

        // 30분봉 지표용 과거 3거래일 + 당일 1분봉 사전 주입
        preloadHistoricalBars(sym);
        preloadTodayBars(sym);

        return "Started " + sym;
    }

    /**
     * 30분봉 RSI 지표용: 과거 3거래일 1분봉을 KIS API에서 조회해 StrategyEngine에 seed.
     * D-3 → D-2 → D-1 순서로 로딩 (시간순).
     */
    @SuppressWarnings("unchecked")
    private void preloadHistoricalBars(String symbol) {
        if (isOverseasSymbol(symbol)) return;

        LocalDate today = LocalDate.now(KST_ZONE);
        List<LocalDate> tradingDays = new ArrayList<>();
        LocalDate d = today.minusDays(1);
        while (tradingDays.size() < 3) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                tradingDays.add(0, d); // prepend: oldest first
            }
            d = d.minusDays(1);
        }

        int totalSeeded = 0;
        for (LocalDate day : tradingDays) {
            String dateStr = String.format("%04d%02d%02d", day.getYear(), day.getMonthValue(), day.getDayOfMonth());
            String fromTime = "153000"; // start from end of day and go backward
            List<Map<String, Object>> dayBars = new ArrayList<>();
            try {
                for (int page = 0; page < 12; page++) {
                    Map<String, Object> raw = kisApiClient.fetchDailyMinuteChart(symbol, dateStr, fromTime);
                    if (!"OK".equals(raw.get("status"))) break;
                    List<Map<String, Object>> chunk = (List<Map<String, Object>>) raw.get("output2");
                    if (chunk == null || chunk.isEmpty()) break;
                    dayBars.addAll(chunk);
                    Map<String, Object> oldest = chunk.get(chunk.size() - 1);
                    String earliest = barStrField(oldest, "stck_cntg_hour", "xhms");
                    if (earliest == null) break;
                    earliest = earliest.replaceAll("[^0-9]", "");
                    if (earliest.length() < 6 || earliest.compareTo("090100") <= 0) break;
                    fromTime = preloadDecrOneMin(earliest);
                }
            } catch (Exception e) {
                logger.warn("[HIST_PRELOAD] {} {} fetch error: {}", symbol, dateStr, e.getMessage());
                continue;
            }
            if (dayBars.isEmpty()) continue;

            dayBars.sort(Comparator.comparing(b ->
                    barStrField(b, "stck_bsop_date", "xymd", "") +
                    barStrField(b, "stck_cntg_hour", "xhms", "")));

            String lastKey = "";
            for (Map<String, Object> bar : dayBars) {
                double close = barDoubleField(bar, "stck_prpr", "stck_clpr");
                if (close <= 0) continue;
                double open   = barDoubleField(bar, "stck_oprc"); if (open   <= 0) open   = close;
                double high   = barDoubleField(bar, "stck_hgpr"); if (high   <= 0) high   = close;
                double low    = barDoubleField(bar, "stck_lwpr"); if (low    <= 0) low    = close;
                double volume = barDoubleField(bar, "cntg_vol", "acml_vol");
                String dStr   = barStrField(bar, "stck_bsop_date", "xymd", "");
                String tStr   = barStrField(bar, "stck_cntg_hour", "xhms", "");
                String key    = dStr + tStr;
                if (key.equals(lastKey)) continue;
                lastKey = key;
                long tsMs = preloadParseTs(dStr, tStr);
                if (tsMs <= 0) continue;
                LocalTime barTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMs), KST_ZONE).toLocalTime();
                if (barTime.isBefore(LocalTime.of(9, 0))) continue;
                strategyEngine.record(symbol, open, high, low, close, volume, tsMs);
                totalSeeded++;
            }
            logger.info("[HIST_PRELOAD] {} {} : {} bars", symbol, dateStr, dayBars.size());
        }
        logger.info("[HIST_PRELOAD] {} total {} bars preloaded from D-3~D-1", symbol, totalSeeded);
    }

    /**
     * KRX 종목 한정: KIS API에서 당일 9:00~ 분봉을 가져와 StrategyEngine에 seed.
     * 이를 통해 시스템 시작 직후에도 최소 히스토리(30봉) 조건을 즉시 충족할 수 있다.
     */
    @SuppressWarnings("unchecked")
    private void preloadTodayBars(String symbol) {
        if (isOverseasSymbol(symbol)) return;  // KRX 전용

        LocalDateTime nowKst  = LocalDateTime.now(KST_ZONE);
        LocalTime     nowTime = nowKst.toLocalTime();
        if (nowTime.isBefore(LocalTime.of(9, 1))) {
            logger.debug("[BAR_PRELOAD] {} : market not yet open, skip", symbol);
            return;
        }

        // 현재 시각부터 역방향으로 9:00까지 페이지 조회
        String today    = String.format("%04d%02d%02d",
                nowKst.getYear(), nowKst.getMonthValue(), nowKst.getDayOfMonth());
        String fromTime = String.format("%02d%02d%02d",
                nowTime.getHour(), nowTime.getMinute(), nowTime.getSecond());

        List<Map<String, Object>> allBars = new ArrayList<>();
        try {
            for (int page = 0; page < 12; page++) {
                Map<String, Object> raw = kisApiClient.fetchDailyMinuteChart(symbol, today, fromTime);
                if (!"OK".equals(raw.get("status"))) break;

                List<Map<String, Object>> chunk = (List<Map<String, Object>>) raw.get("output2");
                if (chunk == null || chunk.isEmpty()) break;
                allBars.addAll(chunk);

                // KIS는 최신→과거 역순. 마지막 원소가 가장 오래된 봉
                Map<String, Object> oldest  = chunk.get(chunk.size() - 1);
                String earliest = barStrField(oldest, "stck_cntg_hour", "xhms");
                if (earliest == null) break;
                earliest = earliest.replaceAll("[^0-9]", "");
                if (earliest.length() < 6 || earliest.compareTo("090100") <= 0) break;
                fromTime = preloadDecrOneMin(earliest);
            }
        } catch (Exception e) {
            logger.warn("[BAR_PRELOAD] {} fetch error: {}", symbol, e.getMessage());
            return;
        }

        if (allBars.isEmpty()) {
            logger.info("[BAR_PRELOAD] {} : no bars available yet", symbol);
            return;
        }

        // 오름차순 정렬 (날짜+시간 문자열 기준)
        allBars.sort(Comparator.comparing(b ->
                barStrField(b, "stck_bsop_date", "xymd", "") +
                barStrField(b, "stck_cntg_hour", "xhms", "")));

        // 중복 제거용 마지막 처리 시간 추적
        String lastKey = "";
        int seeded = 0;
        for (Map<String, Object> bar : allBars) {
            double close = barDoubleField(bar, "stck_prpr", "stck_clpr");
            if (close <= 0) continue;
            double open   = barDoubleField(bar, "stck_oprc");   if (open   <= 0) open   = close;
            double high   = barDoubleField(bar, "stck_hgpr");   if (high   <= 0) high   = close;
            double low    = barDoubleField(bar, "stck_lwpr");   if (low    <= 0) low    = close;
            double volume = barDoubleField(bar, "cntg_vol", "acml_vol");

            String dateStr = barStrField(bar, "stck_bsop_date", "xymd", "");
            String timeStr = barStrField(bar, "stck_cntg_hour", "xhms", "");
            String dedupKey = dateStr + timeStr;
            if (dedupKey.equals(lastKey)) continue;
            lastKey = dedupKey;

            long tsMs = preloadParseTs(dateStr, timeStr);
            if (tsMs <= 0) continue;

            // 9:00 이전 봉 제외 (VWAP은 정규장 시작부터 누적)
            LocalTime barTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMs), KST_ZONE).toLocalTime();
            if (barTime.isBefore(LocalTime.of(9, 0))) continue;

            strategyEngine.record(symbol, open, high, low, close, volume, tsMs);
            seeded++;
        }
        logger.info("[BAR_PRELOAD] {} : {} bars preloaded (total fetched: {})", symbol, seeded, allBars.size());
    }

    private String barStrField(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null && !v.toString().isBlank()) return v.toString().trim();
        }
        return "";
    }

    private double barDoubleField(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) {
                try { double d = Double.parseDouble(v.toString().trim()); if (d > 0) return d; }
                catch (NumberFormatException ignored) {}
            }
        }
        return 0.0;
    }

    private long preloadParseTs(String dateStr, String timeStr) {
        try {
            String d = dateStr.replaceAll("[^0-9]", "");
            String t = timeStr.replaceAll("[^0-9]", "");
            if (d.length() < 8 || t.length() < 6) return 0L;
            return LocalDateTime.of(
                    Integer.parseInt(d.substring(0, 4)),
                    Integer.parseInt(d.substring(4, 6)),
                    Integer.parseInt(d.substring(6, 8)),
                    Integer.parseInt(t.substring(0, 2)),
                    Integer.parseInt(t.substring(2, 4)),
                    Integer.parseInt(t.substring(4, 6)))
                    .atZone(KST_ZONE).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String preloadDecrOneMin(String hhmmss) {
        try {
            int h = Integer.parseInt(hhmmss.substring(0, 2));
            int m = Integer.parseInt(hhmmss.substring(2, 4));
            int totalMins = h * 60 + m - 1;
            if (totalMins < 0) totalMins = 0;
            return String.format("%02d%02d00", totalMins / 60, totalMins % 60);
        } catch (Exception e) {
            return "090000";
        }
    }

    private void syncAllPositions(String reason) {
        if (!ensureAccountConfigReady("syncAllPositions:" + reason)) {
            return;
        }
        Map<String, LivePosition> liveKr = new HashMap<>();
        Map<String, LivePosition> liveUs = new HashMap<>();

        boolean krOk = collectDomesticPositions(liveKr);
        boolean usOk = collectOverseasPositions(liveUs);

        if (krOk) {
            applyLivePositions(liveKr, reason);
            reconcileMissingPositions(liveKr.keySet(), true, reason);
        }

        if (usOk) {
            applyLivePositions(liveUs, reason);
            reconcileMissingPositions(liveUs.keySet(), false, reason);
        }
    }

    private boolean collectDomesticPositions(Map<String, LivePosition> target) {
        try {
            Map<String, Object> balResp = kisApiClient.fetchDomesticBalance();
            if (!"OK".equals(balResp.get("status"))) {
                logger.warn("Position sync: domestic balance non-OK: {}", balResp.get("message"));
                return false;
            }
            logger.info("Fetched domestic balance");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output1 =
                    (List<Map<String, Object>>) balResp.get("output1");
            if (output1 == null) {
                logger.info("Live position loaded (KRX) count=0");
                return true;
            }

            int loaded = 0;
            for (Map<String, Object> item : output1) {
                String symbol = pickString(item, "pdno", "item_cd", "symbol", "stck_shrn_iscd");
                if (!StringUtils.hasText(symbol)) continue;
                int qty = pickInt(item, "hldg_qty", "cblc_qty", "hold_qty");
                if (qty <= 0) continue;
                double avg = pickDouble(item, "pchs_avg_pric", "avg_pric", "avg_unpr", "pchs_unpr");
                String name = normalizeSymbolName(pickString(item,
                        "prdt_name", "prdt_nm", "stck_isnm", "hts_kor_isnm", "name"));
                target.put(symbol, new LivePosition(symbol, name, qty, avg, "KRX"));
                loaded++;
            }
            logger.info("Live position loaded (KRX) count={}", loaded);
            return true;
        } catch (Exception e) {
            logger.warn("Position sync: domestic balance error: {}", e.getMessage());
            return false;
        }
    }

    private boolean collectOverseasPositions(Map<String, LivePosition> target) {
        boolean anyOk = false;
        List<String> exchanges = buildOverseasExchangeCandidates(null);

        for (String ex : exchanges) {
            Map<String, Object> balResp = fetchOverseasBalanceWithRetry(ex, 2);
            if (balResp == null || !"OK".equals(balResp.get("status"))) {
                String msg = balResp != null ? String.valueOf(balResp.get("message")) : "null response";
                logger.warn("Position sync: overseas balance non-OK for {}: {}", ex, msg);
                continue;
            }
            anyOk = true;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output1 =
                    (List<Map<String, Object>>) balResp.get("output1");
            if (output1 == null) continue;

            for (Map<String, Object> item : output1) {
                String symbol = pickString(item, "ovrs_pdno", "pdno", "symbol", "item_cd");
                if (!StringUtils.hasText(symbol)) continue;
                int qty = pickInt(item, "ovrs_cblc_qty", "hldg_qty", "hold_qty", "cblc_qty");
                if (qty <= 0) continue;
                double avg = pickDouble(item, "pchs_avg_pric", "avg_unpr", "pchs_unpr", "avg_pric");
                String name = normalizeSymbolName(pickString(item,
                        "ovrs_item_name", "ovrs_item_kor_name", "hts_kor_isnm", "stck_isnm", "prdt_name", "name"));

                if (!target.containsKey(symbol)) {
                    target.put(symbol, new LivePosition(symbol, name, qty, avg, ex));
                }
                symbolExchange.put(symbol, ex);
            }
        }

        return anyOk;
    }

    private void applyLivePositions(Map<String, LivePosition> livePositions, String reason) {
        if (livePositions.isEmpty()) return;

        boolean recoverHoldingState = "startup".equals(reason) || "login-bootstrap".equals(reason);
        for (LivePosition pos : livePositions.values()) {
            positionService.updatePosition(pos.symbol, pos.symbolName, pos.quantity, pos.avgPrice);
            if (StringUtils.hasText(pos.symbolName)) {
                symbolNameCache.put(pos.symbol, pos.symbolName);
            }
            if (recoverHoldingState && pos.quantity > 0) {
                strategyEngine.forceHoldingState(pos.symbol);
            }
        }
        logger.info("Position sync applied: count={} reason={} recoverHoldingState={}",
                livePositions.size(), reason, recoverHoldingState);
    }

    private void reconcileMissingPositions(Set<String> liveSymbols, boolean krxMarket, String reason) {
        List<AutoPosition> dbPositions = positionService.getAllPositions();
        if (dbPositions == null || dbPositions.isEmpty()) return;

        for (AutoPosition dbPos : dbPositions) {
            String symbol = dbPos.getSymbol();
            if (!StringUtils.hasText(symbol)) continue;

            boolean isKrx = isKrxSymbol(symbol);
            if (krxMarket != isKrx) continue;
            if (liveSymbols.contains(symbol)) continue;
            if (dbPos.getQuantity() == 0) continue;

            positionService.updatePosition(symbol, dbPos.getSymbolName(), 0, dbPos.getAvgPrice());
            strategyEngine.clearStaleHoldState(symbol);
            logger.info("Position cleared (not in live) symbol={} qty={} reason={}",
                    symbol, dbPos.getQuantity(), reason);
        }
    }

    private String normalizeSymbolName(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> fetchOverseasBalanceWithRetry(String exchange, int attempts) {
        String ex = normalizeOverseasOrderExchange(exchange);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Map<String, Object> balResp = kisApiClient.fetchOverseasBalance(ex, "USD");
                if (balResp == null) return null;
                if (!"OK".equals(balResp.get("status"))) {
                    if (attempt < attempts) {
                        logger.warn("fetchOverseasBalance retry {} for {}: {}", attempt, ex, balResp.get("message"));
                        continue;
                    }
                }
                return balResp;
            } catch (Exception e) {
                if (attempt < attempts) {
                    logger.warn("fetchOverseasBalance retry {} failed for {}: {}", attempt, ex, e.getMessage());
                    continue;
                }
                logger.warn("fetchOverseasBalance failed for {}: {}", ex, e.getMessage());
                return null;
            }
        }
        return null;
    }

    private List<String> buildOverseasExchangeCandidates(String exchange) {
        List<String> list = new ArrayList<>();
        String primary = normalizeOverseasOrderExchange(exchange);
        if ("NASD".equals(primary) || "NYSE".equals(primary) || "AMEX".equals(primary)) {
            list.add(primary);
        }
        if (!list.contains("NASD")) list.add("NASD");
        if (!list.contains("NYSE")) list.add("NYSE");
        if (!list.contains("AMEX")) list.add("AMEX");
        return list;
    }

    private boolean isMarketProxy(String symbol) {
        if (symbol == null) return false;
        return KRX_MARKET_PROXY.equalsIgnoreCase(symbol) || US_MARKET_PROXY.equalsIgnoreCase(symbol);
    }

    private void updateMarketContextIfProxy(String symbol) {
        if (KRX_MARKET_PROXY.equalsIgnoreCase(symbol)) {
            strategyEngine.updateMarketContextFromSymbol(symbol, StrategyEngine.Market.KRX);
            logger.info("Market context updated from KRX proxy={}", symbol);
            return;
        }
        if (US_MARKET_PROXY.equalsIgnoreCase(symbol)) {
            strategyEngine.updateMarketContextFromSymbol(symbol, StrategyEngine.Market.US);
            logger.info("Market context updated from US proxy={}", symbol);
        }
    }

    private void ensureMarketProxySchedulers() {
        try {
            if (!schedulers.containsKey(KRX_MARKET_PROXY)) {
                start(KRX_MARKET_PROXY, KRX_MARKET_PROXY_EXCHANGE, null);
                logger.info("Market proxy scheduler started for KRX: {}", KRX_MARKET_PROXY);
            }
        } catch (Exception e) {
            logger.warn("Failed to start KRX market proxy {}: {}", KRX_MARKET_PROXY, e.getMessage());
        }

        try {
            if (!schedulers.containsKey(US_MARKET_PROXY)) {
                start(US_MARKET_PROXY, US_MARKET_PROXY_EXCHANGE, null);
                logger.info("Market proxy scheduler started for US: {}", US_MARKET_PROXY);
            }
        } catch (Exception e) {
            logger.warn("Failed to start US market proxy {}: {}", US_MARKET_PROXY, e.getMessage());
        }
    }
    // ???? ??쎈뻬 ?룐뫂遊???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    private void execute(String symbol) {
        if (!ensureAccountConfigReady("execute")) {
            return;
        }
        if (riskManager.hasHitLossLimit()) {
            logger.warn("Daily loss limit reached; stopping all trading");
            stop();
            return;
        }

        String orderExchange = resolveOrderExchangeForRuntime(symbol);

        StockQuote quote = safeFetchQuote(symbol, toQuoteExchangeHint(orderExchange));
        if (quote == null) return;

        quoteFailCount.remove(symbol);
        orderExchange = syncOrderExchangeWithQuote(symbol, orderExchange);

        double deltaVolume = calculateDeltaVolume(symbol, quote.getVolume());

        String symbolName = resolveSymbolName(symbol, quote);
        double currentVolume1m = updateBarAccumulator(symbol, quote, symbolName, deltaVolume);

        if (isMarketProxy(symbol)) {
            updateMarketContextIfProxy(symbol);
            return;
        }

        PositionSnapshot pos = loadPositionSnapshot(symbol, orderExchange);
        int quantity = pos.quantity;
        double avgPrice = pos.avgPrice;
        if (pos.liveUnavailable) {
            int fails = positionSyncFailCount.merge(symbol, 1, Integer::sum);
            if (fails == 3) {
                logger.warn("[CAUTION] {} position sync failed {} times consecutively - new entries temporarily blocked",
                        symbol, fails);
            }
        } else {
            positionSyncFailCount.remove(symbol);
        }

        if (quantity > 0) {
            cancelBuyFillConfirmTask(symbol);
            strategyEngine.notifyBuyFilled(symbol);
        } else if (buyFillConfirmTasks.containsKey(symbol)) {
            cancelBuyFillConfirmTask(symbol);
            strategyEngine.clearStaleHoldState(symbol);
        }

        Optional<com.autotrading.model.OrderCommand> orderOpt =
                strategyEngine.decide(symbol, quote.getPrice(), currentVolume1m, quantity, avgPrice);

        if (orderOpt.isEmpty()) return;

        com.autotrading.model.OrderCommand command = orderOpt.get();
        if (isOverseasSymbol(symbol)) {
            command.setExchange(orderExchange);
        }

        if (!passesRejectCooldown(symbol, command)) {
            if ("SELL".equals(command.getType())) {
                // 쿨다운 블록 시 StrategyEngine에 알려 sellPending 팬텀 방지
                strategyEngine.notifySellRejected(symbol, "COOLDOWN_BLOCKED");
            }
            return;
        }

        if (!passesMarketSessionGate(symbol, command, orderExchange)) {
            return;
        }

        if ("BUY".equals(command.getType())) {
            if (!passesBuyGate(symbol, command, orderExchange, pos)) return;
        }

        if ("SELL".equals(command.getType())) {
            if ("SELL_TIMEOUT_MARKET_RETRY".equals(command.getReason())) {
                cancelSellFillConfirmTask(symbol);
            }
            if (!passesSellGate(symbol, command, orderExchange)) return;
        }

        if (!riskManager.allowOrder(symbol)) {
            logger.warn("Order blocked by risk manager for {}", symbol);
            return;
        }

        if ("SELL".equals(command.getType()) && command.isMarketOrder() && command.getPrice() <= 0.0) {
            command.setPrice(quote.getPrice());
        }

        if ("BUY".equals(command.getType())) {
            strategyEngine.markBuyPending(symbol);
        }

        Map<String, Object> resp;
        try {
            resp = orderService.placeOrder(command);
        } catch (Exception e) {
            if ("BUY".equals(command.getType())) {
                strategyEngine.notifyBuyRejected(symbol);
            } else {
                strategyEngine.notifySellRejected(symbol, "EXCEPTION:" + e.getMessage());
            }
            riskManager.orderFailed(symbol);
            logger.error("Order EXCEPTION for {} side={} msg={}",
                    symbol, command.getType(), e.getMessage());
            return;
        }

        String respStatus = normalizeStatus(resp.getOrDefault("status", ""));
        String respMsg = String.valueOf(resp.getOrDefault("message", ""));

        logger.info("Order response: symbol={} side={} price={} qty={} status={} message={}",
                symbol, command.getType(), command.getPrice(),
                command.getQuantity(), respStatus, respMsg);

        String logReason = buildLogReason(respStatus, respMsg);
        autoTradingDao.saveOrderLog(
                symbol, command.getType(), command.getQuantity(), command.getPrice(), logReason);

        if (STATUS_ACCEPTED.equals(respStatus)) {
            handleAccepted(symbol, command, quantity, avgPrice, quote.getPrice(), orderExchange);
            riskManager.orderSucceeded(symbol);
        } else if (STATUS_REJECTED.equals(respStatus)) {
            handleRejected(symbol, command, orderExchange, respMsg);
            riskManager.orderFailed(symbol);
        } else {
            handleError(symbol, command, respStatus, respMsg);
            riskManager.orderFailed(symbol);
        }
    }

    private boolean passesMarketSessionGate(String symbol,
                                            com.autotrading.model.OrderCommand command,
                                            String orderExchange) {
        boolean defensiveExit = "SELL".equals(command.getType())
                && (command.isMarketOrder()
                || "STOP_LOSS".equalsIgnoreCase(command.getReason())
                || "EMERGENCY_STOP".equalsIgnoreCase(command.getReason()));

        if (defensiveExit) {
            return true;
        }

        MarketSessionStatus status = resolveMarketSessionStatus(symbol, orderExchange);

        if (!status.tradable) {
            logger.info("{} blocked (market session OFF) for {} exchange={} source={} detail={}",
                    command.getType(), symbol, orderExchange, status.source, status.detail);
            return false;
        }

        return true;
    }

    private boolean passesRejectCooldown(String symbol, com.autotrading.model.OrderCommand command) {
        long nowMs = System.currentTimeMillis();
        Long until = rejectCooldownUntilMs.get(symbol);
        if (until == null || nowMs >= until) {
            return true;
        }

        String lastMsg = lastRejectMessage.getOrDefault(symbol, "");
        long remaining = Math.max(0L, until - nowMs);
        logger.warn("ORDER blocked (reject cooldown) symbol={} side={} remainingMs={} lastMsg={}",
                symbol, command.getType(), remaining, lastMsg);
        return false;
    }

    private MarketSessionStatus resolveMarketSessionStatus(String symbol, String orderExchange) {
        long nowMs = System.currentTimeMillis();
        String cacheKey = buildMarketSessionCacheKey(orderExchange);

        SessionStatusCacheEntry cached = marketSessionCache.get(cacheKey);
        if (cached != null && (nowMs - cached.checkedAtMs) < MARKET_SESSION_CACHE_TTL_MS) {
            return cached.status;
        }

        try {
            MarketSessionStatus apiStatus = tryResolveMarketSessionStatusFromApi(symbol, orderExchange);
            if (apiStatus != null) {
                marketSessionCache.put(cacheKey, new SessionStatusCacheEntry(apiStatus, nowMs));
                return apiStatus;
            }
        } catch (Exception e) {
            logger.warn("Market session API check failed for {}({}) -> fallback. msg={}",
                    symbol, orderExchange, e.getMessage());
        }

        MarketSessionStatus fallback = fallbackMarketSessionStatus(orderExchange);
        marketSessionCache.put(cacheKey, new SessionStatusCacheEntry(fallback, nowMs));
        return fallback;
    }

    private String buildMarketSessionCacheKey(String orderExchange) {
        StrategyEngine.Market market = resolveMarket(orderExchange);
        if (market == StrategyEngine.Market.KRX) {
            return "KRX";
        }
        return normalizeOverseasOrderExchange(orderExchange);
    }

    private MarketSessionStatus tryResolveMarketSessionStatusFromApi(String symbol, String orderExchange) {
        MarketSessionStatus fromMarketData = tryReflectiveSessionCheck(
                marketDataService, symbol, orderExchange);
        if (fromMarketData != null) {
            return fromMarketData;
        }
        return tryReflectiveSessionCheck(kisApiClient, symbol, orderExchange);
    }

    private MarketSessionStatus tryReflectiveSessionCheck(Object target, String symbol, String orderExchange) {
        if (target == null) return null;

        Object result;

        result = invokeIfExists(target, "isTradableNow", new Class[]{String.class, String.class}, symbol, orderExchange);
        if (result != null) return interpretSessionResult(result, target.getClass().getSimpleName(), "isTradableNow(symbol,exchange)");

        result = invokeIfExists(target, "isTradableNow", new Class[]{String.class}, orderExchange);
        if (result != null) return interpretSessionResult(result, target.getClass().getSimpleName(), "isTradableNow(exchange)");

        result = invokeIfExists(target, "isMarketOpen", new Class[]{String.class}, orderExchange);
        if (result != null) return interpretSessionResult(result, target.getClass().getSimpleName(), "isMarketOpen(exchange)");

        result = invokeIfExists(target, "fetchMarketSessionStatus", new Class[]{String.class, String.class}, symbol, orderExchange);
        if (result != null) return interpretSessionResult(result, target.getClass().getSimpleName(), "fetchMarketSessionStatus(symbol,exchange)");

        result = invokeIfExists(target, "fetchMarketSessionStatus", new Class[]{String.class}, orderExchange);
        if (result != null) return interpretSessionResult(result, target.getClass().getSimpleName(), "fetchMarketSessionStatus(exchange)");

        result = invokeIfExists(target, "getMarketSessionStatus", new Class[]{String.class}, orderExchange);
        if (result != null) return interpretSessionResult(result, target.getClass().getSimpleName(), "getMarketSessionStatus(exchange)");

        return null;
    }

    private Object invokeIfExists(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            return method.invoke(target, args);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            logger.warn("Session method invoke failed: {}.{} msg={}",
                    target.getClass().getSimpleName(), methodName, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private MarketSessionStatus interpretSessionResult(Object result, String sourceClass, String sourceMethod) {
        String source = "API";

        if (result instanceof Boolean) {
            boolean tradable = (Boolean) result;
            return new MarketSessionStatus(tradable, source, sourceClass + "." + sourceMethod);
        }

        if (result instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) result;

            Boolean tradable = pickBoolean(map,
                    "tradable", "tradeable", "open", "marketOpen", "available", "isOpen", "isTradable");

            String statusText = pickString(map,
                    "status", "marketStatus", "sessionStatus", "tradingStatus", "market_status", "session_status");

            String detail = pickString(map,
                    "message", "reason", "detail", "desc", "status", "marketStatus", "sessionStatus");

            if (tradable == null && StringUtils.hasText(statusText)) {
                String upper = statusText.trim().toUpperCase();
                if (upper.contains("OPEN") || upper.contains("NORMAL") || upper.contains("TRADING")) {
                    tradable = true;
                } else if (upper.contains("CLOSE") || upper.contains("CLOSED")
                        || upper.contains("HOLIDAY") || upper.contains("HALT")
                        || upper.contains("SUSPEND") || upper.contains("EARLY_CLOSE")) {
                    tradable = false;
                }
            }

            if (tradable != null) {
                return new MarketSessionStatus(tradable, source,
                        sourceClass + "." + sourceMethod + (StringUtils.hasText(detail) ? " | " + detail : ""));
            }
        }

        if (result instanceof String) {
            String text = ((String) result).trim().toUpperCase();
            if (text.contains("OPEN") || text.contains("TRADING")) {
                return new MarketSessionStatus(true, source, sourceClass + "." + sourceMethod + " | " + text);
            }
            if (text.contains("CLOSE") || text.contains("HOLIDAY") || text.contains("HALT")) {
                return new MarketSessionStatus(false, source, sourceClass + "." + sourceMethod + " | " + text);
            }
        }

        return null;
    }

    private MarketSessionStatus fallbackMarketSessionStatus(String orderExchange) {
        StrategyEngine.Market market = resolveMarket(orderExchange);

        if (market == StrategyEngine.Market.KRX) {
            boolean open = isKrxRegularSessionOpen();
            return new MarketSessionStatus(open, "FALLBACK", "time-based KRX session");
        }

        boolean open = isUsRegularSessionOpen();
        return new MarketSessionStatus(open, "FALLBACK", "time-based US session");
    }

    private boolean isKrxRegularSessionOpen() {
        ZonedDateTime now = ZonedDateTime.now(KST_ZONE);
        switch (now.getDayOfWeek()) {
            case SATURDAY:
            case SUNDAY:
                return false;
            default:
                LocalTime time = now.toLocalTime();
                return !time.isBefore(KRX_OPEN_TIME) && time.isBefore(KRX_CLOSE_TIME);
        }
    }

    private boolean isUsRegularSessionOpen() {
        ZonedDateTime now = ZonedDateTime.now(NY_ZONE);
        switch (now.getDayOfWeek()) {
            case SATURDAY:
            case SUNDAY:
                return false;
            default:
                LocalTime time = now.toLocalTime();
                return !time.isBefore(US_OPEN_TIME) && time.isBefore(US_CLOSE_TIME);
        }
    }

    private double calculateDeltaVolume(String symbol, double rawVolume) {
        double prev = prevVolume.getOrDefault(symbol, rawVolume);
        prevVolume.put(symbol, rawVolume);

        if (rawVolume < prev * VOLUME_RESET_RATIO) {
            logger.warn("Volume reset detected for {}: prev={} raw={} -> using rawVol as delta",
                    symbol, prev, rawVolume);
            return rawVolume;
        }
        return Math.max(0.0, rawVolume - prev);
    }

    private double updateBarAccumulator(String symbol,
                                        StockQuote quote,
                                        String symbolName,
                                        double deltaVolume) {
        long nowMs = System.currentTimeMillis();
        long bucketTs = BarAccumulator.bucketTs(nowMs);
        double price = quote.getPrice();

        BarAccumulator accum = barAccumMap.computeIfAbsent(symbol, k -> new BarAccumulator());

        if (accum.isEmpty()) {
            accum.startNewBar(bucketTs, price, deltaVolume);
            return accum.volAccum;
        }

        if (accum.barStartMs != bucketTs) {
            logger.debug("1m bar completed for {} @{} o={} h={} l={} c={} vol={}",
                    symbol, accum.barStartMs,
                    accum.open, accum.high, accum.low, accum.close, accum.volAccum);

            autoTradingDao.savePriceLog(symbol, symbolName, accum.close, accum.volAccum, quote.getTimestamp());

            strategyEngine.record(symbol,
                    accum.open, accum.high, accum.low, accum.close,
                    accum.volAccum, accum.barStartMs);

            accum.startNewBar(bucketTs, price, deltaVolume);
        } else {
            accum.update(price, deltaVolume);
        }
        return accum.volAccum;
    }

    // =========================================================================
    // ?逾?fix2: loadPositionSnapshot
    //    - 筌?Ŋ??揶쏄퉮????쎈솭 ??positionCacheUpdatedMs??FAIL_RETRY 揶쏄쑨爰??곗쨮 ??쇱젟??
    //      ??쇱벉 tick?癒?퐣 ?????揶쏅벡??(疫꿸퀣?? ??쎈솭??猷?揶쏄퉮??????곴퐣 stale 筌?Ŋ???얜똾釉?????
    //    - liveUnavailable=true ????브탢??雅뚯눖揆 筌△뫀??野껋럡???곕떽?
    // =========================================================================
    private PositionSnapshot loadPositionSnapshot(String symbol, String orderExchange) {
        boolean overseas = isOverseasSymbol(symbol);
        refreshPositionCacheIfNeeded("loadPositionSnapshot");

        PositionSnapshot cached = positionCache.get(symbol);
        if (cached != null) {
            if (StringUtils.hasText(cached.exchange)) {
                symbolExchange.put(symbol, cached.exchange);
            }
            if (cached.quantity > 0) {
                syncPositionToDB(symbol, new RealPosition(cached.quantity, cached.avgPrice));
            }
            return cached;
        }

        boolean cacheFresh = overseas ? isUsCacheFresh() : isKrxCacheFresh();
        AutoPosition dbPos = positionService.getPosition(symbol);
        int qty = dbPos != null ? dbPos.getQuantity() : 0;
        double avg = dbPos != null ? dbPos.getAvgPrice() : 0.0;
        String name = dbPos != null ? dbPos.getSymbolName() : null;

        if (!cacheFresh) {
            // ?逾?fix2: stale 筌?Ŋ??紐껊쑓 DB????롮쎗????됱몵筌?野껋럡??- 餓λ쵎??筌띲끇猷?揶쎛?關苑?
            if (qty > 0) {
                logger.warn("[CAUTION] {} position cache stale and DB qty={} > 0 - sell orders may double if live position already closed",
                        symbol, qty);
            }
            return new PositionSnapshot(qty, avg, true, orderExchange, name);
        }

        logger.debug("{} position cache miss for {}, using DB qty={} (cache fresh)",
                overseas ? "Overseas" : "Domestic", symbol, qty);
        return new PositionSnapshot(qty, avg, false, orderExchange, name);
    }

    private void syncPositionToDB(String symbol, RealPosition real) {
        AutoPosition dbPos = positionService.getPosition(symbol);
        int dbQty = dbPos != null ? dbPos.getQuantity() : 0;
        double dbAvg = dbPos != null ? dbPos.getAvgPrice() : 0.0;

        if (dbQty != real.quantity || Math.abs(dbAvg - real.avgPrice) > 0.0001) {
            logger.info("Position sync {}: DB qty={} avg={} -> live qty={} avg={}",
                    symbol, dbQty, dbAvg, real.quantity, real.avgPrice);
            positionService.updatePosition(symbol, real.quantity, real.avgPrice);
        }
    }

    // =========================================================================
    // ?逾?fix2: refreshPositionCacheIfNeeded
    //    疫꿸퀣?? 揶쏄퉮????쎈솭 ??positionCacheUpdatedMs 揶쏄퉮????????TTL 筌왖??롢늺 ??쇰뻻 ??뺣즲??롪돌
    //          krOk=false揶쎛 ?怨쀫꺗????stale 筌?Ŋ???④쑴??????(餓λ쵎??筌띲끇猷??袁る퓮)
    //    ??륁젟: ??쎈솭 ??positionCacheUpdatedMs??FAIL_RETRY_MS ?袁⑸퓠 筌띾슢利??롫즲嚥???쇱젟
    //          ????쇱벉 tick?癒?퐣 ??쥓?ㅵ칰?????袁る릭??筌?tick筌띾뜄??API???癒?굡?귐? ??놁벉
    // =========================================================================
    private void refreshPositionCacheIfNeeded(String reason) {
        long now = System.currentTimeMillis();
        if (now - positionCacheUpdatedMs < POSITION_CACHE_TTL_MS) {
            return;
        }
        if (!positionCacheRefreshing.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!ensureAccountConfigReady("positionCache:" + reason)) {
                // ?逾?fix2: ??쎈솭 ??FAIL_RETRY_MS ??쇰퓠 筌띾슢利??롫즲嚥???쇱젟
                positionCacheUpdatedMs = now - POSITION_CACHE_TTL_MS + POSITION_CACHE_FAIL_RETRY_MS;
                return;
            }

            Map<String, LivePosition> liveKr = new HashMap<>();
            Map<String, LivePosition> liveUs = new HashMap<>();
            boolean krOk = collectDomesticPositions(liveKr);
            boolean usOk = collectOverseasPositions(liveUs);

            if (!krOk && !usOk) {
                // ?逾?fix2: ????뽰삢 筌뤴뫀紐???쎈솭 ????쥓??????袁? ?袁る퉸 TTL ??ν뀧
                positionCacheUpdatedMs = now - POSITION_CACHE_TTL_MS + POSITION_CACHE_FAIL_RETRY_MS;
                logger.warn("Position cache refresh failed (krOk={}, usOk={}) - will retry in {}ms",
                        krOk, usOk, POSITION_CACHE_FAIL_RETRY_MS);
                return;
            }

            Map<String, PositionSnapshot> updated = new HashMap<>(positionCache);
            if (krOk) {
                applyCacheForMarket(updated, liveKr, true);
                lastKrCacheOkMs = now;
            }
            if (usOk) {
                applyCacheForMarket(updated, liveUs, false);
                lastUsCacheOkMs = now;
            }

            positionCache.clear();
            positionCache.putAll(updated);
            // ?逾?fix2: ?봔???源껊궗????????袁⑸뮞??遊?揶쏄퉮??(疫꿸퀣????덉뵬)
            positionCacheUpdatedMs = now;

        } finally {
            positionCacheRefreshing.set(false);
        }
    }

    private void applyCacheForMarket(Map<String, PositionSnapshot> target,
                                     Map<String, LivePosition> livePositions,
                                     boolean krxMarket) {
        if (target == null) return;
        if (livePositions == null) livePositions = new HashMap<>();

        target.entrySet().removeIf(entry -> {
            String symbol = entry.getKey();
            PositionSnapshot snapshot = entry.getValue();
            if (snapshot == null) return false;
            if (krxMarket) {
                return isKrxSymbol(symbol) || "KRX".equalsIgnoreCase(snapshot.exchange);
            }
            return !isKrxSymbol(symbol) && !"KRX".equalsIgnoreCase(snapshot.exchange);
        });

        for (LivePosition pos : livePositions.values()) {
            if (pos.quantity <= 0) continue;
            PositionSnapshot snapshot = new PositionSnapshot(
                    pos.quantity,
                    pos.avgPrice,
                    false,
                    pos.exchange,
                    pos.symbolName
            );
            target.put(pos.symbol, snapshot);
            if (StringUtils.hasText(pos.exchange)) {
                symbolExchange.put(pos.symbol, pos.exchange);
            }
            if (StringUtils.hasText(pos.symbolName)) {
                symbolNameCache.put(pos.symbol, pos.symbolName);
            }
        }
    }

    private boolean isKrxCacheFresh() {
        if (lastKrCacheOkMs <= 0) return false;
        return (System.currentTimeMillis() - lastKrCacheOkMs) <= POSITION_CACHE_MAX_STALE_MS;
    }

    private boolean isUsCacheFresh() {
        if (lastUsCacheOkMs <= 0) return false;
        return (System.currentTimeMillis() - lastUsCacheOkMs) <= POSITION_CACHE_MAX_STALE_MS;
    }

    private boolean passesBuyGate(String symbol,
                                  com.autotrading.model.OrderCommand command,
                                  String orderExchange,
                                  PositionSnapshot pos) {
        Integer failCount = positionSyncFailCount.get(symbol);
        if (failCount != null && failCount >= 3) {
            logger.warn("BUY blocked (position sync failed {}x) for {}", failCount, symbol);
            return false;
        }

        // ?逾?fix2: liveUnavailable ?怨밴묶?癒?퐣 筌띲끉??筌△뫀??(??????븍뜇????怨밴묶?癒?퐣 筌욊쑴??獄쎻뫗?)
        if (pos.liveUnavailable) {
            logger.warn("BUY blocked (live position unavailable - cache stale) for {} exchange={} qty={} avg={}",
                    symbol, pos.exchange, pos.quantity, pos.avgPrice);
            return false;
        }

        Instant now = Instant.now();

        if (isKrxSymbol(symbol)) {
            if (isKrxOpeningBlocked()) {
                logger.info("BUY blocked (opening window 09:00~09:05 KST) for {}", symbol);
                return false;
            }
            if (isKrxOpeningCautious()) {
                logger.info("BUY caution window 09:05~09:15 KST for {} - proceeding with care", symbol);
            }
        }

        Instant slTime = stopLossTime.get(symbol);
        if (slTime != null && now.isBefore(slTime.plusSeconds(COOLDOWN_STOPLOSS_SEC))) {
            long rem = COOLDOWN_STOPLOSS_SEC - (now.getEpochSecond() - slTime.getEpochSecond());
            logger.info("BUY blocked (stop-loss cooldown {}s remaining) for {}", rem, symbol);
            return false;
        }

        Instant sellTime = lastSellTime.get(symbol);
        if (sellTime != null && now.isBefore(sellTime.plusSeconds(COOLDOWN_NORMAL_SEC))) {
            long rem = COOLDOWN_NORMAL_SEC - (now.getEpochSecond() - sellTime.getEpochSecond());
            logger.info("BUY blocked (sell cooldown {}s remaining) for {}", rem, symbol);
            return false;
        }

        if (buyFillConfirmTasks.containsKey(symbol)) {
            logger.info("BUY blocked (fill confirm pending) for {}", symbol);
            return false;
        }

        if (pos.quantity > 0) {
            logger.info("BUY blocked (snapshot qty={} > 0) for {}", pos.quantity, symbol);
            return false;
        }

        return true;
    }

    private boolean passesSellGate(String symbol,
                                   com.autotrading.model.OrderCommand command,
                                   String orderExchange) {
        boolean defensiveExit = command.isMarketOrder()
                || "STOP_LOSS".equalsIgnoreCase(command.getReason())
                || "EMERGENCY_STOP".equalsIgnoreCase(command.getReason());

        RealPosition livePos = isOverseasSymbol(symbol)
                ? fetchOverseasRealPosition(symbol, orderExchange)
                : fetchRealPosition(symbol);

        AutoPosition dbPosNow = positionService.getPosition(symbol);
        int dbQtyNow = dbPosNow != null ? dbPosNow.getQuantity() : 0;
        double dbAvgNow = dbPosNow != null ? dbPosNow.getAvgPrice() : 0.0;

        if (livePos == null && dbQtyNow <= 0 && defensiveExit) {
            logger.warn("STOP_LOSS sell recheck for {} -> retry live position", symbol);
            livePos = isOverseasSymbol(symbol)
                    ? fetchOverseasRealPosition(symbol, orderExchange)
                    : fetchRealPosition(symbol);

            dbPosNow = positionService.getPosition(symbol);
            dbQtyNow = dbPosNow != null ? dbPosNow.getQuantity() : 0;
            dbAvgNow = dbPosNow != null ? dbPosNow.getAvgPrice() : 0.0;
        }

        if (livePos != null && (livePos.quantity != dbQtyNow
                || Math.abs(livePos.avgPrice - dbAvgNow) > 0.0001)) {
            logger.info("Pre-SELL position sync {}: DB qty={} -> live qty={} avg={}",
                    symbol, dbQtyNow, livePos.quantity, livePos.avgPrice);
            positionService.updatePosition(symbol, livePos.quantity, livePos.avgPrice);
            dbQtyNow = livePos.quantity;
        }

        int availableQty = livePos != null ? livePos.quantity : dbQtyNow;
        if (availableQty <= 0) {
            logger.info("SELL blocked (no holding) for {} reason={}", symbol, command.getReason());
            strategyEngine.clearStaleHoldState(symbol);
            return false;
        }

        if (command.getQuantity() > availableQty) {
            logger.info("SELL qty adjusted {}: requested={} -> available={}",
                    symbol, command.getQuantity(), availableQty);
            command.setQuantity(availableQty);
        }

        if (livePos == null) {
            logger.warn("SELL pre-check unavailable for {} -> proceeding with DB qty={}", symbol, dbQtyNow);
        }
        return true;
    }

    private void handleAccepted(String symbol,
                                com.autotrading.model.OrderCommand command,
                                int quantity,
                                double avgPrice,
                                double lastSeenPrice,
                                String orderExchange) {
        rejectCooldownUntilMs.remove(symbol);
        lastRejectMessage.remove(symbol);
        if ("BUY".equals(command.getType())) {
            int expectedQty = quantity + command.getQuantity();
            logger.info("BUY accepted (fill confirm pending): symbol={} expectedQty={} price={}",
                    symbol, expectedQty, command.getPrice());
            confirmBuyFilled(symbol, command.getExchange(), expectedQty);

        } else if ("SELL".equals(command.getType())) {
            strategyEngine.notifySellAccepted(symbol);

            int expectedRemainingQty = Math.max(0, quantity - command.getQuantity());
            boolean defensiveExit = command.isMarketOrder();
            double referencePrice = command.getPrice() > 0.0 ? command.getPrice() : lastSeenPrice;

            logger.info("SELL accepted (fill confirm pending): symbol={} prevQty={} sellQty={} expectedRemaining={} refPrice={} defensive={}",
                    symbol, quantity, command.getQuantity(), expectedRemainingQty, referencePrice, defensiveExit);

            confirmSellFilled(new SellFillContext(
                    symbol,
                    orderExchange,
                    quantity,
                    command.getQuantity(),
                    expectedRemainingQty,
                    avgPrice,
                    defensiveExit,
                    referencePrice,
                    command.getReason(),
                    command.isMarketOrder()
            ));
        }
    }

    private void handleRejected(String symbol,
                                com.autotrading.model.OrderCommand command,
                                String orderExchange,
                                String respMsg) {
        logger.warn("Order REJECTED for {} side={} msg={}", symbol, command.getType(), respMsg);
        long nowMs = System.currentTimeMillis();
        String msg = respMsg != null ? respMsg : "";
        boolean priceMissing = msg.contains("雅뚯눖揆???") || msg.contains("雅뚯눖揆?닌됲뀋");
        long cooldown = priceMissing ? REJECT_COOLDOWN_PRICE_MS : REJECT_COOLDOWN_MS;
        rejectCooldownUntilMs.put(symbol, nowMs + cooldown);
        lastRejectMessage.put(symbol, msg);

        if ("BUY".equals(command.getType())) {
            cancelBuyFillConfirmTask(symbol);
            strategyEngine.notifyBuyRejected(symbol);
        } else if ("SELL".equals(command.getType())) {
            cancelSellFillConfirmTask(symbol);
            strategyEngine.notifySellRejected(symbol, msg);

            RealPosition livePos = isOverseasSymbol(symbol)
                    ? fetchOverseasRealPosition(symbol, orderExchange)
                    : fetchRealPosition(symbol);

            if (livePos != null) {
                syncPositionToDB(symbol, livePos);
                if (livePos.quantity <= 0) {
                    strategyEngine.clearStaleHoldState(symbol);
                }
            }
        }
    }

    private void handleError(String symbol,
                             com.autotrading.model.OrderCommand command,
                             String respStatus,
                             String respMsg) {
        if ("BUY".equals(command.getType())) {
            cancelBuyFillConfirmTask(symbol);
            strategyEngine.notifyBuyRejected(symbol);
            logger.error("Order ERROR (BUY) for {} status={} -> pending cleared. msg={}",
                    symbol, respStatus, respMsg);
        } else {
            cancelSellFillConfirmTask(symbol);
            strategyEngine.notifySellRejected(symbol, respMsg);
            logger.error("Order ERROR (SELL) for {} status={} -> sell pending cleared. msg={}",
                    symbol, respStatus, respMsg);
        }
    }

    private void confirmBuyFilled(String symbol, String exchange, int expectedQty) {
        cancelBuyFillConfirmTask(symbol);
        AtomicInteger attempts = new AtomicInteger(0);

        Runnable task = () -> {
            int attempt = attempts.incrementAndGet();
            RealPosition real = isOverseasSymbol(symbol)
                    ? fetchOverseasRealPosition(symbol, exchange)
                    : fetchRealPosition(symbol);

            if (real != null && real.quantity >= expectedQty) {
                syncPositionToDB(symbol, real);
                strategyEngine.notifyBuyFilled(symbol);
                cancelBuyFillConfirmTask(symbol);
                logger.info("BUY fill confirmed for {} qty={} expected={}",
                        symbol, real.quantity, expectedQty);
                return;
            }

            if (attempt >= BUY_FILL_CONFIRM_MAX_ATTEMPTS) {
                cancelBuyFillConfirmTask(symbol);
                logger.warn("BUY fill not confirmed for {} (expectedQty={}) after {} attempts -> next live sync",
                        symbol, expectedQty, attempt);
            }
        };

        ScheduledFuture<?> future = buyFillConfirmExecutor.scheduleAtFixedRate(
                task,
                BUY_FILL_CONFIRM_INTERVAL_MS,
                BUY_FILL_CONFIRM_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        buyFillConfirmTasks.put(symbol, future);
    }

    private void cancelBuyFillConfirmTask(String symbol) {
        ScheduledFuture<?> task = buyFillConfirmTasks.remove(symbol);
        if (task != null) {
            task.cancel(false);
        }
    }

    // =========================================================================
    // ?逾?fix3: confirmSellFilled - timeout ??筌ｌ꼶??揶쏅벤??
    //    疫꿸퀣???얜챷??
    //      1) notifySellRejected() ?紐꾪뀱 ??sellPending=false筌???랁?entryState ??μ벉
    //      2) retryReal ??쎈솭 ??DB????롮쎗 ?븍뜆?ょ㎉?獄쎻뫗??
    //    ??륁젟:
    //      1) timeout ????쎈７筌왖??疫꿸퀣???곗쨮 notifySellFilled ?癒?뮉 clearStaleHoldState ?紐꾪뀱
    //      2) retryReal 鈺곌퀬?????쎈솭??롢늺 獄쎻뫗堉?怨몄몵嚥?DB ??롮쎗??0??곗쨮 ?λ뜃由??+ 野껋럡??嚥≪뮄??
    //      3) defensiveExit(?癒?쟿) 筌ｋ떯猿??類ㅼ뵥 ??쎈솭 ????甕???retryReal ??뺣즲
    // =========================================================================
    private void confirmSellFilled(SellFillContext ctx) {
        cancelSellFillConfirmTask(ctx.symbol);
        AtomicInteger attempts = new AtomicInteger(0);

        Runnable task = () -> {
            int attempt = attempts.incrementAndGet();

            RealPosition real = isOverseasSymbol(ctx.symbol)
                    ? fetchOverseasRealPosition(ctx.symbol, ctx.exchange)
                    : fetchRealPosition(ctx.symbol);

            if (real != null) {
                boolean confirmed = real.quantity <= ctx.expectedRemainingQty;

                if (confirmed) {
                    syncPositionToDB(ctx.symbol, real);
                    if (real.quantity <= 0) {
                        // notifySellFilled 이전에 호출 — 이후 엔진 entryState가 초기화됨
                        StrategyEngine.EntrySnapshot snap = strategyEngine.getEntrySnapshot(ctx.symbol);
                        tradeHistoryService.recordTrade(
                                ctx.symbol,
                                isOverseasSymbol(ctx.symbol) ? "US" : "KRX",
                                snap,
                                ctx.avgPrice,
                                ctx.referencePrice,
                                ctx.previousQty,
                                ctx.reason,
                                false);
                    }
                    strategyEngine.notifySellFilled(ctx.symbol, real.quantity);
                    cancelSellFillConfirmTask(ctx.symbol);

                    double soldQty = Math.max(0, ctx.previousQty - real.quantity);
                    double pnl = (ctx.referencePrice - ctx.avgPrice) * soldQty;
                    double pnlPct = ctx.avgPrice > 0.0
                            ? ((ctx.referencePrice - ctx.avgPrice) / ctx.avgPrice) * 100.0
                            : 0.0;

                    logger.info("SELL fill confirmed for {} prevQty={} remainingQty={} soldQty={} refPnl={} ({}%) defensive={}",
                            ctx.symbol, ctx.previousQty, real.quantity, soldQty,
                            String.format("%.2f", pnl), String.format("%.2f", pnlPct), ctx.defensiveExit);

                    if (ctx.defensiveExit) {
                        if (isShortCooldownExit(ctx.reason)) {
                            lastSellTime.put(ctx.symbol, Instant.now());
                        } else {
                            stopLossTime.put(ctx.symbol, Instant.now());
                        }
                        if (pnl < 0) {
                            riskManager.addLoss(Math.abs(pnl));
                        }
                    } else {
                        lastSellTime.put(ctx.symbol, Instant.now());
                    }
                    return;
                }
            }

            int maxAttempts = isOverseasSymbol(ctx.symbol)
                    ? SELL_FILL_CONFIRM_MAX_ATTEMPTS_OVERSEAS
                    : SELL_FILL_CONFIRM_MAX_ATTEMPTS;

            // 중간 진행 상황 로그 (절반 도달 시 1회)
            if (attempt == maxAttempts / 2) {
                if (real == null) {
                    logger.warn("SELL_FILL_CHECK_MID {} attempt={}/{} — API null (포지션 조회 실패 반복)",
                            ctx.symbol, attempt, maxAttempts);
                } else {
                    logger.warn("SELL_FILL_CHECK_MID {} attempt={}/{} — qty={} > expected={} (미체결 대기 중)",
                            ctx.symbol, attempt, maxAttempts, real.quantity, ctx.expectedRemainingQty);
                }
            }

            if (attempt >= maxAttempts) {
                cancelSellFillConfirmTask(ctx.symbol);

                // ?逾?fix3: timeout ????쎈７筌왖???????(?癒?쟿??????甕???
                RealPosition retryReal = isOverseasSymbol(ctx.symbol)
                        ? fetchOverseasRealPosition(ctx.symbol, ctx.exchange)
                        : fetchRealPosition(ctx.symbol);

                if (ctx.defensiveExit && retryReal == null) {
                    // ?癒?쟿 筌ｋ떯猿??類ㅼ뵥 ?븍뜃? ????甕곕뜄彛??곕떽? ?????
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    retryReal = isOverseasSymbol(ctx.symbol)
                            ? fetchOverseasRealPosition(ctx.symbol, ctx.exchange)
                            : fetchRealPosition(ctx.symbol);
                }

                if (retryReal != null) {
                    syncPositionToDB(ctx.symbol, retryReal);
                    if (retryReal.quantity <= 0) {
                        // ?逾?fix3: ??쇱젫嚥?筌ｋ떯猿????notifySellFilled嚥?entryState ?類ｂ봺
                        StrategyEngine.EntrySnapshot snap = strategyEngine.getEntrySnapshot(ctx.symbol);
                        tradeHistoryService.recordTrade(
                                ctx.symbol,
                                isOverseasSymbol(ctx.symbol) ? "US" : "KRX",
                                snap,
                                ctx.avgPrice,
                                ctx.referencePrice,
                                ctx.previousQty,
                                ctx.reason,
                                false);
                        strategyEngine.notifySellFilled(ctx.symbol, 0);
                        double soldQty = Math.max(0, ctx.previousQty);
                        double pnl = (ctx.referencePrice - ctx.avgPrice) * soldQty;
                        if (ctx.defensiveExit) {
                            if (isShortCooldownExit(ctx.reason)) {
                                lastSellTime.put(ctx.symbol, Instant.now());
                            } else {
                                stopLossTime.put(ctx.symbol, Instant.now());
                            }
                            if (pnl < 0) {
                                riskManager.addLoss(Math.abs(pnl));
                            }
                        } else {
                            lastSellTime.put(ctx.symbol, Instant.now());
                        }
                        logger.info("SELL fill confirmed (timeout fallback) for {} - live qty=0", ctx.symbol);
                    } else if (retryReal.quantity <= ctx.expectedRemainingQty) {
                        // ?봔??筌ｋ떯猿??野껋럩??
                        strategyEngine.notifySellFilled(ctx.symbol, retryReal.quantity);
                        double soldQty = Math.max(0, ctx.previousQty - retryReal.quantity);
                        double pnl = (ctx.referencePrice - ctx.avgPrice) * soldQty;
                        if (ctx.defensiveExit) {
                            if (isShortCooldownExit(ctx.reason)) {
                                lastSellTime.put(ctx.symbol, Instant.now());
                            } else {
                                stopLossTime.put(ctx.symbol, Instant.now());
                            }
                            if (pnl < 0) {
                                riskManager.addLoss(Math.abs(pnl));
                            }
                        } else {
                            lastSellTime.put(ctx.symbol, Instant.now());
                        }
                        logger.info("SELL partial fill confirmed (timeout fallback) for {} remainingQty={}",
                                ctx.symbol, retryReal.quantity);
                    } else {
                        // 筌ｋ떯猿?????- sellPending ??곸젫筌???랁??????揶쎛?館釉?칰???
                        // ?逾?fix3: 疫꿸퀣?덌㎗?롮쓥 notifySellRejected X ??notifySellRejected??sellPending筌?false嚥?筌띾슢諭얏?
                        //           entryState(partialTakeProfitDone ?????醫????嚥??袁⑥셽????쇰뻻 筌띲끇猷???뺣즲 揶쎛??
                        if (!ctx.defensiveExit && !ctx.marketOrder && isTakeProfitReason(ctx.reason)) {
                            strategyEngine.markSellFallbackToMarket(ctx.symbol, ctx.reason);
                            logger.warn("SELL fallback scheduled for {} reason={} (unfilled LIMIT)",
                                    ctx.symbol, ctx.reason);
                        }
                        strategyEngine.notifySellRejected(ctx.symbol, "FILL_UNCONFIRMED");
                        logger.warn("SELL fill not confirmed for {} after {} attempts - live qty={} still holding, sell retry allowed",
                                ctx.symbol, attempt, retryReal.quantity);
                    }
                } else {
                    // ?逾?fix3: ??쎈７筌왖???袁⑹읈 鈺곌퀬???븍뜃? ??DB???醫듚?疫꿸퀣???곗쨮 野껋럡????sellPending ??곸젫
                    //   (疫꿸퀣?? notifySellRejected ?紐꾪뀱, entryState ??λ툡???????揶쎛??- ???봔?브쑴? ?醫?)
                    strategyEngine.notifySellRejected(ctx.symbol, "FILL_UNRESOLVABLE");
                    logger.error("[ACTION_NEEDED] SELL fill unresolvable for {} after {} attempts - live position unknown. Manual check required. DB qty={}",
                            ctx.symbol, attempt,
                            Optional.ofNullable(positionService.getPosition(ctx.symbol))
                                    .map(AutoPosition::getQuantity).orElse(-1));
                }
            }
        };

        ScheduledFuture<?> future = sellFillConfirmExecutor.scheduleAtFixedRate(
                task,
                SELL_FILL_CONFIRM_INTERVAL_MS,
                SELL_FILL_CONFIRM_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        sellFillConfirmTasks.put(ctx.symbol, future);
    }

    private void cancelSellFillConfirmTask(String symbol) {
        ScheduledFuture<?> task = sellFillConfirmTasks.remove(symbol);
        if (task != null) {
            task.cancel(false);
        }
    }

    private RealPosition fetchRealPosition(String symbol) {
        try {
            Map<String, Object> balResp = kisApiClient.fetchDomesticBalance();
            if (!"OK".equals(balResp.get("status"))) {
                logger.warn("fetchDomesticBalance non-OK: {}", balResp.get("message"));
                return null;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output1 =
                    (List<Map<String, Object>>) balResp.get("output1");
            if (output1 == null) return new RealPosition(0, 0.0);

            for (Map<String, Object> item : output1) {
                String pdno = String.valueOf(item.getOrDefault("pdno", ""));
                if (!symbol.equalsIgnoreCase(pdno)) continue;
                int qty = parseIntSafe(item.get("hldg_qty"));
                double avg = parseDoubleSafe(item.get("pchs_avg_pric"));
                return new RealPosition(qty, avg);
            }
            return new RealPosition(0, 0.0);
        } catch (Exception e) {
            logger.warn("fetchRealPosition failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private RealPosition fetchOverseasRealPosition(String symbol, String exchange) {
        List<String> exchanges = buildOverseasExchangeCandidates(exchange);
        for (String ex : exchanges) {
            try {
                Map<String, Object> balResp = fetchOverseasBalanceWithRetry(ex, 2);
                if (balResp == null || !"OK".equals(balResp.get("status"))) {
                    String msg = balResp != null ? String.valueOf(balResp.get("message")) : "null response";
                    logger.warn("fetchOverseasBalance non-OK for {}({}): {}", symbol, ex, msg);
                    continue;
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> output1 =
                        (List<Map<String, Object>>) balResp.get("output1");
                if (output1 == null) continue;

                for (Map<String, Object> item : output1) {
                    String pdno = pickString(item, "ovrs_pdno", "pdno", "symbol", "item_cd");
                    if (!symbol.equalsIgnoreCase(pdno)) continue;
                    int qty = pickInt(item, "ovrs_cblc_qty", "hldg_qty", "hold_qty", "cblc_qty");
                    double avg = pickDouble(item, "pchs_avg_pric", "avg_unpr", "pchs_unpr", "avg_pric");
                    symbolExchange.put(symbol, ex);
                    return new RealPosition(qty, avg);
                }
            } catch (Exception e) {
                logger.warn("fetchOverseasRealPosition failed for {}({}): {}", symbol, ex, e.getMessage());
            }
        }
        return null;
    }

    private StockQuote safeFetchQuote(String symbol, String quoteExchangeHint) {
        try {
            return marketDataService.fetchPrice(symbol, quoteExchangeHint);
        } catch (IllegalStateException e) {
            int fail = quoteFailCount.merge(symbol, 1, Integer::sum);
            if (fail == 1 || fail % 12 == 0 || fail == OVERSEAS_QUOTE_FAIL_LIMIT) {
                logger.warn("Quote fetch failed for {} (count={}): {}", symbol, fail, e.getMessage());
            }
            if (isOverseasSymbol(symbol) && fail >= OVERSEAS_QUOTE_FAIL_LIMIT) {
                logger.error("Auto-stop {} due to repeated overseas quote failures", symbol);
                stopSymbol(symbol);
            }
            return null;
        }
    }

    private boolean isKrxOpeningBlocked() {
        LocalTime now = LocalDateTime.now(KST_ZONE).toLocalTime();
        return !now.isBefore(KRX_OPENING_BLOCK_START) && now.isBefore(KRX_OPENING_BLOCK_END);
    }

    private boolean ensureAccountConfigReady(String caller) {
        if (kisApiClient.isAccountConfigValid()) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastAccountConfigWarnMs > 10_000L) {
            lastAccountConfigWarnMs = now;
            logger.error("KIS account config missing - blocking {} (set kis.accountNo/kis.accountProductCode)", caller);
        }
        return false;
    }

    private boolean isTakeProfitReason(String reason) {
        if (reason == null) return false;
        return reason.startsWith("TAKE_PROFIT_");
    }

    private boolean isShortCooldownExit(String reason) {
        if (reason == null) return false;
        return "BREAKEVEN_GUARD".equals(reason)
                || "SELL_TIMEOUT_MARKET_RETRY".equals(reason)
                || "EOD_FORCE_SELL".equals(reason)
                || reason.startsWith("TRAIL_");
    }

    private boolean isKrxOpeningCautious() {
        LocalTime now = LocalDateTime.now(KST_ZONE).toLocalTime();
        return !now.isBefore(KRX_OPENING_BLOCK_END) && now.isBefore(KRX_OPENING_CAUTION_END);
    }

    private boolean isKrxSymbol(String symbol) {
        return symbol != null && symbol.matches("^\\d{5,6}$");
    }

    private boolean isOverseasSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) return false;
        String normalized = symbol.trim().toUpperCase();

        String cached = symbolExchange.get(normalized);
        if (StringUtils.hasText(cached)) {
            String ex = cached.trim().toUpperCase();
            if ("KRX".equals(ex) || "KR".equals(ex) || "KOSPI".equals(ex) || "KOSDAQ".equals(ex)) return false;
            if ("NASD".equals(ex) || "NASDAQ".equals(ex) || "NAS".equals(ex)
                    || "NYSE".equals(ex) || "NYS".equals(ex)
                    || "AMEX".equals(ex) || "AMS".equals(ex)) return true;
        }
        return !normalized.matches("^\\d{5,6}$");
    }

    private StrategyEngine.Market resolveMarket(String exchUpper) {
        if (exchUpper == null) return StrategyEngine.Market.KRX;
        switch (exchUpper) {
            case "KRX":
            case "KR":
            case "KOSPI":
            case "KOSDAQ":
                return StrategyEngine.Market.KRX;
            default:
                return StrategyEngine.Market.US;
        }
    }

    private String resolveOrderExchangeForStart(String symbol, String requestedExchange) {
        if (!isOverseasSymbol(symbol)) return "KRX";
        if (StringUtils.hasText(requestedExchange)) {
            return normalizeOverseasOrderExchange(requestedExchange);
        }
        WatchlistItem watch = watchlistDao.findBySymbol(symbol);
        if (watch != null && StringUtils.hasText(watch.getExchange())) {
            return normalizeOverseasOrderExchange(watch.getExchange());
        }
        return resolveOrderExchangeForRuntime(symbol);
    }

    private String resolveOrderExchangeForRuntime(String symbol) {
        if (!isOverseasSymbol(symbol)) return "KRX";
        String cached = symbolExchange.get(symbol);
        if (StringUtils.hasText(cached)) return normalizeOverseasOrderExchange(cached);
        String resolved = marketDataService.resolveExchangeForOrder(symbol);
        String normalized = normalizeOverseasOrderExchange(resolved);
        symbolExchange.put(symbol, normalized);
        return normalized;
    }

    private String syncOrderExchangeWithQuote(String symbol, String currentOrderExchange) {
        if (!isOverseasSymbol(symbol)) return currentOrderExchange;
        String resolved = normalizeOverseasOrderExchange(
                marketDataService.resolveExchangeForOrder(symbol));
        if (!resolved.equals(currentOrderExchange)) {
            logger.info("Exchange auto-corrected for {}: {} -> {}", symbol, currentOrderExchange, resolved);
            symbolExchange.put(symbol, resolved);
            return resolved;
        }
        return currentOrderExchange;
    }

    private String normalizeOverseasOrderExchange(String exchange) {
        if (!StringUtils.hasText(exchange)) return "NASD";
        switch (exchange.trim().toUpperCase()) {
            case "NAS":
            case "NASDAQ":
            case "NASD":
                return "NASD";
            case "NYS":
            case "NYSE":
                return "NYSE";
            case "AMS":
            case "AMEX":
                return "AMEX";
            default:
                return exchange.trim().toUpperCase();
        }
    }

    private String toQuoteExchangeHint(String orderExchange) {
        if (!StringUtils.hasText(orderExchange)) return null;
        switch (normalizeOverseasOrderExchange(orderExchange)) {
            case "NASD":
                return "NAS";
            case "NYSE":
                return "NYS";
            case "AMEX":
                return "AMS";
            default:
                return orderExchange;
        }
    }

    private String resolveSymbolName(String symbol, StockQuote quote) {
        String fromQuote = quote != null ? quote.getName() : null;
        if (StringUtils.hasText(fromQuote)) {
            symbolNameCache.put(symbol, fromQuote.trim());
            return fromQuote.trim();
        }

        String cached = symbolNameCache.get(symbol);
        if (StringUtils.hasText(cached)) return cached;

        AutoPosition position = positionService.getPosition(symbol);
        if (position != null && StringUtils.hasText(position.getSymbolName())) {
            symbolNameCache.put(symbol, position.getSymbolName().trim());
            return position.getSymbolName().trim();
        }
        return symbol;
    }

    private String normalizeStatus(Object rawStatus) {
        if (rawStatus == null) return STATUS_ERROR;
        String s = rawStatus.toString().trim().toUpperCase();
        if (s.equals("ACCEPTED") || s.equals("0") || s.equals("00") || s.equals("SUCCESS")) {
            return STATUS_ACCEPTED;
        }
        if (s.equals("REJECTED") || s.equals("1") || s.equals("REJECT")) {
            return STATUS_REJECTED;
        }
        return STATUS_ERROR;
    }

    private String buildLogReason(String respStatus, String respMsg) {
        String reason = "Strategy | " + respStatus;
        if (respMsg != null && !respMsg.isBlank()) {
            String clean = respMsg.replaceAll("[\\r\\n]+", " ").trim();
            if (clean.length() > 180) clean = clean.substring(0, 180);
            reason = reason + " | " + clean;
        }
        return reason;
    }

    private int parseIntSafe(Object val) {
        if (val == null) return 0;
        try {
            return (int) Double.parseDouble(val.toString().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(Object val) {
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Boolean pickBoolean(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String key : keys) {
            Object v = row.get(key);
            if (v == null) continue;

            if (v instanceof Boolean) {
                return (Boolean) v;
            }

            String s = String.valueOf(v).trim().toUpperCase();
            if ("TRUE".equals(s) || "Y".equals(s) || "YES".equals(s) || "1".equals(s) || "OPEN".equals(s)) {
                return true;
            }
            if ("FALSE".equals(s) || "N".equals(s) || "NO".equals(s) || "0".equals(s) || "CLOSED".equals(s)) {
                return false;
            }
        }
        return null;
    }

    private String pickString(Map<String, Object> row, String... keys) {
        if (row == null) return "";
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) return s;
            }
        }
        return "";
    }

    private int pickInt(Map<String, Object> row, String... keys) {
        return parseIntSafe(pickString(row, keys));
    }

    private double pickDouble(Map<String, Object> row, String... keys) {
        return parseDoubleSafe(pickString(row, keys));
    }

    public synchronized String stopSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return "Invalid symbol";
        String sym = symbol.trim().toUpperCase();
        if (isMarketProxy(sym)) {
            return "Market proxy stop is blocked: " + sym;
        }

        ScheduledFuture<?> task = tickTasks.remove(sym);
        if (task != null) task.cancel(false);

        ScheduledExecutorService exec = schedulers.remove(sym);
        if (exec != null) exec.shutdownNow();

        cancelBuyFillConfirmTask(sym);
        cancelSellFillConfirmTask(sym);
        strategyEngine.resetSymbol(sym);

        barAccumMap.remove(sym);
        prevVolume.remove(sym);
        lastSellTime.remove(sym);
        stopLossTime.remove(sym);
        symbolExchange.remove(sym);
        quoteFailCount.remove(sym);
        symbolNameCache.remove(sym);
        // ?逾?fix(??곸겫): stopSymbol ??뽯퓠??reject ?묅뫀????類ｂ봺
        rejectCooldownUntilMs.remove(sym);
        lastRejectMessage.remove(sym);
        positionSyncFailCount.remove(sym);
        marketSessionCache.clear();

        if (exec == null) return "Not Running: " + sym;
        logger.info("Scheduler stopped for {} -> state cleared", sym);
        return "Stopped " + sym;
    }

    /** 현재 실행 중인 모든 종목에 동일한 주문금액 적용 */
    public synchronized int setBuyAmountAll(double amount) {
        int count = 0;
        for (String sym : schedulers.keySet()) {
            strategyEngine.setBuyAmount(sym, amount);
            count++;
        }
        logger.info("setBuyAmountAll: amount={} applied to {} symbols", amount, count);
        return count;
    }

    public synchronized String stop() {
        if (schedulers.isEmpty()) {
            buyFillConfirmTasks.values().forEach(f -> f.cancel(false));
            buyFillConfirmTasks.clear();
            sellFillConfirmTasks.values().forEach(f -> f.cancel(false));
            sellFillConfirmTasks.clear();
            return "Not Running";
        }

        List<String> running = new ArrayList<>(schedulers.keySet());
        tickTasks.values().forEach(f -> f.cancel(false));
        tickTasks.clear();
        schedulers.values().forEach(ScheduledExecutorService::shutdownNow);
        schedulers.clear();

        for (String sym : running) {
            cancelBuyFillConfirmTask(sym);
            cancelSellFillConfirmTask(sym);
            strategyEngine.resetSymbol(sym);
        }

        buyFillConfirmTasks.clear();
        sellFillConfirmTasks.clear();
        barAccumMap.clear();
        prevVolume.clear();
        lastSellTime.clear();
        stopLossTime.clear();
        symbolExchange.clear();
        quoteFailCount.clear();
        symbolNameCache.clear();
        marketSessionCache.clear();
        // ?逾?fix(??곸겫): stop() ?袁⑷퍥 ?類? ??reject ?묅뫀???猷??類ｂ봺
        rejectCooldownUntilMs.clear();
        lastRejectMessage.clear();
        positionSyncFailCount.clear();

        logger.info("Scheduler stopped (all symbols) -> all state cleared");
        return "Stopped";
    }

    public String status() {
        return schedulers.isEmpty()
                ? "STOPPED"
                : "RUNNING (" + String.join(",", schedulers.keySet()) + ")";
    }

    public List<Map<String, String>> runningSymbols() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (String symbol : schedulers.keySet()) {
            Map<String, String> row = new HashMap<>();
            row.put("symbol", symbol);
            row.put("exchange", resolveOrderExchangeForRuntime(symbol));
            rows.add(row);
        }
        rows.sort((a, b) -> a.get("symbol").compareTo(b.get("symbol")));
        return rows;
    }
}

