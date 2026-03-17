package com.autotrading.service.impl;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.model.DashboardData;
import com.autotrading.service.AutoTradingService;
import com.autotrading.service.DashboardService;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final AutoTradingDao autoTradingDao;
    private final AutoTradingService autoTradingService;

    public DashboardServiceImpl(AutoTradingDao autoTradingDao, AutoTradingService autoTradingService) {
        this.autoTradingDao = autoTradingDao;
        this.autoTradingService = autoTradingService;
    }

    @Override
    public DashboardData load(int limit) {
        DashboardData data = new DashboardData();
        data.setStatus(autoTradingService.status());
        data.setWatchlistCount(autoTradingDao.countWatchlist());
        data.setPositionCount(autoTradingDao.countPositions());
        data.setRecentOrders(autoTradingDao.findRecentOrders(limit));
        data.setRecentPrices(autoTradingDao.findRecentPrices(limit));
        return data;
    }
}
