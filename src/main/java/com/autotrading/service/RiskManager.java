package com.autotrading.service;

import org.springframework.stereotype.Component;
// 5번 줄 아래에 추가
import org.springframework.scheduling.annotation.Scheduled;
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

    public synchronized void orderSucceeded(String symbol) {
        inFlightOrders.remove(symbol);
    }

    public synchronized void orderFailed(String symbol) {
        inFlightOrders.remove(symbol);
    }

    /**
     * Backward-compatible alias. Prefer orderSucceeded/orderFailed explicitly.
     */
    public synchronized void orderCompleted(String symbol) {
        orderSucceeded(symbol);
    }

    public synchronized void addLoss(double loss) {
        dailyLoss += loss;
    }

    public synchronized boolean hasHitLossLimit() {
        return dailyLoss >= dailyLossLimit;
        
    }
    // 35번 줄 아래에 추가
    @Scheduled(cron = "0 0 0 * * *")
    public synchronized void resetDailyLoss() {
        dailyLoss = 0;
    }
}
