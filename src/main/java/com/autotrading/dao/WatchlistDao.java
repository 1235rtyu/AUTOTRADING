package com.autotrading.dao;

import com.autotrading.model.WatchlistItem;

import java.util.List;

public interface WatchlistDao {
    List<WatchlistItem> findAll();
    WatchlistItem findBySymbol(String symbol);
    int add(String symbol, String exchange, String folder);
    int setFolder(int id, String folder);
    int clearFolder(String folder);
    int remove(int id);
}
