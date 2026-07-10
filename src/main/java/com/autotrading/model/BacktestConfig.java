package com.autotrading.model;

/** 백테스트 전용 전략 파라미터 — 실전 StrategyEngine 상수를 변경하지 않고 주입 */
public class BacktestConfig {

    // ── 모드 활성화 ──────────────────────────────────────────────────────
    public boolean enablePullback       = false;
    public boolean enableBreakout       = true;
    public boolean enableEarlyMomentum  = false;

    // ── 진입 시간 창 (KRX) ───────────────────────────────────────────────
    public int entryEndHour   = 15;   // ← 10: BREAKOUT 15:20까지 허용 (ORB는 10:00 하드코딩으로 별도 관리)
    public int entryEndMinute = 20; // ← 30: 실전과 동일하게 15:20 맞춤
    public int slowModeEntryEndHour   = 13;  // ← 12: 12:00 차단 시 11~12시 손실거래 유지+12~13시 양호 거래 제거 → 역효과 확인
    public int slowModeEntryEndMinute = 0;

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
    public int    maxSamePatternEntry = 3;

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
    public int    breakoutMinScore      = 60;    // D등급 하한 (← 80: maxScore=73과 충돌 버그 수정, D등급 60~73 허용)
    public int    breakoutMaxScore      = 73;    // D등급 전용 (← 0: ORB와 동일 등급 구조, A등급 역설 해소)
    public double vwapMaxGapBreakoutPct = 1.0;   // ← 1.5: VWAP 이격 상한 축소 (너무 연장된 진입 차단)
    public double breakoutRetestLower   = 1.0;
    public double breakoutRetestUpper   = 0.1;
    public double breakoutStrongVolMult = 2.0;
    public double breakoutKrxVolMult    = 1.4;   // ← 2.0: 돌파 직전 거래량 감소 특성 반영 (1건→다수 목표)
    public boolean breakoutRequireAcceleration  = true;
    public boolean breakoutRequireMultiUptrend  = false; // ← true: MA 2개 이상 상승으로 완화 (3중 조건에서 1건만 발생)
    public boolean breakoutOverheatBlock        = true;
    public double breakoutMinVelocityMid       = 0.0; // trendScore≥2 방향성으로 대체
    public double breakoutMinVelocityLong      = 0.0;
    public int    breakoutRequiredBullishBars  = 2;

    // ── BREAKOUT 청산 ───────────────────────────────────────────────────
    public double breakoutStopPct   = 1.5;   // 유지
    public double breakoutTpPct     = 2.0;   // 유지
    public double breakoutTrailSt   = 1.3;   // ← 1.5: 조기 보호 (TRAIL 2건 -0.02% 본전 탈출)
    public double breakoutTrailDrop = 0.8;   // ← 1.2: 수익 보호 강화 (진입 품질 상승 시 더 달리기)

    // ── EARLY_MOMENTUM 진입 조건 ─────────────────────────────────────────
    public int    emMinScore   = 80;
    public double emVelocity   = 0.003;
    public double emVolumeMult = 2.0;
    public boolean em3TrendUp  = true;

    // ── EARLY_MOMENTUM 청산 ──────────────────────────────────────────────
    public double emStopPct = 2.0;
    public double emTpPct   = 2.2;

    // ── 공통 청산 조건 ──────────────────────────────────────────────────
    public double  emergencyStopPct  = 2.5;   // ← 3.0: 4연패 손실폭 압축
    public boolean useVwapBreak      = true;
    public double  vwapBreakBuffer   = 0.5;   // ← 1.0: VWAP_BREAK 6건 전손 → 버퍼 축소로 조기 청산
    public boolean useBreakevenGuard = true;
    public double  breakevenPeak     = 1.5;   // ← 2.0: 조기 본전 보호 강화
    public double  breakevenLoss     = 0.3;
    public boolean useFailedBreakout = true;
    public boolean useFailedPullback = true;
    public int     vwapBreakGraceSec = 360;   // ← 300: VWAP 이탈 후 6분 유예 (UI 기본값도 동기화)
    public boolean useEodForceSell   = true;

    // ── 비용 조건 ────────────────────────────────────────────────────────
    public double slippagePct = 0.0;
    public double feePct      = 0.015;
    public double taxPct      = 0.18;

    // ── STRONG_PULLBACK 진입 조건 ────────────────────────────────────────
    public boolean enableStrongPullback  = true;
    public double  spPullbackMinPct      = 0.2;   // 최근 고점 대비 눌림 최소 (%) ← 0.5: 상승장 마이크로 눌림 허용
    public double  spPullbackMaxPct      = 3.8;   // 최근 고점 대비 눌림 최대 (%) ← 3.5
    public double  spVwapMinAbovePct     = 0.2;   // VWAP 대비 최소 위치 (%)
    public double  spVol3RatioMax        = 0.85;  // 3봉 평균 < 10봉 평균 × 이 값 ← 0.7
    public double  spBodyRatioMin        = 0.4;   // 양봉 몸통 비율 최소 ← 0.5
    public int     spMinScore            = 76;    // ← 72: 저품질 눌림 신호 제거

    // ── STRONG_PULLBACK 청산 ─────────────────────────────────────────────
    public double  spStopPct             = 1.5;   // ← 1.8: 빠른 손절로 손실 확대 방지
    public double  spTpPct               = 3.0;   // 고정 익절 (%, 0 = 비활성)
    public double  spTrailSt             = 2.0;   // 트레일 시작 수익 (%)
    public double  spTrailDrop           = 0.8;   // 트레일 고점 대비 하락 (%)

    // ── VWAP_RECLAIM 진입 조건 ───────────────────────────────────────────
    public boolean enableVwapReclaim     = false;
    public int     vrLookbackBars        = 12;    // 최근 N봉 내 VWAP 이탈 확인
    public double  vrVolMult             = 2.0;   // 현재 거래량 >= 5봉 평균 × 이 값
    public int     vrMinAboveVwapBars    = 5;     // 연속 VWAP 위 봉 수
    public int     vrMinScore            = 83;    // 진입 최소 점수 (← 87)
    public int     vrMaxScore            = 90;    // 진입 최대 점수 (← 88: 범위 83~90)

    // ── VWAP_RECLAIM 청산 ────────────────────────────────────────────────
    public double  vrStopPct             = 1.5;   // 손절 (%) ← 1.0: VWAP 근처 흔들림 허용
    public double  vrTpPct               = 2.2;   // 고정 익절 (%, 0 = 비활성)
    public double  vrTrailSt             = 1.8;   // 트레일 시작 수익 (%)
    public double  vrTrailDrop           = 1.0;   // 트레일 고점 대비 하락 (%)

    // ── 재진입 제한 ──────────────────────────────────────────────────────
    public boolean blockSameDayAfterStop = true;

    // ── 진입 등급 필터 ───────────────────────────────────────────────────────
    public boolean blockSSGrade = true;   // SS등급(95-100) 차단
    public boolean blockSGrade  = false;  // S등급(90-94) — BREAKOUT 유효 범위 85-94로 확대 (← true)
    public boolean blockAGrade  = true;   // A등급(85-89) 차단 (← false: A등급 13건 PF 0.75 — 손실 주도)
    public boolean blockBGrade  = false;  // B등급(80-84) 허용 (← true: 실전도 A+B만 허용)

    // ── THIRTY_MIN_RSI_BB_CROSS 진입 조건 ────────────────────────────────────
    public boolean enable30mRsiBbCross   = true;
    public int     rsiBb30mRsiPeriod     = 5;    // ← 14: KRX 하루 390봉 내 30분봉 13개 → 기존 기간으로 valid 불가
    public int     rsiBb30mSignalPeriod  = 3;    // ← 9
    public int     rsiBb30mBbPeriod      = 8;    // ← 20
    public double  rsiBb30mStdMult       = 2.0;
    public int     rsiBb30mVolBars       = 20;    // 거래량 비교 기준 30분봉 수
    public int     rsiBb30mMinScore      = 60;    // 시장 컨텍스트 최소 점수 (signalScore 게이트)

    // ── THIRTY_MIN_RSI_BB_CROSS 청산 ─────────────────────────────────────────
    public double  rsiBb30mStopPct       = 2.5;   // 손절 (%)
    public double  rsiBb30mTpPct         = 4.0;   // 고정 익절 (%)
    public double  rsiBb30mTrailSt       = 2.0;   // 트레일 시작 수익 (%)
    public double  rsiBb30mTrailDrop     = 1.0;   // 트레일 고점 대비 하락 (%)

    // ── RED_TO_GREEN 진입 조건 ────────────────────────────────────────────────
    public boolean enableR2G         = true;
    public double  r2gMaxCrossPct    = 0.08; // 전일종가 최대 +8% 이내 (← 5%: 상승장 갭업 8%+ 허용)
    public double  r2gStopPct        = 2.5;
    public double  r2gTpPct          = 4.0;
    public double  r2gTrailSt        = 2.0;
    public double  r2gTrailDrop      = 1.0;

    // ── 레거시 (하위 호환) ────────────────────────────────────────────────
    /** @deprecated per-mode stop/TP/trail 사용 권장; -1 = 미설정 */
    public double stopLossPct   = -1;
    public double takeProfitPct = -1;
    public double trailStartPct = -1;
    public double trailDropPct  = -1;

    // ── RSI_BOLLINGER_REBOUND 모드 ──────────────────────────────────────
    public boolean enableRsiBbRebound        = false;  // ← true: 백테스트 PF 0.74(손실), 52건 중 SS등급 전부 → 비활성화
    public int     rsiBbMinScore             = 80;   // ← 90: 5분봉 BB하단+RSI 조건 자체가 엄격 → 점수 기준 완화
    public int     rsiBbRsiPeriod            = 14;
    public int     rsiBbSignalPeriod         = 9;
    public int     rsiBbPeriod               = 20;
    public double  rsiBbStdMult              = 2.0;
    public double  rsiBbLowerTouchBufferPct  = 0.2;
    public double  rsiBbMaxBreakdownPct      = 0.5;
    public double  rsiBbMinVwapPct           = -0.5;  // 미사용 (VWAP 조건 제거됨)
    public double  rsiBbRsiLowThreshold      = 40.0;  // 과매도 기준 (← 32.0: 0건 발생 → 추가 완화)

    // ── RSI_BOLLINGER_REBOUND 청산 ────────────────────────────────────────
    public double  rsiBbStopPct              = 1.5;   // ← 2.0: STOP_LOSS 13건 평균 -2.23% → 손절 축소
    public double  rsiBbTpPct               = 0.0;   // 고정익절 비활성 (0 = 미사용)
    public double  rsiBbTrailSt             = 2.0;   // 트레일 시작 +2% (← 1.2)
    public double  rsiBbTrailDrop           = 0.6;   // ← 0.8: Trail 95.7% 승률 → 수익 보호 강화

    // ── OPENING_RANGE_BREAKOUT 진입 조건 ────────────────────────────────
    public boolean enableOpeningRangeBreakout = true;   // ORB PF 2.94 확인 → 활성화 (← false)
    public int     orbMaxScore                = 73;    // ORB 최대 점수 — D등급 집중 (← 무제한: 등급 역설 확인)
    public double  orbStopPct                 = 1.3;   // 손절 (%) ← 1.8
    public double  orbTpPct                   = 2.0;   // 고정 익절 (%, 0 = 비활성) ← 2.2
    public double  orbTrailSt                 = 1.5;   // 트레일 시작 수익 (%) ← 1.8
    public double  orbTrailDrop               = 0.8;   // ← 0.6: TRAIL_ORB +0.71% 조기 청산 개선

    // ── VWAP_RECLAIM_V2 진입 조건 ─────────────────────────────────────────
    public boolean enableVwapReclaimV2        = true;
    public double  vr2MinIntradayGainPct      = 0.4;   // ← 0.8: VR2 0건 → 추가 완화
    public int     vr2VwapBelowLookback       = 50;    // 최근 50봉 내 VWAP 이탈 확인 (← 30: 대형주 상승장에서 조건 미충족)
    public double  vr2VolMult                 = 1.3;   // ← 1.5: 눌림 구간 거래량 감소 특성 반영
    public double  vr2RsiLow                  = 45.0;  // RSI 하한
    public double  vr2RsiHigh                 = 75.0;  // ← 70.0: 강세장 RSI 70 초과 빈발 → 창 확대
    public double  vr2FromDayHighMaxPct       = 4.0;   // 당일 고점 대비 최대 이탈 %
    public double  vr2MaxVwapGapPct           = 2.0;   // VWAP 이격 최대 +2.0% (← 1.5%)
    public int     vr2MinScore                = 75;    // ← 80: 조건 완화에 따른 점수 기준 하향
    public boolean vr2RequireVwapBelow        = false; // VWAP 이탈 필수 여부 (← true: 상승장에서 이탈 미발생 → 해제)

    // ── VWAP_RECLAIM_V2 청산 ──────────────────────────────────────────────
    public double  vr2StopPct                 = 1.8;   // 손절 (%)
    public double  vr2TpPct                   = 3.0;   // 고정 익절 (%)
    public double  vr2TrailSt                 = 2.0;   // 트레일 시작 수익 (%)
    public double  vr2TrailDrop               = 1.0;   // 트레일 고점 대비 하락 (%)
}
