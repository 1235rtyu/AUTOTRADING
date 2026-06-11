package com.autotrading.model;

/** 백테스트 전용 전략 파라미터 — 실전 StrategyEngine 상수를 변경하지 않고 주입 */
public class BacktestConfig {

    // ── 모드 활성화 ──────────────────────────────────────────────────────
    public boolean enablePullback       = true;
    public boolean enableBreakout       = true;
    public boolean enableEarlyMomentum  = false;

    // ── 공통 진입 조건 ──────────────────────────────────────────────────
    public int    minHistoryBars      = 30;
    public int    minHistoryMinutes   = 30;
    public double minPrice            = 1000.0;
    public double vwapHardLimitPct    = 8.0;
    public double minTurnoverKrx      = 50_000_000.0;
    public double minAvgTurnoverKrx   = 30_000_000.0;
    public double minTurnoverUs       = 10_000.0;
    public boolean useMarketFilter    = false;
    public double volumeMult          = 1.5;
    public int    buyCooldownSec      = 90;
    public int    maxDailyEntryCount  = 2;
    public int    maxSamePatternEntry = 1;

    // ── PULLBACK 진입 조건 ──────────────────────────────────────────────
    public int    pullbackMinScore      = 80;
    public double pullbackUpperPct      = 1.0;
    public double pullbackLowerPct      = 2.0;
    public double vwapMaxGapPullbackPct = 1.0;
    public double pullbackVolumeMult    = 1.0;
    public double pullbackVelocityShort = 0.0010;
    public double pullbackVelocityMid   = 0.0;
    public int    pullbackRequiredBullishBars        = 1;
    public boolean pullbackRequireAboveVwap          = true;
    public boolean pullbackRequireVwapSlope          = true;
    public boolean pullbackRequireRecentHighBreakout = false;

    // ── PULLBACK 청산 ───────────────────────────────────────────────────
    public double pullbackStopPct   = 2.3;
    public double pullbackTpPct     = 3.2;
    public double pullbackTrailSt   = 2.2;
    public double pullbackTrailDrop = 1.6;

    // ── BREAKOUT 진입 조건 ──────────────────────────────────────────────
    public int    breakoutMinScore      = 83;
    public double vwapMaxGapBreakoutPct = 2.2;
    public double breakoutRetestLower   = 1.0;
    public double breakoutRetestUpper   = 0.1;
    public double breakoutStrongVolMult = 2.0;
    public double breakoutKrxVolMult    = 1.8;   // KRX 돌파 최종 거래량 배수 (BREAKOUT_KRX_MIN_VOLUME_MULT)
    public boolean breakoutRequireAcceleration  = true;
    public boolean breakoutRequireMultiUptrend  = true;
    public boolean breakoutOverheatBlock        = true;
    public double breakoutMinVelocityMid       = 0.001;
    public double breakoutMinVelocityLong      = 0.0005;
    public int    breakoutRequiredBullishBars  = 2;

    // ── BREAKOUT 청산 ───────────────────────────────────────────────────
    public double breakoutStopPct   = 2.0;
    public double breakoutTpPct     = 3.0;
    public double breakoutTrailSt   = 2.3;
    public double breakoutTrailDrop = 1.5;

    // ── EARLY_MOMENTUM 진입 조건 ─────────────────────────────────────────
    public int    emMinScore   = 80;
    public double emVelocity   = 0.003;
    public double emVolumeMult = 2.0;
    public boolean em3TrendUp  = true;

    // ── EARLY_MOMENTUM 청산 ──────────────────────────────────────────────
    public double emStopPct = 2.0;
    public double emTpPct   = 2.2;

    // ── 공통 청산 조건 ──────────────────────────────────────────────────
    public double  emergencyStopPct  = 5.5;
    public boolean useVwapBreak      = true;
    public double  vwapBreakBuffer   = 0.5;
    public boolean useBreakevenGuard = true;
    public double  breakevenPeak     = 1.8;
    public double  breakevenLoss     = -0.3;
    public boolean useFailedBreakout = true;
    public boolean useFailedPullback = true;
    public int     vwapBreakGraceSec = 360;
    public boolean useEodForceSell   = true;

    // ── 비용 조건 ────────────────────────────────────────────────────────
    public double slippagePct = 0.0;
    public double feePct      = 0.015;
    public double taxPct      = 0.18;

    // ── STRONG_PULLBACK 진입 조건 ────────────────────────────────────────
    public boolean enableStrongPullback  = true;
    public double  spPullbackMinPct      = 1.2;   // 최근 고점 대비 눌림 최소 (%)
    public double  spPullbackMaxPct      = 2.5;   // 최근 고점 대비 눌림 최대 (%)
    public double  spVwapMinAbovePct     = 0.2;   // VWAP 대비 최소 위치 (%)
    public double  spVol3RatioMax        = 0.6;   // 3봉 평균 < 10봉 평균 × 이 값
    public double  spBodyRatioMin        = 0.6;   // 양봉 몸통 비율 최소
    public int     spMinScore            = 85;    // 진입 최소 점수

    // ── STRONG_PULLBACK 청산 ─────────────────────────────────────────────
    public double  spStopPct             = 1.8;   // 손절 (%)
    public double  spTpPct               = 3.0;   // 고정 익절 (%, 0 = 비활성)
    public double  spTrailSt             = 2.0;   // 트레일 시작 수익 (%)
    public double  spTrailDrop           = 0.8;   // 트레일 고점 대비 하락 (%)

    // ── VWAP_RECLAIM 진입 조건 ───────────────────────────────────────────
    public boolean enableVwapReclaim     = true;
    public int     vrLookbackBars        = 12;    // 최근 N봉 내 VWAP 이탈 확인
    public double  vrVolMult             = 2.0;   // 현재 거래량 >= 5봉 평균 × 이 값
    public int     vrMinAboveVwapBars    = 3;     // 연속 VWAP 위 봉 수
    public int     vrMinScore            = 80;    // 진입 최소 점수

    // ── VWAP_RECLAIM 청산 ────────────────────────────────────────────────
    public double  vrStopPct             = 1.5;   // 손절 (%)
    public double  vrTpPct               = 2.0;   // 고정 익절 (%, 0 = 비활성)
    public double  vrTrailSt             = 1.5;   // 트레일 시작 수익 (%)
    public double  vrTrailDrop           = 1.0;   // 트레일 고점 대비 하락 (%)

    // ── 진입 등급 필터 ───────────────────────────────────────────────────────
    public boolean blockSGrade = true;   // S등급(90-94) 진입 차단
    public boolean blockAGrade = false;  // A등급(85-89) 진입 차단

    // ── 레거시 (하위 호환) ────────────────────────────────────────────────
    /** @deprecated per-mode stop/TP/trail 사용 권장; -1 = 미설정 */
    public double stopLossPct   = -1;
    public double takeProfitPct = -1;
    public double trailStartPct = -1;
    public double trailDropPct  = -1;
}
