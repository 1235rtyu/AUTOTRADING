package com.autotrading.service.impl;

import com.autotrading.dao.WatchlistDao;
import com.autotrading.model.WatchlistItem;
import com.autotrading.service.WatchlistService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WatchlistServiceImpl implements WatchlistService {
    private final WatchlistDao watchlistDao;

    public WatchlistServiceImpl(WatchlistDao watchlistDao) {
        this.watchlistDao = watchlistDao;
    }

    @Override
    public List<WatchlistItem> getWatchlist() {
        return watchlistDao.findAll();
    }

    @Override
    public void addSymbol(String symbol) {
        addSymbol(symbol, null);
    }

    @Override
    public void addSymbol(String symbol, String exchange) {
        if (!StringUtils.hasText(symbol)) {
            return;
        }
        String normalizedSymbol = symbol.trim().toUpperCase();
        String normalizedExchange = normalizeExchange(normalizedSymbol, exchange);
        // Prevent duplicate rows for the same symbol (case-insensitive by normalization).
        WatchlistItem exists = watchlistDao.findBySymbol(normalizedSymbol);
        if (exists != null) {
            return;
        }
        watchlistDao.add(normalizedSymbol, normalizedExchange);
    }

    @Override
    public String getExchangeHint(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return null;
        }
        WatchlistItem item = watchlistDao.findBySymbol(symbol.trim().toUpperCase());
        if (item == null || !StringUtils.hasText(item.getExchange())) {
            return null;
        }
        return item.getExchange().trim().toUpperCase();
    }

    @Override
    public void remove(int id) {
        watchlistDao.remove(id);
    }

    private String normalizeExchange(String symbol, String exchange) {
        if (StringUtils.hasText(exchange)) {
            String upper = exchange.trim().toUpperCase();
            if (upper.startsWith("NY")) return "NYS";
            if (upper.startsWith("AM")) return "AMS";
            if (upper.startsWith("NA")) return "NAS";
        }
        return isOverseas(symbol) ? "NAS" : "KRX";
    }

    private boolean isOverseas(String symbol) {
        return StringUtils.hasText(symbol) && Character.isLetter(symbol.charAt(0));
    }
}
