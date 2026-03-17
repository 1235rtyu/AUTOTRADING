package com.autotrading.dao;

import com.autotrading.model.AutoPosition;
import com.autotrading.model.OrderLog;
import com.autotrading.model.PriceLog;
import com.autotrading.model.StockQuote;

import java.time.LocalDateTime;
import java.util.List;

public interface AutoTradingDao {
    void savePriceLog(String symbol, double price, double volume, LocalDateTime timestamp);
    void saveOrderLog(String symbol, String side, int quantity, double price, String reason);
    void savePosition(String symbol, int quantity, double avgPrice);
    AutoPosition findPosition(String symbol);
    void saveStrategyRecord(String symbol, String strategyType, String status);
    StockQuote findLastQuote(String symbol);
    List<OrderLog> findRecentOrders(int limit);
    List<PriceLog> findRecentPrices(int limit);
    int countWatchlist();
    int countPositions();
}
