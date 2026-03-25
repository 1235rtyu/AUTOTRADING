package com.autotrading.dao.impl;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.mapper.AutoTradingMapper;
import com.autotrading.model.AutoPosition;
import com.autotrading.model.DailyProfitPoint;
import com.autotrading.model.MonitorAggregateRow;
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
    public void savePriceLog(String symbol, String symbolName, double price, double volume, LocalDateTime timestamp) {
        mapper.insertPriceLog(symbol, symbolName, price, volume, timestamp);
    }

    @Override
    public void saveOrderLog(String symbol, String side, int quantity, double price, String reason) {
        mapper.insertOrderLog(symbol, side, quantity, price, reason);
    }

    @Override
    public void savePosition(String symbol, String symbolName, int quantity, double avgPrice) {
        mapper.upsertPosition(symbol, symbolName, quantity, avgPrice);
    }

    @Override
    public AutoPosition findPosition(String symbol) {
        return mapper.findPosition(symbol);
    }

    @Override
    public List<AutoPosition> findAllPositions() {
        return mapper.findAllPositions();
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
    public List<OrderLog> findRecentKrOrders(int limit) {
        return mapper.findRecentKrOrders(limit);
    }

    @Override
    public List<OrderLog> findRecentUsOrders(int limit) {
        return mapper.findRecentUsOrders(limit);
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

    @Override
    public MonitorAggregateRow findMonitorAggregate(String market) {
        return mapper.findMonitorAggregate(market);
    }

    @Override
    public double findTodaySignedAmount(String market) {
        Double value = mapper.findTodaySignedAmount(market);
        return value == null ? 0.0 : value;
    }

    @Override
    public List<DailyProfitPoint> findDailySignedAmounts(String market, LocalDateTime startAt, LocalDateTime endAt) {
        return mapper.findDailySignedAmounts(market, startAt, endAt);
    }
}
