package com.autotrading.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TradeHistory {

    private Long          id;
    private LocalDate     tradeDate;
    private String        symbol;
    private String        market;
    private String        entryMode;

    private double        entryPrice;
    private int           entryQty;
    private LocalDateTime entryTime;
    private Integer       entrySignalScore;
    private Double        entryVwapDistPct;
    private Double        entryVelocitySht;

    private double        exitPrice;
    private int           exitQty;
    private LocalDateTime exitTime;
    private String        exitReason;
    private String        exitType;

    private int           holdSeconds;
    private double        pnlAmount;
    private double        pnlPct;
    private double        weightedPnl;
    private Double        peakPnlPct;
    private double        feeAmount;
    private double        slippagePct;
    private boolean       isPartial;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public String getEntryMode() { return entryMode; }
    public void setEntryMode(String entryMode) { this.entryMode = entryMode; }

    public double getEntryPrice() { return entryPrice; }
    public void setEntryPrice(double entryPrice) { this.entryPrice = entryPrice; }

    public int getEntryQty() { return entryQty; }
    public void setEntryQty(int entryQty) { this.entryQty = entryQty; }

    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }

    public Integer getEntrySignalScore() { return entrySignalScore; }
    public void setEntrySignalScore(Integer entrySignalScore) { this.entrySignalScore = entrySignalScore; }

    public Double getEntryVwapDistPct() { return entryVwapDistPct; }
    public void setEntryVwapDistPct(Double entryVwapDistPct) { this.entryVwapDistPct = entryVwapDistPct; }

    public Double getEntryVelocitySht() { return entryVelocitySht; }
    public void setEntryVelocitySht(Double entryVelocitySht) { this.entryVelocitySht = entryVelocitySht; }

    public double getExitPrice() { return exitPrice; }
    public void setExitPrice(double exitPrice) { this.exitPrice = exitPrice; }

    public int getExitQty() { return exitQty; }
    public void setExitQty(int exitQty) { this.exitQty = exitQty; }

    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }

    public String getExitReason() { return exitReason; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }

    public String getExitType() { return exitType; }
    public void setExitType(String exitType) { this.exitType = exitType; }

    public int getHoldSeconds() { return holdSeconds; }
    public void setHoldSeconds(int holdSeconds) { this.holdSeconds = holdSeconds; }

    public double getPnlAmount() { return pnlAmount; }
    public void setPnlAmount(double pnlAmount) { this.pnlAmount = pnlAmount; }

    public double getPnlPct() { return pnlPct; }
    public void setPnlPct(double pnlPct) { this.pnlPct = pnlPct; }

    public double getWeightedPnl() { return weightedPnl; }
    public void setWeightedPnl(double weightedPnl) { this.weightedPnl = weightedPnl; }

    public Double getPeakPnlPct() { return peakPnlPct; }
    public void setPeakPnlPct(Double peakPnlPct) { this.peakPnlPct = peakPnlPct; }

    public double getFeeAmount() { return feeAmount; }
    public void setFeeAmount(double feeAmount) { this.feeAmount = feeAmount; }

    public double getSlippagePct() { return slippagePct; }
    public void setSlippagePct(double slippagePct) { this.slippagePct = slippagePct; }

    public boolean isPartial() { return isPartial; }
    public void setPartial(boolean partial) { isPartial = partial; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
