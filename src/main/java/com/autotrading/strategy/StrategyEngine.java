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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StrategyEngine {

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngine.class);

    public enum Market { KRX, US }

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private static final int HISTORY_CAPACITY = 150;           // 🔴 fix1: 50→150 (tick 드랍 대비 여유)
    private static final int MIN_HISTORY_TICKS = 36;
    private static final int MIN_HISTORY_SPAN_SECONDS = 180;

    private static final int VELOCITY_WINDOW_MIN_SECONDS = 30;
    private static final int VELOCITY_WINDOW_MAX_SECONDS = 90;

    private static final int TREND_SHORT_MIN_SECONDS = 10;
    private static final int TREND_SHORT_MAX_SECONDS = 20;
    private static final int TREND_MID_MIN_SECONDS = 20;
    private static final int TREND_MID_MAX_SECONDS = 40;
    private static final int TREND_LONG_MIN_SECONDS = 40;
    private static final int TREND_LONG_MAX_SECONDS = 80;

    private static final double MOMENTUM_PRICE_NEAR_HIGH = 0.992;
    private static final double MOMENTUM_VOLUME_MULT = 1.3;
    private static final double STRONG_BREAKOUT_VOLUME_MULT = 1.5;
    private static final double PULLBACK_UPPER_FROM_HIGH = 0.995;
    private static final double PULLBACK_LOWER_FROM_HIGH = 0.98;
    private static final double PULLBACK_VOLUME_MULT = 0.8;
    private static final double VOLUME_BREAKOUT_VOLUME_MULT = 1.5;
    private static final double VOLUME_SURGE_MULT_FOR_SIZE_UP = 2.0;
    private static final double LOW_VOLUME_SKIP_MULT_KRX = 0.3;
    private static final double LOW_VOLUME_SKIP_MULT_US = 0.10;

    private static final double MIN_KRX_PRICE = 1000.0;

    private static final double MIN_KRX_LATEST_TURNOVER = 30_000_000.0;
    private static final double MIN_KRX_AVG_TURNOVER = 20_000_000.0;

    private static final double MIN_US_LATEST_TURNOVER = 10_000.0;
    private static final double MIN_US_AVG_TURNOVER = 6_000.0;

    private static final double STOP_LOSS_MULT_KRX = 0.978;
    private static final double STOP_LOSS_MULT_US  = 0.975;

    private static final double EMERGENCY_STOP_MULT_KRX = 0.945;
    private static final double EMERGENCY_STOP_MULT_US  = 0.945;

    private static final double TAKE_PROFIT_PARTIAL_MULT = 1.020;
    private static final double TAKE_PROFIT_FINAL_MULT   = 1.035;
    private static final long   SELL_MARKET_FALLBACK_TTL_MS = 60_000L;

    private static final double BASE_SIZE = 1.0;
    private static final double SIZE_UP_MULT = 1.5;

    private static final long BUY_COOLDOWN_MS = 60_000L;
    private static final long PENDING_TIMEOUT_MS = 30_000L;
    private static final long SELL_RETRY_COOLDOWN_MS = 5_000L;
    private static final long SELL_PENDING_TIMEOUT_MS = 15_000L;

    private static final long MAX_HOLD_WITHOUT_PROFIT_MS_KRX = 360_000L;
    private static final long MAX_HOLD_WITHOUT_PROFIT_MS_US  = 480_000L;
    private static final double TIME_STOP_MIN_PROFIT_MULT = 1.002;
    private static final double TIME_STOP_NEG_VELOCITY_KRX = -0.0005;
    private static final double TIME_STOP_NEG_VELOCITY_US  = -0.0005;

    private static final int FIXED_BUY_QTY = 1;

    private final MarketDataService marketDataService;
    private final Map<String, SymbolState> states = new ConcurrentHashMap<>();

    private static class SymbolState {
        final PriceHistory history = new PriceHistory(HISTORY_CAPACITY);

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

        boolean forceMarketOnNextSell;
        long forceMarketUntilMs;
        String forceMarketReason;
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

        boolean volumeBreakNearHigh;
        boolean volumeBreakVelocityOk;
        boolean volumeBreakVolumeOk;

        int signalScore;
        int signalCount;

        boolean timeWindowBlocked;
        boolean marketFilterPassed;
        boolean cheapStockBlocked;
        boolean turnoverFilterPassed;
        boolean absoluteLiquidityPassed;

        String rejectReason;

        boolean isBuyCandidate(Market market) {
            if (!enoughHistory) return false;
            if (timeWindowBlocked) return false;
            if (!marketFilterPassed) return false;
            if (cheapStockBlocked) return false;
            if (!turnoverFilterPassed) return false;
            if (!absoluteLiquidityPassed) return false;

            if (signalCount >= 2) {
                return true;
            }

            if (pullbackEntry && multiUptrend && velocityShort >= 0.0) {
                return market == Market.US ? signalScore >= 45 : signalScore >= 50;
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
            logger.info("BUY amount cleared for {} (fixed qty mode)", normalized);
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
        LocalDateTime ts = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        synchronized (st) {
            st.history.addTick(close, Math.max(0.0, volume), ts);
        }
    }

    // =========================================================================
    // 🔴 fix1: shouldBuy - fetchPrice를 synchronized 블록 밖으로 이동
    //    (네트워크 I/O를 lock 안에서 호출하면 응답 지연 시 tick 루프 전체가 블로킹됨)
    // =========================================================================
    public boolean shouldBuy(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return false;

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);
        LocalDateTime now = nowByMarket(market);

        // 🔴 fix1: lock 밖에서 네트워크 호출
        StockQuote quote;
        try {
            quote = marketDataService.fetchPrice(normalized, market == Market.KRX ? "KRX" : null);
        } catch (Exception e) {
            logger.warn("BUY WAIT {} reason=QUOTE_ERROR msg={}", normalized, e.getMessage());
            return false;
        }

        // lock 안에서는 순수 상태 계산만 수행
        synchronized (st) {
            st.history.addTick(
                    quote.getPrice(),
                    Math.max(0.0, quote.getVolume()),
                    normalizeTimestamp(quote.getTimestamp(), market)
            );

            BuySignal signal = buildBuySignal(st, market, quote.getPrice(), quote.getVolume(), now);
            if (!canBuy(st, normalized, market, now, signal)) {
                return false;
            }

            st.lastBuySignalMs = System.currentTimeMillis();
            return true;
        }
    }

    // =========================================================================
    // 🔴 fix1: shouldSell - fetchPrice를 synchronized 블록 밖으로 이동
    // =========================================================================
    public boolean shouldSell(String symbol, double buyPrice) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || buyPrice <= 0.0) return false;

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);

        // 🔴 fix1: lock 밖에서 네트워크 호출
        StockQuote quote;
        try {
            quote = marketDataService.fetchPrice(normalized, market == Market.KRX ? "KRX" : null);
        } catch (Exception e) {
            logger.warn("SELL WAIT {} reason=QUOTE_ERROR msg={}", normalized, e.getMessage());
            return false;
        }

        synchronized (st) {
            st.history.addTick(
                    quote.getPrice(),
                    Math.max(0.0, quote.getVolume()),
                    normalizeTimestamp(quote.getTimestamp(), market)
            );

            SellDecision decision = evaluateSellDecision(st, market, quote.getPrice(), 1, buyPrice, System.currentTimeMillis());
            return decision.shouldSell;
        }
    }

    // =========================================================================
    // 🔴 fix1: decide - 호출자(SchedulerService.execute)에서 이미 lock 밖에서
    //    fetchPrice를 완료한 뒤 price/volume을 인자로 넘겨주므로 여기는 순수 계산만.
    //    (기존 코드도 인자 방식이라 구조는 유지, addTick도 lock 안에서 처리)
    // =========================================================================
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
            st.history.addTick(currentPrice, Math.max(0.0, currentVolume1m), now);

            logger.info("DECIDE [{}] {} price={} qty={} avgPrice={} vol1m={} buyPending={} sellPending={}",
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

                BuySignal signal = buildBuySignal(st, market, currentPrice, currentVolume1m, now);
                if (!canBuy(st, normalized, market, now, signal)) {
                    return Optional.empty();
                }

                double positionSize = determinePositionSize(signal, market);
                if (positionSize <= 0.0) {
                    logger.info("BUY WAIT [{}] {} reason=POSITION_SIZE_ZERO count={} score={}",
                            market, normalized, signal.signalCount, signal.signalScore);
                    return Optional.empty();
                }

                double orderPrice = roundToTickSize(currentPrice, market);
                int qty = resolveBuyQuantity(st, orderPrice, positionSize);
                if (qty < 1) {
                    logger.info("BUY WAIT [{}] {} reason=BUY_AMOUNT_TOO_SMALL amount={} order={} size={}",
                            market,
                            normalized,
                            fmt(st.buyAmountPerOrder),
                            fmt(orderPrice),
                            fmt(positionSize));
                    return Optional.empty();
                }

                st.lastBuySignalMs = nowMs;
                logger.info(
                        "BUY [{}] {} price={} qty={} size={} count={} score={} m={} p={} v={} strong={} multi={} trendScore={} vel={} v1={} v2={} v3={} high={} low={} vol={}/{} turnover={}/{} absLiq={} reason={}",
                        market,
                        normalized,
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

            SellDecision sellDecision = evaluateSellDecision(st, market, currentPrice, currentQuantity, avgPrice, nowMs);

            logger.info("SELL_CHECK [{}] {} price={} avgPrice={} qty={} decision={} reason={} marketOrder={} pnl={} partialDone={} highSinceEntry={}",
                    market,
                    normalized,
                    fmt(currentPrice),
                    fmt(avgPrice),
                    currentQuantity,
                    sellDecision.shouldSell,
                    sellDecision.reason,
                    sellDecision.marketOrder,
                    fmtPct((currentPrice - avgPrice) / avgPrice),
                    st.partialTakeProfitDone,
                    fmt(st.highestSinceEntry));

            if (!sellDecision.shouldSell) {
                return Optional.empty();
            }

            boolean forceMarket = false;
            synchronized (st) {
                if (st.forceMarketOnNextSell) {
                    if (nowMs <= st.forceMarketUntilMs) {
                        forceMarket = true;
                    }
                    st.forceMarketOnNextSell = false;
                    st.forceMarketUntilMs = 0L;
                    st.forceMarketReason = null;
                }
            }

            boolean marketOrder = sellDecision.marketOrder;
            if (!marketOrder && forceMarket && isTakeProfitReason(sellDecision.reason)) {
                marketOrder = true;
                logger.warn("SELL fallback to MARKET for {} reason={} (retry after unfilled LIMIT)",
                        normalized, sellDecision.reason);
            }

            if ("TAKE_PROFIT_PARTIAL".equals(sellDecision.reason) || "TAKE_PROFIT_PARTIAL_FULL".equals(sellDecision.reason)) {
                st.partialTakeProfitDone = true;
            }

            st.lastSellSignalMs = nowMs;
            st.sellPending = true;
            st.sellPendingSinceMs = nowMs;

            double orderPrice = marketOrder
                    ? 0.0
                    : roundToTickSize(currentPrice, market);

            logger.info("SELL {} [{}] {} mode={} price={} buyPrice={} qty={} pnl={} partialDone={} highSinceEntry={}",
                    sellDecision.reason,
                    market,
                    normalized,
                    marketOrder ? "MARKET" : "LIMIT",
                    marketOrder ? "MKT" : fmt(orderPrice),
                    fmt(avgPrice),
                    sellDecision.quantity,
                    fmtPct((currentPrice - avgPrice) / avgPrice),
                    st.partialTakeProfitDone,
                    fmt(st.highestSinceEntry));

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
        signal.ticks = st.history.size();
        signal.spanSeconds = historySpanSeconds(st.history);
        signal.enoughHistory = st.history.hasEnoughHistory(MIN_HISTORY_TICKS, MIN_HISTORY_SPAN_SECONDS);
        signal.price = price;
        signal.volume = Math.max(0.0, volume);
        signal.averageVolume = st.history.averageVolume();

        signal.velocityShort = st.history.velocitySeconds(TREND_SHORT_MIN_SECONDS, TREND_SHORT_MAX_SECONDS);
        signal.velocityMid = st.history.velocitySeconds(TREND_MID_MIN_SECONDS, TREND_MID_MAX_SECONDS);
        signal.velocityLong = st.history.velocitySeconds(TREND_LONG_MIN_SECONDS, TREND_LONG_MAX_SECONDS);
        signal.velocity = st.history.velocitySeconds(VELOCITY_WINDOW_MIN_SECONDS, VELOCITY_WINDOW_MAX_SECONDS);

        signal.shortUp = signal.velocityShort >= multiTrendShortMin(market);
        signal.midUp = signal.velocityMid >= multiTrendMidMin(market);
        signal.longUp = signal.velocityLong >= multiTrendLongMin(market);

        signal.trendScore = 0;
        if (signal.shortUp) signal.trendScore++;
        if (signal.midUp) signal.trendScore++;
        if (signal.longUp) signal.trendScore++;

        signal.multiUptrend = (market == Market.US)
                ? (signal.trendScore >= 2 && (signal.shortUp || signal.midUp))
                : (signal.trendScore >= 2 && (signal.shortUp || signal.midUp));

        signal.recentHigh = st.history.recentHigh();
        signal.recentLow = st.history.recentLow();
        signal.latestTurnover = st.history.latestTurnover();
        signal.averageTurnover = st.history.averageTurnover(20);

        if (!signal.enoughHistory) {
            signal.rejectReason = "HISTORY_SHORT";
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

        signal.momentumBreakout = evaluateMomentumBreakout(signal, market);
        signal.pullbackEntry = evaluatePullbackEntry(st, signal, market);
        signal.volumeBreakout = evaluateVolumeBreakout(signal, market);
        signal.strongBreakout = evaluateStrongBreakout(signal, market);

        signal.signalCount = countSignals(signal.momentumBreakout, signal.pullbackEntry, signal.volumeBreakout);
        signal.signalScore = calculateSignalScore(signal, market);
        signal.rejectReason = deriveRejectReason(signal, market);

        return signal;
    }

    private boolean canBuy(SymbolState st,
                           String symbol,
                           Market market,
                           LocalDateTime now,
                           BuySignal signal) {
        if (!signal.enoughHistory) {
            logger.info("BUY WAIT [{}] {} reason=HISTORY_SHORT ticks={} spanSec={} reqTicks={} reqSpanSec={}",
                    market, symbol, signal.ticks, signal.spanSeconds, MIN_HISTORY_TICKS, MIN_HISTORY_SPAN_SECONDS);
            return false;
        }

        long nowMs = System.currentTimeMillis();
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

        if (market == Market.KRX && isKrMarketCautiousWindow(now)) {
            boolean cautiousPassed = (signal.signalCount >= 2 || signal.strongBreakout)
                    && signal.multiUptrend
                    && signal.velocity >= strongVelocityMin(market)
                    && signal.volume >= signal.averageVolume * STRONG_BREAKOUT_VOLUME_MULT;

            if (!cautiousPassed) {
                logger.info("BUY WAIT [{}] {} reason=OPENING_CAUTION count={} multi={} trendScore={} vel={} v1={} v2={} v3={} vol={}/{}",
                        market,
                        symbol,
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
                    "BUY WAIT [{}] {} reason={} count={} score={} m={} p={} v={} strong={} multi={} trendScore={} vel={} v1={} v2={} v3={} price={} high={} low={} vol={}/{} absLiq={} pbZone={} pbRec={} pbVelOk={} pbVolOk={} pbAvgS={} pbAvgL={} momNear={} momVelOk={} momVolOk={}",
                    market,
                    symbol,
                    signal.rejectReason,
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
                                              double currentPrice,
                                              int currentQuantity,
                                              double avgPrice,
                                              long nowMs) {
        if (currentQuantity <= 0 || avgPrice <= 0.0 || currentPrice <= 0.0) {
            return SellDecision.none();
        }

        double stopLossMult = stopLossMult(market);
        double emergencyStopMult = emergencyStopMult(market);

        if (currentPrice <= avgPrice * emergencyStopMult) {
            return new SellDecision(true, currentQuantity, "EMERGENCY_STOP", true);
        }

        if (currentPrice <= avgPrice * stopLossMult) {
            return new SellDecision(true, currentQuantity, "STOP_LOSS", true);
        }

        if (!st.partialTakeProfitDone && currentPrice >= avgPrice * TAKE_PROFIT_PARTIAL_MULT) {
            int partialQty = Math.max(1, currentQuantity / 2);
            if (partialQty >= currentQuantity) {
                return new SellDecision(true, currentQuantity, "TAKE_PROFIT_PARTIAL_FULL", true);
            }
            return new SellDecision(true, partialQty, "TAKE_PROFIT_PARTIAL", true);
        }

        if (currentPrice >= avgPrice * TAKE_PROFIT_FINAL_MULT) {
            return new SellDecision(true, currentQuantity, "TAKE_PROFIT_FINAL", true);
        }

        long holdMs = nowMs - st.entryTimeMs;
        long maxHoldMs = maxHoldWithoutProfitMs(market);
        double timeStopNegVelocity = timeStopNegVelocity(market);
        double latestVelocity = st.history.velocitySeconds(10, 30);

        if (st.entryTimeMs > 0
                && holdMs >= maxHoldMs
                && currentPrice < avgPrice * TIME_STOP_MIN_PROFIT_MULT
                && latestVelocity <= timeStopNegVelocity) {
            return new SellDecision(true, currentQuantity, "TIME_STOP", true);
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
        return true;
    }

    private boolean passesTurnoverFilter(BuySignal signal, Market market) {
        if (signal.averageTurnover <= 0.0) {
            return true;
        }
        if (market == Market.US) {
            return signal.latestTurnover >= signal.averageTurnover * 0.25;
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
        signal.pullbackZone = signal.recentHigh > 0.0
                && signal.price <= signal.recentHigh * PULLBACK_UPPER_FROM_HIGH
                && signal.price >= signal.recentHigh * PULLBACK_LOWER_FROM_HIGH;

        signal.pullbackAvgShort = st.history.averagePrice(3);
        signal.pullbackAvgLong = st.history.averagePrice(6);
        signal.pullbackRecovering = signal.pullbackAvgShort > 0.0
                && signal.pullbackAvgLong > 0.0
                && signal.pullbackAvgShort >= signal.pullbackAvgLong
                && signal.price >= signal.pullbackAvgShort;

        signal.pullbackVelocityOk = signal.velocityShort >= 0.0;
        signal.pullbackVolumeOk = signal.averageVolume > 0.0
                && signal.volume >= signal.averageVolume * PULLBACK_VOLUME_MULT;

        boolean trendOk = (signal.shortUp || signal.midUp || st.history.isShortTermUptrend(5, 15));

        return signal.pullbackZone
                && signal.pullbackRecovering
                && signal.pullbackVelocityOk
                && signal.pullbackVolumeOk
                && trendOk;
    }

    private boolean evaluateVolumeBreakout(BuySignal signal, Market market) {
        signal.volumeBreakNearHigh = signal.recentHigh > 0.0
                && signal.price >= signal.recentHigh * MOMENTUM_PRICE_NEAR_HIGH;
        signal.volumeBreakVelocityOk = signal.velocity >= volumeBreakVelocityMin(market);
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
                && signal.price >= signal.recentHigh * 0.997;
    }

    private int countSignals(boolean momentumBreakout, boolean pullbackEntry, boolean volumeBreakout) {
        int count = 0;
        if (momentumBreakout) count++;
        if (pullbackEntry) count++;
        if (volumeBreakout) count++;
        return count;
    }

    private int calculateSignalScore(BuySignal signal, Market market) {
        int score = 0;

        if (signal.pullbackEntry) score += 40;
        if (signal.volumeBreakout) score += 20;
        if (signal.momentumBreakout) score += 10;
        if (signal.strongBreakout) score += 10;

        if (signal.turnoverFilterPassed) score += 8;
        if (signal.absoluteLiquidityPassed) score += 8;
        score += signal.trendScore * 6;

        if (signal.pullbackRecovering) score += 8;
        if (signal.pullbackVelocityOk) score += 6;

        if (signal.velocity >= strongVelocityMin(market)) score += 6;
        else if (signal.velocity >= momentumVelocityMin(market)) score += 3;

        if (signal.volume > 0.0 && signal.averageVolume > 0.0) {
            double volRatio = signal.volume / signal.averageVolume;
            if (volRatio >= 2.0) score += 8;
            else if (volRatio >= 1.0) score += 4;
        }

        return score;
    }

    private String deriveRejectReason(BuySignal signal, Market market) {
        if (!signal.multiUptrend) return "MULTI_UPTREND_FAIL";
        if (signal.signalCount == 0) return "NO_ENTRY";
        if (signal.signalCount == 1 && !signal.pullbackEntry) return "SINGLE_SIGNAL_NO_PULLBACK";
        if (signal.signalCount == 1 && signal.pullbackEntry && signal.signalScore < (market == Market.US ? 45 : 50)) {
            return "SCORE_LOW";
        }
        if (market != Market.US && signal.signalCount < 2) return "SIGNAL_COUNT_LOW";
        return "FILTERED";
    }

    private double determinePositionSize(BuySignal signal, Market market) {
        if (signal.strongBreakout
                && signal.volume > 0.0
                && signal.averageVolume > 0.0
                && signal.volume >= signal.averageVolume * VOLUME_SURGE_MULT_FOR_SIZE_UP) {
            return SIZE_UP_MULT;
        }

        return signal.isBuyCandidate(market) ? BASE_SIZE : 0.0;
    }

    private int resolveBuyQuantity(SymbolState st, double orderPrice, double positionSize) {
        if (orderPrice <= 0.0) {
            return 0;
        }

        if (st.buyAmountPerOrder > 0.0) {
            double targetNotional = st.buyAmountPerOrder * positionSize;
            int qty = (int) Math.floor(targetNotional / orderPrice);
            return Math.max(0, qty);
        }

        if (positionSize >= 1.25) {
            return 2;
        }
        return FIXED_BUY_QTY;
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

    private long historySpanSeconds(PriceHistory history) {
        return history.spanSeconds();
    }

    private boolean isKrMarketBlockedTime(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return time.isBefore(LocalTime.of(9, 1)) || time.isAfter(LocalTime.of(15, 19));
    }

    private boolean isKrMarketCautiousWindow(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return !time.isBefore(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(9, 5));
    }

    private double lowVolumeSkipMult(Market market) {
        return market == Market.US ? LOW_VOLUME_SKIP_MULT_US : LOW_VOLUME_SKIP_MULT_KRX;
    }

    private double momentumVelocityMin(Market market) {
        return market == Market.US ? 0.006 : 0.004;
    }

    private double volumeBreakVelocityMin(Market market) {
        return market == Market.US ? 0.010 : 0.006;
    }

    private double strongVelocityMin(Market market) {
        return market == Market.US ? 0.012 : 0.008;
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

    private double stopLossMult(Market market) {
        return market == Market.US ? STOP_LOSS_MULT_US : STOP_LOSS_MULT_KRX;
    }

    private double emergencyStopMult(Market market) {
        return market == Market.US ? EMERGENCY_STOP_MULT_US : EMERGENCY_STOP_MULT_KRX;
    }

    private long maxHoldWithoutProfitMs(Market market) {
        return market == Market.US ? MAX_HOLD_WITHOUT_PROFIT_MS_US : MAX_HOLD_WITHOUT_PROFIT_MS_KRX;
    }

    private double timeStopNegVelocity(Market market) {
        return market == Market.US ? TIME_STOP_NEG_VELOCITY_US : TIME_STOP_NEG_VELOCITY_KRX;
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
                || "TAKE_PROFIT_PARTIAL_FULL".equals(reason)
                || "TAKE_PROFIT_FINAL".equals(reason)
                || "TRAILING_STOP".equals(reason);
    }

    private String entryReason(BuySignal signal) {
        if (signal.pullbackEntry) {
            return "PULLBACK_REBOUND";
        }
        if (signal.volumeBreakout) {
            return "VOLUME_BREAKOUT";
        }
        if (signal.momentumBreakout) {
            return "MOMENTUM";
        }
        return "ENTRY";
    }

    private String fmt(double value) {
        return String.format("%.4f", value);
    }

    private String fmtPct(double value) {
        return String.format("%.2f%%", value * 100.0);
    }
}
