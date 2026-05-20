package com.autotrading.mapper;

import com.autotrading.model.TradeHistory;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface TradeHistoryMapper {

    void insertTradeHistory(TradeHistory tradeHistory);

    List<TradeHistory> findByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    List<TradeHistory> findBySymbolAndDateRange(@Param("symbol") String symbol,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

    void aggregateDailyStats(@Param("tradeDate") LocalDate tradeDate);
}
