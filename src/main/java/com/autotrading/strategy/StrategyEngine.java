package com.autotrading.strategy;

import com.autotrading.market.MarketDataService;
import com.autotrading.model.OrderCommand;
import com.autotrading.model.StockQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StrategyEngine {

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngine.class);

    public enum Market { KRX, US }

    private static final int HISTORY_CAPACITY = 50;
    // 3-minute warmup profile (with 5-second scheduler tick ~= 36 ticks).
    private static final int MIN_HISTORY_TICKS = 36;
    private static final int MIN_HISTORY_SPAN_SECONDS = 180;
    private static final int VELOCITY_WINDOW_MIN_SECONDS = 30;
    private static final int VELOCITY_WINDOW_MAX_SECONDS = 90;

    private static final double MOMENTUM_VELOCITY_MIN = 0.02;
    private static final double MOMENTUM_VOLUME_MULT = 1.5;
    private static final double MOMENTUM_HIGH_BUFFER = 0.995;

    private static final double PULLBACK_HIGH_MULT = 0.98;
    private static final double PULLBACK_LOW_MULT = 1.01;
    private static final double PULLBACK_VOLUME_MULT = 0.8;

    private static final double VOL_BREAK_HIGH_MULT = 0.995;
    private static final double VOL_BREAK_VOLUME_MULT = 2.0;
    private static final double VOL_BREAK_VELOCITY_MIN = 0.01;

    private static final double LOW_VOLUME_SKIP_MULT = 0.3;

    private static final double STOP_LOSS_MULT = 0.985;   // -1.5%
    private static final double TAKE_PROFIT_MULT = 1.035; // +3.5%

    private static final double BASE_SIZE = 1.0;
    private static final double SIZE_UP_MULT = 1.5;
    private static final double SIZE_DOWN_MULT = 0.5;
    private static final double SIZE_UP_VELOCITY = 0.03;
    private static final double SIZE_DOWN_VELOCITY = 0.015;
    private static final double SIZE_UP_VOLUME_MULT = 3.0;

    private static final long BUY_COOLDOWN_MS = 60_000L;
    private static final long PENDING_TIMEOUT_MS = 30_000L;
    private static final long SELL_RETRY_COOLDOWN_MS = 5_000L;

    private static final int FIXED_BUY_QTY = 1;

    private final MarketDataService marketDataService;
    private final Map<String, SymbolState> states = new ConcurrentHashMap<>();

    private static class SymbolState {
        final PriceHistory history = new PriceHistory(HISTORY_CAPACITY);
        Market market;
        boolean buyPending;
        long buyPendingSinceMs;
        long lastBuySignalMs;
        long lastSellSignalMs;
        long entryTimeMs;
        double highestSinceEntry;
        double buyAmountPerOrder;
    }

    private static class BuySignal {
        final boolean enoughHistory;
        final double price;
        final double volume;
        final double averageVolume;
        final double velocity;
        final double recentHigh;
        final double recentLow;
        final boolean lowVolumeSkip;
        final boolean momentumBreakout;
        final boolean pullbackEntry;
        final boolean volumeBreakout;

        BuySignal(boolean enoughHistory,
                  double price,
                  double volume,
                  double averageVolume,
                  double velocity,
                  double recentHigh,
                  double recentLow,
                  boolean lowVolumeSkip,
                  boolean momentumBreakout,
                  boolean pullbackEntry,
                  boolean volumeBreakout) {
            this.enoughHistory = enoughHistory;
            this.price = price;
            this.volume = volume;
            this.averageVolume = averageVolume;
            this.velocity = velocity;
            this.recentHigh = recentHigh;
            this.recentLow = recentLow;
            this.lowVolumeSkip = lowVolumeSkip;
            this.momentumBreakout = momentumBreakout;
            this.pullbackEntry = pullbackEntry;
            this.volumeBreakout = volumeBreakout;
        }

        boolean isBuyCandidate() {
            return momentumBreakout || pullbackEntry || volumeBreakout;
        }
    }

    public StrategyEngine(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public void resetAll() {
        states.clear();
    }

    public void resetSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        states.remove(symbol.trim().toUpperCase());
    }

    public void setMarket(String symbol, Market market) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        state(symbol).market = market;
    }

    /**
     * Per-symbol buy amount in quote currency.
     * - KRX: KRW
     * - US: USD
     * If <= 0, fixed quantity mode is used.
     */
    public void setBuyAmount(String symbol, Double buyAmount) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        SymbolState st = state(symbol);
        double normalized = (buyAmount == null || !Double.isFinite(buyAmount) || buyAmount <= 0.0)
                ? 0.0 : buyAmount;
        synchronized (st) {
            st.buyAmountPerOrder = normalized;
        }
        if (normalized > 0.0) {
            logger.info("BUY amount set for {}: {}", symbol, String.format("%.2f", normalized));
        } else {
            logger.info("BUY amount cleared for {} (fixed qty mode)", symbol);
        }
    }

    public void clearStaleHoldState(String symbol) {
        SymbolState st = state(symbol);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0;
            st.entryTimeMs = 0;
            st.highestSinceEntry = 0;
            st.lastSellSignalMs = System.currentTimeMillis();
        }
    }

    public void markBuyPending(String symbol) {
        SymbolState st = state(symbol);
        synchronized (st) {
            st.buyPending = true;
            st.buyPendingSinceMs = System.currentTimeMillis();
        }
    }

    public void cancelBuyPending(String symbol) {
        SymbolState st = state(symbol);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0;
        }
    }

    public void notifyBuyAccepted(String symbol) {
        cancelBuyPending(symbol);
    }

    public void notifyBuyFilled(String symbol) {
        SymbolState st = state(symbol);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0;
            if (st.entryTimeMs == 0) {
                st.entryTimeMs = System.currentTimeMillis();
            }
        }
    }

    public void notifyBuyRejected(String symbol) {
        cancelBuyPending(symbol);
    }

    public void notifySellAccepted(String symbol) {
        SymbolState st = state(symbol);
        synchronized (st) {
            st.entryTimeMs = 0;
            st.highestSinceEntry = 0;
            st.lastSellSignalMs = System.currentTimeMillis();
        }
    }

    public void notifySellRejected(String symbol) {
        SymbolState st = state(symbol);
        synchronized (st) {
            st.lastSellSignalMs = System.currentTimeMillis();
        }
    }

    public void record(String symbol,
                       double open,
                       double high,
                       double low,
                       double close,
                       double volume,
                       long timestamp) {
        if (symbol == null || symbol.isBlank() || close <= 0.0) {
            return;
        }
        SymbolState st = state(symbol);
        LocalDateTime ts = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        synchronized (st) {
            st.history.addTick(close, Math.max(0.0, volume), ts);
        }
    }

    /**
     * Pulls a validated quote from MarketDataService and evaluates buy signal.
     */
    public boolean shouldBuy(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            return false;
        }

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);

        StockQuote quote;
        try {
            quote = marketDataService.fetchPrice(normalized, market == Market.KRX ? "KRX" : null);
        } catch (Exception e) {
            logger.warn("BUY WAIT {} reason=QUOTE_ERROR msg={}", normalized, e.getMessage());
            return false;
        }

        long now = System.currentTimeMillis();

        synchronized (st) {
            st.history.addTick(quote.getPrice(), quote.getVolume(), normalizeTimestamp(quote.getTimestamp()));
            BuySignal signal = evaluateBuySignal(st);
            if (!canBuy(st, normalized, market, now, signal)) {
                return false;
            }
            st.lastBuySignalMs = now;
            logger.info("BUY SIGNAL [{}] {} vel={} vol={}/{} A={} B={} C={}",
                    market,
                    normalized,
                    fmtPct(signal.velocity),
                    fmt(signal.volume),
                    fmt(signal.averageVolume),
                    signal.momentumBreakout,
                    signal.pullbackEntry,
                    signal.volumeBreakout);
            return true;
        }
    }

    /**
     * Pulls a validated quote from MarketDataService and evaluates exit.
     */
    public boolean shouldSell(String symbol, double buyPrice) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || buyPrice <= 0.0) {
            return false;
        }

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
            st.history.addTick(quote.getPrice(), quote.getVolume(), normalizeTimestamp(quote.getTimestamp()));
        }
        return hitStopLossOrTakeProfit(quote.getPrice(), buyPrice);
    }

    /**
     * Backward-compatible path used by SchedulerService.
     * This path avoids extra network calls by using already fetched quote/volume.
     */
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
        long now = System.currentTimeMillis();

        synchronized (st) {
            st.history.addTick(currentPrice, Math.max(0.0, currentVolume1m), LocalDateTime.now());

            if (currentQuantity <= 0) {
                BuySignal signal = evaluateBuySignal(st);
                if (!canBuy(st, normalized, market, now, signal)) {
                    return Optional.empty();
                }

                double positionSize = calculatePositionSize(signal.velocity, signal.volume, signal.averageVolume);
                double orderPrice = roundToTickSize(currentPrice, market);
                int qty = resolveBuyQuantity(st, orderPrice, positionSize);
                if (qty < 1) {
                    logger.info("BUY WAIT [{}] {} reason=BUY_AMOUNT_TOO_SMALL amount={} order={}",
                            market,
                            normalized,
                            fmt(st.buyAmountPerOrder),
                            fmt(orderPrice));
                    return Optional.empty();
                }

                st.lastBuySignalMs = now;
                logger.info("BUY [{}] {} price={} qty={} size={} vel={} vol={}/{} A={} B={} C={}",
                        market,
                        normalized,
                        fmt(orderPrice),
                        qty,
                        fmt(positionSize),
                        fmtPct(signal.velocity),
                        fmt(signal.volume),
                        fmt(signal.averageVolume),
                        signal.momentumBreakout,
                        signal.pullbackEntry,
                        signal.volumeBreakout);
                return Optional.of(new OrderCommand(normalized, qty, "BUY", orderPrice));
            }

            if (avgPrice <= 0.0) {
                return Optional.empty();
            }
            if (!hitStopLossOrTakeProfit(currentPrice, avgPrice)) {
                if (currentPrice > st.highestSinceEntry) {
                    st.highestSinceEntry = currentPrice;
                }
                return Optional.empty();
            }
            if ((now - st.lastSellSignalMs) < SELL_RETRY_COOLDOWN_MS) {
                return Optional.empty();
            }

            st.lastSellSignalMs = now;
            double orderPrice = roundToTickSize(currentPrice, market);
            String reason = (currentPrice <= avgPrice * STOP_LOSS_MULT) ? "STOP_LOSS" : "TAKE_PROFIT";
            logger.info("SELL {} [{}] {} price={} buyPrice={} pnl={}",
                    reason,
                    market,
                    normalized,
                    fmt(orderPrice),
                    fmt(avgPrice),
                    fmtPct((currentPrice - avgPrice) / avgPrice));
            return Optional.of(new OrderCommand(normalized, currentQuantity, "SELL", orderPrice));
        }
    }

    private boolean canBuy(SymbolState st,
                           String symbol,
                           Market market,
                           long now,
                           BuySignal signal) {
        if (!signal.enoughHistory) {
            logger.info("BUY WAIT [{}] {} reason=HISTORY_SHORT ticks={} spanSec<={}",
                    market, symbol, st.history.size(), MIN_HISTORY_SPAN_SECONDS);
            return false;
        }

        if (st.buyPending) {
            long elapsed = now - st.buyPendingSinceMs;
            if (elapsed < PENDING_TIMEOUT_MS) {
                logger.info("BUY WAIT [{}] {} reason=PENDING elapsedMs={}", market, symbol, elapsed);
                return false;
            }
            st.buyPending = false;
            st.buyPendingSinceMs = 0;
            logger.warn("BUY pending timeout cleared for {}", symbol);
        }

        long cooldownLeft = BUY_COOLDOWN_MS - (now - st.lastBuySignalMs);
        if (cooldownLeft > 0) {
            logger.info("BUY WAIT [{}] {} reason=COOLDOWN remainingMs={}", market, symbol, cooldownLeft);
            return false;
        }

        if (signal.lowVolumeSkip) {
            logger.info("BUY WAIT [{}] {} reason=LOW_VOLUME price={} vol={} avgVol={}",
                    market, symbol, fmt(signal.price), fmt(signal.volume), fmt(signal.averageVolume));
            return false;
        }

        if (!signal.isBuyCandidate()) {
            logger.info("BUY WAIT [{}] {} reason=NO_ENTRY price={} vel={} high={} low={} vol={}/{}",
                    market,
                    symbol,
                    fmt(signal.price),
                    fmtPct(signal.velocity),
                    fmt(signal.recentHigh),
                    fmt(signal.recentLow),
                    fmt(signal.volume),
                    fmt(signal.averageVolume));
            return false;
        }
        return true;
    }

    private BuySignal evaluateBuySignal(SymbolState st) {
        if (!st.history.hasEnoughHistory(MIN_HISTORY_TICKS, MIN_HISTORY_SPAN_SECONDS)) {
            return new BuySignal(false, 0, 0, 0, 0, 0, 0, false, false, false, false);
        }

        PriceHistory.Tick latest = st.history.latest();
        if (latest == null || latest.getPrice() <= 0) {
            return new BuySignal(false, 0, 0, 0, 0, 0, 0, false, false, false, false);
        }

        double price = latest.getPrice();
        double volume = latest.getVolume();
        double avgVolume = st.history.averageVolume();
        double velocity = st.history.velocity(VELOCITY_WINDOW_MIN_SECONDS, VELOCITY_WINDOW_MAX_SECONDS);
        double recentHigh = st.history.recentHigh();
        double recentLow = st.history.recentLow();

        boolean lowVolumeSkip = avgVolume > 0 && volume < avgVolume * LOW_VOLUME_SKIP_MULT;

        boolean momentumBreakout = velocity >= MOMENTUM_VELOCITY_MIN
                && avgVolume > 0
                && volume >= avgVolume * MOMENTUM_VOLUME_MULT
                && recentHigh > 0
                && price < recentHigh * MOMENTUM_HIGH_BUFFER;

        boolean pullbackEntry = recentHigh > 0
                && recentLow > 0
                && price <= recentHigh * PULLBACK_HIGH_MULT
                && price >= recentLow * PULLBACK_LOW_MULT
                && avgVolume > 0
                && volume >= avgVolume * PULLBACK_VOLUME_MULT;

        boolean volumeBreakout = recentHigh > 0
                && price >= recentHigh * VOL_BREAK_HIGH_MULT
                && avgVolume > 0
                && volume >= avgVolume * VOL_BREAK_VOLUME_MULT
                && velocity >= VOL_BREAK_VELOCITY_MIN;

        return new BuySignal(true, price, volume, avgVolume, velocity, recentHigh, recentLow, lowVolumeSkip,
                momentumBreakout, pullbackEntry, volumeBreakout);
    }

    private double calculatePositionSize(double velocity, double volume, double averageVolume) {
        if (velocity >= SIZE_UP_VELOCITY || (averageVolume > 0 && volume >= averageVolume * SIZE_UP_VOLUME_MULT)) {
            return SIZE_UP_MULT;
        }
        if (velocity < SIZE_DOWN_VELOCITY) {
            return SIZE_DOWN_MULT;
        }
        return BASE_SIZE;
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

    private boolean hitStopLossOrTakeProfit(double currentPrice, double buyPrice) {
        if (buyPrice <= 0.0 || currentPrice <= 0.0) {
            return false;
        }
        return currentPrice <= buyPrice * STOP_LOSS_MULT
                || currentPrice >= buyPrice * TAKE_PROFIT_MULT;
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

    private LocalDateTime normalizeTimestamp(LocalDateTime ts) {
        return ts != null ? ts : LocalDateTime.now();
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
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

    private String fmt(double value) {
        return String.format("%.4f", value);
    }

    private String fmtPct(double value) {
        return String.format("%.2f%%", value * 100.0);
    }
}
