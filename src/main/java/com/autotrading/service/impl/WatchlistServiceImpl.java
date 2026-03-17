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
        if (!StringUtils.hasText(symbol)) {
            return;
        }
        watchlistDao.add(symbol.trim());
    }

    @Override
    public void remove(int id) {
        watchlistDao.remove(id);
    }
}
