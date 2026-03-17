package com.autotrading.service;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class RiskManager {
    private final Set<String> inFlightOrders = new HashSet<>();
    private double dailyLossLimit = 100000;
    private double dailyLoss = 0;

    public synchronized boolean allowOrder(String symbol) {
        if (inFlightOrders.contains(symbol)) {
            return false;
        }
        if (dailyLoss >= dailyLossLimit) {
            return false;
        }
        inFlightOrders.add(symbol);
        return true;
    }

    public synchronized void orderCompleted(String symbol) {
        inFlightOrders.remove(symbol);
    }

    public synchronized void addLoss(double loss) {
        dailyLoss += loss;
    }

    public synchronized boolean hasHitLossLimit() {
        return dailyLoss >= dailyLossLimit;
    }
}
