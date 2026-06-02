package com.autotrading.model;

/** 백테스트 전용 전략 파라미터 — 실전 StrategyEngine 상수를 변경하지 않고 주입 */
public class BacktestConfig {

    // ── 진입 조건 ───────────────────────────────────────────────────
    public int    pullbackMinScore      = 80;          // PULLBACK 최소 점수
    public int    breakoutMinScore      = 78;          // BREAKOUT 최소 점수 (KRX 기준)
    public double vwapMaxGapBreakoutPct = 2.2;         // BREAKOUT VWAP 이격 상한 %
    public double vwapMaxGapPullbackPct = 1.0;         // PULLBACK VWAP 이격 상한 %
    public double pullbackUpperPct      = 1.0;         // 고점 대비 상단 한계 % (PULLBACK_UPPER_FROM_HIGH = 0.990)
    public double pullbackLowerPct      = 2.0;         // 고점 대비 하단 한계 % (PULLBACK_LOWER_FROM_HIGH = 0.980)
    public double volumeMult            = 1.5;         // 거래량 배수 (평균 대비, MOMENTUM_VOLUME_MULT)
    public double minTurnoverKrx        = 50_000_000.0; // 최소 거래대금 KRX (원)
    public double minTurnoverUs         = 10_000.0;     // 최소 거래대금 US (USD)

    // ── 청산 조건 ───────────────────────────────────────────────────
    public double stopLossPct       = 2.3;   // 손절 % (STOP_KRX_PULLBACK=0.977 → 2.3%)
    public double takeProfitPct     = 3.2;   // 익절 % (TP_KRX_PULLBACK=1.032 → 3.2%)
    public double trailStartPct     = 2.2;   // 트레일 시작 % (TRAIL_START_KRX_PULLBACK=0.022)
    public double trailDropPct      = 1.6;   // 트레일 고점 하락 % (TRAIL_DROP_KRX_PULLBACK=0.016)
    public int    vwapBreakGraceSec = 360;    // VWAP 이탈 유예 시간 초 (VWAP_BREAK_GRACE_MS=360000)
    public int    softTimeStopSec   = 1200;  // 소프트 타임스탑 초 (20분)
    public int    midTimeStopSec    = 2400;  // 중간 타임스탑 초 (40분)
    public int    hardTimeStopSec   = 5400;  // 하드 타임스탑 초 (90분)

    // ── 운영 조건 ───────────────────────────────────────────────────
    public int    maxDailyEntryCount  = 2;     // 하루 최대 진입횟수 (MAX_DAILY_ENTRY_COUNT)
    public int    maxSamePatternEntry = 1;     // 동일 패턴 재진입 제한 (MAX_SAME_PATTERN_ENTRY_COUNT)
    public double slippagePct         = 0.0;   // 슬리피지 % (one-way, PnL 차감)
    public double feePct              = 0.015; // 수수료+세금 % (KRX 약 0.015%, US 약 0.01%)
}
