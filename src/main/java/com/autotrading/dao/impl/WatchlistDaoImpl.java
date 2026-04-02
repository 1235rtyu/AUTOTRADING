package com.autotrading.dao.impl;

import com.autotrading.dao.WatchlistDao;
import com.autotrading.mapper.WatchlistMapper;
import com.autotrading.model.WatchlistItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WatchlistDaoImpl implements WatchlistDao {
    private final WatchlistMapper mapper;

    public WatchlistDaoImpl(WatchlistMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<WatchlistItem> findAll() {
        return mapper.findAll();
    }

    @Override
    public WatchlistItem findBySymbol(String symbol) {
        return mapper.findBySymbol(symbol);
    }

    @Override
    public int add(String symbol, String exchange, String folder) {
        return mapper.insert(symbol, exchange, folder);
    }

    @Override
    public int setFolder(int id, String folder) {
        return mapper.updateFolder(id, folder);
    }

    @Override
    public int clearFolder(String folder) {
        return mapper.clearFolder(folder);
    }

    @Override
    public int remove(int id) {
        return mapper.delete(id);
    }
}
