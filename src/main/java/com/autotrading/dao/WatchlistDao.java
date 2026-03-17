package com.autotrading.dao;

import com.autotrading.model.WatchlistItem;

import java.util.List;

public interface WatchlistDao {
    List<WatchlistItem> findAll();
    int add(String symbol);
    int remove(int id);
}
