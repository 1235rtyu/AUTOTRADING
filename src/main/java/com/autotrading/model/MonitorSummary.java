package com.autotrading.model;

import java.time.LocalDateTime;

public class MonitorSummary {
    private String market;
    private double totalEvaluationAmount;
    private double totalProfitAmount;
    private double totalProfitRate;
    private double todayProfitAmount;
    private double todayRealizedProfitAmount;
    private double todayBuyAmount;
    private double todaySellAmount;
    private int holdingCount;
    private int runningStrategyCount;
    private LocalDateTime updatedAt;

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public double getTotalEvaluationAmount() {
        return totalEvaluationAmount;
    }

    public void setTotalEvaluationAmount(double totalEvaluationAmount) {
        this.totalEvaluationAmount = totalEvaluationAmount;
    }

    public double getTotalProfitAmount() {
        return totalProfitAmount;
    }

    public void setTotalProfitAmount(double totalProfitAmount) {
        this.totalProfitAmount = totalProfitAmount;
    }

    public double getTotalProfitRate() {
        return totalProfitRate;
    }

    public void setTotalProfitRate(double totalProfitRate) {
        this.totalProfitRate = totalProfitRate;
    }

    public double getTodayProfitAmount() {
        return todayProfitAmount;
    }

    public void setTodayProfitAmount(double todayProfitAmount) {
        this.todayProfitAmount = todayProfitAmount;
    }

    public double getTodayRealizedProfitAmount() {
        return todayRealizedProfitAmount;
    }

    public void setTodayRealizedProfitAmount(double todayRealizedProfitAmount) {
        this.todayRealizedProfitAmount = todayRealizedProfitAmount;
    }

    public double getTodayBuyAmount() {
        return todayBuyAmount;
    }

    public void setTodayBuyAmount(double todayBuyAmount) {
        this.todayBuyAmount = todayBuyAmount;
    }

    public double getTodaySellAmount() {
        return todaySellAmount;
    }

    public void setTodaySellAmount(double todaySellAmount) {
        this.todaySellAmount = todaySellAmount;
    }

    public int getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(int holdingCount) {
        this.holdingCount = holdingCount;
    }

    public int getRunningStrategyCount() {
        return runningStrategyCount;
    }

    public void setRunningStrategyCount(int runningStrategyCount) {
        this.runningStrategyCount = runningStrategyCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
