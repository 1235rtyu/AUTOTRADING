package com.autotrading.service;

import com.autotrading.strategy.StrategyEngine.EntrySnapshot;

import java.time.LocalDate;

public interface TradeHistoryService {

    /**
     * 매도 체결 확인 후 호출 — notifySellFilled() 호출 직전에 실행해야 함
     * (이후 엔진 상태가 초기화되므로 스냅샷 데이터가 사라짐)
     *
     * @param symbol       종목코드
     * @param market       "KRX" or "US"
     * @param snap         StrategyEngine에서 추출한 진입 스냅샷
     * @param avgPrice     브로커 평균단가 (ctx.avgPrice)
     * @param exitPrice    체결가 (ctx.referencePrice)
     * @param soldQty      실제 체결 수량
     * @param exitReason   청산 이유 (e.g. TRAIL_BREAKOUT)
     * @param isPartial    부분체결 포함 여부
     */
    void recordTrade(String symbol,
                     String market,
                     EntrySnapshot snap,
                     double avgPrice,
                     double exitPrice,
                     int soldQty,
                     String exitReason,
                     boolean isPartial);

    /**
     * 일별 집계 실행 — 장 마감 후 호출
     */
    void aggregateDailyStats(LocalDate tradeDate);
}
