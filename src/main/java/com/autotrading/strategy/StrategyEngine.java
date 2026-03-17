package com.autotrading.strategy;

import com.autotrading.indicator.IndicatorService;
import com.autotrading.model.OrderCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StrategyEngine {

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngine.class);

    private final IndicatorService indicatorService;

    private static class SymbolState {
        List<Double> prices = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();
        double highestSinceEntry = 0;
    }

    private final Map<String, SymbolState> states = new ConcurrentHashMap<>();

    // EMA200 안정화를 위해 충분한 데이터 확보
    private final int minTicks = 210;

    private final int rsiPeriod = 14;
    private final int bbPeriod = 20;

    private final int maxHistory = 400;

    public StrategyEngine(IndicatorService indicatorService) {
        this.indicatorService = indicatorService;
    }

    private SymbolState state(String symbol) {
        return states.computeIfAbsent(symbol, s -> new SymbolState());
    }

    public void record(String symbol, double closePrice, double volume) {

        SymbolState st = state(symbol);

        st.prices.add(closePrice);
        st.volumes.add(volume);

        if (st.prices.size() > maxHistory) {
            st.prices.remove(0);
        }

        if (st.volumes.size() > maxHistory) {
            st.volumes.remove(0);
        }
    }

    public Optional<OrderCommand> decide(
            String symbol,
            double currentPrice,
            double currentVolume,
            int currentQuantity,
            double avgPrice
    ) {

        SymbolState st = state(symbol);

        record(symbol, currentPrice, currentVolume);

        if (st.prices.size() < minTicks) {
            logger.debug("Collecting data {} ({}/{})",
                    symbol, st.prices.size(), minTicks);
            return Optional.empty();
        }

        double rsi =
                indicatorService.calculateRSI(st.prices, rsiPeriod);

        double ema20 =
                indicatorService.calculateEMA(st.prices, 20);

        double ema50 =
                indicatorService.calculateEMA(st.prices, 50);

        double ema200 =
                indicatorService.calculateEMA(st.prices, 200);

        IndicatorService.BollingerBands bands =
                indicatorService.calculateBollingerBands(st.prices, bbPeriod, 2);

        double atr =
                indicatorService.calculateATR(st.prices, 14);

        double avgVolume =
                indicatorService.calculateAverage(st.volumes, 20);

        logger.debug(
                "symbol={} price={} rsi={} ema20={} ema50={} ema200={} volume={}",
                symbol, currentPrice, rsi, ema20, ema50, ema200, currentVolume
        );

        boolean uptrend =
                ema50 > ema200;

        boolean pullback =
                currentPrice <= ema20 * 1.01;

        boolean momentum =
                rsi > 40 && rsi < 60;

        boolean volumeSpike =
                currentVolume > avgVolume * 1.2;

        if (currentQuantity == 0 && uptrend && pullback && momentum && volumeSpike) {

            st.highestSinceEntry = currentPrice;

            logger.info(
                    "BUY {} price={} rsi={} ema50={} ema200={}",
                    symbol, currentPrice, rsi, ema50, ema200
            );

            return Optional.of(
                    new OrderCommand(symbol, 1, "BUY", currentPrice)
            );
        }

        if (currentQuantity > 0) {

            st.highestSinceEntry =
                    Math.max(st.highestSinceEntry, currentPrice);

            double profit =
                    (currentPrice - avgPrice) / avgPrice;

            double trailingStop =
                    st.highestSinceEntry - (atr * 2.5);

            double stopLoss =
                    avgPrice - (atr * 2);

            if (currentPrice <= stopLoss) {

                logger.info(
                        "STOP LOSS {} price={} avg={}",
                        symbol, currentPrice, avgPrice
                );

                return Optional.of(
                        new OrderCommand(symbol, currentQuantity, "SELL", currentPrice)
                );
            }

            if (profit >= 0.04) {

                logger.info(
                        "TAKE PROFIT 4% {} price={}",
                        symbol, currentPrice
                );

                return Optional.of(
                        new OrderCommand(symbol, currentQuantity, "SELL", currentPrice)
                );
            }

            if (profit >= 0.02 && currentPrice < trailingStop) {

                logger.info(
                        "TRAILING STOP {} price={}",
                        symbol, currentPrice
                );

                return Optional.of(
                        new OrderCommand(symbol, currentQuantity, "SELL", currentPrice)
                );
            }

            if (rsi >= 70) {

                logger.info(
                        "RSI EXIT {} rsi={} price={}",
                        symbol, rsi, currentPrice
                );

                return Optional.of(
                        new OrderCommand(symbol, currentQuantity, "SELL", currentPrice)
                );
            }
        }

        logger.debug(
                "No signal {} price={} rsi={} uptrend={} pullback={} volSpike={} pos={}",
                symbol, currentPrice, rsi, uptrend, pullback, volumeSpike, currentQuantity
        );

        return Optional.empty();
    }
}