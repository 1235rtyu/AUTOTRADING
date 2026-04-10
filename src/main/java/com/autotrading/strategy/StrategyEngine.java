package com.autotrading.strategy;

import com.autotrading.market.MarketDataService;
import com.autotrading.model.OrderCommand;
import com.autotrading.model.StockQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StrategyEngine {

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngine.class);

    public enum Market { KRX, US }

    private enum EntryMode { NONE, PULLBACK, BREAKOUT, VOLUME_BREAKOUT }

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");

    // =========================
    // History
    // =========================
    private static final int TICK_HISTORY_CAPACITY = 180;
    private static final int MINUTE_HISTORY_CAPACITY = 150;
    private static final int MIN_HISTORY_BARS = 8;
    private static final int MIN_HISTORY_SPAN_SECONDS = 300;

    private static final int VELOCITY_WINDOW_MIN_SECONDS = 180;
    private static final int VELOCITY_WINDOW_MAX_SECONDS = 300;

    private static final int TREND_SHORT_MIN_SECONDS = 120;
    private static final int TREND_SHORT_MAX_SECONDS = 180;
    private static final int TREND_MID_MIN_SECONDS = 180;
    private static final int TREND_MID_MAX_SECONDS = 300;
    private static final int TREND_LONG_MIN_SECONDS = 300;
    private static final int TREND_LONG_MAX_SECONDS = 480;

    // =========================
    // Entry timing
    // =========================
    private static final long ENTRY_READY_TTL_MS = 35_000L;
    private static final long ENTRY_READY_MIN_DELAY_MS = 2_000L;
    private static final int BUY_RECENT_RANGE_BARS = 6;

    // =========================
    // Momentum / breakout
    // =========================
    private static final double MOMENTUM_PRICE_NEAR_HIGH = 0.9970;
    private static final double MOMENTUM_VOLUME_MULT = 1.8;
    private static final double STRONG_BREAKOUT_VOLUME_MULT = 1.9;
    private static final double VOLUME_BREAKOUT_VOLUME_MULT = 2.0;
    private static final double VOLUME_SURGE_MULT_FOR_SIZE_UP = 2.3;

    // =========================
    // Pullback zone (shared for KRX/US)
    // =========================
    private static final double PULLBACK_UPPER_FROM_HIGH = 0.980;
    private static final double PULLBACK_LOWER_FROM_HIGH = 0.960;

    private static final double PULLBACK_VOLUME_MULT = 0.90;

    private static final double LOW_VOLUME_SKIP_MULT = 0.85;

    // =========================
    // VWAP filter
    // =========================
    private static final double VWAP_MAX_DISTANCE_PCT = 0.0080;
    private static final double VWAP_NEAR_DISTANCE_PCT = 0.0040;
    private static final double VWAP_BREAK_SELL_BUFFER = 0.9990;

    // =========================
    // Liquidity / price filter
    // =========================
    private static final double MIN_KRX_PRICE = 1000.0;

    private static final double MIN_KRX_LATEST_TURNOVER = 30_000_000.0;
    private static final double MIN_KRX_AVG_TURNOVER = 20_000_000.0;

    private static final double MIN_US_LATEST_TURNOVER = 10_000.0;
    private static final double MIN_US_AVG_TURNOVER = 6_000.0;

    // =========================
    // Risk / Exit (shared settings)
    // =========================
    private static final double STOP_LOSS_MULT    = 0.985;   // -1.5%
    private static final double EMERGENCY_STOP_MULT = 0.945; // emergency stop

    private static final double TAKE_PROFIT_PARTIAL_MULT = 1.020;         // +2.0% partial take profit
    private static final double BREAKEVEN_STOP_AFTER_PARTIAL_MULT = 1.003; // +0.3% breakeven stop after partial TP

    private static final double MAX_DAILY_LOSS_PCT = 0.04; // 일일 최대 손실 4%

    private static final long SELL_MARKET_FALLBACK_TTL_MS = 60_000L;

    // TRAIL_FROM_HIGH_MULT removed: replaced by dynamic profit-rate-based trailing

    // =========================
    // Sizing
    // =========================
    private static final double BASE_SIZE_PULLBACK = 1.20;
    private static final double BASE_SIZE_BREAKOUT = 0.80;
    private static final double SIZE_UP_MULT = 1.50;

    // =========================
    // Execution control
    // =========================
    private static final long BUY_COOLDOWN_MS = 90_000L;
    private static final long PENDING_TIMEOUT_MS = 30_000L;
    private static final long SELL_RETRY_COOLDOWN_MS = 5_000L;
    private static final long SELL_PENDING_TIMEOUT_MS = 15_000L;

    private static final long REENTER_PROFIT_COOLDOWN_MS = 300_000L;
    private static final long REENTER_TRAIL_COOLDOWN_MS  = 120_000L;  // 2m after TRAIL_DYNAMIC exit
    private static final long REENTER_STOPLOSS_COOLDOWN_MS = 900_000L;

    private static final int MAX_DAILY_ENTRY_COUNT = 2;
    private static final int MAX_SAME_PATTERN_ENTRY_COUNT = 1;

    // =========================
    // Time stop
    // =========================
    private static final long MAX_HOLD_SOFT_MS = 900_000L;    // 15m soft time stop
    private static final long MAX_HOLD_HARD_MS = 1_800_000L; // 30m hard time stop
    private static final double TIME_STOP_SOFT_PROFIT_MULT = 1.003; // exit if below +0.3% at soft stop

    // FIXED_BUY_QTY removed: entry requires buyAmountPerOrder to be set
    private static final long MARKET_CONTEXT_TTL_MS = 300_000L;

    private final MarketDataService marketDataService;
    private final Map<String, SymbolState> states = new ConcurrentHashMap<>();
    private final Map<Market, MarketContext> marketContext = new ConcurrentHashMap<>();
    // 일일 손실 추적: 날짜별 실현 손익률 누적 합계
    private final Map<java.time.LocalDate, Double> dailyPnlAccumulator = new ConcurrentHashMap<>();

    private static class MarketContext {
        boolean choppyMarket;
        boolean marketWeak;
        double velocityShort;
        double shortAvg;
        double longAvg;
        double lastPrice;
        long updatedAtMs;
        String sourceSymbol;
    }

    private static class SymbolState {
        final PriceHistory tickHistory = new PriceHistory(TICK_HISTORY_CAPACITY);
        final MinuteBarHistory minuteHistory = new MinuteBarHistory(MINUTE_HISTORY_CAPACITY);

        Market market;

        boolean buyPending;
        long buyPendingSinceMs;

        boolean sellPending;
        long sellPendingSinceMs;

        long lastBuySignalMs;
        long lastSellSignalMs;

        long entryTimeMs;
        double highestSinceEntry;
        double buyAmountPerOrder;

        boolean partialTakeProfitDone;
        double entryPriceSnapshot;
        double lastKnownProfitRate;

        BuySignal entrySignal;
        long entryReadyAtMs;
        long entryReadyUntilMs;
        double entryReadyClose;
        String entryReadyPatternKey;
        LocalDateTime lastEntryReadyBarTs;      // actual ENTRY_READY success bar
        LocalDateTime lastHistoryRebuildBarTs;  // history rebuild attempt bar

        long lastProfitExitTimeMs;
        long lastTrailExitTimeMs;
        long lastStopLossExitTimeMs;
        int dailyEntryCount;
        java.time.LocalDate lastEntryDay;
        String lastEntryPatternKey;
        int samePatternEntryCount;

        boolean forceMarketOnNextSell;
        long forceMarketUntilMs;
        String forceMarketReason;

        EntryMode entryMode = EntryMode.NONE;
    }

    private static class BuySignal {
        boolean enoughHistory;
        int ticks;
        long spanSeconds;

        double price;
        double volume;
        double averageVolume;

        double velocity;
        double velocityShort;
        double velocityMid;
        double velocityLong;

        boolean shortUp;
        boolean midUp;
        boolean longUp;
        int trendScore;
        boolean multiUptrend;

        double recentHigh;
        double recentLow;
        double latestTurnover;
        double averageTurnover;

        boolean lowVolumeSkip;

        boolean momentumBreakout;
        boolean pullbackEntry;
        boolean volumeBreakout;
        boolean strongBreakout;

        boolean momentumNearHigh;
        boolean momentumVelocityOk;
        boolean momentumVolumeOk;

        boolean pullbackZone;
        boolean pullbackRecovering;
        boolean pullbackVelocityOk;
        boolean pullbackVolumeOk;
        double pullbackAvgShort;
        double pullbackAvgLong;
        double pullbackDepthFromHigh;

        boolean volumeBreakNearHigh;
        boolean volumeBreakVelocityOk;
        boolean volumeBreakVolumeOk;

        double vwap;
        double prevVwap;
        double vwapDistancePct;
        boolean aboveVwap;
        boolean nearVwap;
        boolean vwapSlopeUp;
        boolean vwapTooFar;
        boolean breakoutRetestReady;
        boolean breakoutRetestRecovering;

        boolean choppyMarket;
        boolean marketWeak;

        int signalScore;
        int signalCount;

        boolean timeWindowBlocked;
        boolean marketFilterPassed;
        boolean cheapStockBlocked;
        boolean turnoverFilterPassed;
        boolean absoluteLiquidityPassed;

        String rejectReason;
        String patternKey;
        EntryMode entryMode = EntryMode.NONE;

        boolean isBuyCandidate(Market market) {
            if (!enoughHistory) return false;
            if (timeWindowBlocked) return false;
            if (!marketFilterPassed) return false;
            if (cheapStockBlocked) return false;
            if (!turnoverFilterPassed) return false;
            if (!absoluteLiquidityPassed) return false;
            if (lowVolumeSkip) return false;
            if (!aboveVwap) return false;
            if (!vwapSlopeUp) return false;
            if (vwapTooFar) return false;

            // Primary: pullback rebound
            if (pullbackEntry) {
                return signalScore >= 60 && nearVwap;
            }

            // Breakout path requires retest confirmation
            if (volumeBreakout || strongBreakout) {
                return breakoutRetestReady
                        && breakoutRetestRecovering
                        && signalCount >= 3
                        && signalScore >= 65;
            }

            return false;
        }
    }

    private static class SellDecision {
        final boolean shouldSell;
        final int quantity;
        final String reason;
        final boolean marketOrder;

        SellDecision(boolean shouldSell, int quantity, String reason, boolean marketOrder) {
            this.shouldSell = shouldSell;
            this.quantity = quantity;
            this.reason = reason;
            this.marketOrder = marketOrder;
        }

        static SellDecision none() {
            return new SellDecision(false, 0, "", false);
        }
    }

    public StrategyEngine(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public void resetAll() {
        states.clear();
        marketContext.clear();
    }

    public void resetSymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        states.remove(normalized);
    }

    public void setMarket(String symbol, Market market) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        state(normalized).market = market;
    }

    public void setBuyAmount(String symbol, Double buyAmount) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        double normalizedAmount = (buyAmount == null || !Double.isFinite(buyAmount) || buyAmount <= 0.0)
                ? 0.0
                : buyAmount;

        synchronized (st) {
            st.buyAmountPerOrder = normalizedAmount;
        }

        if (normalizedAmount > 0.0) {
            logger.info("BUY amount set for {}: {}", normalized, String.format("%.2f", normalizedAmount));
        } else {
            logger.info("BUY amount cleared for {} (amount-based entry disabled)", normalized);
        }
    }

    public void clearStaleHoldState(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            st.sellPending = false;
            st.sellPendingSinceMs = 0L;
            st.lastSellSignalMs = System.currentTimeMillis();
            resetEntryState(st);
        }
    }

    public void markBuyPending(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = true;
            st.buyPendingSinceMs = System.currentTimeMillis();
        }
    }

    public void cancelBuyPending(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
        }
    }

    public void notifyBuyAccepted(String symbol) {
        cancelBuyPending(symbol);
    }

    public void notifyBuyFilled(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            if (st.entryTimeMs == 0L) {
                st.entryTimeMs = System.currentTimeMillis();
            }
        }
    }

    public void notifyBuyRejected(String symbol) {
        cancelBuyPending(symbol);
    }

    public void notifySellAccepted(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.lastSellSignalMs = System.currentTimeMillis();
        }
    }

    public void notifySellFilled(String symbol) {
        notifySellFilled(symbol, 0);
    }

    public void notifySellFilled(String symbol, int remainingQty) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.sellPending = false;
            st.sellPendingSinceMs = 0L;
            st.lastSellSignalMs = System.currentTimeMillis();

            if (remainingQty <= 0) {
                resetEntryState(st);
            }
        }
    }

    public void notifySellRejected(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.sellPending = false;
            st.sellPendingSinceMs = 0L;
            st.lastSellSignalMs = System.currentTimeMillis();
        }
    }

    public void markSellFallbackToMarket(String symbol, String reason) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.forceMarketOnNextSell = true;
            st.forceMarketUntilMs = System.currentTimeMillis() + SELL_MARKET_FALLBACK_TTL_MS;
            st.forceMarketReason = reason;
        }
    }

    public void updateMarketContextFromSymbol(String marketProxySymbol, Market market) {
        String normalized = normalizeSymbol(marketProxySymbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        MinuteBarHistory.MinuteBar latest;
        double shortAvg;
        double longAvg;
        double velocityShort;

        synchronized (st) {
            latest = st.minuteHistory.latest();
            shortAvg = st.minuteHistory.averagePrice(3);
            longAvg = st.minuteHistory.averagePrice(6);
            velocityShort = st.minuteHistory.velocitySeconds(TREND_SHORT_MIN_SECONDS, TREND_SHORT_MAX_SECONDS);
        }

        if (latest == null || latest.getClose() <= 0.0) return;
        double price = latest.getClose();

        boolean choppy = Math.abs(velocityShort) < 0.0005
                && Math.abs(shortAvg - longAvg) < (price * 0.001);
        boolean weak = velocityShort <= -0.001
                && shortAvg < longAvg;

        MarketContext ctx = marketContext.computeIfAbsent(market, m -> new MarketContext());
        synchronized (ctx) {
            ctx.choppyMarket = choppy;
            ctx.marketWeak = weak;
            ctx.velocityShort = velocityShort;
            ctx.shortAvg = shortAvg;
            ctx.longAvg = longAvg;
            ctx.lastPrice = price;
            ctx.updatedAtMs = System.currentTimeMillis();
            ctx.sourceSymbol = normalized;
        }

        logger.info("MARKET_CONTEXT [{}] proxy={} velShort={} shortAvg={} longAvg={} choppy={} weak={}",
                market, normalized, fmtPct(velocityShort), fmt(shortAvg), fmt(longAvg), choppy, weak);
    }

    public void record(String symbol,
                       double open,
                       double high,
                       double low,
                       double close,
                       double volume,
                       long timestamp) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || close <= 0.0) return;

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);
        // Use market timezone for bar timestamps (avoid system default drift)
        ZoneId zone = market == Market.KRX ? KST_ZONE : NY_ZONE;
        LocalDateTime ts = Instant.ofEpochMilli(timestamp)
                .atZone(zone)
                .toLocalDateTime();
        long nowMs = System.currentTimeMillis();

        synchronized (st) {
            st.minuteHistory.addBar(open, high, low, close, Math.max(0.0, volume), ts);

            logger.info("MINUTE_BAR_CREATED {} o={} h={} l={} c={} v={} ts={}",
                    normalized, fmt(open), fmt(high), fmt(low), fmt(close), fmt(volume), timestamp);

            BuySignal signal = buildBuySignal(st, market, close, Math.max(0.0, volume), ts);

            if (isEntryReady(signal, market)) {
                st.entrySignal = signal;
                st.entryReadyAtMs = nowMs;
                st.entryReadyUntilMs = nowMs + ENTRY_READY_TTL_MS;
                st.entryReadyClose = close;
                st.entryReadyPatternKey = signal.patternKey;
                st.lastEntryReadyBarTs = ts;

                logger.info("ENTRY_READY [{}] {} mode={} price={} pattern={} score={} count={} multiUp={} weak={} choppy={} TTL={}ms",
                        market,
                        normalized,
                        signal.entryMode,
                        fmt(st.entryReadyClose),
                        st.entryReadyPatternKey,
                        signal.signalScore,
                        signal.signalCount,
                        signal.multiUptrend,
                        signal.marketWeak,
                        signal.choppyMarket,
                        ENTRY_READY_TTL_MS);
                logger.info("ENTRY_READY_DETAIL [{}] {} mode={} vwap={} dist={} nearVwap={} tooFar={} retestReady={} retestRecovering={}",
                        market,
                        normalized,
                        signal.entryMode,
                        fmt(signal.vwap),
                        fmtPct(signal.vwapDistancePct),
                        signal.nearVwap,
                        signal.vwapTooFar,
                        signal.breakoutRetestReady,
                        signal.breakoutRetestRecovering);
            } else {
                logger.info(
                        "ENTRY_REJECT (minute) [{}] {} reason={} mode={} signals={} score={} uptrend={} price={} high={} low={} pbDepth={} volume={}/{} turnover={}/{} vel(short/mid/long)={}/{}/{}",
                        market,
                        normalized,
                        signal.rejectReason,
                        signal.entryMode,
                        signal.signalCount,
                        signal.signalScore,
                        signal.multiUptrend,
                        fmt(signal.price),
                        fmt(signal.recentHigh),
                        fmt(signal.recentLow),
                        fmtPct(signal.pullbackDepthFromHigh),
                        fmt(signal.volume),
                        fmt(signal.averageVolume),
                        fmt(signal.latestTurnover),
                        fmt(signal.averageTurnover),
                        fmtPct(signal.velocityShort),
                        fmtPct(signal.velocityMid),
                        fmtPct(signal.velocityLong)
                );

                clearEntryReadyState(st);
            }
        }
    }

    public boolean shouldBuy(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return false;

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);
        LocalDateTime now = nowByMarket(market);

        StockQuote quote;
        try {
            quote = marketDataService.fetchPrice(normalized, market == Market.KRX ? "KRX" : null);
        } catch (Exception e) {
            logger.warn("BUY WAIT {} reason=QUOTE_ERROR msg={}", normalized, e.getMessage());
            return false;
        }

        synchronized (st) {
            st.tickHistory.addTick(
                    quote.getPrice(),
                    Math.max(0.0, quote.getVolume()),
                    normalizeTimestamp(quote.getTimestamp(), market)
            );

            long nowMs = System.currentTimeMillis();
            if (!ensureEntryReady(st, market, normalized, nowMs)) {
                return false;
            }

            if (!canBuy(st, normalized, market, now, st.entrySignal)) {
                return false;
            }

            if (!passesTickEntryGate(st, normalized, market, quote.getPrice())) {
                return false;
            }

            st.lastBuySignalMs = nowMs;
            return true;
        }
    }

    public boolean shouldSell(String symbol, double buyPrice, int currentQuantity) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || buyPrice <= 0.0) return false;

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);

        StockQuote quote;
        try {
            quote = marketDataService.fetchPrice(normalized, market == Market.KRX ? "KRX" : null);
        } catch (Exception e) {
            logger.warn("SELL WAIT {} reason=QUOTE_ERROR msg={}", normalized, e.getMessage());
            return false;
        }

        synchronized (st) {
            st.tickHistory.addTick(
                    quote.getPrice(),
                    Math.max(0.0, quote.getVolume()),
                    normalizeTimestamp(quote.getTimestamp(), market)
            );

            SellDecision decision = evaluateSellDecision(st, market, normalized, quote.getPrice(), currentQuantity, buyPrice, System.currentTimeMillis());
            return decision.shouldSell;
        }
    }

    public Optional<OrderCommand> decide(String symbol,
                                         double currentPrice,
                                         double currentVolume1m,
                                         int currentQuantity,
                                         double avgPrice) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || currentPrice <= 0.0) {
            return Optional.empty();
        }

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);
        long nowMs = System.currentTimeMillis();
        LocalDateTime now = nowByMarket(market);

        synchronized (st) {
            st.tickHistory.addTick(currentPrice, Math.max(0.0, currentVolume1m), now);

            logger.debug("DECIDE [{}] {} price={} qty={} avgPrice={} vol1m={} buyPending={} sellPending={}",
                    market,
                    normalized,
                    fmt(currentPrice),
                    currentQuantity,
                    fmt(avgPrice),
                    fmt(currentVolume1m),
                    st.buyPending,
                    st.sellPending);

            if (currentQuantity <= 0) {
                resetEntryState(st);

                if (!ensureEntryReady(st, market, normalized, nowMs)) {
                    return Optional.empty();
                }

                BuySignal signal = st.entrySignal;
                if (signal == null) {
                    return Optional.empty();
                }

                logger.info("ENTRY_EVAL [{}] {} ready={} pending={} weak={} choppy={} cheap={} turnoverFail={} volumeSkip={} multiUp={} score={} count={} mode={}",
                        market,
                        normalized,
                        st.entrySignal != null,
                        st.buyPending,
                        signal.marketWeak,
                        signal.choppyMarket,
                        signal.cheapStockBlocked,
                        !signal.turnoverFilterPassed,
                        signal.lowVolumeSkip,
                        signal.multiUptrend,
                        signal.signalScore,
                        signal.signalCount,
                        signal.entryMode
                );

                if (!canBuy(st, normalized, market, now, signal)) {
                    logger.info("ENTRY_RESULT [{}] {} result=SKIP reason=CANNOT_BUY score={} count={} weak={} multiUp={}",
                            market,
                            normalized,
                            signal.signalScore,
                            signal.signalCount,
                            signal.marketWeak,
                            signal.multiUptrend
                    );
                    return Optional.empty();
                }

                if (!passesTickEntryGate(st, normalized, market, currentPrice)) {
                    logger.info("ENTRY_RESULT [{}] {} result=WAIT reason=TICK_GATE score={} count={} weak={} multiUp={}",
                            market,
                            normalized,
                            signal.signalScore,
                            signal.signalCount,
                            signal.marketWeak,
                            signal.multiUptrend
                    );
                    return Optional.empty();
                }

                double positionSize = determinePositionSize(signal, market);
                if (positionSize <= 0.0) {
                    logger.info("BUY WAIT [{}] {} reason=POSITION_SIZE_ZERO mode={} count={} score={}",
                            market, normalized, signal.entryMode, signal.signalCount, signal.signalScore);
                    return Optional.empty();
                }

                double orderPrice = roundToTickSize(currentPrice, market);
                int qty = resolveBuyQuantity(st, orderPrice, positionSize);
                if (qty < 1) {
                    double targetNotional = st.buyAmountPerOrder * positionSize;
                    int rawQty = orderPrice > 0 ? (int) Math.floor(targetNotional / orderPrice) : 0;
                    logger.info("BUY WAIT [{}] {} reason=BUY_AMOUNT_TOO_SMALL amount={} positionSize={}x targetNotional={} orderPrice={} calcQty={}",
                            market,
                            normalized,
                            fmt(st.buyAmountPerOrder),
                            fmt(positionSize),
                            fmt(targetNotional),
                            fmt(orderPrice),
                            rawQty);
                    return Optional.empty();
                }

                st.lastBuySignalMs = nowMs;
                updateEntryCounters(st, signal, now.toLocalDate());
                st.entryMode = signal.entryMode;
                String executedPatternKey = st.entryReadyPatternKey;
                clearEntryReadyState(st);
                st.buyPending = true;
                st.buyPendingSinceMs = nowMs;

                logger.info("ENTRY_EXECUTED [{}] {} mode={} buyFilled price={} qty={} pattern={} reason={}",
                        market,
                        normalized,
                        st.entryMode,
                        fmt(orderPrice),
                        qty,
                        executedPatternKey,
                        "conditions_met");
                logger.info("ENTRY_RESULT [{}] {} result=BUY reason=CONDITIONS_MET score={} count={} weak={} multiUp={}",
                        market,
                        normalized,
                        signal.signalScore,
                        signal.signalCount,
                        signal.marketWeak,
                        signal.multiUptrend
                );

                logger.info(
                        "BUY [{}] {} mode={} price={} qty={} size={} count={} score={} m={} p={} v={} strong={} multi={} trendScore={} vel={} v1={} v2={} v3={} high={} low={} pbDepth={} vol={}/{} turnover={}/{} absLiq={} reason={}",
                        market,
                        normalized,
                        signal.entryMode,
                        fmt(orderPrice),
                        qty,
                        fmt(positionSize),
                        signal.signalCount,
                        signal.signalScore,
                        signal.momentumBreakout,
                        signal.pullbackEntry,
                        signal.volumeBreakout,
                        signal.strongBreakout,
                        signal.multiUptrend,
                        signal.trendScore,
                        fmtPct(signal.velocity),
                        fmtPct(signal.velocityShort),
                        fmtPct(signal.velocityMid),
                        fmtPct(signal.velocityLong),
                        fmt(signal.recentHigh),
                        fmt(signal.recentLow),
                        fmtPct(signal.pullbackDepthFromHigh),
                        fmt(signal.volume),
                        fmt(signal.averageVolume),
                        fmt(signal.latestTurnover),
                        fmt(signal.averageTurnover),
                        signal.absoluteLiquidityPassed,
                        entryReason(signal)
                );

                return Optional.of(new OrderCommand(
                        normalized,
                        qty,
                        "BUY",
                        orderPrice,
                        false,
                        "ENTRY"
                ));
            }

            if (avgPrice <= 0.0) {
                logger.warn("SELL_SKIP [{}] {} reason=INVALID_AVG_PRICE price={} qty={} avgPrice={}",
                        market,
                        normalized,
                        fmt(currentPrice),
                        currentQuantity,
                        fmt(avgPrice));
                return Optional.empty();
            }

            refreshHoldingState(st, avgPrice, currentPrice, nowMs);

            if (st.sellPending) {
                long elapsed = nowMs - st.sellPendingSinceMs;
                if (elapsed < SELL_PENDING_TIMEOUT_MS) {
                    logger.info("SELL_WAIT [{}] {} reason=PENDING elapsedMs={}",
                            market, normalized, elapsed);
                    return Optional.empty();
                }
                st.sellPending = false;
                st.sellPendingSinceMs = 0L;
                logger.warn("SELL pending timeout cleared for {}", normalized);
            }

            if ((nowMs - st.lastSellSignalMs) < SELL_RETRY_COOLDOWN_MS) {
                logger.info("SELL_WAIT [{}] {} reason=RETRY_COOLDOWN remainingMs={}",
                        market, normalized, (SELL_RETRY_COOLDOWN_MS - (nowMs - st.lastSellSignalMs)));
                return Optional.empty();
            }

            SellDecision sellDecision = evaluateSellDecision(st, market, normalized, currentPrice, currentQuantity, avgPrice, nowMs);

            long holdMs = st.entryTimeMs > 0 ? (nowMs - st.entryTimeMs) : 0L;
            logger.info("SELL_CHECK [{}] {} mode={} decision={} reason={} pnl={} holdMs={} highSinceEntry={} price={}",
                    market,
                    normalized,
                    st.entryMode,
                    sellDecision.shouldSell,
                    sellDecision.reason,
                    fmtPct((currentPrice - avgPrice) / avgPrice),
                    holdMs,
                    fmt(st.highestSinceEntry),
                    fmt(currentPrice));

            if (!sellDecision.shouldSell) {
                return Optional.empty();
            }

            if (isProfitExitReason(sellDecision.reason)) {
                st.lastProfitExitTimeMs = nowMs;
            } else if (isTrailExitReason(sellDecision.reason)) {
                st.lastTrailExitTimeMs = nowMs;
            } else if (isStopLossExitReason(sellDecision.reason)) {
                st.lastStopLossExitTimeMs = nowMs;
            }

            boolean forceMarket = false;
            if (st.forceMarketOnNextSell) {
                if (nowMs <= st.forceMarketUntilMs) {
                    forceMarket = true;
                }
                st.forceMarketOnNextSell = false;
                st.forceMarketUntilMs = 0L;
                st.forceMarketReason = null;
            }

            boolean marketOrder = sellDecision.marketOrder;
            if (!marketOrder && forceMarket && isTakeProfitReason(sellDecision.reason)) {
                marketOrder = true;
                logger.warn("SELL fallback to MARKET for {} reason={} (retry after unfilled LIMIT)",
                        normalized, sellDecision.reason);
            }

            if ("TAKE_PROFIT_PARTIAL".equals(sellDecision.reason)) {
                st.partialTakeProfitDone = true;
            }

            st.lastSellSignalMs = nowMs;
            st.sellPending = true;
            st.sellPendingSinceMs = nowMs;

            double orderPrice = marketOrder ? 0.0 : roundToTickSize(currentPrice, market);
            double logPrice = marketOrder ? currentPrice : orderPrice;

            double tradePnlPct = (currentPrice - avgPrice) / avgPrice;
            java.time.LocalDate today = now.toLocalDate();
            dailyPnlAccumulator.merge(today, tradePnlPct, (a, b) -> a + b);
            logger.info("SELL_EXECUTED [{}] {} mode={} sellFilled price={} qty={} reason={} pnl={} dailyPnl={}",
                    market,
                    normalized,
                    st.entryMode,
                    fmt(logPrice),
                    sellDecision.quantity,
                    sellDecision.reason,
                    fmtPct(tradePnlPct),
                    fmtPct(dailyPnlAccumulator.getOrDefault(today, 0.0)));

            return Optional.of(new OrderCommand(
                    normalized,
                    sellDecision.quantity,
                    "SELL",
                    orderPrice,
                    marketOrder,
                    sellDecision.reason
            ));
        }
    }

    private BuySignal buildBuySignal(SymbolState st,
                                     Market market,
                                     double price,
                                     double volume,
                                     LocalDateTime now) {
        BuySignal signal = new BuySignal();
        signal.ticks = st.minuteHistory.size();
        signal.spanSeconds = historySpanSeconds(st.minuteHistory);
        signal.enoughHistory = st.minuteHistory.hasEnoughHistory(MIN_HISTORY_BARS, MIN_HISTORY_SPAN_SECONDS);

        signal.price = price;
        signal.volume = Math.max(0.0, volume);
        signal.averageVolume = st.minuteHistory.averageVolume(20);

        signal.velocity = st.minuteHistory.velocitySeconds(VELOCITY_WINDOW_MIN_SECONDS, VELOCITY_WINDOW_MAX_SECONDS);
        signal.velocityShort = st.minuteHistory.velocitySeconds(TREND_SHORT_MIN_SECONDS, TREND_SHORT_MAX_SECONDS);
        signal.velocityMid = st.minuteHistory.velocitySeconds(TREND_MID_MIN_SECONDS, TREND_MID_MAX_SECONDS);
        signal.velocityLong = st.minuteHistory.velocitySeconds(TREND_LONG_MIN_SECONDS, TREND_LONG_MAX_SECONDS);

        signal.shortUp = signal.velocityShort > 0.0;
        signal.midUp = signal.velocityMid > 0.0;
        signal.longUp = signal.velocityLong > 0.0;

        signal.trendScore = (signal.shortUp ? 1 : 0) + (signal.midUp ? 1 : 0) + (signal.longUp ? 1 : 0);
        signal.multiUptrend = signal.trendScore >= 2;

        signal.recentHigh = st.minuteHistory.highestHigh(BUY_RECENT_RANGE_BARS);
        signal.recentLow = st.minuteHistory.lowestLow(BUY_RECENT_RANGE_BARS);
        signal.latestTurnover = signal.price * signal.volume;
        signal.averageTurnover = st.minuteHistory.averageTurnover(20);

        signal.pullbackAvgShort = st.minuteHistory.averagePrice(3);
        signal.pullbackAvgLong = st.minuteHistory.averagePrice(6);

        signal.vwap = st.minuteHistory.sessionVwap();
        signal.prevVwap = st.minuteHistory.sessionVwapPrevious();
        signal.aboveVwap = signal.vwap > 0.0 && signal.price > signal.vwap;
        signal.vwapSlopeUp = signal.vwap > 0.0 && signal.prevVwap > 0.0 && signal.vwap >= signal.prevVwap;
        signal.vwapDistancePct = signal.vwap > 0.0 ? ((signal.price - signal.vwap) / signal.vwap) : 0.0;
        signal.nearVwap = signal.vwap > 0.0 && Math.abs(signal.vwapDistancePct) <= VWAP_NEAR_DISTANCE_PCT;
        signal.vwapTooFar = signal.vwap > 0.0 && signal.vwapDistancePct > VWAP_MAX_DISTANCE_PCT;

        signal.choppyMarket = Math.abs(signal.velocityShort) < 0.0005
                && Math.abs(signal.pullbackAvgShort - signal.pullbackAvgLong) < (signal.price * 0.001);
        signal.marketWeak = signal.velocityShort <= -0.001
                && signal.pullbackAvgShort < signal.pullbackAvgLong;

        if (!signal.enoughHistory) {
            signal.rejectReason = "NOT_ENOUGH_HISTORY";
            return signal;
        }

        signal.timeWindowBlocked = !passesTimeWindow(market, now);
        signal.marketFilterPassed = passesMarketFilter(market);
        signal.cheapStockBlocked = !passesCheapStockFilter(market, price);
        signal.turnoverFilterPassed = passesTurnoverFilter(signal, market);
        signal.absoluteLiquidityPassed = passesAbsoluteLiquidityFilter(signal, market);

        double lowVolumeSkipMult = lowVolumeSkipMult(market);
        signal.lowVolumeSkip = signal.averageVolume > 0.0
                && signal.volume < signal.averageVolume * lowVolumeSkipMult;

        // momentum
        signal.momentumNearHigh = signal.recentHigh > 0.0 && signal.price >= (signal.recentHigh * MOMENTUM_PRICE_NEAR_HIGH);
        signal.momentumVelocityOk = signal.velocityShort > 0.0 && signal.velocityMid >= 0.0;
        signal.momentumVolumeOk = signal.averageVolume > 0.0 && signal.volume >= (signal.averageVolume * MOMENTUM_VOLUME_MULT);
        signal.momentumBreakout = signal.momentumNearHigh && signal.momentumVelocityOk && signal.momentumVolumeOk;

        // pullback
        signal.pullbackDepthFromHigh = signal.recentHigh > 0.0
                ? ((signal.price - signal.recentHigh) / signal.recentHigh)
                : 0.0;

        double fromHighRatio = signal.recentHigh > 0.0 ? (signal.price / signal.recentHigh) : 0.0;
        signal.pullbackZone = fromHighRatio >= PULLBACK_LOWER_FROM_HIGH && fromHighRatio <= PULLBACK_UPPER_FROM_HIGH;

        signal.pullbackRecovering =
                signal.pullbackAvgShort > 0.0
                        && signal.pullbackAvgLong > 0.0
                        && signal.pullbackAvgShort >= signal.pullbackAvgLong
                        && signal.velocityShort > 0.0;

        signal.pullbackVelocityOk = signal.velocityShort > 0.0;
        signal.pullbackVolumeOk = signal.averageVolume > 0.0 && signal.volume >= (signal.averageVolume * PULLBACK_VOLUME_MULT);

        signal.pullbackEntry =
                signal.pullbackZone
                        && signal.pullbackRecovering
                        && signal.pullbackVelocityOk
                        && signal.pullbackVolumeOk
                        && signal.nearVwap;

        // volume breakout
        signal.volumeBreakNearHigh = signal.recentHigh > 0.0 && signal.price >= (signal.recentHigh * 0.9990);
        signal.volumeBreakVelocityOk = signal.velocityShort > 0.0 && signal.velocityMid >= 0.0;
        signal.volumeBreakVolumeOk = signal.averageVolume > 0.0 && signal.volume >= (signal.averageVolume * VOLUME_BREAKOUT_VOLUME_MULT);

        signal.strongBreakout =
                signal.volumeBreakNearHigh
                        && signal.volumeBreakVelocityOk
                        && signal.averageVolume > 0.0
                        && signal.volume >= (signal.averageVolume * STRONG_BREAKOUT_VOLUME_MULT);

        signal.volumeBreakout =
                signal.volumeBreakNearHigh
                        && signal.volumeBreakVelocityOk
                        && signal.volumeBreakVolumeOk;

        // breakout retest
        double breakoutRetestUpper = signal.recentHigh > 0.0 ? (signal.recentHigh * 1.0030) : 0.0;
        double breakoutRetestLower = signal.recentHigh > 0.0 ? (signal.recentHigh * 0.9980) : 0.0;
        signal.breakoutRetestReady =
                signal.recentHigh > 0.0
                        && signal.price >= breakoutRetestLower
                        && signal.price <= breakoutRetestUpper;

        signal.breakoutRetestRecovering =
                signal.breakoutRetestReady
                        && signal.aboveVwap
                        && signal.vwapSlopeUp
                        && signal.velocityShort > 0.0
                        && signal.pullbackAvgShort >= signal.pullbackAvgLong
                        && signal.price >= signal.pullbackAvgShort;

        // score
        signal.signalScore = 0;
        signal.signalCount = 0;
        signal.entryMode = EntryMode.NONE;

        if (signal.aboveVwap) {
            signal.signalScore += 20;
            signal.signalCount++;
        }
        if (signal.vwapSlopeUp) {
            signal.signalScore += 10;
        }
        if (signal.vwapTooFar) {
            signal.signalScore -= 20;
        }

        if (signal.multiUptrend) {
            signal.signalScore += 10;
            signal.signalCount++;
        }
        if (signal.trendScore == 3) {
            signal.signalScore += 5;
        }

        if (signal.pullbackZone) {
            signal.signalScore += 10;
        }
        if (signal.pullbackRecovering) {
            signal.signalScore += 10;
            signal.signalCount++;
        }
        if (signal.pullbackEntry) {
            signal.signalScore += 15;
            signal.signalCount++;
            signal.entryMode = EntryMode.PULLBACK;
        }

        if (signal.volumeBreakout) {
            signal.signalScore += 10;
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) {
                signal.entryMode = EntryMode.VOLUME_BREAKOUT;
            }
        }
        if (signal.strongBreakout) {
            signal.signalScore += 8;
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) {
                signal.entryMode = EntryMode.BREAKOUT;
            }
        }
        if (signal.breakoutRetestReady && signal.breakoutRetestRecovering) {
            signal.signalScore += 12;
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) {
                signal.entryMode = EntryMode.BREAKOUT;
            }
        }

        if (signal.averageVolume > 0.0 && signal.volume >= signal.averageVolume) {
            signal.signalScore += 5;
        }
        if (signal.absoluteLiquidityPassed) {
            signal.signalScore += 5;
        }

        if (signal.marketWeak) {
            signal.signalScore -= 15;
        }
        if (signal.choppyMarket) {
            signal.signalScore -= 8;
        }

        if (!signal.enoughHistory) {
            signal.rejectReason = "NOT_ENOUGH_HISTORY";
        } else if (signal.timeWindowBlocked) {
            signal.rejectReason = "TIME_WINDOW_BLOCKED";
        } else if (!signal.marketFilterPassed) {
            signal.rejectReason = "MARKET_FILTER_BLOCKED";
        } else if (signal.cheapStockBlocked) {
            signal.rejectReason = "CHEAP_STOCK_BLOCKED";
        } else if (!signal.turnoverFilterPassed) {
            signal.rejectReason = "TURNOVER_FILTER_BLOCKED";
        } else if (!signal.absoluteLiquidityPassed) {
            signal.rejectReason = "ABSOLUTE_LIQUIDITY_BLOCKED";
        } else if (signal.lowVolumeSkip) {
            signal.rejectReason = "LOW_VOLUME_SKIP";
        } else if (!signal.aboveVwap) {
            signal.rejectReason = "BELOW_VWAP";
        } else if (!signal.vwapSlopeUp) {
            signal.rejectReason = "VWAP_SLOPE_DOWN";
        } else if (signal.vwapTooFar) {
            signal.rejectReason = "VWAP_TOO_FAR";
        } else if (signal.pullbackEntry) {
            signal.rejectReason = "PULLBACK_SCORE_LOW";
        } else if (signal.volumeBreakout || signal.strongBreakout) {
            signal.rejectReason = "BREAKOUT_RETEST_NOT_READY";
        } else {
            signal.rejectReason = "NO_ENTRY_READY";
        }

        signal.patternKey = signal.entryMode.name() + "|VWAP=" + signal.aboveVwap + "|TREND=" + signal.trendScore;

        return signal;
    }

    private boolean isEntryReady(BuySignal signal, Market market) {
        if (!signal.enoughHistory) return false;
        if (signal.timeWindowBlocked) return false;
        if (!signal.marketFilterPassed) return false;
        if (signal.cheapStockBlocked) return false;
        if (!signal.turnoverFilterPassed) return false;
        if (!signal.absoluteLiquidityPassed) return false;
        if (signal.lowVolumeSkip) return false;

        MarketContext ctx = marketContext.get(market);
        if (ctx != null && !isMarketContextExpired(ctx)) {
            if (ctx.marketWeak) {
                signal.rejectReason = "MARKET_WEAK";
                return false;
            }
            if (ctx.choppyMarket && signal.pullbackEntry) {
                signal.rejectReason = "MARKET_CHOPPY_PULLBACK";
                return false;
            }
        }

        return signal.isBuyCandidate(market);
    }

    // Avoid NO_ENTRY_READY spam when record() lags behind tick calls
    private boolean ensureEntryReady(SymbolState st,
                                     Market market,
                                     String symbol,
                                     long nowMs) {
        if (st.entrySignal == null) {
            MinuteBarHistory.MinuteBar latest = st.minuteHistory.latest();
            if (latest != null
                    && st.lastHistoryRebuildBarTs != null
                    && st.lastHistoryRebuildBarTs.equals(latest.getTimestamp())) {
                logger.debug("BUY_WAIT [{}] {} reason=REBUILD_ALREADY barTs={}", market, symbol, latest.getTimestamp());
                return false;
            }
            if (!tryBuildEntryReadyFromHistory(st, market, symbol, nowMs)) {
                if (st.minuteHistory.hasEnoughHistory(MIN_HISTORY_BARS, MIN_HISTORY_SPAN_SECONDS)) {
                    logger.info("BUY_WAIT [{}] {} reason=ENTRY_NOT_BUILT", market, symbol);
                    logger.info("BUY_SKIP [{}] {} reason=NO_ENTRY_READY", market, symbol);
                }
                return false;
            }
        }

        if (st.entryReadyUntilMs > 0 && nowMs > st.entryReadyUntilMs) {
            logger.info("BUY_WAIT [{}] {} reason=ENTRY_EXPIRED", market, symbol);
            clearEntryReadyState(st);
            return false;
        }

        if (nowMs < st.entryReadyAtMs + ENTRY_READY_MIN_DELAY_MS) {
            logger.info("BUY_WAIT [{}] {} reason=MIN_DELAY", market, symbol);
            return false;
        }

        return true;
    }

    private boolean tryBuildEntryReadyFromHistory(SymbolState st,
                                                  Market market,
                                                  String symbol,
                                                  long nowMs) {
        MinuteBarHistory.MinuteBar latest = st.minuteHistory.latest();
        if (latest == null) {
            return false;
        }
        if (st.lastHistoryRebuildBarTs != null && st.lastHistoryRebuildBarTs.equals(latest.getTimestamp())) {
            return false;
        }
        st.lastHistoryRebuildBarTs = latest.getTimestamp();

        BuySignal signal = buildBuySignal(st, market, latest.getClose(), latest.getVolume(), latest.getTimestamp());
        if (!isEntryReady(signal, market)) {
            return false;
        }

        st.entrySignal = signal;
        st.entryReadyAtMs = nowMs;
        st.entryReadyUntilMs = nowMs + ENTRY_READY_TTL_MS;
        st.entryReadyClose = latest.getClose();
        st.entryReadyPatternKey = signal.patternKey;
        st.lastEntryReadyBarTs = latest.getTimestamp();

        logger.info("ENTRY_READY [{}] {} mode={} price={} pattern={} score={} TTL={}ms",
                market,
                symbol,
                signal.entryMode,
                fmt(st.entryReadyClose),
                st.entryReadyPatternKey,
                signal.signalScore,
                ENTRY_READY_TTL_MS);
        logger.info("ENTRY_READY_DETAIL [{}] {} mode={} vwap={} dist={} nearVwap={} tooFar={} retestReady={} retestRecovering={}",
                market,
                symbol,
                signal.entryMode,
                fmt(signal.vwap),
                fmtPct(signal.vwapDistancePct),
                signal.nearVwap,
                signal.vwapTooFar,
                signal.breakoutRetestReady,
                signal.breakoutRetestRecovering);

        return true;
    }

    private void clearEntryReadyState(SymbolState st) {
        st.entrySignal = null;
        st.entryReadyAtMs = 0L;
        st.entryReadyUntilMs = 0L;
        st.entryReadyClose = 0.0;
        st.entryReadyPatternKey = null;
        // lastEntryReadyBarTs is kept to avoid re-evaluating the same bar repeatedly
    }

    private boolean passesTickEntryGate(SymbolState st,
                                        String symbol,
                                        Market market,
                                        double currentPrice) {
        BuySignal signal = st.entrySignal;
        if (signal == null) {
            return false;
        }

        List<PriceHistory.Tick> recentTicks = st.tickHistory.latestTicks(4);
        if (recentTicks.size() < 3) {
            logger.info("BUY_WAIT [{}] {} reason=TICK_NOT_ENOUGH", market, symbol);
            return false;
        }

        double p1 = recentTicks.get(recentTicks.size() - 3).getPrice();
        double p2 = recentTicks.get(recentTicks.size() - 2).getPrice();
        double p3 = recentTicks.get(recentTicks.size() - 1).getPrice();

        boolean rising3Ticks = p1 <= p2 && p2 <= p3;
        if (!rising3Ticks) {
            logger.info("BUY_WAIT [{}] {} reason=TICK_NOT_RISING p1={} p2={} p3={}",
                    market, symbol, fmt(p1), fmt(p2), fmt(p3));
            return false;
        }

        if (signal.pullbackEntry) {
            if (signal.vwapDistancePct > 0.0035) {
                logger.info("BUY_WAIT [{}] {} reason=PULLBACK_TOO_FAR_FROM_VWAP dist={}",
                        market, symbol, fmtPct(signal.vwapDistancePct));
                return false;
            }
            return currentPrice > p2;
        }

        if (signal.volumeBreakout || signal.strongBreakout) {
            if (!signal.breakoutRetestReady || !signal.breakoutRetestRecovering) {
                logger.info("BUY_WAIT [{}] {} reason=BREAKOUT_RETEST_NOT_READY", market, symbol);
                return false;
            }
            boolean inRange = currentPrice >= signal.recentHigh * 0.9995
                    && currentPrice <= signal.recentHigh * 1.0015
                    && currentPrice > p2;
            if (!inRange) {
                logger.info("BUY_WAIT [{}] {} reason=BREAKOUT_GATE price={} recentHigh={} distFromHigh={} p2={} retestReady={} retestRecovering={}",
                        market,
                        symbol,
                        fmt(currentPrice),
                        fmt(signal.recentHigh),
                        signal.recentHigh > 0.0 ? fmtPct((currentPrice - signal.recentHigh) / signal.recentHigh) : "0.00%",
                        fmt(p2),
                        signal.breakoutRetestReady,
                        signal.breakoutRetestRecovering);
                return false;
            }
            return true;
        }

        return false;
    }

    private boolean canBuy(SymbolState st,
                           String symbol,
                           Market market,
                           LocalDateTime now,
                           BuySignal signal) {
        if (!signal.enoughHistory) {
            logger.info("BUY WAIT [{}] {} reason=HISTORY_SHORT bars={} spanSec={} reqBars={} reqSpanSec={}",
                    market, symbol, signal.ticks, signal.spanSeconds, MIN_HISTORY_BARS, MIN_HISTORY_SPAN_SECONDS);
            return false;
        }

        long nowMs = System.currentTimeMillis();
        resetDailyEntryIfNeeded(st, now.toLocalDate());

        // 일일 손실 한도 (-4%) 초과 시 신규 진입 전면 차단
        double todayPnl = dailyPnlAccumulator.getOrDefault(now.toLocalDate(), 0.0);
        if (todayPnl <= -MAX_DAILY_LOSS_PCT) {
            logger.warn("BUY_SKIP [{}] {} reason=DAILY_LOSS_LIMIT dailyPnl={}",
                    market, symbol, fmtPct(todayPnl));
            return false;
        }

        if (st.dailyEntryCount >= MAX_DAILY_ENTRY_COUNT) {
            long profitGap = st.lastProfitExitTimeMs > 0 ? (nowMs - st.lastProfitExitTimeMs) : -1L;
            long stopGap = st.lastStopLossExitTimeMs > 0 ? (nowMs - st.lastStopLossExitTimeMs) : -1L;
            logger.info("BUY_SKIP [{}] {} reason=REENTER_LIMIT profitGapMs={} stopGapMs={} dailyCount={}",
                    market, symbol, profitGap, stopGap, st.dailyEntryCount);
            return false;
        }

        if (st.lastTrailExitTimeMs > 0 && nowMs - st.lastTrailExitTimeMs < REENTER_TRAIL_COOLDOWN_MS) {
            long trailGap = nowMs - st.lastTrailExitTimeMs;
            long stopGap = st.lastStopLossExitTimeMs > 0 ? (nowMs - st.lastStopLossExitTimeMs) : -1L;
            logger.info("BUY_SKIP [{}] {} reason=REENTER_LIMIT trailGapMs={} stopGapMs={} dailyCount={}",
                    market, symbol, trailGap, stopGap, st.dailyEntryCount);
            return false;
        }

        if (st.lastProfitExitTimeMs > 0 && nowMs - st.lastProfitExitTimeMs < REENTER_PROFIT_COOLDOWN_MS) {
            long profitGap = nowMs - st.lastProfitExitTimeMs;
            long stopGap = st.lastStopLossExitTimeMs > 0 ? (nowMs - st.lastStopLossExitTimeMs) : -1L;
            logger.info("BUY_SKIP [{}] {} reason=REENTER_LIMIT profitGapMs={} stopGapMs={} dailyCount={}",
                    market, symbol, profitGap, stopGap, st.dailyEntryCount);
            return false;
        }

        if (st.lastStopLossExitTimeMs > 0 && nowMs - st.lastStopLossExitTimeMs < REENTER_STOPLOSS_COOLDOWN_MS) {
            long profitGap = st.lastProfitExitTimeMs > 0 ? (nowMs - st.lastProfitExitTimeMs) : -1L;
            long stopGap = nowMs - st.lastStopLossExitTimeMs;
            logger.info("BUY_SKIP [{}] {} reason=REENTER_LIMIT profitGapMs={} stopGapMs={} dailyCount={}",
                    market, symbol, profitGap, stopGap, st.dailyEntryCount);
            return false;
        }

        if (signal.patternKey != null
                && signal.patternKey.equals(st.lastEntryPatternKey)
                && st.samePatternEntryCount >= MAX_SAME_PATTERN_ENTRY_COUNT) {
            long profitGap = st.lastProfitExitTimeMs > 0 ? (nowMs - st.lastProfitExitTimeMs) : -1L;
            long stopGap = st.lastStopLossExitTimeMs > 0 ? (nowMs - st.lastStopLossExitTimeMs) : -1L;
            logger.info("BUY_SKIP [{}] {} reason=REENTER_LIMIT profitGapMs={} stopGapMs={} dailyCount={} pattern={}",
                    market, symbol, profitGap, stopGap, st.dailyEntryCount, signal.patternKey);
            return false;
        }

        if (st.buyPending) {
            long elapsed = nowMs - st.buyPendingSinceMs;
            if (elapsed < PENDING_TIMEOUT_MS) {
                logger.info("BUY WAIT [{}] {} reason=PENDING elapsedMs={}", market, symbol, elapsed);
                return false;
            }
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            logger.warn("BUY pending timeout cleared for {}", symbol);
        }

        long cooldownLeft = BUY_COOLDOWN_MS - (nowMs - st.lastBuySignalMs);
        if (cooldownLeft > 0) {
            logger.info("BUY WAIT [{}] {} reason=COOLDOWN remainingMs={}", market, symbol, cooldownLeft);
            return false;
        }

        // During KRX opening window, allow only stronger setups
        if (market == Market.KRX && isKrMarketCautiousWindow(now) && signal.entryMode != EntryMode.PULLBACK) {
            boolean cautiousPassed = (signal.signalCount >= 2 || signal.strongBreakout)
                    && signal.multiUptrend
                    && signal.velocity >= strongVelocityMin(market)
                    && signal.volume >= signal.averageVolume * STRONG_BREAKOUT_VOLUME_MULT;

            if (!cautiousPassed) {
                logger.info("BUY WAIT [{}] {} reason=OPENING_CAUTION mode={} count={} multi={} trendScore={} vel={} v1={} v2={} v3={} vol={}/{}",
                        market,
                        symbol,
                        signal.entryMode,
                        signal.signalCount,
                        signal.multiUptrend,
                        signal.trendScore,
                        fmtPct(signal.velocity),
                        fmtPct(signal.velocityShort),
                        fmtPct(signal.velocityMid),
                        fmtPct(signal.velocityLong),
                        fmt(signal.volume),
                        fmt(signal.averageVolume));
                return false;
            }
        }

        if (!signal.marketFilterPassed) {
            logger.info("BUY WAIT [{}] {} reason=MARKET_FILTER_FAIL", market, symbol);
            return false;
        }

        if (signal.timeWindowBlocked) {
            logger.info("BUY WAIT [{}] {} reason=TIME_WINDOW_BLOCKED", market, symbol);
            return false;
        }

        if (signal.cheapStockBlocked) {
            logger.info("BUY WAIT [{}] {} reason=CHEAP_STOCK_BLOCKED price={} min={}",
                    market, symbol, fmt(signal.price), fmt(MIN_KRX_PRICE));
            return false;
        }

        if (!signal.turnoverFilterPassed) {
            logger.info("BUY WAIT [{}] {} reason=TURNOVER_FILTER_FAIL turnover={}/{}",
                    market, symbol, fmt(signal.latestTurnover), fmt(signal.averageTurnover));
            return false;
        }

        if (!signal.absoluteLiquidityPassed) {
            logger.info("BUY WAIT [{}] {} reason=ABSOLUTE_LIQUIDITY_FAIL latestTurnover={} avgTurnover={}",
                    market, symbol, fmt(signal.latestTurnover), fmt(signal.averageTurnover));
            return false;
        }

        if (signal.lowVolumeSkip) {
            logger.info("BUY WAIT [{}] {} reason=LOW_VOLUME price={} vol={} avgVol={}",
                    market, symbol, fmt(signal.price), fmt(signal.volume), fmt(signal.averageVolume));
            return false;
        }

        if (!signal.isBuyCandidate(market)) {
            logger.info(
                    "BUY WAIT [{}] {} reason={} mode={} count={} score={} m={} p={} v={} strong={} multi={} trendScore={} vel={} v1={} v2={} v3={} price={} high={} low={} pbDepth={} vol={}/{} absLiq={} pbZone={} pbRec={} pbVelOk={} pbVolOk={} pbAvgS={} pbAvgL={} momNear={} momVelOk={} momVolOk={}",
                    market,
                    symbol,
                    signal.rejectReason,
                    signal.entryMode,
                    signal.signalCount,
                    signal.signalScore,
                    signal.momentumBreakout,
                    signal.pullbackEntry,
                    signal.volumeBreakout,
                    signal.strongBreakout,
                    signal.multiUptrend,
                    signal.trendScore,
                    fmtPct(signal.velocity),
                    fmtPct(signal.velocityShort),
                    fmtPct(signal.velocityMid),
                    fmtPct(signal.velocityLong),
                    fmt(signal.price),
                    fmt(signal.recentHigh),
                    fmt(signal.recentLow),
                    fmtPct(signal.pullbackDepthFromHigh),
                    fmt(signal.volume),
                    fmt(signal.averageVolume),
                    signal.absoluteLiquidityPassed,
                    signal.pullbackZone,
                    signal.pullbackRecovering,
                    signal.pullbackVelocityOk,
                    signal.pullbackVolumeOk,
                    fmt(signal.pullbackAvgShort),
                    fmt(signal.pullbackAvgLong),
                    signal.momentumNearHigh,
                    signal.momentumVelocityOk,
                    signal.momentumVolumeOk
            );
            return false;
        }

        return true;
    }

    private SellDecision evaluateSellDecision(SymbolState st,
                                              Market market,
                                              String symbol,
                                              double currentPrice,
                                              int currentQuantity,
                                              double avgPrice,
                                              long nowMs) {
        if (currentQuantity <= 0 || avgPrice <= 0.0 || currentPrice <= 0.0) {
            return SellDecision.none();
        }

        double pnlMult = currentPrice / avgPrice;
        double sessionVwap = st.minuteHistory.sessionVwap();

        // Emergency stop (항상 적용)
        if (pnlMult <= EMERGENCY_STOP_MULT) {
            return new SellDecision(true, currentQuantity, "EMERGENCY_STOP", true);
        }

        // 부분 익절 전: -1.5% 손절 / 부분 익절 후: +0.3% 손절 (수익 보호)
        if (st.partialTakeProfitDone) {
            if (pnlMult < BREAKEVEN_STOP_AFTER_PARTIAL_MULT) {
                logger.info("SELL [BREAKEVEN_STOP] {} pnl={} threshold={}",
                        symbol, pnlMult - 1.0, BREAKEVEN_STOP_AFTER_PARTIAL_MULT - 1.0);
                return new SellDecision(true, currentQuantity, "BREAKEVEN_STOP", true);
            }
        } else {
            if (pnlMult <= STOP_LOSS_MULT) {
                return new SellDecision(true, currentQuantity, "STOP_LOSS", true);
            }
        }

        // Partial take profit (+2%)
        if (!st.partialTakeProfitDone && pnlMult >= TAKE_PROFIT_PARTIAL_MULT) {
            int partialQty = Math.max(1, currentQuantity / 2);
            return new SellDecision(true, partialQty, "TAKE_PROFIT_PARTIAL", false);
        }

        // Dynamic trailing: trailMult determined by PEAK profit rate (not current)
        // This prevents the trailing from self-disabling as price drops
        double peakProfitRate = st.highestSinceEntry > 0.0
                ? (st.highestSinceEntry - avgPrice) / avgPrice
                : 0.0;
        double trailMult;
        if (peakProfitRate >= 0.050) {
            trailMult = 0.995;
        } else if (peakProfitRate >= 0.030) {
            trailMult = 0.993;
        } else if (peakProfitRate >= 0.015) {
            trailMult = 0.990;
        } else {
            trailMult = 0.0;
        }
        boolean trailingActive = st.highestSinceEntry > 0.0 && trailMult > 0.0;

        if (trailingActive && currentPrice <= st.highestSinceEntry * trailMult) {
            double currentProfitRate = (currentPrice - avgPrice) / avgPrice;
            logger.info("SELL [TRAIL_DYNAMIC] {} pnl={} peakPnl={} high={} current={} mult={}",
                    symbol, currentProfitRate, peakProfitRate, st.highestSinceEntry, currentPrice, trailMult);
            return new SellDecision(true, currentQuantity, "TRAIL_DYNAMIC", true);
        }

        // After partial TP, apply VWAP-loss based exit
        if (st.partialTakeProfitDone && sessionVwap > 0.0 && currentPrice < sessionVwap) {
            return new SellDecision(true, currentQuantity, "VWAP_LOST_AFTER_PARTIAL", true);
        }

        // VWAP_BREAK: only fire when trailing is NOT yet active (avoids conflict with trailing in profit zone)
        if (!trailingActive && sessionVwap > 0.0 && currentPrice < (sessionVwap * VWAP_BREAK_SELL_BUFFER)) {
            return new SellDecision(true, currentQuantity, "VWAP_BREAK", true);
        }

        long holdMs = st.entryTimeMs > 0 ? (nowMs - st.entryTimeMs) : 0L;
        if (st.entryTimeMs > 0 && holdMs >= MAX_HOLD_HARD_MS) {
            return new SellDecision(true, currentQuantity, "TIME_STOP_HARD", true);
        }
        if (st.entryTimeMs > 0 && holdMs >= MAX_HOLD_SOFT_MS && pnlMult < TIME_STOP_SOFT_PROFIT_MULT) {
            return new SellDecision(true, currentQuantity, "TIME_STOP_SOFT", true);
        }

        return SellDecision.none();
    }

    private boolean passesTimeWindow(Market market, LocalDateTime now) {
        if (market != Market.KRX) return true;
        return !isKrMarketBlockedTime(now);
    }

    private boolean passesCheapStockFilter(Market market, double price) {
        if (market != Market.KRX) return true;
        return price >= MIN_KRX_PRICE;
    }

    private boolean passesMarketFilter(Market market) {
        MarketContext ctx = marketContext.get(market);
        if (ctx == null) return true;
        if (isMarketContextExpired(ctx)) return true;
        return !ctx.marketWeak;
    }

    private boolean isMarketContextExpired(MarketContext ctx) {
        return ctx == null || (System.currentTimeMillis() - ctx.updatedAtMs > MARKET_CONTEXT_TTL_MS);
    }

    private boolean passesTurnoverFilter(BuySignal signal, Market market) {
        if (signal.averageTurnover <= 0.0) {
            return true;
        }
        return signal.latestTurnover >= signal.averageTurnover * 0.25;
    }

    private boolean passesAbsoluteLiquidityFilter(BuySignal signal, Market market) {
        if (market == Market.US) {
            return signal.latestTurnover >= MIN_US_LATEST_TURNOVER
                    && signal.averageTurnover >= MIN_US_AVG_TURNOVER;
        }
        return signal.latestTurnover >= MIN_KRX_LATEST_TURNOVER
                && signal.averageTurnover >= MIN_KRX_AVG_TURNOVER;
    }

    private boolean evaluateMomentumBreakout(BuySignal signal, Market market) {
        signal.momentumVelocityOk = signal.velocity >= momentumVelocityMin(market);
        signal.momentumNearHigh = signal.recentHigh > 0.0
                && signal.price >= signal.recentHigh * MOMENTUM_PRICE_NEAR_HIGH;
        signal.momentumVolumeOk = signal.averageVolume > 0.0
                && signal.volume >= signal.averageVolume * MOMENTUM_VOLUME_MULT;

        return signal.multiUptrend
                && signal.momentumVelocityOk
                && signal.momentumNearHigh
                && signal.momentumVolumeOk;
    }

    private boolean evaluatePullbackEntry(SymbolState st, BuySignal signal, Market market) {
        double pullbackUpper = pullbackUpperFromHigh(market);
        double pullbackLower = pullbackLowerFromHigh(market);

        signal.pullbackZone = signal.recentHigh > 0.0
                && signal.price <= signal.recentHigh * pullbackUpper
                && signal.price >= signal.recentHigh * pullbackLower;

        signal.pullbackAvgShort = st.minuteHistory.averagePrice(3);
        signal.pullbackAvgLong = st.minuteHistory.averagePrice(6);

        signal.pullbackRecovering = signal.pullbackAvgShort > 0.0
                && signal.pullbackAvgLong > 0.0
                && signal.pullbackAvgShort >= signal.pullbackAvgLong
                && signal.price >= signal.pullbackAvgShort * 0.999;

        signal.pullbackVelocityOk = signal.velocityShort >= 0.0005;

        signal.pullbackVolumeOk = signal.averageVolume > 0.0
                && signal.volume >= signal.averageVolume * pullbackVolumeMult(market);

        signal.pullbackDepthFromHigh = signal.recentHigh > 0.0
                ? ((signal.recentHigh - signal.price) / signal.recentHigh)
                : 0.0;

        boolean trendOk = signal.shortUp || signal.midUp || st.minuteHistory.isShortTermUptrend(5, 15);

        return signal.pullbackZone
                && signal.pullbackRecovering
                && signal.pullbackVelocityOk
                && signal.pullbackVolumeOk
                && trendOk;
    }

    private boolean evaluateVolumeBreakout(BuySignal signal, Market market) {
        signal.volumeBreakNearHigh = signal.recentHigh > 0.0
                && signal.price >= signal.recentHigh * 0.999;
        signal.volumeBreakVelocityOk = signal.velocityShort >= 0.001;
        signal.volumeBreakVolumeOk = signal.averageVolume > 0.0
                && signal.volume >= signal.averageVolume * VOLUME_BREAKOUT_VOLUME_MULT;

        return signal.multiUptrend
                && signal.volumeBreakNearHigh
                && signal.volumeBreakVolumeOk
                && signal.volumeBreakVelocityOk;
    }

    private boolean evaluateStrongBreakout(BuySignal signal, Market market) {
        return signal.multiUptrend
                && signal.velocity >= strongVelocityMin(market)
                && signal.averageVolume > 0.0
                && signal.volume >= signal.averageVolume * STRONG_BREAKOUT_VOLUME_MULT
                && signal.recentHigh > 0.0
                && signal.price >= signal.recentHigh * 0.9992;
    }

    private int countSignals(boolean momentumBreakout, boolean pullbackEntry, boolean volumeBreakout, boolean strongBreakout) {
        int count = 0;
        if (momentumBreakout) count++;
        if (pullbackEntry) count++;
        if (volumeBreakout || strongBreakout) count++;
        return count;
    }

    private int calculateSignalScore(BuySignal signal, Market market) {
        int score = 0;

        // Pullback weighted as primary signal
        if (signal.pullbackEntry) score += 55;
        if (signal.volumeBreakout) score += 16;
        if (signal.momentumBreakout) score += 6;
        if (signal.strongBreakout) score += 8;

        if (signal.turnoverFilterPassed) score += 8;
        if (signal.absoluteLiquidityPassed) score += 8;

        score += signal.trendScore * 6;

        if (signal.pullbackRecovering) score += 10;
        if (signal.pullbackVelocityOk) score += 8;

        if (signal.velocity >= strongVelocityMin(market)) score += 5;
        else if (signal.velocity >= momentumVelocityMin(market)) score += 2;

        if (signal.volume > 0.0 && signal.averageVolume > 0.0) {
            double volRatio = signal.volume / signal.averageVolume;
            if (volRatio >= 2.0) score += 7;
            else if (volRatio >= 1.0) score += 4;
        }

        // Bonus for moderate pullback depth range
        if (signal.pullbackDepthFromHigh >= 0.015 && signal.pullbackDepthFromHigh <= 0.040) {
            score += 6;
        }

        // Penalize momentum-only entries
        if (signal.momentumBreakout && !signal.pullbackEntry && !signal.volumeBreakout) {
            score -= 8;
        }

        return score;
    }

    private String deriveRejectReason(BuySignal signal, Market market) {
        if (signal.signalCount == 0) return "NO_ENTRY";

        if (signal.pullbackEntry) {
            if (signal.signalScore < 58) return "PULLBACK_SCORE_LOW";
            return "FILTERED";
        }

        if (signal.volumeBreakout || signal.strongBreakout) {
            if (signal.signalScore < 55) return "BREAKOUT_SCORE_LOW";
            if (signal.signalCount < 2) return "BREAKOUT_SIGNAL_COUNT_LOW";
            return "FILTERED";
        }

        return "MOMENTUM_ONLY_BLOCKED";
    }

    private EntryMode deriveEntryMode(BuySignal signal) {
        if (signal.pullbackEntry) return EntryMode.PULLBACK;
        if (signal.volumeBreakout) return EntryMode.VOLUME_BREAKOUT;
        if (signal.strongBreakout || signal.momentumBreakout) return EntryMode.BREAKOUT;
        return EntryMode.NONE;
    }

    private double determinePositionSize(BuySignal signal, Market market) {
        if (!signal.isBuyCandidate(market)) {
            return 0.0;
        }

        if (signal.entryMode == EntryMode.PULLBACK) {
            if (signal.volume > 0.0
                    && signal.averageVolume > 0.0
                    && signal.volume >= signal.averageVolume * VOLUME_SURGE_MULT_FOR_SIZE_UP) {
                return SIZE_UP_MULT;
            }
            return BASE_SIZE_PULLBACK;
        }

        if (signal.entryMode == EntryMode.VOLUME_BREAKOUT || signal.entryMode == EntryMode.BREAKOUT) {
            if (signal.strongBreakout
                    && signal.volume > 0.0
                    && signal.averageVolume > 0.0
                    && signal.volume >= signal.averageVolume * VOLUME_SURGE_MULT_FOR_SIZE_UP) {
                return 1.0;
            }
            return BASE_SIZE_BREAKOUT;
        }

        return 0.0;
    }

    private int resolveBuyQuantity(SymbolState st, double orderPrice, double positionSize) {
        if (orderPrice <= 0.0) return 0;
        if (st.buyAmountPerOrder <= 0.0) return 0;
        // positionSize = notional multiplier (e.g. 1.20x PULLBACK, 0.80x BREAKOUT, 1.50x SIZE_UP)
        double targetNotional = st.buyAmountPerOrder * positionSize;
        int qty = (int) Math.floor(targetNotional / orderPrice);
        // Require at least 2 shares for amount-based entries
        return qty >= 2 ? qty : 0;
    }

    private void refreshHoldingState(SymbolState st, double avgPrice, double currentPrice, long nowMs) {
        if (st.entryTimeMs == 0L) {
            st.entryTimeMs = nowMs;
        }
        if (st.entryPriceSnapshot <= 0.0) {
            st.entryPriceSnapshot = avgPrice;
        }
        if (currentPrice > st.highestSinceEntry) {
            st.highestSinceEntry = currentPrice;
        }
        if (avgPrice > 0.0) {
            st.lastKnownProfitRate = (currentPrice - avgPrice) / avgPrice;
        }
    }

    private void resetEntryState(SymbolState st) {
        st.entryTimeMs = 0L;
        st.highestSinceEntry = 0.0;
        st.partialTakeProfitDone = false;
        st.entryPriceSnapshot = 0.0;
        st.lastKnownProfitRate = 0.0;
        st.entryMode = EntryMode.NONE;
    }

    private void resetDailyEntryIfNeeded(SymbolState st, java.time.LocalDate today) {
        if (today == null) return;
        if (st.lastEntryDay == null || !st.lastEntryDay.equals(today)) {
            st.lastEntryDay = today;
            st.dailyEntryCount = 0;
            st.lastEntryPatternKey = null;
            st.samePatternEntryCount = 0;
        }
    }

    private void updateEntryCounters(SymbolState st, BuySignal signal, java.time.LocalDate entryDay) {
        resetDailyEntryIfNeeded(st, entryDay);
        st.dailyEntryCount++;

        if (signal == null || signal.patternKey == null) {
            st.lastEntryPatternKey = null;
            st.samePatternEntryCount = 0;
            return;
        }

        if (signal.patternKey.equals(st.lastEntryPatternKey)) {
            st.samePatternEntryCount++;
        } else {
            st.lastEntryPatternKey = signal.patternKey;
            st.samePatternEntryCount = 1;
        }
    }

    private SymbolState state(String symbol) {
        return states.computeIfAbsent(symbol, key -> new SymbolState());
    }

    private Market detectMarket(String symbol, SymbolState st) {
        if (st.market != null) {
            return st.market;
        }
        return symbol.matches("^\\d{5,6}$") ? Market.KRX : Market.US;
    }

    private LocalDateTime normalizeTimestamp(LocalDateTime ts, Market market) {
        return ts != null ? ts : nowByMarket(market);
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
    }

    private LocalDateTime nowByMarket(Market market) {
        if (market == Market.KRX) {
            return LocalDateTime.now(KST_ZONE);
        }
        return LocalDateTime.now();
    }

    private long historySpanSeconds(MinuteBarHistory history) {
        return history.spanSeconds();
    }

    private boolean isKrMarketBlockedTime(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        // 9:15 이전: VWAP 미수렴 구간 차단
        // 14:50 이후: 30분 타임스탑 감안 신규 진입 차단
        return time.isBefore(LocalTime.of(9, 15)) || time.isAfter(LocalTime.of(14, 49));
    }

    private boolean isKrMarketCautiousWindow(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return !time.isBefore(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(9, 5));
    }

    private double lowVolumeSkipMult(Market market) {
        return LOW_VOLUME_SKIP_MULT;
    }

    private double momentumVelocityMin(Market market) {
        return market == Market.US ? 0.009 : 0.006;
    }

    private double strongVelocityMin(Market market) {
        return market == Market.US ? 0.014 : 0.010;
    }

    private double pullbackUpperFromHigh(Market market) {
        return PULLBACK_UPPER_FROM_HIGH;
    }

    private double pullbackLowerFromHigh(Market market) {
        return PULLBACK_LOWER_FROM_HIGH;
    }

    private double pullbackVolumeMult(Market market) {
        return PULLBACK_VOLUME_MULT;
    }

    private double multiTrendShortMin(Market market) {
        return market == Market.US ? 0.0015 : 0.0010;
    }

    private double multiTrendMidMin(Market market) {
        return market == Market.US ? 0.0025 : 0.0015;
    }

    private double multiTrendLongMin(Market market) {
        return market == Market.US ? 0.0035 : 0.0020;
    }

    private double roundToTickSize(double price, Market market) {
        if (market == Market.US) {
            if (price >= 1.0) {
                return Math.round(price * 100.0) / 100.0;
            }
            return Math.round(price * 10000.0) / 10000.0;
        }

        if (price < 1_000) return Math.round(price);
        if (price < 5_000) return Math.round(price / 5.0) * 5.0;
        if (price < 10_000) return Math.round(price / 10.0) * 10.0;
        if (price < 50_000) return Math.round(price / 50.0) * 50.0;
        if (price < 100_000) return Math.round(price / 100.0) * 100.0;
        if (price < 500_000) return Math.round(price / 500.0) * 500.0;
        return Math.round(price / 1000.0) * 1000.0;
    }

    private boolean isTakeProfitReason(String reason) {
        if (reason == null) return false;
        return "TAKE_PROFIT_PARTIAL".equals(reason)
                || "VWAP_LOST_AFTER_PARTIAL".equals(reason)
                || "BREAKEVEN_STOP".equals(reason);
    }

    private boolean isProfitExitReason(String reason) {
        if (reason == null) return false;
        // TRAIL_DYNAMIC: 별도 2분 쿨다운 처리 (isTrailExitReason)
        // BREAKEVEN_STOP: 부분익절 후 +0.3% 청산 → 수익 실현으로 분류
        return "TAKE_PROFIT_PARTIAL".equals(reason)
                || "VWAP_LOST_AFTER_PARTIAL".equals(reason)
                || "BREAKEVEN_STOP".equals(reason);
    }

    private boolean isTrailExitReason(String reason) {
        return "TRAIL_DYNAMIC".equals(reason);
    }

    private boolean isStopLossExitReason(String reason) {
        if (reason == null) return false;
        return "STOP_LOSS".equals(reason)
                || "EMERGENCY_STOP".equals(reason)
                || "TIME_STOP_SOFT".equals(reason)
                || "TIME_STOP_HARD".equals(reason);
    }

    private String entryReason(BuySignal signal) {
        if (signal.entryMode == EntryMode.PULLBACK) {
            return "PULLBACK_REBOUND";
        }
        if (signal.entryMode == EntryMode.VOLUME_BREAKOUT) {
            return "VOLUME_BREAKOUT";
        }
        if (signal.entryMode == EntryMode.BREAKOUT) {
            return "BREAKOUT";
        }
        return "ENTRY";
    }

    private void logEntryWait(Market market,
                              String symbol,
                              String reason,
                              double currentPrice,
                              double basePrice,
                              double velocityShort,
                              double volumeDelta) {
        logger.info("ENTRY_WAIT (tick) [{}] {} reason={} price={} readyClose={} velShort={} volDelta={}",
                market,
                symbol,
                reason,
                fmt(currentPrice),
                fmt(basePrice),
                fmtPct(velocityShort),
                fmt(volumeDelta));
    }

    private String buildPatternKey(Market market, BuySignal signal, LocalDateTime barTime) {
        LocalDateTime ts = barTime != null ? barTime : LocalDateTime.now();
        String timeKey = ts.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        StringBuilder sb = new StringBuilder();
        sb.append(timeKey).append("|");
        if (signal.pullbackEntry) sb.append("P");
        if (signal.momentumBreakout) sb.append("M");
        if (signal.volumeBreakout) sb.append("V");
        if (signal.strongBreakout) sb.append("S");
        sb.append("|").append(signal.entryMode);
        sb.append("|").append(market);
        return sb.toString();
    }

    private String fmt(double value) {
        return String.format("%.4f", value);
    }

    private String fmtPct(double value) {
        return String.format("%.2f%%", value * 100.0);
    }
}
