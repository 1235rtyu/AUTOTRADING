package com.autotrading.dao;

import com.autotrading.model.WatchlistItem;

import java.util.List;

public interface WatchlistDao {
    List<WatchlistItem> findAll();
    WatchlistItem findBySymbol(String symbol);
    int add(String symbol, String exchange);
    int remove(int id);
}
