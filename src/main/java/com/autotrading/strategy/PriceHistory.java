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
        private final LocalDateTime timestamp;

        Tick(double price, double volume, LocalDateTime timestamp) {
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
        }

        public double getPrice() {
            return price;
        }

        public double getVolume() {
            return volume;
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
        ticks.addLast(new Tick(price, normalizedVolume, ts));
        while (ticks.size() > capacity) {
            ticks.pollFirst();
        }
    }

    public synchronized Tick latest() {
        return ticks.peekLast();
    }

    public synchronized int size() {
        return ticks.size();
    }

    public synchronized boolean hasEnoughHistory(int minTicks, int minSpanSeconds) {
        if (ticks.size() < minTicks) {
            return false;
        }
        Tick first = ticks.peekFirst();
        Tick last = ticks.peekLast();
        if (first == null || last == null) {
            return false;
        }
        long span = Math.abs(Duration.between(first.timestamp, last.timestamp).getSeconds());
        return span >= minSpanSeconds;
    }

    public synchronized double recentHigh() {
        double high = 0.0;
        for (Tick tick : ticks) {
            high = Math.max(high, tick.price);
        }
        return high;
    }

    public synchronized double recentLow() {
        if (ticks.isEmpty()) {
            return 0.0;
        }
        double low = Double.MAX_VALUE;
        for (Tick tick : ticks) {
            low = Math.min(low, tick.price);
        }
        return low == Double.MAX_VALUE ? 0.0 : low;
    }

    public synchronized double averageVolume() {
        if (ticks.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        int count = 0;
        for (Tick tick : ticks) {
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

    /**
     * Velocity = (latestPrice - anchorPrice) / anchorPrice
     * where anchor tick is selected within [minWindowSeconds, maxWindowSeconds].
     */
    public synchronized double velocity(int minWindowSeconds, int maxWindowSeconds) {
        if (ticks.size() < 2) {
            return 0.0;
        }
        List<Tick> list = new ArrayList<>(ticks);
        Tick latest = list.get(list.size() - 1);
        Tick anchor = null;

        for (int i = list.size() - 2; i >= 0; i--) {
            Tick candidate = list.get(i);
            long diffSec = Math.abs(Duration.between(candidate.timestamp, latest.timestamp).getSeconds());
            if (diffSec > maxWindowSeconds) {
                break;
            }
            if (diffSec >= minWindowSeconds) {
                anchor = candidate;
            }
        }

        if (anchor == null || anchor.price <= 0.0) {
            return 0.0;
        }
        return (latest.price - anchor.price) / anchor.price;
    }

    public synchronized void clear() {
        ticks.clear();
    }
}
