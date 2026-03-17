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
    public int add(String symbol) {
        return mapper.insert(symbol);
    }

    @Override
    public int remove(int id) {
        return mapper.delete(id);
    }
}
