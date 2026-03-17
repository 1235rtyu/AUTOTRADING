package com.autotrading.model;

import java.util.List;

public class DashboardData {
    private String status;
    private int watchlistCount;
    private int positionCount;
    private List<OrderLog> recentOrders;
    private List<PriceLog> recentPrices;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getWatchlistCount() { return watchlistCount; }
    public void setWatchlistCount(int watchlistCount) { this.watchlistCount = watchlistCount; }
    public int getPositionCount() { return positionCount; }
    public void setPositionCount(int positionCount) { this.positionCount = positionCount; }
    public List<OrderLog> getRecentOrders() { return recentOrders; }
    public void setRecentOrders(List<OrderLog> recentOrders) { this.recentOrders = recentOrders; }
    public List<PriceLog> getRecentPrices() { return recentPrices; }
    public void setRecentPrices(List<PriceLog> recentPrices) { this.recentPrices = recentPrices; }
}
