package com.autotrading.service.impl;

import com.autotrading.mapper.TradeHistoryMapper;
import com.autotrading.model.TradeHistory;
import com.autotrading.service.TradeHistoryService;
import com.autotrading.strategy.StrategyEngine.EntrySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TradeHistoryServiceImpl implements TradeHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TradeHistoryServiceImpl.class);

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final ZoneId NY_ZONE  = ZoneId.of("America/New_York");

    // KRX: 수수료 0.015% + 증권거래세 0.18% ≈ 0.20%
    private static final double FEE_RATE_KRX = 0.0020;
    // US: 매수/매도 합산 약 0.25% (증권사별 상이)
    private static final double FEE_RATE_US  = 0.0025;

    private final TradeHistoryMapper tradeHistoryMapper;

    public TradeHistoryServiceImpl(TradeHistoryMapper tradeHistoryMapper) {
        this.tradeHistoryMapper = tradeHistoryMapper;
    }

    @Override
    public void recordTrade(String symbol,
                            String market,
                            EntrySnapshot snap,
                            double avgPrice,
                            double exitPrice,
                            int soldQty,
                            String exitReason,
                            boolean isPartial) {
        if (snap == null || avgPrice <= 0.0 || exitPrice <= 0.0 || soldQty <= 0) {
            logger.warn("TRADE_HISTORY skip {} — invalid params snap={} avgPrice={} exitPrice={} soldQty={}",
                    symbol, snap, avgPrice, exitPrice, soldQty);
            return;
        }

        try {
            long nowMs   = System.currentTimeMillis();
            ZoneId zone  = "KRX".equals(market) ? KST_ZONE : NY_ZONE;
            double feeRate = "KRX".equals(market) ? FEE_RATE_KRX : FEE_RATE_US;

            double pnlPct    = (exitPrice - avgPrice) / avgPrice;
            double feeAmount = exitPrice * soldQty * feeRate;
            double pnlAmount = (exitPrice - avgPrice) * soldQty - feeAmount;

            double peakPnlPct = snap.highestSinceEntry > 0.0 && avgPrice > 0.0
                    ? (snap.highestSinceEntry - avgPrice) / avgPrice
                    : 0.0;

            // weightedPnl: 1주문 기준금액(buyAmountPerOrder) 대비 실현 금액 비율로 스케일링 (엔진과 동일 공식)
            double soldNotional = avgPrice * soldQty;
            double orderBase    = snap.buyAmountPerOrder > 0.0 ? snap.buyAmountPerOrder : soldNotional;
            double weightedPnl  = pnlPct * (soldNotional / orderBase);

            int holdSeconds = snap.entryTimeMs > 0
                    ? (int) Math.max(0, (nowMs - snap.entryTimeMs) / 1000L)
                    : 0;

            LocalDateTime entryTime = snap.entryTimeMs > 0
                    ? Instant.ofEpochMilli(snap.entryTimeMs).atZone(zone).toLocalDateTime()
                    : null;
            LocalDateTime exitTime  = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDateTime();
            LocalDate     tradeDate = exitTime.toLocalDate();

            TradeHistory rec = new TradeHistory();
            rec.setTradeDate(tradeDate);
            rec.setSymbol(symbol);
            rec.setMarket(market);
            rec.setEntryMode(snap.entryMode);
            rec.setEntryPrice(avgPrice);
            rec.setEntryQty(snap.entryQty > 0 ? snap.entryQty : soldQty);
            rec.setEntryTime(entryTime);
            rec.setEntrySignalScore(snap.signalScore > 0 ? snap.signalScore : null);
            rec.setEntryVwapDistPct(snap.vwapDistPct != 0.0 ? snap.vwapDistPct : null);
            rec.setEntryVelocitySht(snap.velocityShort != 0.0 ? snap.velocityShort : null);
            rec.setExitPrice(exitPrice);
            rec.setExitQty(soldQty);
            rec.setExitTime(exitTime);
            rec.setExitReason(exitReason != null ? exitReason : "UNKNOWN");
            rec.setExitType(resolveExitType(exitReason));
            rec.setHoldSeconds(holdSeconds);
            rec.setPnlAmount(round2(pnlAmount));
            rec.setPnlPct(pnlPct);
            rec.setWeightedPnl(weightedPnl);
            rec.setPeakPnlPct(snap.highestSinceEntry > 0.0 ? peakPnlPct : null);
            rec.setFeeAmount(round2(feeAmount));
            rec.setSlippagePct(0.0);
            rec.setPartial(isPartial);

            tradeHistoryMapper.insertTradeHistory(rec);

            logger.info("TRADE_HISTORY saved {} mode={} pnl={}% hold={}s exitReason={}",
                    symbol, snap.entryMode,
                    String.format("%.3f", pnlPct * 100),
                    holdSeconds, exitReason);

        } catch (Exception e) {
            logger.error("TRADE_HISTORY insert failed for {} — {}", symbol, e.getMessage(), e);
        }
    }

    @Override
    public void aggregateDailyStats(LocalDate tradeDate) {
        try {
            tradeHistoryMapper.aggregateDailyStats(tradeDate);
            logger.info("TRADE_STATS aggregated for {}", tradeDate);
        } catch (Exception e) {
            logger.error("TRADE_STATS aggregation failed for {} — {}", tradeDate, e.getMessage(), e);
        }
    }

    private String resolveExitType(String reason) {
        if (reason == null) return "NONE";
        if (reason.startsWith("TAKE_PROFIT_"))                           return "PROFIT";
        if (reason.startsWith("TRAIL_") || "BREAKEVEN_GUARD".equals(reason)) return "TRAIL";
        if (reason.startsWith("STOP_LOSS_") || "EMERGENCY_STOP".equals(reason)
                || "FAILED_BREAKOUT".equals(reason) || "FAILED_PULLBACK".equals(reason)
                || "EARLY_MOMENTUM_DEAD".equals(reason)
                || "VWAP_BREAK".equals(reason))                          return "STOPLOSS";
        if (reason.startsWith("TIME_STOP") || "EOD_FORCE_SELL".equals(reason)
                || "SELL_TIMEOUT_MARKET_RETRY".equals(reason))           return "TIMESTOP";
        return "NONE";
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
