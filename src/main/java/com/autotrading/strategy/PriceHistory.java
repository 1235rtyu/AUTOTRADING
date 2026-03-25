package com.autotrading.strategy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class PriceHistory {

    public static class Tick {
        private final double price;
        private final double volume;
        private final double turnover;
        private final LocalDateTime timestamp;

        Tick(double price, double volume, double turnover, LocalDateTime timestamp) {
            this.price = price;
            this.volume = volume;
            this.turnover = turnover;
            this.timestamp = timestamp;
        }

        public double getPrice() {
            return price;
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
    private final Deque<Tick> ticks = new ArrayDeque<>();

    public PriceHistory(int capacity) {
        this.capacity = Math.max(10, capacity);
    }

    public synchronized void addTick(double price, double volume, LocalDateTime timestamp) {
        if (!Double.isFinite(price) || price <= 0.0) {
            return;
        }

        LocalDateTime ts = timestamp != null ? timestamp : LocalDateTime.now();
        double normalizedVolume = Double.isFinite(volume) && volume > 0.0 ? volume : 0.0;
        double turnover = price * normalizedVolume;

        ticks.addLast(new Tick(price, normalizedVolume, turnover, ts));

        while (ticks.size() > capacity) {
            ticks.pollFirst();
        }
    }

    public synchronized Tick oldest() {
        return ticks.peekFirst();
    }

    public synchronized Tick latest() {
        return ticks.peekLast();
    }

    public synchronized int size() {
        return ticks.size();
    }

    public synchronized long spanSeconds() {
        Tick first = ticks.peekFirst();
        Tick last = ticks.peekLast();

        if (first == null || last == null) {
            return 0L;
        }

        return Math.abs(Duration.between(first.timestamp, last.timestamp).getSeconds());
    }

    public synchronized boolean hasEnoughHistory(int minTicks, int minSpanSeconds) {
        if (ticks.size() < minTicks) {
            return false;
        }
        return spanSeconds() >= minSpanSeconds;
    }

    public synchronized double recentHigh() {
        return recentHigh(0);
    }

    public synchronized double recentHigh(int lastN) {
        if (ticks.isEmpty()) {
            return 0.0;
        }

        double high = 0.0;
        for (Tick tick : lastTicksView(lastN)) {
            high = Math.max(high, tick.price);
        }
        return high;
    }

    public synchronized double recentLow() {
        return recentLow(0);
    }

    public synchronized double recentLow(int lastN) {
        if (ticks.isEmpty()) {
            return 0.0;
        }

        double low = Double.MAX_VALUE;
        for (Tick tick : lastTicksView(lastN)) {
            low = Math.min(low, tick.price);
        }
        return low == Double.MAX_VALUE ? 0.0 : low;
    }

    public synchronized double averageVolume() {
        return averageVolume(0);
    }

    public synchronized double averageVolume(int lastN) {
        if (ticks.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;
        for (Tick tick : lastTicksView(lastN)) {
            if (tick.volume >= 0.0) {
                sum += tick.volume;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }
        return sum / count;
    }

    public synchronized double averageTurnover() {
        return averageTurnover(0);
    }

    public synchronized double averageTurnover(int lastN) {
        if (ticks.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;
        for (Tick tick : lastTicksView(lastN)) {
            if (tick.turnover >= 0.0) {
                sum += tick.turnover;
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public synchronized double recentTurnoverSum(int lastN) {
        if (ticks.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Tick tick : lastTicksView(lastN)) {
            if (tick.turnover >= 0.0) {
                sum += tick.turnover;
            }
        }
        return sum;
    }

    public synchronized double latestTurnover() {
        Tick latest = ticks.peekLast();
        return latest != null ? latest.turnover : 0.0;
    }

    public synchronized double averagePrice(int lastN) {
        if (ticks.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;
        for (Tick tick : lastTicksView(lastN)) {
            if (tick.price > 0.0) {
                sum += tick.price;
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public synchronized double simpleMovingAverage(int lastN) {
        return averagePrice(lastN);
    }

    public synchronized boolean isShortTermUptrend(int shortN, int longN) {
        int safeShort = Math.max(1, shortN);
        int safeLong = Math.max(safeShort + 1, longN);

        if (ticks.size() < safeLong) {
            return false;
        }

        double shortAvg = averagePrice(safeShort);
        double longAvg = averagePrice(safeLong);

        return shortAvg > 0.0 && longAvg > 0.0 && shortAvg > longAvg;
    }

    public synchronized boolean isPriceNearRecentHigh(double ratio) {
        Tick latest = ticks.peekLast();
        if (latest == null || latest.price <= 0.0) {
            return false;
        }

        double safeRatio = ratio > 0.0 ? ratio : 1.0;
        double high = recentHigh();

        return high > 0.0 && latest.price >= high * safeRatio;
    }

    public synchronized boolean isPriceInPullbackZone(double upperFromHigh, double lowerFromHigh) {
        Tick latest = ticks.peekLast();
        if (latest == null || latest.price <= 0.0) {
            return false;
        }

        double high = recentHigh();
        if (high <= 0.0) {
            return false;
        }

        double upper = Math.max(upperFromHigh, lowerFromHigh);
        double lower = Math.min(upperFromHigh, lowerFromHigh);

        return latest.price <= high * upper && latest.price >= high * lower;
    }

    /**
     * Velocity = (latestPrice - anchorPrice) / anchorPrice
     * where anchor tick is selected within [minWindowSeconds, maxWindowSeconds].
     */
    public synchronized double velocity(int minWindowSeconds, int maxWindowSeconds) {
        return velocitySeconds(minWindowSeconds, maxWindowSeconds);
    }

    public synchronized double velocitySeconds(int minWindowSeconds, int maxWindowSeconds) {
        if (ticks.size() < 2) {
            return 0.0;
        }

        List<Tick> list = new ArrayList<>(ticks);
        Tick latest = list.get(list.size() - 1);
        Tick anchor = null;

        int safeMin = Math.max(0, minWindowSeconds);
        int safeMax = Math.max(safeMin + 1, maxWindowSeconds);

        for (int i = list.size() - 2; i >= 0; i--) {
            Tick candidate = list.get(i);
            long diffSec = Math.abs(Duration.between(candidate.timestamp, latest.timestamp).getSeconds());

            if (diffSec > safeMax) {
                break;
            }
            if (diffSec >= safeMin) {
                anchor = candidate;
            }
        }

        if (anchor == null || anchor.price <= 0.0) {
            return 0.0;
        }

        return (latest.price - anchor.price) / anchor.price;
    }

    public synchronized double velocityByLastTicks(int ticksBack) {
        if (ticks.size() < 2) {
            return 0.0;
        }

        List<Tick> list = new ArrayList<>(ticks);
        Tick latest = list.get(list.size() - 1);

        int safeBack = Math.max(1, ticksBack);
        int anchorIndex = list.size() - 1 - safeBack;
        if (anchorIndex < 0) {
            anchorIndex = 0;
        }

        Tick anchor = list.get(anchorIndex);
        if (anchor.price <= 0.0) {
            return 0.0;
        }

        return (latest.price - anchor.price) / anchor.price;
    }

    public synchronized List<Tick> latestTicks(int n) {
        return new ArrayList<>(lastTicksView(n));
    }

    public synchronized List<Double> latestPrices(int n) {
        List<Double> out = new ArrayList<>();
        for (Tick tick : lastTicksView(n)) {
            out.add(tick.price);
        }
        return out;
    }

    public synchronized List<Double> latestVolumes(int n) {
        List<Double> out = new ArrayList<>();
        for (Tick tick : lastTicksView(n)) {
            out.add(tick.volume);
        }
        return out;
    }

    private List<Tick> lastTicksView(int lastN) {
        if (ticks.isEmpty()) {
            return List.of();
        }

        List<Tick> list = new ArrayList<>(ticks);
        int size = list.size();
        int safeN = lastN <= 0 ? size : Math.min(lastN, size);

        if (safeN <= 0) {
            return List.of();
        }

        return list.subList(size - safeN, size);
    }

    public synchronized void clear() {
        ticks.clear();
    }
}