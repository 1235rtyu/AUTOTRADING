package com.autotrading.position;

import com.autotrading.dao.AutoTradingDao;
import com.autotrading.model.AutoPosition;
import org.springframework.stereotype.Service;

@Service
public class PositionService {
    private final AutoTradingDao autoTradingDao;

    public PositionService(AutoTradingDao autoTradingDao) {
        this.autoTradingDao = autoTradingDao;
    }

    public void updatePosition(String symbol, String symbolName, int quantity, double avgPrice) {
        autoTradingDao.savePosition(symbol, symbolName, quantity, avgPrice);
    }

    public void updatePosition(String symbol, int quantity, double avgPrice) {
        updatePosition(symbol, null, quantity, avgPrice);
    }

    public AutoPosition getPosition(String symbol) {
        return autoTradingDao.findPosition(symbol);
    }
}
