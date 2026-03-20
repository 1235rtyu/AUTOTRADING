package com.autotrading.service;

import com.autotrading.model.WatchlistItem;

import java.util.List;

public interface WatchlistService {
    List<WatchlistItem> getWatchlist();
    void addSymbol(String symbol);
    void addSymbol(String symbol, String exchange);
    String getExchangeHint(String symbol);
    void remove(int id);
}
