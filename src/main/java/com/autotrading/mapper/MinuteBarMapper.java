package com.autotrading.mapper;

import com.autotrading.model.MinuteBar;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MinuteBarMapper {

    void batchInsert(@Param("list") List<MinuteBar> bars);

    List<MinuteBar> findByRange(@Param("market")  String market,
                                 @Param("symbol")  String symbol,
                                 @Param("start")   LocalDateTime start,
                                 @Param("end")     LocalDateTime end);

    int countByRange(@Param("market")  String market,
                     @Param("symbol")  String symbol,
                     @Param("start")   LocalDateTime start,
                     @Param("end")     LocalDateTime end);
}
