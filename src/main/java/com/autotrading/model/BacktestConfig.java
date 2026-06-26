package com.autotrading.model;

/** 백테스트 전용 전략 파라미터 — 실전 StrategyEngine 상수를 변경하지 않고 주입 */
public class BacktestConfig {

    // ── 모드 활성화 ──────────────────────────────────────────────────────
    public boolean enablePullback       = false;
    public boolean enableBreakout       = true;
    public boolean enableEarlyMomentum  = false;

    // ── 진입 시간 창 (KRX) ───────────────────────────────────────────────
    public int entryEndHour   = 13;
    public int entryEndMinute = 30;

    // ── 일일 손익 한도 ───────────────────────────────────────────────────
    public double maxDailyLossPct   = 3.0;
    public double maxDailyProfitPct = 5.0;

    // ── 공통 진입 조건 ──────────────────────────────────────────────────
    public int    minHistoryBars      = 30;
    public int    minHistoryMinutes   = 30;
    public double minPrice            = 1000.0;
    public double vwapHardLimitPct    = 8.0;
    public double minTurnoverKrx      = 50_000_000.0;
    public double minAvgTurnoverKrx   = 30_000_000.0;
    public double minTurnoverUs       = 10_000.0;
    public boolean useMarketFilter    = true;   // 시장 약세 시 전 모드 차단 (8연패 방지)
    public double volumeMult          = 1.5;
    public int    buyCooldownSec      = 60;
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
    public double vwapMaxGapBreakoutPct = 1.5;
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
    public double breakoutTpPct     = 2.0;   // ← 2.5: 2.5% 도달 전 역전 多, 2.0%로 복원
    public double breakoutTrailSt   = 1.8;
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
    public double  emergencyStopPct  = 3.0;
    public boolean useVwapBreak      = true;
    public double  vwapBreakBuffer   = 1.0;   // ← 0.5: 정상 되돌림을 손절 오인 방지
    public boolean useBreakevenGuard = true;
    public double  breakevenPeak     = 1.5;   // ← 2.0: 조기 본전 보호 강화
    public double  breakevenLoss     = -0.1;
    public boolean useFailedBreakout = true;
    public boolean useFailedPullback = true;
    public int     vwapBreakGraceSec = 480;   // ← 360: 조기 청산 방지
    public boolean useEodForceSell   = true;

    // ── 비용 조건 ────────────────────────────────────────────────────────
    public double slippagePct = 0.0;
    public double feePct      = 0.015;
    public double taxPct      = 0.18;

    // ── STRONG_PULLBACK 진입 조건 ────────────────────────────────────────
    public boolean enableStrongPullback  = true;
    public double  spPullbackMinPct      = 0.8;   // 최근 고점 대비 눌림 최소 (%) ← 1.2
    public double  spPullbackMaxPct      = 3.8;   // 최근 고점 대비 눌림 최대 (%) ← 3.5
    public double  spVwapMinAbovePct     = 0.2;   // VWAP 대비 최소 위치 (%)
    public double  spVol3RatioMax        = 0.7;   // 3봉 평균 < 10봉 평균 × 이 값 ← 0.8
    public double  spBodyRatioMin        = 0.4;   // 양봉 몸통 비율 최소 ← 0.5
    public int     spMinScore            = 83;    // 진입 최소 점수 ← 85

    // ── STRONG_PULLBACK 청산 ─────────────────────────────────────────────
    public double  spStopPct             = 1.8;   // 손절 (%)
    public double  spTpPct               = 3.0;   // 고정 익절 (%, 0 = 비활성)
    public double  spTrailSt             = 2.0;   // 트레일 시작 수익 (%)
    public double  spTrailDrop           = 0.8;   // 트레일 고점 대비 하락 (%)

    // ── VWAP_RECLAIM 진입 조건 ───────────────────────────────────────────
    public boolean enableVwapReclaim     = true;
    public int     vrLookbackBars        = 12;    // 최근 N봉 내 VWAP 이탈 확인
    public double  vrVolMult             = 2.0;   // 현재 거래량 >= 5봉 평균 × 이 값
    public int     vrMinAboveVwapBars    = 5;     // 연속 VWAP 위 봉 수
    public int     vrMinScore            = 87;    // 진입 최소 점수
    public int     vrMaxScore            = 88;    // 진입 최대 점수 (89점 PF 0.45 → 과열 직전 차단)

    // ── VWAP_RECLAIM 청산 ────────────────────────────────────────────────
    public double  vrStopPct             = 1.0;   // 손절 (%) ← 1.2: 설정-실제 편차 축소
    public double  vrTpPct               = 2.2;   // 고정 익절 (%, 0 = 비활성)
    public double  vrTrailSt             = 1.8;   // 트레일 시작 수익 (%)
    public double  vrTrailDrop           = 1.0;   // 트레일 고점 대비 하락 (%)

    // ── 재진입 제한 ──────────────────────────────────────────────────────
    public boolean blockSameDayAfterStop = true;

    // ── 진입 등급 필터 ───────────────────────────────────────────────────────
    public boolean blockSSGrade = true;   // SS등급(95-100) 차단
    public boolean blockSGrade  = true;   // S등급(90-94) 차단
    public boolean blockAGrade  = false;  // A등급(85-89) 허용
    public boolean blockBGrade  = false;  // B등급(80-84) 허용

    // ── 레거시 (하위 호환) ────────────────────────────────────────────────
    /** @deprecated per-mode stop/TP/trail 사용 권장; -1 = 미설정 */
    public double stopLossPct   = -1;
    public double takeProfitPct = -1;
    public double trailStartPct = -1;
    public double trailDropPct  = -1;

    // ── RSI_BOLLINGER_REBOUND 모드 ──────────────────────────────────────
    public boolean enableRsiBbRebound        = false;  // PF 0.62 (순손실 -2.34%) → 비활성
    public int     rsiBbMinScore             = 75;
    public int     rsiBbRsiPeriod            = 14;
    public int     rsiBbSignalPeriod         = 9;
    public int     rsiBbPeriod               = 20;
    public double  rsiBbStdMult              = 2.0;
    public double  rsiBbLowerTouchBufferPct  = 0.2;
    public double  rsiBbMaxBreakdownPct      = 0.5;
    public double  rsiBbMinVwapPct           = -0.5;
    public double  rsiBbRsiLowThreshold      = 45.0;

    // ── RSI_BOLLINGER_REBOUND 청산 ────────────────────────────────────────
    public double  rsiBbStopPct              = 1.2;   // ← 1.0: 손절 확대
    public double  rsiBbTpPct               = 1.6;   // ← 1.8: 빠른 익절
    public double  rsiBbTrailSt             = 1.2;   // ← 1.4
    public double  rsiBbTrailDrop           = 0.6;   // ← 0.7

    // ── OPENING_RANGE_BREAKOUT 진입 조건 ────────────────────────────────
    public boolean enableOpeningRangeBreakout = false;  // 백테스트 검증 후 활성화
    public double  orbStopPct                 = 1.8;   // 손절 (%)
    public double  orbTpPct                   = 2.2;   // 고정 익절 (%, 0 = 비활성)
    public double  orbTrailSt                 = 1.8;   // 트레일 시작 수익 (%)
    public double  orbTrailDrop               = 1.0;   // 트레일 고점 대비 하락 (%)
}
