package com.autotrading.strategy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MinuteBarHistory {

    public static class FiveMinuteBar {
        public final boolean valid;
        public final double open;
        public final double high;
        public final double low;
        public final double close;
        public final double volume;
        FiveMinuteBar(boolean valid, double open, double high, double low, double close, double volume) {
            this.valid = valid; this.open = open; this.high = high;
            this.low = low; this.close = close; this.volume = volume;
        }
    }

    public static class BollingerBands {
        public final boolean valid;
        public final double middle;
        public final double upper;
        public final double lower;
        public final double bandwidth;
        BollingerBands(boolean valid, double middle, double upper, double lower, double bandwidth) {
            this.valid = valid; this.middle = middle; this.upper = upper;
            this.lower = lower; this.bandwidth = bandwidth;
        }
    }

    public static class RsiResult {
        public final boolean valid;
        public final double rsi;
        public final double rsiSignal;
        public final double prevRsi;
        public final double prevRsiSignal;
        public final boolean crossedUp;
        RsiResult(boolean valid, double rsi, double rsiSignal, double prevRsi, double prevRsiSignal, boolean crossedUp) {
            this.valid = valid; this.rsi = rsi; this.rsiSignal = rsiSignal;
            this.prevRsi = prevRsi; this.prevRsiSignal = prevRsiSignal; this.crossedUp = crossedUp;
        }
    }

    public static class MinuteBar {
        private final double open;
        private final double high;
        private final double low;
        private final double close;
        private final double volume;
        private final double turnover;
        private final LocalDateTime timestamp;

        MinuteBar(double open, double high, double low, double close, double volume, LocalDateTime timestamp) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
            this.turnover = close * volume;
            this.timestamp = timestamp;
        }

        public double getOpen() {
            return open;
        }

        public double getHigh() {
            return high;
        }

        public double getLow() {
            return low;
        }

        public double getClose() {
            return close;
        }

        public double getVolume() {
            return volume;
        }

        public double getTurnover() {
            return turnover;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    private final int capacity;
    private final Deque<MinuteBar> bars = new ArrayDeque<>();

    public MinuteBarHistory(int capacity) {
        this.capacity = Math.max(5, capacity);
    }

    public synchronized void addBar(double open,
                                    double high,
                                    double low,
                                    double close,
                                    double volume,
                                    LocalDateTime timestamp) {
        if (!Double.isFinite(close) || close <= 0.0) {
            return;
        }

        LocalDateTime ts = timestamp != null ? timestamp : LocalDateTime.now();
        double normalizedVolume = Double.isFinite(volume) && volume > 0.0 ? volume : 0.0;
        double safeHigh = Double.isFinite(high) && high > 0.0 ? high : close;
        double safeLow = Double.isFinite(low) && low > 0.0 ? low : close;
        double safeOpen = Double.isFinite(open) && open > 0.0 ? open : close;

        bars.addLast(new MinuteBar(safeOpen, safeHigh, safeLow, close, normalizedVolume, ts));

        while (bars.size() > capacity) {
            bars.pollFirst();
        }
    }

    public synchronized MinuteBar oldest() {
        return bars.peekFirst();
    }

    public synchronized MinuteBar latest() {
        return bars.peekLast();
    }

    public synchronized int size() {
        return bars.size();
    }

    public synchronized void clear() {
        bars.clear();
    }

    public synchronized long spanSeconds() {
        MinuteBar first = bars.peekFirst();
        MinuteBar last = bars.peekLast();

        if (first == null || last == null) {
            return 0L;
        }

        return Math.abs(Duration.between(first.getTimestamp(), last.getTimestamp()).getSeconds());
    }

    public synchronized boolean hasEnoughHistory(int minBars, int minSpanSeconds) {
        if (bars.size() < minBars) {
            return false;
        }
        return spanSeconds() >= minSpanSeconds;
    }

    public synchronized double recentHigh() {
        return recentHigh(0);
    }

    public synchronized double recentHigh(int lastN) {
        if (bars.isEmpty()) {
            return 0.0;
        }

        double high = 0.0;
        for (MinuteBar bar : lastBarsView(lastN)) {
            high = Math.max(high, bar.getHigh());
        }
        return high;
    }

    public synchronized double highestHigh(int lastN) {
        return recentHigh(lastN);
    }

    public synchronized double recentLow() {
        return recentLow(0);
    }

    public synchronized double recentLow(int lastN) {
        if (bars.isEmpty()) {
            return 0.0;
        }

        double low = Double.MAX_VALUE;
        for (MinuteBar bar : lastBarsView(lastN)) {
            low = Math.min(low, bar.getLow());
        }
        return low == Double.MAX_VALUE ? 0.0 : low;
    }

    public synchronized double lowestLow(int lastN) {
        return recentLow(lastN);
    }

    public synchronized double averageVolume() {
        return averageVolume(0);
    }

    public synchronized double averageVolume(int lastN) {
        if (bars.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;
        for (MinuteBar bar : lastBarsView(lastN)) {
            if (bar.getVolume() >= 0.0) {
                sum += bar.getVolume();
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    public synchronized double averageTurnover() {
        return averageTurnover(0);
    }

    public synchronized double averageTurnover(int lastN) {
        if (bars.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;
        for (MinuteBar bar : lastBarsView(lastN)) {
            if (bar.getTurnover() >= 0.0) {
                sum += bar.getTurnover();
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    public synchronized double latestTurnover() {
        MinuteBar latest = bars.peekLast();
        return latest != null ? latest.getTurnover() : 0.0;
    }

    public synchronized double sessionVwap() {
        if (bars.isEmpty()) {
            return 0.0;
        }
        double pv = 0.0;
        double vv = 0.0;
        for (MinuteBar bar : bars) {
            if (bar.getClose() <= 0.0 || bar.getVolume() <= 0.0) {
                continue;
            }
            pv += bar.getClose() * bar.getVolume();
            vv += bar.getVolume();
        }
        return vv > 0.0 ? (pv / vv) : 0.0;
    }

    public synchronized double sessionVwapPrevious() {
        if (bars.size() <= 1) {
            return sessionVwap();
        }
        List<MinuteBar> list = latestBars(0);
        if (list.size() <= 1) {
            return sessionVwap();
        }
        double pv = 0.0;
        double vv = 0.0;
        for (int i = 0; i < list.size() - 1; i++) {
            MinuteBar bar = list.get(i);
            if (bar.getClose() <= 0.0 || bar.getVolume() <= 0.0) {
                continue;
            }
            pv += bar.getClose() * bar.getVolume();
            vv += bar.getVolume();
        }
        return vv > 0.0 ? (pv / vv) : 0.0;
    }

    public synchronized double averagePrice(int lastN) {
        if (bars.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;
        for (MinuteBar bar : lastBarsView(lastN)) {
            if (bar.getClose() > 0.0) {
                sum += bar.getClose();
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public synchronized boolean isShortTermUptrend(int shortN, int longN) {
        int safeShort = Math.max(1, shortN);
        int safeLong = Math.max(safeShort + 1, longN);

        if (bars.size() < safeLong) {
            return false;
        }

        double shortAvg = averagePrice(safeShort);
        double longAvg = averagePrice(safeLong);

        return shortAvg > 0.0 && longAvg > 0.0 && shortAvg > longAvg;
    }

    /**
     * Velocity = (latestClose - anchorClose) / anchorClose
     * where anchor bar is selected within [minWindowSeconds, maxWindowSeconds].
     */
    public synchronized double velocitySeconds(int minWindowSeconds, int maxWindowSeconds) {
        if (bars.size() < 2) {
            return 0.0;
        }

        int minWindow = Math.max(1, minWindowSeconds);
        int maxWindow = Math.max(minWindow, maxWindowSeconds);

        MinuteBar latest = bars.peekLast();
        if (latest == null) {
            return 0.0;
        }

        MinuteBar anchor = null;
        for (MinuteBar bar : bars) {
            long diff = Math.abs(Duration.between(bar.getTimestamp(), latest.getTimestamp()).getSeconds());
            if (diff >= minWindow && diff <= maxWindow) {
                anchor = bar;
            }
        }

        if (anchor == null || anchor.getClose() <= 0.0) {
            return 0.0;
        }

        return (latest.getClose() - anchor.getClose()) / anchor.getClose();
    }

    public synchronized List<MinuteBar> latestBars(int n) {
        List<MinuteBar> list = new ArrayList<>();
        if (bars.isEmpty()) {
            return list;
        }

        for (MinuteBar bar : lastBarsView(n)) {
            list.add(bar);
        }
        return list;
    }

    private List<MinuteBar> lastBarsView(int lastN) {
        List<MinuteBar> list = new ArrayList<>();
        if (bars.isEmpty()) {
            return list;
        }

        int limit = lastN <= 0 ? bars.size() : Math.min(lastN, bars.size());
        int index = 0;
        int skip = bars.size() - limit;
        for (MinuteBar bar : bars) {
            if (index++ < skip) {
                continue;
            }
            list.add(bar);
        }
        return list;
    }

    public synchronized FiveMinuteBar latestFiveMinuteBar() {
        List<MinuteBar> list = latestBars(5);
        if (list.size() < 5) return new FiveMinuteBar(false, 0, 0, 0, 0, 0);
        double open   = list.get(0).getOpen();
        double high   = 0;
        double low    = Double.MAX_VALUE;
        double volume = 0;
        for (MinuteBar b : list) {
            if (b.getHigh() > high) high = b.getHigh();
            if (b.getLow() < low) low = b.getLow();
            volume += b.getVolume();
        }
        double close = list.get(list.size() - 1).getClose();
        return new FiveMinuteBar(true, open, high, low == Double.MAX_VALUE ? 0 : low, close, volume);
    }

    public synchronized BollingerBands computeBollingerBands(int period, double mult) {
        List<MinuteBar> list = latestBars(period);
        if (list.size() < period) return new BollingerBands(false, 0, 0, 0, 0);
        double sum = 0;
        for (MinuteBar b : list) sum += b.getClose();
        double middle = sum / period;
        double variance = 0;
        for (MinuteBar b : list) { double d = b.getClose() - middle; variance += d * d; }
        double stddev = Math.sqrt(variance / period);
        double upper = middle + mult * stddev;
        double lower = middle - mult * stddev;
        double bandwidth = middle > 0 ? (upper - lower) / middle : 0;
        return new BollingerBands(true, middle, upper, lower, bandwidth);
    }

    public synchronized RsiResult computeRsiSignal(int rsiPeriod, int signalPeriod) {
        int needed = rsiPeriod + signalPeriod + 1;
        List<MinuteBar> list = latestBars(needed + 2);
        if (list.size() < needed) return new RsiResult(false, 50, 50, 50, 50, false);

        // close 배열
        double[] closes = new double[list.size()];
        for (int i = 0; i < list.size(); i++) closes[i] = list.get(i).getClose();

        // Wilder RSI 계산
        double[] gains = new double[closes.length - 1];
        double[] losses = new double[closes.length - 1];
        for (int i = 0; i < closes.length - 1; i++) {
            double diff = closes[i + 1] - closes[i];
            gains[i]  = diff > 0 ? diff : 0;
            losses[i] = diff < 0 ? -diff : 0;
        }

        // 최초 평균 (단순 평균으로 시드)
        double avgGain = 0, avgLoss = 0;
        for (int i = 0; i < rsiPeriod; i++) { avgGain += gains[i]; avgLoss += losses[i]; }
        avgGain /= rsiPeriod; avgLoss /= rsiPeriod;

        // RSI 시리즈 빌드 (signalPeriod + 2개 필요)
        int rsiNeeded = signalPeriod + 2;
        double[] rsiSeries = new double[rsiNeeded];
        int rsiIdx = 0;

        for (int i = rsiPeriod; i < gains.length && rsiIdx < rsiNeeded; i++) {
            avgGain = (avgGain * (rsiPeriod - 1) + gains[i]) / rsiPeriod;
            avgLoss = (avgLoss * (rsiPeriod - 1) + losses[i]) / rsiPeriod;
            double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
            rsiSeries[rsiIdx++] = 100 - (100 / (1 + rs));
        }
        if (rsiIdx < rsiNeeded) return new RsiResult(false, 50, 50, 50, 50, false);

        // Signal (SMA of last signalPeriod RSI values)
        double sigSum = 0;
        for (int i = 0; i < signalPeriod; i++) sigSum += rsiSeries[i];
        double prevRsiSignal = sigSum / signalPeriod;

        double sigSum2 = 0;
        for (int i = 1; i <= signalPeriod; i++) sigSum2 += rsiSeries[i];
        double curRsiSignal = sigSum2 / signalPeriod;

        double prevRsi = rsiSeries[signalPeriod - 1];
        double curRsi  = rsiSeries[signalPeriod];

        boolean crossedUp = prevRsi <= prevRsiSignal && curRsi > curRsiSignal;
        return new RsiResult(true, curRsi, curRsiSignal, prevRsi, prevRsiSignal, crossedUp);
    }
}
