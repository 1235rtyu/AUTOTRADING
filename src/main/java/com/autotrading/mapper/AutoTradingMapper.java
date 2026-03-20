package com.autotrading.mapper;

import com.autotrading.model.AutoPosition;
import com.autotrading.model.DailyProfitPoint;
import com.autotrading.model.MonitorAggregateRow;
import com.autotrading.model.OrderLog;
import com.autotrading.model.PriceLog;
import com.autotrading.model.StockQuote;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AutoTradingMapper {
    void insertPriceLog(@Param("symbol") String symbol,
                        @Param("symbolName") String symbolName,
                        @Param("price") double price,
                        @Param("volume") double volume,
                        @Param("createdAt") LocalDateTime createdAt);

    void insertOrderLog(@Param("symbol") String symbol,
                        @Param("side") String side,
                        @Param("quantity") int quantity,
                        @Param("price") double price,
                        @Param("reason") String reason);

    void upsertPosition(@Param("symbol") String symbol,
                        @Param("symbolName") String symbolName,
                        @Param("quantity") int quantity,
                        @Param("avgPrice") double avgPrice);

    AutoPosition findPosition(@Param("symbol") String symbol);

    void insertStrategy(@Param("symbol") String symbol,
                        @Param("strategyType") String strategyType,
                        @Param("status") String status);

    StockQuote findLastQuote(@Param("symbol") String symbol);

    List<OrderLog> findRecentOrders(@Param("limit") int limit);
    List<OrderLog> findRecentKrOrders(@Param("limit") int limit);
    List<OrderLog> findRecentUsOrders(@Param("limit") int limit);

    List<PriceLog> findRecentPrices(@Param("limit") int limit);

    int countWatchlist();

    int countPositions();

    MonitorAggregateRow findMonitorAggregate(@Param("market") String market);

    Double findTodaySignedAmount(@Param("market") String market);

    List<DailyProfitPoint> findDailySignedAmounts(@Param("market") String market,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt);
}
