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
    // D-1 봉 보존 버퍼: 날짜 경계에서 clear() 대신 rotateToPrevDay()를 호출하면
    // 당일 봉이 여기로 이동하고 VWAP는 bars만 보지만 30분봉 계산은 두 버퍼를 합산한다.
    private List<MinuteBar> prevDayBars = new ArrayList<>();

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

    /** 날짜 경계 전환: 현재 봉을 D-1 버퍼로 옮기고 당일 봉을 비운다.
     *  VWAP 등 당일 전용 계산은 bars만 보므로 영향 없음.
     *  30분봉 RSI/BB는 latestBarsMultiDay()를 통해 D-1 봉을 포함한다. */
    public synchronized void rotateToPrevDay() {
        prevDayBars = new ArrayList<>(bars);
        bars.clear();
    }

    /** D-1 마지막 봉의 종가 — RED_TO_GREEN 기준가로 사용 */
    public synchronized double prevDayLastClose() {
        if (prevDayBars == null || prevDayBars.isEmpty()) return 0.0;
        return prevDayBars.get(prevDayBars.size() - 1).getClose();
    }

    /** D-1 + 당일 봉을 합산하여 최근 n봉 반환 (30분봉 계산 전용) */
    private List<MinuteBar> latestBarsMultiDay(int n) {
        List<MinuteBar> current = new ArrayList<>(bars);
        if (current.size() >= n) {
            return current.subList(current.size() - n, current.size());
        }
        List<MinuteBar> result = new ArrayList<>(n);
        if (!prevDayBars.isEmpty()) {
            int needed = n - current.size();
            int from = Math.max(0, prevDayBars.size() - needed);
            result.addAll(prevDayBars.subList(from, prevDayBars.size()));
        }
        result.addAll(current);
        return result;
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

    /** 1분봉을 5분 단위로 집계해 최근 n개 5분봉 반환 (오래된 순) */
    private List<FiveMinuteBar> buildFiveMinuteBars(int n) {
        List<MinuteBar> raw = latestBars(n * 5);
        List<FiveMinuteBar> result = new ArrayList<>();
        int complete = raw.size() / 5;
        int startIdx = raw.size() - complete * 5;
        for (int i = 0; i < complete; i++) {
            int from = startIdx + i * 5;
            double open = raw.get(from).getOpen();
            double high = 0;
            double low  = Double.MAX_VALUE;
            double vol  = 0;
            for (int j = from; j < from + 5; j++) {
                if (raw.get(j).getHigh() > high) high = raw.get(j).getHigh();
                if (raw.get(j).getLow()  < low)  low  = raw.get(j).getLow();
                vol += raw.get(j).getVolume();
            }
            double close = raw.get(from + 4).getClose();
            result.add(new FiveMinuteBar(true, open, high,
                    low == Double.MAX_VALUE ? 0 : low, close, vol));
        }
        return result;
    }

    public synchronized BollingerBands computeFiveMinuteBollingerBands(int period, double mult) {
        List<FiveMinuteBar> bars5 = buildFiveMinuteBars(period);
        if (bars5.size() < period) return new BollingerBands(false, 0, 0, 0, 0);
        List<FiveMinuteBar> sub = bars5.subList(bars5.size() - period, bars5.size());
        double sum = 0;
        for (FiveMinuteBar b : sub) sum += b.close;
        double middle = sum / period;
        double variance = 0;
        for (FiveMinuteBar b : sub) { double d = b.close - middle; variance += d * d; }
        double stddev = Math.sqrt(variance / period);
        double upper = middle + mult * stddev;
        double lower = middle - mult * stddev;
        double bandwidth = middle > 0 ? (upper - lower) / middle : 0;
        return new BollingerBands(true, middle, upper, lower, bandwidth);
    }

    public synchronized RsiResult computeFiveMinuteRsiSignal(int rsiPeriod, int signalPeriod) {
        int needed = rsiPeriod + signalPeriod + 3;
        List<FiveMinuteBar> bars5 = buildFiveMinuteBars(needed);
        if (bars5.size() < rsiPeriod + signalPeriod + 1)
            return new RsiResult(false, 50, 50, 50, 50, false);
        List<FiveMinuteBar> sub = bars5.size() > needed
                ? bars5.subList(bars5.size() - needed, bars5.size()) : bars5;

        double[] closes = new double[sub.size()];
        for (int i = 0; i < sub.size(); i++) closes[i] = sub.get(i).close;

        double[] gains  = new double[closes.length - 1];
        double[] losses = new double[closes.length - 1];
        for (int i = 0; i < closes.length - 1; i++) {
            double diff = closes[i + 1] - closes[i];
            gains[i]  = diff > 0 ?  diff : 0;
            losses[i] = diff < 0 ? -diff : 0;
        }

        double avgGain = 0, avgLoss = 0;
        for (int i = 0; i < rsiPeriod; i++) { avgGain += gains[i]; avgLoss += losses[i]; }
        avgGain /= rsiPeriod; avgLoss /= rsiPeriod;

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

    /** 1분봉을 30분 단위로 집계해 최근 n개 30분봉 반환 (오래된 순) */
    private List<FiveMinuteBar> buildThirtyMinuteBars(int n) {
        List<MinuteBar> raw = latestBarsMultiDay(n * 30);
        List<FiveMinuteBar> result = new ArrayList<>();
        int complete = raw.size() / 30;
        int startIdx = raw.size() - complete * 30;
        for (int i = 0; i < complete; i++) {
            int from = startIdx + i * 30;
            double open = raw.get(from).getOpen();
            double high = 0, low = Double.MAX_VALUE, vol = 0;
            for (int j = from; j < from + 30; j++) {
                if (raw.get(j).getHigh() > high) high = raw.get(j).getHigh();
                if (raw.get(j).getLow()  < low)  low  = raw.get(j).getLow();
                vol += raw.get(j).getVolume();
            }
            double close = raw.get(from + 29).getClose();
            result.add(new FiveMinuteBar(true, open, high,
                    low == Double.MAX_VALUE ? 0 : low, close, vol));
        }
        return result;
    }

    public synchronized BollingerBands computeThirtyMinuteBollingerBands(int period, double mult) {
        List<FiveMinuteBar> bars30 = buildThirtyMinuteBars(period);
        if (bars30.size() < period) return new BollingerBands(false, 0, 0, 0, 0);
        List<FiveMinuteBar> sub = bars30.subList(bars30.size() - period, bars30.size());
        double sum = 0;
        for (FiveMinuteBar b : sub) sum += b.close;
        double middle = sum / period;
        double variance = 0;
        for (FiveMinuteBar b : sub) { double d = b.close - middle; variance += d * d; }
        double stddev = Math.sqrt(variance / period);
        double upper = middle + mult * stddev;
        double lower = middle - mult * stddev;
        double bandwidth = middle > 0 ? (upper - lower) / middle : 0;
        return new BollingerBands(true, middle, upper, lower, bandwidth);
    }

    public synchronized RsiResult computeThirtyMinuteRsiSignal(int rsiPeriod, int signalPeriod) {
        int needed = rsiPeriod + signalPeriod + 3;
        List<FiveMinuteBar> bars30 = buildThirtyMinuteBars(needed);
        if (bars30.size() < rsiPeriod + signalPeriod + 1)
            return new RsiResult(false, 50, 50, 50, 50, false);
        List<FiveMinuteBar> sub = bars30.size() > needed
                ? bars30.subList(bars30.size() - needed, bars30.size()) : bars30;

        double[] closes = new double[sub.size()];
        for (int i = 0; i < sub.size(); i++) closes[i] = sub.get(i).close;

        double[] gains  = new double[closes.length - 1];
        double[] losses = new double[closes.length - 1];
        for (int i = 0; i < closes.length - 1; i++) {
            double diff = closes[i + 1] - closes[i];
            gains[i]  = diff > 0 ?  diff : 0;
            losses[i] = diff < 0 ? -diff : 0;
        }

        double avgGain = 0, avgLoss = 0;
        for (int i = 0; i < rsiPeriod; i++) { avgGain += gains[i]; avgLoss += losses[i]; }
        avgGain /= rsiPeriod; avgLoss /= rsiPeriod;

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

    /** 최근 n개 완성 30분봉의 평균 거래량 (직전봉 제외) */
    public synchronized double thirtyMinuteAverageVolume(int n) {
        List<FiveMinuteBar> bars30 = buildThirtyMinuteBars(n + 1);
        if (bars30.size() < 2) return 0;
        int endIdx = bars30.size() - 1;
        int startIdx = Math.max(0, endIdx - n);
        double sum = 0;
        int count = 0;
        for (int i = startIdx; i < endIdx; i++) {
            sum += bars30.get(i).volume;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    /** 가장 최근 완성 30분봉의 거래량 */
    public synchronized double latestCompleteThirtyMinuteVolume() {
        List<FiveMinuteBar> bars30 = buildThirtyMinuteBars(2);
        return bars30.isEmpty() ? 0 : bars30.get(bars30.size() - 1).volume;
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
