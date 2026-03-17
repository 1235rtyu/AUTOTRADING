package com.autotrading.dao.impl;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.mapper.AutoTradingMapper;
import com.autotrading.model.AutoPosition;
import com.autotrading.model.OrderLog;
import com.autotrading.model.PriceLog;
import com.autotrading.model.StockQuote;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AutoTradingDaoImpl implements AutoTradingDao {
    private final AutoTradingMapper mapper;

    public AutoTradingDaoImpl(AutoTradingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void savePriceLog(String symbol, double price, double volume, LocalDateTime timestamp) {
        mapper.insertPriceLog(symbol, price, volume, timestamp);
    }

    @Override
    public void saveOrderLog(String symbol, String side, int quantity, double price, String reason) {
        mapper.insertOrderLog(symbol, side, quantity, price, reason);
    }

    @Override
    public void savePosition(String symbol, int quantity, double avgPrice) {
        mapper.upsertPosition(symbol, quantity, avgPrice);
    }

    @Override
    public AutoPosition findPosition(String symbol) {
        return mapper.findPosition(symbol);
    }

    @Override
    public void saveStrategyRecord(String symbol, String strategyType, String status) {
        mapper.insertStrategy(symbol, strategyType, status);
    }

    @Override
    public StockQuote findLastQuote(String symbol) {
        return mapper.findLastQuote(symbol);
    }

    @Override
    public List<OrderLog> findRecentOrders(int limit) {
        return mapper.findRecentOrders(limit);
    }

    @Override
    public List<PriceLog> findRecentPrices(int limit) {
        return mapper.findRecentPrices(limit);
    }

    @Override
    public int countWatchlist() {
        return mapper.countWatchlist();
    }

    @Override
    public int countPositions() {
        return mapper.countPositions();
    }
}
