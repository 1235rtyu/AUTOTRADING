package com.autotrading.service.impl;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.model.OrderLog;
import com.autotrading.service.HistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryServiceImpl implements HistoryService {
    private final AutoTradingDao autoTradingDao;

    public HistoryServiceImpl(AutoTradingDao autoTradingDao) {
        this.autoTradingDao = autoTradingDao;
    }

    @Override
    public List<OrderLog> getRecentOrders(int limit) {
        return autoTradingDao.findRecentOrders(limit);
    }

    @Override
    public List<OrderLog> getRecentKrOrders(int limit) {
        return autoTradingDao.findRecentKrOrders(limit);
    }

    @Override
    public List<OrderLog> getRecentUsOrders(int limit) {
        return autoTradingDao.findRecentUsOrders(limit);
    }
}
