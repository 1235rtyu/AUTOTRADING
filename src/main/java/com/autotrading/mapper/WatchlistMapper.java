package com.autotrading.mapper;

import com.autotrading.model.WatchlistItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WatchlistMapper {
    List<WatchlistItem> findAll();
    int insert(@Param("symbol") String symbol);
    int delete(@Param("id") int id);
}
