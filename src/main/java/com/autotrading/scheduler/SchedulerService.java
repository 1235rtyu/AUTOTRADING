package com.autotrading.scheduler;

import com.autotrading.market.MarketDataService;
import com.autotrading.order.OrderService;
import com.autotrading.position.PositionService;
import com.autotrading.dao.AutoTradingDao;
import com.autotrading.model.AutoPosition;
import com.autotrading.service.RiskManager;
import com.autotrading.strategy.StrategyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final MarketDataService marketDataService;
    private final StrategyEngine strategyEngine;
    private final OrderService orderService;
    private final PositionService positionService;
    private final AutoTradingDao autoTradingDao;
    private final RiskManager riskManager;

    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    public SchedulerService(MarketDataService marketDataService,
                            StrategyEngine strategyEngine,
                            OrderService orderService,
                            PositionService positionService,
                            AutoTradingDao autoTradingDao,
                            RiskManager riskManager) {
        this.marketDataService = marketDataService;
        this.strategyEngine = strategyEngine;
        this.orderService = orderService;
        this.positionService = positionService;
        this.autoTradingDao = autoTradingDao;
        this.riskManager = riskManager;
    }

    public synchronized String start(String symbol) {
        if (timers.containsKey(symbol)) {
            return "Already Running: " + symbol;
        }
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    execute(symbol);
                } catch (Exception e) {
                    logger.error("Strategy execution exception for {}", symbol, e);
                }
            }
        }, 0, 10_000); // 가격 수집 주기: 10초
        timers.put(symbol, timer);
        logger.info("Auto-trading scheduler started for {}", symbol);
        return "Started " + symbol;
    }

    private void execute(String symbol) {
        if (riskManager.hasHitLossLimit()) {
            logger.warn("Daily loss limit reached; stopping trading");
            stop();
            return;
        }

        var quote = marketDataService.fetchPrice(symbol);
        autoTradingDao.savePriceLog(symbol, quote.getPrice(), quote.getVolume(), quote.getTimestamp());

        AutoPosition position = positionService.getPosition(symbol);
        int quantity = 0;
        double avgPrice = 0;
        if (position != null) {
            quantity = position.getQuantity();
            avgPrice = position.getAvgPrice();
        }

        strategyEngine.record(symbol, quote.getPrice(), quote.getVolume());
        var orderOpt = strategyEngine.decide(symbol, quote.getPrice(), quote.getVolume(), quantity, avgPrice);
        if (orderOpt.isPresent()) {
            if (!riskManager.allowOrder(symbol)) {
                logger.warn("Order blocked by risk manager for {}", symbol);
                return;
            }
            var command = orderOpt.get();
            var resp = orderService.placeOrder(command);
            autoTradingDao.saveOrderLog(symbol, command.getType(), command.getQuantity(), command.getPrice(), "Strategy");
            if ("BUY".equals(command.getType()) && resp.get("status").equals("ACCEPTED")) {
                int newQty = quantity + command.getQuantity();
                double newAvg = newQty == 0 ? 0 : ((avgPrice * quantity) + command.getPrice() * command.getQuantity()) / newQty;
                positionService.updatePosition(symbol, newQty, newAvg);
            } else if ("SELL".equals(command.getType()) && resp.get("status").equals("ACCEPTED")) {
                int newQty = Math.max(0, quantity - command.getQuantity());
                positionService.updatePosition(symbol, newQty, newQty == 0 ? 0 : avgPrice);
                double pnl = (command.getPrice() - avgPrice) * command.getQuantity();
                if (pnl < 0) riskManager.addLoss(Math.abs(pnl));
            }
            riskManager.orderCompleted(symbol);
        }
    }

    public synchronized String stop() {
        if (timers.isEmpty()) {
            return "Not Running";
        }
        timers.values().forEach(Timer::cancel);
        timers.clear();
        logger.info("Auto-trading scheduler stopped (all symbols)");
        return "Stopped";
    }

    public String status() {
        return timers.isEmpty() ? "STOPPED" : "RUNNING (" + String.join(",", timers.keySet()) + ")";
    }
}
