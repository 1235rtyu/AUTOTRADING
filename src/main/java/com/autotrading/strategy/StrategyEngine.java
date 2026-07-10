package com.autotrading.strategy;

import com.autotrading.market.MarketDataService;
import com.autotrading.model.OrderCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StrategyEngine {

    private static final Logger logger = LoggerFactory.getLogger(StrategyEngine.class);

    public enum Market { KRX, US }

    private enum EntryMode { NONE, BREAKOUT, OPENING_RANGE_BREAKOUT, THIRTY_MIN_RSI_BB_CROSS, RED_TO_GREEN }

    private enum PositionPhase {
        NONE,      // 포지션 없음
        ENTERING,  // 매수 주문 진행 중
        HOLDING,   // 풀 포지션 보유
        EXITING    // 매도 주문 진행 중
    }

    private enum ExitType {
        NONE, PROFIT, TRAIL, STOPLOSS, TIMESTOP, VWAP_BREAK
    }

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");

    // =========================
    // History
    // =========================
    private static final int TICK_HISTORY_CAPACITY = 180;       // 틱 히스토리 최대 보관 수 (tick gate 판단용)
    private static final int MINUTE_HISTORY_CAPACITY = 1500;    // 30분봉 3일치 (1170봉) 확보
    private static final int MIN_HISTORY_BARS = 8;              // 신호 판단 최소 분봉 수 (이하면 데이터 부족)
    private static final int MIN_HISTORY_SPAN_SECONDS = 300;    // 신호 판단 최소 기간 (5분 미만 무시)

    private static final int VELOCITY_WINDOW_MIN_SECONDS = 180; // 대표 속도 계산 윈도우 하한 (3분)
    private static final int VELOCITY_WINDOW_MAX_SECONDS = 300; // 대표 속도 계산 윈도우 상한 (5분)

    // 단기/중기/장기 추세 속도 계산 윈도우 (분~분 사이 구간 평균으로 안정화)
    private static final int TREND_SHORT_MIN_SECONDS = 120;     // 단기 추세 시작 (2분)
    private static final int TREND_SHORT_MAX_SECONDS = 180;     // 단기 추세 끝 (3분)
    private static final int TREND_MID_MIN_SECONDS = 180;       // 중기 추세 시작 (3분)
    private static final int TREND_MID_MAX_SECONDS = 300;       // 중기 추세 끝 (5분)
    private static final int TREND_LONG_MIN_SECONDS = 300;      // 장기 추세 시작 (5분)
    private static final int TREND_LONG_MAX_SECONDS = 480;      // 장기 추세 끝 (8분)

    // =========================
    // Entry timing
    // =========================
    private static final long ENTRY_READY_TTL_MS = 20_000L;    // 신호 유효 시간: 20초 내 틱 진입 불가 시 만료
    private static final long ENTRY_READY_MIN_DELAY_MS = 500L; // 신호 발생 후 최소 대기 (빠른 체결을 위해 0.5초)
    private static final int BUY_RECENT_RANGE_BARS = 15;       // 최근 고점/저점 산출 범위 (최근 15개 분봉)

    // =========================
    // Momentum / breakout
    // =========================
    private static final double MOMENTUM_PRICE_NEAR_HIGH = 0.9970;  // 고점 대비 -0.3% 이내여야 모멘텀 인식
    private static final double BREAKOUT_RETEST_LOWER = 0.9970; // ← 0.9980: 리테스트 하단 -0.3%로 확대
    private static final double BREAKOUT_RETEST_UPPER = 1.0080; // ← 1.0050: 리테스트 상단 +0.8%로 확대
    private static final double MOMENTUM_VOLUME_MULT = 1.5;          // 모멘텀 진입 최소 거래량 (평균의 1.5배)
    private static final double STRONG_BREAKOUT_VOLUME_MULT = 1.3;   // (← 1.5: 진입 기회 확대)
    private static final double VOLUME_BREAKOUT_VOLUME_MULT = 1.3;   // (← 1.6: 진입 기회 확대)
    private static final double VOLUME_SURGE_MULT_FOR_SIZE_UP = 2.3; // 사이즈 업 조건: 거래량 평균 2.3배 이상

    // KRX BREAKOUT 추가 진입 조건 (진짜 돌파만 허용)
    private static final double BREAKOUT_KRX_MIN_VWAP_GAP       = 0.003; // VWAP 대비 최소 +0.3% 이상 (← 0.005)
    private static final double BREAKOUT_KRX_MIN_VOLUME_MULT    = 1.3;  // ← 1.4: 진입 기회 확대

    // LOW_VOLUME_SKIP_MULT removed: per-mode volume ratio used in isBuyCandidate

    // =========================
    // VWAP filter
    // =========================
    private static final double VWAP_NEAR_DISTANCE_PCT  = 0.0040; // pullback nearVwap 판단 기준
    private static final double VWAP_BREAK_SELL_BUFFER  = 0.9960; // VWAP -0.4% (← 0.9950: 조기 exit으로 평균 손실 축소)
    private static final double VWAP_SLOPE_MIN_PCT       = 0.0002; // VWAP slope 최소 상승폭
    // 극단적 과열만 safety-net으로 차단 — 실제 per-mode 상한은 isBuyCandidate에서 체크
    private static final double VWAP_TOO_FAR_HARD_LIMIT   = 0.080;  // 8% 초과만 즉시 차단
    // Pullback은 VWAP 대비 최대 -0.4% 아래까지 진입 허용 (회복 확인 조건 필요)


    // =========================
    // Per-mode entry thresholds (진입 모드별 필터 기준)
    // =========================
    // VWAP 이격 허용 상한 (price above VWAP 기준)
    private static final double VWAP_MAX_GAP_BREAKOUT        = 0.015; // 1.5% (← 2.2%에서 하향: VWAP 이격 클수록 되돌림 위험)
    private static final boolean ENABLE_BREAKOUT_ENTRY         = true;  // BREAKOUT PF 1.23 (2026-06 백테스트 재확인 → 재활성)

    // ── OPENING_RANGE_BREAKOUT 상수 ───────────────────────────────────────────
    private static final boolean ENABLE_OPENING_RANGE_BREAKOUT = true;  // ORB 백테스트 PF 2.94 확인 → 활성화 (← false)
    private static final double STOP_KRX_ORB       = 0.987; // -1.3%  (← 0.982)
    private static final double TP_KRX_ORB         = 1.020; // +2.0%  (← 1.022)
    private static final double TRAIL_ST_KRX_ORB   = 0.015; // +1.5%  (← 0.018)
    private static final double TRAIL_DR_KRX_ORB   = 0.006; // -0.6% (← 0.8%: TRAIL avg +0.70% → 수익 보호 강화)
    private static final int    ORB_MAX_SCORE       = 100;  // 제한 해제 (← 73: 30M 시간창 분리로 ORB 경쟁력 복구)

    // ── THIRTY_MIN_RSI_BB_CROSS 상수 ─────────────────────────────────────────
    // 30분봉 RSI 골든크로스 + 볼린저 하단 터치 반등 매매
    private static final boolean ENABLE_30M_RSI_BB_CROSS_ENTRY = true;
    private static final int     THIRTY_MIN_RSI_BB_MIN_SCORE   = 80;    // (← 60: BB 하단 반등 품질 강화)
    private static final double STOP_KRX_30M_RSI_BB    = 0.985; // -1.5% (← -2.5%: BB 하단 이탈 즉시 손절)
    private static final double TP_KRX_30M_RSI_BB      = 1.020; // +2.0% (← +2.5%: EOD 미달성 방지, 조기 익절)
    private static final double TRAIL_ST_KRX_30M_RSI_BB = 0.010; // +1.0%부터 트레일 (← +1.2%: 수익 조기 보호)
    private static final double TRAIL_DR_KRX_30M_RSI_BB = 0.010; // 고점 대비 -1.0% 청산

    // ── RED_TO_GREEN 상수 ─────────────────────────────────────────────────────
    // D-1 종가 돌파 반전 매매: 당일 하락 후 전일종가 상향 돌파 시 모멘텀 추격
    private static final boolean ENABLE_R2G_ENTRY      = true;
    private static final double STOP_KRX_R2G           = 0.982; // -1.8% (← -2.5%: 전일종가 지지선 이탈 즉시 손절)
    private static final double TP_KRX_R2G             = 1.040; // +4.0%
    private static final double TRAIL_ST_KRX_R2G       = 0.020; // +2.0%부터 트레일
    private static final double TRAIL_DR_KRX_R2G       = 0.010; // 고점 -1.0%
    private static final double R2G_MIN_CROSS_PCT       = 0.003; // 전일종가 최소 +0.3% 돌파 (← 0.1%: 노이즈 필터)
    private static final double R2G_MAX_CROSS_PCT       = 0.050; // 전일종가 최대 +5.0% 이내 (← 3.0%: 상승장 갭업 3%+ 허용)
    private static final double R2G_MIN_VOLUME_MULT     = 1.2;   // ← 1.5: 진입 기회 확대

    // 거래량 비율 (volume / avgVolume 최소값)
    private static final double VOLUME_RATIO_BREAKOUT        = 1.20;

    // 거래대금 비율 (latestTurnover / avgTurnover 최소값)
    private static final double TURNOVER_RATIO_BREAKOUT      = 1.00;

    // 고득점(≥70) 시 필터 완화
    private static final int    HIGH_CONVICTION_SCORE         = 85;
    private static final double HIGH_CONVICTION_VOLUME_MULT   = 0.85;  // 볼륨 기준 -15%
    private static final double HIGH_CONVICTION_TURNOVER_MULT = 0.85;  // 거래대금 기준 -15%

    // =========================
    // Liquidity / price filter
    // =========================
    private static final double MIN_KRX_PRICE = 1000.0;

    private static final double MIN_KRX_LATEST_TURNOVER = 50_000_000.0; // 5천만원 (기존 3천만 → 상향)
    private static final double MIN_KRX_AVG_TURNOVER    = 30_000_000.0; // 3천만원 (기존 2천만 → 상향)

    private static final double MIN_US_LATEST_TURNOVER = 10_000.0;
    private static final double MIN_US_AVG_TURNOVER = 6_000.0;

    // =========================
    // Risk / Exit (공통)
    // =========================
    private static final double EMERGENCY_STOP_MULT = 0.970; // emergency stop -3.0% (← -4.0%: EMERGENCY -8% 방지)

    private static final double MAX_DAILY_LOSS_PCT   = 0.03; // 일일 최대 손실 3% (← 6%: 하루 손실 제한 강화)
    private static final double MAX_DAILY_PROFIT_PCT = 0.05; // 일일 수익 목표 +5% 달성 시 신규 진입 차단

    private static final long SELL_MARKET_FALLBACK_TTL_MS = 60_000L;

    // Breakeven guard: 한번이라도 수익 찍었다가 손실 구간으로 되돌아오면 시장가 청산
    private static final double BREAKEVEN_GUARD_PEAK_THRESHOLD = 0.015;  // ← 0.020: 조기 본전 보호 강화
    private static final long   BREAKEVEN_GUARD_MIN_HOLD_MS    = 180_000L; // 진입 후 최소 3분 경과
    private static final double BREAKEVEN_GUARD_LOSS_TRIGGER   = 0.003;  // +1.5% 찍은 후 +0.3% 이하로 내려오면 청산

    // =========================
    // Risk / Exit (KRX — entryMode별)
    // =========================
    // PULLBACK: 손절 -2.3%, 익절 +3.2%, 트레일 시작 +2.2%, 고점 하락 -1.6%
    private static final double STOP_KRX_PULLBACK              = 0.977;
    private static final double TP_KRX_PULLBACK                = 1.032;
    private static final double TRAIL_START_KRX_PULLBACK       = 0.022;
    private static final double TRAIL_DROP_KRX_PULLBACK        = 0.016;

    // BREAKOUT: 손절 -1.5%(← 2.0%), 익절 +2.0%, 트레일 시작 +1.5%(← 1.8%), 고점 하락 -1.2%(← 1.5%)
    private static final double STOP_KRX_BREAKOUT              = 0.985; // ← 0.980: VWAP_BREAK 평균 -1.2% → 공식 손절 축소
    private static final double TP_KRX_BREAKOUT                = 1.020; // ← 1.025: 2.5% 도달 전 역전 多, 2.0%로 복원
    private static final double TRAIL_START_KRX_BREAKOUT       = 0.015; // ← 0.018: EOD 3건 트레일 미작동 → 조기 보호
    private static final double TRAIL_DROP_KRX_BREAKOUT        = 0.012; // ← 0.015: 수익 보호 강화

    // =========================
    // Sizing
    // =========================
    private static final double BASE_SIZE_DEFAULT  = 1.00; // ORB / 30M / R2G 기본 사이즈
    private static final double BASE_SIZE_BREAKOUT = 0.80;
    private static final double SIZE_UP_MULT = 1.50;

    // =========================
    // Execution control
    // =========================
    private static final long BUY_COOLDOWN_MS = 60_000L;          // ← 90초: 매수 신호 간 최소 쿨다운 (60초)
    private static final long PENDING_TIMEOUT_MS = 30_000L;        // 매수 주문 미체결 타임아웃 (30초)
    private static final long SELL_RETRY_COOLDOWN_MS = 5_000L;     // 매도 재시도 최소 간격 (5초)
    private static final long SELL_PENDING_TIMEOUT_MS = 15_000L;   // 매도 주문 미체결 타임아웃 (15초)
    private static final double TIMEOUT_SLIPPAGE_BUFFER = 0.002;   // 시장가 타임아웃 슬리피지 추정 (-0.2%, 일일손실 누적 보수적 처리)

    private static final long REENTER_PROFIT_COOLDOWN_MS = 300_000L;   // 익절 후 동일 종목 재진입 금지 (5분)
    private static final long REENTER_TRAIL_COOLDOWN_MS  = 120_000L;   // 트레일 청산 후 재진입 금지 (2분)
    // 손절 이유별 재진입 쿨다운 (강한 종목은 빠른 회복 가능 → 이유별 차별화)
    private static final long REENTER_STOPLOSS_COOLDOWN_MS          = 900_000L; // STOP_LOSS / VWAP_BREAK / EMERGENCY: 15분
    private static final long REENTER_FAILED_BREAKOUT_COOLDOWN_MS   = 600_000L; // FAILED_BREAKOUT: 10분

    private static final int MAX_DAILY_ENTRY_COUNT = 2;           // 종목당 일일 최대 진입 횟수
    private static final int MAX_SAME_PATTERN_ENTRY_COUNT = 1;    // 동일 패턴 연속 진입 최대 횟수 (중복 추격 방지)

    // =========================
    private static final long VWAP_BREAK_GRACE_MS = 300_000L; // 매수 후 5분간 VWAP_BREAK 유예 (← 180_000L: 3분 과도 → 13건 전패)

    private static final long MARKET_CONTEXT_TTL_MS = 300_000L; // 시장 컨텍스트 유효 시간 (5분 초과 시 만료)

    private final MarketDataService marketDataService;
    private final Map<String, SymbolState> states = new ConcurrentHashMap<>();
    private final Map<Market, MarketContext> marketContext = new ConcurrentHashMap<>();

    // Backtest mode: inject bar timestamp so cooldown / session logic uses simulated time
    private volatile long backtestNowMs = 0L;
    public void setBacktestNowMs(long ms) { this.backtestNowMs = ms; }
    private long nowMs() { return backtestNowMs > 0 ? backtestNowMs : System.currentTimeMillis(); }

    // Backtest config: overrides production constants without touching static finals
    private volatile com.autotrading.model.BacktestConfig backtestConfig = null;
    public void setBacktestConfig(com.autotrading.model.BacktestConfig cfg) { this.backtestConfig = cfg; }
    // 일일 손실 추적: 날짜별 실현 손익률 누적 합계
    private final Map<java.time.LocalDate, Double> dailyPnlAccumulator = new ConcurrentHashMap<>();

    private static class MarketContext {
        boolean marketWeak;
        double velocityShort;
        double shortAvg;
        double longAvg;
        double lastPrice;
        long updatedAtMs;
        String sourceSymbol;
    }

    private static class SymbolState {
        final PriceHistory tickHistory = new PriceHistory(TICK_HISTORY_CAPACITY);
        final MinuteBarHistory minuteHistory = new MinuteBarHistory(MINUTE_HISTORY_CAPACITY);

        Market market;

        boolean buyPending;
        long buyPendingSinceMs;

        boolean sellPending;
        long sellPendingSinceMs;

        long lastBuySignalMs;
        long lastSellSignalMs;

        long entryTimeMs;
        double highestSinceEntry;
        double buyAmountPerOrder;
        int    entryQty;

        double entryPriceSnapshot;
        double lastKnownProfitRate;

        BuySignal entrySignal;
        long entryReadyAtMs;
        long entryReadyUntilMs;
        double entryReadyClose;
        String entryReadyPatternKey;
        LocalDateTime lastEntryReadyBarTs;      // actual ENTRY_READY success bar
        LocalDateTime lastHistoryRebuildBarTs;  // history rebuild attempt bar

        long lastProfitExitTimeMs;
        long lastTrailExitTimeMs;
        long lastStopLossExitTimeMs;
        String lastStopLossExitReason;  // 쿨다운 차별화용 (이유별 재진입 대기 시간 다름)
        int dailyEntryCount;
        java.time.LocalDate lastEntryDay;
        String lastEntryPatternKey;
        int samePatternEntryCount;

        // ── OPENING_RANGE_BREAKOUT 상태 ──────────────────────────────────
        double openingRangeHigh  = 0.0;
        double openingRangeLow   = 0.0;
        boolean openingRangeReady = false;

        boolean forceMarketOnNextSell;
        long forceMarketUntilMs;
        String forceMarketReason;

        EntryMode entryMode = EntryMode.NONE;
        PositionPhase positionPhase = PositionPhase.NONE;
        ExitType lastExitType = ExitType.NONE;
        String lastPendingSellReason;     // 진행 중인 매도 주문 이유 (거절 시 롤백용)
        int pendingSellQty;               // 진행 중인 매도 주문 수량
        double pendingSellWeightedPnl;    // SELL_FILLED 시 dailyPnl에 반영할 값 (주문 생성 시점에는 미반영)
        double pendingSellTradePnlPct;    // SELL_FILLED 시 statsDailyExitPnlSum에 반영할 값

        // 진입 시점 지표 스냅샷 (BUY 주문 생성 시 저장 — clearEntryReadyState 후에도 유지)
        int    entrySignalScore;
        String entrySignalGrade;
        double entryVwapDistPct;
        double entryVelocityShort;
        double entryVolumeRatio;
        double entryTurnoverRatio;
        int    entryScoreVwap;
        int    entryScoreTrend;
        int    entryScoreVolume;
        int    entryScoreTurnover;
        int    entryScoreHigh;
        int    entryScorePattern;
        double entryVelocityMid;
        double entryVelocityLong;
        int    entryTrendScore;
        double entryFromHighPct;

        // 일별 통계 (진입 기회 / 실행 / 청산 성과)
        int statsDailyEntryReadyCount;
        int statsDailyExecCount;
        Map<String, Integer> statsDailyExitReasonCounts = new java.util.HashMap<>();
        double statsDailyExitPnlSum;

        // 백테스트 진단: 마지막 신호 거절 사유
        String lastRejectReason = "NO_DATA";

        // 당일 상태 추적
        double dayOpenPrice = 0.0;
        java.time.LocalDate dayOpenDate = null;
        double maxDayHigh = 0.0;           // 당일 최고가
    }

    private static class BuySignal {
        boolean enoughHistory;
        int ticks;
        long spanSeconds;

        double price;
        double volume;
        double averageVolume;

        double velocity;
        double velocityShort;
        double velocityMid;
        double velocityLong;

        boolean shortUp;
        boolean midUp;
        boolean longUp;
        int trendScore;
        boolean multiUptrend;

        double recentHigh;
        double recentLow;
        double latestTurnover;
        double averageTurnover;

        boolean lowVolumeSkip;

        boolean momentumBreakout;
        boolean pullbackEntry;
        boolean volumeBreakout;
        boolean strongBreakout;
        boolean earlyMomentum; // velShort/velMid 기반 초기 급등 포착

        boolean momentumNearHigh;
        boolean momentumVelocityOk;
        boolean momentumVolumeOk;

        boolean pullbackZone;
        boolean pullbackRecovering;
        boolean pullbackVelocityOk;
        boolean pullbackVolumeOk;
        double pullbackAvgShort;
        double pullbackAvgLong;
        double pullbackDepthFromHigh;
        int recentBullishBars;           // 최근 3봉 중 양봉 수
        boolean recentHighBreakout;      // 현재가 > 이전 2봉 최고가
        boolean consecutiveHigherClose;  // 최근 3봉 종가 연속 상승
        double fromHighPct;              // (price/recentHigh - 1) * 100

        boolean volumeBreakNearHigh;
        boolean volumeBreakVelocityOk;
        boolean volumeBreakVolumeOk;

        double vwap;
        double prevVwap;
        double vwapDistancePct;
        boolean aboveVwap;
        boolean nearVwap;
        boolean vwapSlopeUp;
        boolean vwapTooFar;
        boolean breakoutRetestReady;
        boolean breakoutRetestRecovering;

        boolean choppyMarket;
        boolean marketWeak;

        int    signalScore;
        int    signalCount;
        double volumeRatio;
        double turnoverRatio;
        int    scoreVwap;
        int    scoreTrend;
        int    scoreVolume;
        int    scoreTurnover;
        int    scoreHigh;
        int    scorePattern;

        // ── BREAKOUT 과열 차단용 RSI/BB 지표 ─────────────────────────────
        double  bbUpper;
        double  rsi;

        // ── THIRTY_MIN_RSI_BB_CROSS 전용 신호 ─────────────────────────────
        boolean thirtyMinRsiBbCondsMet;
        int     thirtyMinRsiBbScore;
        double  thirtyMinBbLower;
        double  thirtyMinBbMiddle;
        double  thirtyMinRsi;
        double  thirtyMinRsiSignal;
        boolean thirtyMinRsiGoldenCross;
        double  thirtyMinCurrentVol;
        double  thirtyMinAvgVol;

        // ── VWAP_RECLAIM_V2 전용 신호 ─────────────────────────────────────
        boolean vr2CondsMet;
        int     vr2Score;
        double  vr2DayMaxGainPct;
        boolean vr2HadVwapBelow30m;
        double  vr2Rsi;
        double  vr2FromDayHighPct;

        // ── OPENING_RANGE_BREAKOUT 전용 신호 ─────────────────────────────
        boolean orbBreakout;    // ORB 조건 충족

        // ── RED_TO_GREEN 전용 신호 ────────────────────────────────────────
        boolean r2gCross;       // D-1 종가 상향 돌파 신호
        double  r2gPrevClose;   // D-1 종가 기준가
        double  r2gCrossPct;    // D-1 종가 대비 돌파 %

        boolean timeWindowBlocked;
        boolean slowModeTimeWindowBlocked;
        boolean marketFilterPassed;
        boolean cheapStockBlocked;
        boolean turnoverFilterPassed;
        boolean absoluteLiquidityPassed;

        String rejectReason;
        String patternKey;
        EntryMode entryMode = EntryMode.NONE;

        boolean isBuyCandidate(Market market, com.autotrading.model.BacktestConfig cfg) {
            // --- 하드 게이트 (모드 무관) ---
            if (!enoughHistory) return false;
            if (effectiveTimeWindowBlocked()) return false;
            if (!marketFilterPassed) return false;
            if (cheapStockBlocked) return false;
            if (!absoluteLiquidityPassed) return false; // 절대 유동성 (50M KRX)
            // 30M / R2G는 VWAP 안 봄 (mean-reversion 계열)
            if (entryMode != EntryMode.THIRTY_MIN_RSI_BB_CROSS && entryMode != EntryMode.RED_TO_GREEN && vwapTooFar) return false;
            if (entryMode == EntryMode.NONE) return false;

            // 모드 활성화 플래그 (실전: 상수 기반, 백테스트: cfg 기반)
            boolean enableBO = cfg != null ? cfg.enableBreakout : ENABLE_BREAKOUT_ENTRY;
            if (entryMode == EntryMode.BREAKOUT && !enableBO) return false;
            boolean enableOrb = cfg != null ? cfg.enableOpeningRangeBreakout : ENABLE_OPENING_RANGE_BREAKOUT;
            if (entryMode == EntryMode.OPENING_RANGE_BREAKOUT && !enableOrb) return false;
            boolean enable30mRsiBb = cfg != null ? cfg.enable30mRsiBbCross : ENABLE_30M_RSI_BB_CROSS_ENTRY;
            if (entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS && !enable30mRsiBb) return false;

            // THIRTY_MIN_RSI_BB_CROSS: 30분봉 RSI 골든크로스 + 볼린저 하단 — 등급/VWAP 미적용
            if (entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS) {
                if (!thirtyMinRsiBbCondsMet) return false;
                int minSc30m = cfg != null ? cfg.rsiBb30mMinScore : THIRTY_MIN_RSI_BB_MIN_SCORE;
                if (signalScore < minSc30m) return false;
                return true;
            }

            // RED_TO_GREEN: D-1 종가 상향 돌파 반전 — VWAP/등급 미적용
            if (entryMode == EntryMode.RED_TO_GREEN) {
                boolean enableR2g = cfg != null ? cfg.enableR2G : ENABLE_R2G_ENTRY;
                if (!enableR2g) return false;
                if (!r2gCross) return false;
                if (signalScore < 65) return false;   // (← 55: D등급 하단 노이즈 차단)
                if (signalScore >= 75) return false;  // B/C등급 차단: PF 0.47/0.89 확인 → D등급(65-74)만 허용
                return true;
            }

            // OPENING_RANGE_BREAKOUT: ORB 독립 조건으로 판단
            if (entryMode == EntryMode.OPENING_RANGE_BREAKOUT) {
                if (!orbBreakout) return false;
                if (signalScore < 60) return false;
                int orbMax = cfg != null && cfg.orbMaxScore > 0 ? cfg.orbMaxScore : ORB_MAX_SCORE;
                if (signalScore > orbMax) return false;
                return true;
            }

            // BREAKOUT: VWAP slope 추가 체크
            if (entryMode != EntryMode.BREAKOUT && !vwapSlopeUp) return false;

            // --- BREAKOUT per-mode 기준값 ---
            boolean highConviction = signalScore >= HIGH_CONVICTION_SCORE;

            double allowedVwapGap        = VWAP_MAX_GAP_BREAKOUT;
            double requiredVolumeRatio   = VOLUME_RATIO_BREAKOUT;
            double requiredTurnoverRatio = TURNOVER_RATIO_BREAKOUT;
            int minScore                 = 85; // ← 83: B등급(83-84) PF 0.37 확인 → 85로 상향

            // Backtest config: score threshold & VWAP gap override
            if (cfg != null) {
                allowedVwapGap = cfg.vwapMaxGapBreakoutPct / 100.0;
                minScore       = cfg.breakoutMinScore;
            }

            // 고득점이면 일부 기준 완화
            if (highConviction) {
                requiredVolumeRatio   *= HIGH_CONVICTION_VOLUME_MULT;
                requiredTurnoverRatio *= HIGH_CONVICTION_TURNOVER_MULT;
            }

            // --- BREAKOUT per-mode 필터 ---
            if (vwapDistancePct > allowedVwapGap) return false;
            if (averageVolume > 0.0 && volume < averageVolume * requiredVolumeRatio) return false;
            if (averageTurnover > 0.0 && latestTurnover < averageTurnover * requiredTurnoverRatio) return false;
            if (signalScore < minScore) return false;
            if (cfg != null && cfg.breakoutMaxScore > 0 && signalScore > cfg.breakoutMaxScore) return false;
            // SS/S/A/B 등급 차단
            boolean blockSS = (cfg == null) || cfg.blockSSGrade;
            boolean blockS  = cfg != null && cfg.blockSGrade;
            boolean blockA  = cfg != null && cfg.blockAGrade;
            boolean blockB  = cfg != null && cfg.blockBGrade;
            if (blockSS && signalScore >= 95) return false;
            if (blockS  && signalScore >= 90 && signalScore < 95) return false;
            if (blockA  && signalScore >= 85 && signalScore < 90) return false;
            if (blockB  && signalScore >= 80 && signalScore < 85) return false;

            // --- BREAKOUT 진입 조건 ---
            {
                boolean cfgOverheat = cfg == null || cfg.breakoutOverheatBlock;
                boolean cfgAccel    = cfg == null || cfg.breakoutRequireAcceleration;
                boolean cfgMultiUp  = cfg != null ? cfg.breakoutRequireMultiUptrend : false;

                if (cfgOverheat && recentHigh > 0.0 && velocityShort >= 0.009 && vwapDistancePct >= 0.015) {
                    boolean veryNearHigh = price >= recentHigh * 0.9995;
                    boolean nearHighVolumeSurge = price >= recentHigh * 0.998
                            && averageVolume > 0.0
                            && volume >= averageVolume * 2.5;
                    if (veryNearHigh || nearHighVolumeSurge) return false;
                }
                if (cfgAccel && velocityShort <= velocityMid) return false;
                double cfgVelMid  = cfg != null ? cfg.breakoutMinVelocityMid  : 0.0;
                double cfgVelLong = cfg != null ? cfg.breakoutMinVelocityLong : 0.0;
                if (velocityMid < cfgVelMid) return false;
                if (velocityLong < cfgVelLong) return false;
                // RSI / 볼린저 상단 과열 차단
                if (bbUpper > 0.0 && price >= bbUpper) return false;
                if (rsi > 0.0 && rsi > 70.0) return false;
                boolean trendOk = cfgMultiUp ? trendScore == 3 : trendScore >= 2;
                return (volumeBreakout || strongBreakout)
                        && breakoutRetestReady
                        && breakoutRetestRecovering
                        && trendOk
                        && vwapDistancePct >= BREAKOUT_KRX_MIN_VWAP_GAP
                        && averageVolume > 0.0 && volume >= averageVolume * (cfg != null ? cfg.breakoutKrxVolMult : BREAKOUT_KRX_MIN_VOLUME_MULT);
            }
        }

        boolean effectiveTimeWindowBlocked() {
            boolean isSlowMode = entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS
                              || entryMode == EntryMode.RED_TO_GREEN;
            return isSlowMode ? slowModeTimeWindowBlocked : timeWindowBlocked;
        }
    }

    private static class SellDecision {
        final boolean shouldSell;
        final int quantity;
        final String reason;
        final boolean marketOrder;

        SellDecision(boolean shouldSell, int quantity, String reason, boolean marketOrder) {
            this.shouldSell = shouldSell;
            this.quantity = quantity;
            this.reason = reason;
            this.marketOrder = marketOrder;
        }

        static SellDecision none() {
            return new SellDecision(false, 0, "", false);
        }
    }

    private static class ExitProfile {
        final double stopLossMult;
        final Double takeProfitMult;    // null = 고정 익절 없음
        final Double trailStartProfit;  // null = 트레일 없음
        final Double trailDropFromHigh;
        final boolean useTrailing;
        final String modeTag;           // sell reason suffix (e.g. "PULLBACK")

        ExitProfile(double stopLossMult, Double takeProfitMult,
                    Double trailStartProfit, Double trailDropFromHigh,
                    boolean useTrailing, String modeTag) {
            this.stopLossMult      = stopLossMult;
            this.takeProfitMult    = takeProfitMult;
            this.trailStartProfit  = trailStartProfit;
            this.trailDropFromHigh = trailDropFromHigh;
            this.useTrailing       = useTrailing;
            this.modeTag           = modeTag;
        }
    }

    public static class EntrySnapshot {
        public final String entryMode;
        public final double entryPrice;
        public final long   entryTimeMs;
        public final double highestSinceEntry;
        public final int    signalScore;
        public final String signalGrade;
        public final double vwapDistPct;
        public final double velocityShort;
        public final double velocityMid;
        public final double velocityLong;
        public final int    trendScore;
        public final double fromHighPct;
        public final double volumeRatio;
        public final double turnoverRatio;
        public final int    scoreVwap;
        public final int    scoreTrend;
        public final int    scoreVolume;
        public final int    scoreTurnover;
        public final int    scoreHigh;
        public final int    scorePattern;
        public final double buyAmountPerOrder;
        public final int    entryQty;

        EntrySnapshot(SymbolState st) {
            this.entryMode         = st.entryMode != null ? st.entryMode.name() : "UNKNOWN";
            this.entryPrice        = st.entryPriceSnapshot;
            this.entryTimeMs       = st.entryTimeMs;
            this.highestSinceEntry = st.highestSinceEntry;
            this.signalScore       = st.entrySignalScore;
            this.signalGrade       = st.entrySignalGrade != null ? st.entrySignalGrade : "D";
            this.vwapDistPct       = st.entryVwapDistPct;
            this.velocityShort     = st.entryVelocityShort;
            this.velocityMid       = st.entryVelocityMid;
            this.velocityLong      = st.entryVelocityLong;
            this.trendScore        = st.entryTrendScore;
            this.fromHighPct       = st.entryFromHighPct;
            this.volumeRatio       = st.entryVolumeRatio;
            this.turnoverRatio     = st.entryTurnoverRatio;
            this.scoreVwap         = st.entryScoreVwap;
            this.scoreTrend        = st.entryScoreTrend;
            this.scoreVolume       = st.entryScoreVolume;
            this.scoreTurnover     = st.entryScoreTurnover;
            this.scoreHigh         = st.entryScoreHigh;
            this.scorePattern      = st.entryScorePattern;
            this.buyAmountPerOrder = st.buyAmountPerOrder;
            this.entryQty          = st.entryQty;
        }
    }

    public EntrySnapshot getEntrySnapshot(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return null;
        SymbolState st = state(normalized);
        synchronized (st) {
            if (st.entryPriceSnapshot <= 0.0) return null;
            return new EntrySnapshot(st);
        }
    }

    public String getLastRejectReason(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return "UNKNOWN";
        SymbolState st = state(normalized);
        synchronized (st) { return st.lastRejectReason; }
    }

    public StrategyEngine(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public void logDailyStats(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        SymbolState st = state(normalized);
        synchronized (st) {
            Market market = st.market != null ? st.market : Market.KRX;
            java.time.LocalDate today = nowByMarket(market).toLocalDate();
            double dailyPnl = dailyPnlAccumulator.getOrDefault(today, 0.0);
            logger.info("DAILY_STATS [{}] {} date={} entryReady={} execCount={} exitPnlSum={} weightedDailyPnl={} exitsByReason={}",
                    market, normalized, today,
                    st.statsDailyEntryReadyCount,
                    st.statsDailyExecCount,
                    fmtPct(st.statsDailyExitPnlSum),
                    fmtPct(dailyPnl),
                    st.statsDailyExitReasonCounts);
        }
    }

    public void resetAll() {
        states.clear();
        marketContext.clear();
    }

    public void resetSymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        states.remove(normalized);
    }

    /** Backtesting only: reset per-day counters so each simulated trading day starts fresh. */
    public void advanceBacktestDay(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        SymbolState st = state(normalized);
        synchronized (st) {
            st.minuteHistory.rotateToPrevDay(); // ← clear() 버그픽: D-1 봉을 prevDayBars로 보존 후 당일 봉만 비움
            st.tickHistory.clear();
            clearEntryReadyState(st);
            resetEntryState(st);
            st.lastEntryDay = null;
            st.dailyEntryCount = 0;
            st.lastEntryPatternKey = null;
            st.samePatternEntryCount = 0;
            st.statsDailyEntryReadyCount = 0;
            st.statsDailyExecCount = 0;
            st.statsDailyExitReasonCounts.clear();
            st.statsDailyExitPnlSum = 0.0;
            st.maxDayHigh = 0.0;
            st.dayOpenPrice = 0.0;
            st.dayOpenDate  = null;
            st.openingRangeHigh  = 0.0;
            st.openingRangeLow   = 0.0;
            st.openingRangeReady = false;
        }
        // dailyPnlAccumulator는 날짜 키 기반 — 날짜 전진 시 resetDailyEntryIfNeeded에서 자동 정리됨
    }

    public void setMarket(String symbol, Market market) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        state(normalized).market = market;
    }

    public void setBuyAmount(String symbol, Double buyAmount) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        double normalizedAmount = (buyAmount == null || !Double.isFinite(buyAmount) || buyAmount <= 0.0)
                ? 0.0
                : buyAmount;

        synchronized (st) {
            st.buyAmountPerOrder = normalizedAmount;
        }

        if (normalizedAmount > 0.0) {
            logger.info("BUY amount set for {}: {}", normalized, String.format("%.2f", normalizedAmount));
        } else {
            logger.info("BUY amount cleared for {} (amount-based entry disabled)", normalized);
        }
    }

    
    public void clearStaleHoldState(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            st.sellPending = false;
            st.sellPendingSinceMs = 0L;
            st.lastSellSignalMs = nowMs();
            resetEntryState(st);
        }
    }

    public void markBuyPending(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = true;
            st.buyPendingSinceMs = nowMs();
        }
    }

    public void cancelBuyPending(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
        }
    }

    public void notifyBuyAccepted(String symbol) {
        cancelBuyPending(symbol);
    }

    public void notifyBuyFilled(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            // phase=NONE이면 이미 청산 완료 — 브로커 API 지연으로 오는 stale 알림 무시
            if (st.positionPhase == PositionPhase.NONE) return;
            // phase=EXITING이면 매도 주문 진행 중 — HOLDING으로 덮어쓰지 않음
            if (st.positionPhase == PositionPhase.EXITING) return;
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            if (st.entryTimeMs == 0L) {
                st.entryTimeMs = nowMs();
            }
            boolean wasAlreadyHolding = (st.positionPhase == PositionPhase.HOLDING);
            st.positionPhase = PositionPhase.HOLDING;
            // 매 5초 틱마다 execute()에서 호출되므로, ENTERING→HOLDING 전환 시점에만 로그
            if (!wasAlreadyHolding) {
                logger.info("BUY_FILLED {} phase={}", normalizeSymbol(symbol), st.positionPhase);
            }
        }
    }

    public void forceHoldingState(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        SymbolState st = state(normalized);
        synchronized (st) {
            if (st.positionPhase != PositionPhase.NONE) return;
            st.positionPhase = PositionPhase.HOLDING;
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            if (st.entryTimeMs == 0L) {
                st.entryTimeMs = nowMs();
            }
            logger.info("FORCE_HOLDING {} phase=HOLDING (startup recovery)", normalized);
        }
    }

    public void notifyBuyRejected(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;
        SymbolState st = state(normalized);
        synchronized (st) {
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            st.positionPhase = PositionPhase.NONE;
            logger.warn("BUY_REJECTED {} phase={}", normalized, st.positionPhase);
        }
    }

    public void notifySellAccepted(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.lastSellSignalMs = nowMs();
        }
    }

    public void notifySellFilled(String symbol) {
        notifySellFilled(symbol, 0);
    }

    public void notifySellFilled(String symbol, int remainingQty) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.lastSellSignalMs = nowMs();

            String filledReason = st.lastPendingSellReason;

            // 체결 확인 후 dailyPnl 반영 — filledQty=0이면 실체결 없으므로 PnL 반영 건너뜀
            if (st.pendingSellWeightedPnl != 0.0 || st.pendingSellTradePnlPct != 0.0) {
                int filledQty = st.pendingSellQty - remainingQty;
                if (filledQty <= 0) {
                    logger.warn("SELL_FILLED {} skipped PnL: filledQty={} pendingSellQty={} remainingQty={}",
                            normalized, filledQty, st.pendingSellQty, remainingQty);
                } else if (st.pendingSellQty <= 0) {
                    logger.warn("SELL_FILLED {} skipped PnL: invalid pendingSellQty={} filledQty={}",
                            normalized, st.pendingSellQty, filledQty);
                } else {
                    double fillRatio = (double) filledQty / st.pendingSellQty;
                    double realizedWeightedPnl = st.pendingSellWeightedPnl * fillRatio;
                    double realizedTradePnlPct = st.pendingSellTradePnlPct * fillRatio;

                    java.time.LocalDate today = nowByMarket(st.market != null ? st.market : Market.KRX).toLocalDate();
                    dailyPnlAccumulator.merge(today, realizedWeightedPnl, (a, b) -> a + b);
                    st.statsDailyExitPnlSum += realizedTradePnlPct;
                    if (filledReason != null) {
                        st.statsDailyExitReasonCounts.merge(filledReason, 1, (a, b) -> a + b);
                    }
                    logger.info("SELL_FILLED_PNL {} reason={} filledQty={}/{} fillRatio={} weightedPnl={} dailyPnl={}",
                            normalized, filledReason,
                            filledQty, st.pendingSellQty,
                            String.format("%.2f", fillRatio),
                            fmtPct(realizedWeightedPnl),
                            fmtPct(dailyPnlAccumulator.getOrDefault(today, 0.0)));

                    st.pendingSellWeightedPnl -= realizedWeightedPnl;
                    st.pendingSellTradePnlPct -= realizedTradePnlPct;
                    st.pendingSellQty = remainingQty;
                }
            }

            if (remainingQty <= 0) {
                // 전량 청산
                st.sellPending = false;
                st.sellPendingSinceMs = 0L;
                st.lastExitType = toExitType(filledReason);
                resetEntryState(st);
                st.positionPhase = PositionPhase.NONE;
            } else {
                // 부분 체결 — sellPending 유지하여 중복 매도 방지, 타임아웃 카운터 리셋
                st.sellPending = true;
                st.sellPendingSinceMs = nowMs();
                st.pendingSellQty = remainingQty;
                st.positionPhase = PositionPhase.EXITING;
            }

            logger.info("SELL_FILLED {} reason={} remainingQty={} phase={} exitType={}",
                    normalized, filledReason, remainingQty,
                    st.positionPhase, st.lastExitType);
        }
    }

    public void notifySellRejected(String symbol, String kisMsg) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            String rejectedReason = st.lastPendingSellReason;

            st.sellPending = false;
            st.sellPendingSinceMs = 0L;
            st.lastSellSignalMs = nowMs();

            // positionPhase를 매도 진행 이전으로 복원
            if (st.positionPhase == PositionPhase.EXITING) {
                st.positionPhase = PositionPhase.HOLDING;
            }

            // dailyPnl은 주문 생성 시 반영하지 않았으므로 롤백 불필요 — 그냥 소거
            st.pendingSellWeightedPnl = 0.0;
            st.pendingSellTradePnlPct = 0.0;

            logger.warn("SELL_REJECTED {} reason={} phase={} kisMsg={}",
                    normalized, rejectedReason, st.positionPhase, kisMsg);

            st.lastPendingSellReason = null;
            st.pendingSellQty = 0;
        }
    }

    public void markSellFallbackToMarket(String symbol, String reason) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        synchronized (st) {
            st.forceMarketOnNextSell = true;
            st.forceMarketUntilMs = nowMs() + SELL_MARKET_FALLBACK_TTL_MS;
            st.forceMarketReason = reason;
        }
    }

    public void updateMarketContextFromSymbol(String marketProxySymbol, Market market) {
        String normalized = normalizeSymbol(marketProxySymbol);
        if (normalized == null) return;

        SymbolState st = state(normalized);
        MinuteBarHistory.MinuteBar latest;
        double shortAvg;
        double longAvg;
        double velocityShort;

        double sessionVwapProxy;
        synchronized (st) {
            latest = st.minuteHistory.latest();
            shortAvg = st.minuteHistory.averagePrice(3);
            longAvg = st.minuteHistory.averagePrice(6);
            velocityShort = st.minuteHistory.velocitySeconds(TREND_SHORT_MIN_SECONDS, TREND_SHORT_MAX_SECONDS);
            sessionVwapProxy = st.minuteHistory.sessionVwap();
        }

        if (latest == null || latest.getClose() <= 0.0) return;
        double price = latest.getClose();

        boolean choppy = Math.abs(velocityShort) < 0.0005
                && Math.abs(shortAvg - longAvg) < (price * 0.001);

        // weak 판정 강화:
        //  1) 단기 속도가 0 미만이고 단기 평균이 장기 평균 아래 (기존보다 완화: -0.001 → 0.0)
        //  2) OR 프록시 ETF가 세션 VWAP 대비 0.15% 이상 하락 (일중 하락장 감지)
        boolean trendWeak = velocityShort < 0.0 && shortAvg < longAvg;
        boolean belowVwap = sessionVwapProxy > 0.0 && price < sessionVwapProxy * 0.9985;
        boolean weak = trendWeak || belowVwap;

        MarketContext ctx = marketContext.computeIfAbsent(market, m -> new MarketContext());
        synchronized (ctx) {
            ctx.marketWeak = weak;
            ctx.velocityShort = velocityShort;
            ctx.shortAvg = shortAvg;
            ctx.longAvg = longAvg;
            ctx.lastPrice = price;
            ctx.updatedAtMs = nowMs();
            ctx.sourceSymbol = normalized;
        }

        logger.debug("MARKET_CONTEXT [{}] proxy={} velShort={} shortAvg={} longAvg={} vwap={} belowVwap={} trendWeak={} choppy={} weak={}",
                market, normalized, fmtPct(velocityShort), fmt(shortAvg), fmt(longAvg),
                fmt(sessionVwapProxy), belowVwap, trendWeak, choppy, weak);
    }

    public void record(String symbol,
                       double open,
                       double high,
                       double low,
                       double close,
                       double volume,
                       long timestamp) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || close <= 0.0) return;

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);
        // Use market timezone for bar timestamps (avoid system default drift)
        ZoneId zone = market == Market.KRX ? KST_ZONE : NY_ZONE;
        LocalDateTime ts = Instant.ofEpochMilli(timestamp)
                .atZone(zone)
                .toLocalDateTime();
        long nowMs = nowMs();

        synchronized (st) {
            // 날짜 경계 감지: 당일 봉을 D-1 버퍼로 이동 후 bars 초기화
            // VWAP 등 당일 전용 계산은 bars만 참조하므로 영향 없고,
            // 30분봉 RSI/BB는 latestBarsMultiDay()로 D-1 봉을 포함해 계산한다.
            java.time.LocalDate barDate = ts.toLocalDate();
            if (st.dayOpenDate != null && !barDate.equals(st.dayOpenDate)) {
                st.minuteHistory.rotateToPrevDay();
                st.maxDayHigh = 0.0;
                st.openingRangeHigh  = 0.0;
                st.openingRangeLow   = 0.0;
                st.openingRangeReady = false;
            }
            if (st.dayOpenDate == null || !barDate.equals(st.dayOpenDate)) {
                st.dayOpenDate  = barDate;
                st.dayOpenPrice = open > 0.0 ? open : close;
            }

            st.minuteHistory.addBar(open, high, low, close, Math.max(0.0, volume), ts);
            if (high > st.maxDayHigh) st.maxDayHigh = high;

            // ORB: 09:00~09:10 고가/저가 저장 (KRX only)
            if (market == Market.KRX) {
                LocalTime barTime = ts.toLocalTime();
                if (!barTime.isBefore(LocalTime.of(9, 0)) && barTime.isBefore(LocalTime.of(9, 10))) {
                    st.openingRangeHigh = st.openingRangeHigh <= 0 ? high : Math.max(st.openingRangeHigh, high);
                    st.openingRangeLow  = st.openingRangeLow  <= 0 ? low  : Math.min(st.openingRangeLow,  low);
                }
                if (!barTime.isBefore(LocalTime.of(9, 10)) && st.openingRangeHigh > 0) {
                    st.openingRangeReady = true;
                }
            }

            BuySignal signal = buildBuySignal(st, market, close, Math.max(0.0, volume), ts);

            if (isEntryReady(signal, market)) {
                st.lastRejectReason = "NONE";
                st.statsDailyEntryReadyCount++;
                st.entrySignal = signal;
                st.entryReadyAtMs = nowMs;
                st.entryReadyUntilMs = nowMs + ENTRY_READY_TTL_MS;
                st.entryReadyClose = close;
                st.entryReadyPatternKey = signal.patternKey;
                st.lastEntryReadyBarTs = ts;
            } else {
                st.lastRejectReason = signal.rejectReason != null ? signal.rejectReason : "UNKNOWN";
                clearEntryReadyState(st);
            }
        }
    }


    public Optional<OrderCommand> decide(String symbol,
                                         double currentPrice,
                                         double currentVolume1m,
                                         int currentQuantity,
                                         double avgPrice) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null || currentPrice <= 0.0) {
            return Optional.empty();
        }

        SymbolState st = state(normalized);
        Market market = detectMarket(normalized, st);
        long nowMs = nowMs();
        LocalDateTime now = nowByMarket(market);

        synchronized (st) {
            st.tickHistory.addTick(currentPrice, Math.max(0.0, currentVolume1m), now);

            logger.debug("DECIDE [{}] {} price={} qty={} avgPrice={} vol1m={} buyPending={} sellPending={}",
                    market,
                    normalized,
                    fmt(currentPrice),
                    currentQuantity,
                    fmt(avgPrice),
                    fmt(currentVolume1m),
                    st.buyPending,
                    st.sellPending);

            if (currentQuantity <= 0) {
                // 미결 주문이 있는 동안은 entry state 초기화 금지 (stale state 방지)
                if (!st.buyPending && !st.sellPending) {
                    resetEntryState(st);
                    st.positionPhase = PositionPhase.NONE;
                }

                if (!ensureEntryReady(st, market, normalized, nowMs)) {
                    return Optional.empty();
                }

                BuySignal signal = st.entrySignal;
                if (signal == null) {
                    return Optional.empty();
                }

                if (!canBuy(st, normalized, market, now, signal)) {
                    return Optional.empty();
                }

                if (!passesTickEntryGate(st, normalized, market, currentPrice)) {
                    return Optional.empty();
                }

                double positionSize = determinePositionSize(signal, market);
                if (positionSize <= 0.0) {
                    logger.warn("BUY_SKIP [{}] {} reason=POSITION_SIZE_ZERO mode={}", market, normalized, signal.entryMode);
                    return Optional.empty();
                }

                double orderPrice = roundToTickSize(currentPrice, market);
                int qty = resolveBuyQuantity(st, orderPrice, positionSize);
                if (qty < 1) {
                    logger.warn("BUY_SKIP [{}] {} reason=AMOUNT_TOO_SMALL amount={} size={}x price={}",
                            market, normalized, fmt(st.buyAmountPerOrder), fmt(positionSize), fmt(orderPrice));
                    return Optional.empty();
                }

                st.lastBuySignalMs = nowMs;
                st.statsDailyExecCount++;
                updateEntryCounters(st, signal, now.toLocalDate());
                st.entryMode          = signal.entryMode;
                if (signal.entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS) {
                    st.entrySignalScore = signal.thirtyMinRsiBbScore;
                } else {
                    st.entrySignalScore = signal.signalScore;
                }
                int _sc = st.entrySignalScore;
                st.entrySignalGrade = _sc >= 95 ? "SS" : _sc >= 90 ? "S" : _sc >= 85 ? "A" : _sc >= 80 ? "B" : _sc >= 75 ? "C" : "D";
                st.entryVwapDistPct   = signal.vwapDistancePct;
                st.entryVelocityShort = signal.velocityShort;
                st.entryVolumeRatio   = signal.volumeRatio;
                st.entryTurnoverRatio = signal.turnoverRatio;
                st.entryScoreVwap     = signal.scoreVwap;
                st.entryScoreTrend    = signal.scoreTrend;
                st.entryScoreVolume   = signal.scoreVolume;
                st.entryScoreTurnover = signal.scoreTurnover;
                st.entryScoreHigh     = signal.scoreHigh;
                st.entryScorePattern  = signal.scorePattern;
                st.entryVelocityMid   = signal.velocityMid;
                st.entryVelocityLong  = signal.velocityLong;
                st.entryTrendScore    = signal.trendScore;
                st.entryFromHighPct   = signal.fromHighPct;
                st.entryQty           = qty;
                clearEntryReadyState(st);
                st.buyPending = true;
                st.buyPendingSinceMs = nowMs;
                st.positionPhase = PositionPhase.ENTERING;

                int logScore = signal.entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS
                        ? signal.thirtyMinRsiBbScore
                        : signal.signalScore;
                String logGrade = logScore >= 95 ? "SS" : logScore >= 90 ? "S"
                        : logScore >= 85 ? "A" : logScore >= 80 ? "B" : logScore >= 75 ? "C" : "D";
                boolean logHighConviction = signal.signalScore >= HIGH_CONVICTION_SCORE;
                boolean logBreakoutAccel = signal.velocityShort > signal.velocityMid;
                double logFromHighPct = signal.recentHigh > 0.0
                        ? (signal.price - signal.recentHigh) / signal.recentHigh : 0.0;
                double logAllowedVwapGap = signal.entryMode == EntryMode.BREAKOUT ? VWAP_MAX_GAP_BREAKOUT
                        : 0.05;
                String spVrLog = "";
                if (signal.entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS) {
                    spVrLog = String.format(" | 30M_RSI_BB: score=%d rsi=%.1f sig=%.1f bbLow=%.0f cross=%b vol=%.0f avgVol=%.0f",
                            signal.thirtyMinRsiBbScore, signal.thirtyMinRsi, signal.thirtyMinRsiSignal,
                            signal.thirtyMinBbLower, signal.thirtyMinRsiGoldenCross,
                            signal.thirtyMinCurrentVol, signal.thirtyMinAvgVol);
                } else if (signal.entryMode == EntryMode.RED_TO_GREEN) {
                    spVrLog = String.format(" | R2G: prevClose=%.0f crossPct=%.2f%% vol=%.0f avgVol=%.0f velShort=%.4f",
                            signal.r2gPrevClose, signal.r2gCrossPct * 100.0,
                            signal.volume, signal.averageVolume, signal.velocityShort);
                }
                logger.info(
                        "BUY [{}] {} mode={} price={} qty={} size={}x score={}{}" +
                        " | WHY: {}" +
                        " | VWAP: {}({} slope={}) allowedGap={}" +
                        " | TREND: score={} vel={}/{}/{} accel={}" +
                        " | VOL: {}/{} TO: {}/{}" +
                        " | HIGH: {} fromHigh={}" +
                        "{}" +
                        " | entryNo={}",
                        market, normalized,
                        signal.entryMode,
                        fmt(orderPrice), qty,
                        fmt(positionSize),
                        logScore,
                        "[" + logGrade + "]" + (logHighConviction ? "(CONV)" : ""),
                        buildBuyWhyString(signal),
                        fmtPct(signal.vwapDistancePct),
                        signal.aboveVwap ? "ABOVE" : "BELOW",
                        signal.vwapSlopeUp ? "UP" : "DN",
                        fmtPct(logAllowedVwapGap),
                        signal.trendScore,
                        fmtPct(signal.velocityShort),
                        fmtPct(signal.velocityMid),
                        fmtPct(signal.velocityLong),
                        logBreakoutAccel,
                        fmt(signal.volume), fmt(signal.averageVolume),
                        fmt(signal.latestTurnover), fmt(signal.averageTurnover),
                        fmt(signal.recentHigh),
                        fmtPct(logFromHighPct),
                        spVrLog,
                        st.dailyEntryCount
                );

                return Optional.of(new OrderCommand(
                        normalized,
                        qty,
                        "BUY",
                        orderPrice,
                        false,
                        "ENTRY"
                ));
            }

            if (avgPrice <= 0.0) {
                logger.warn("SELL_SKIP [{}] {} reason=INVALID_AVG_PRICE price={} qty={} avgPrice={}",
                        market,
                        normalized,
                        fmt(currentPrice),
                        currentQuantity,
                        fmt(avgPrice));
                return Optional.empty();
            }

            // phase=NONE인데 브로커가 qty>0을 계속 보내는 경우 (stale 알림) 무시
            // SELL_FILLED 이후 브로커 API 지연으로 잔량이 남아있는 것처럼 보이는 phantom 포지션 차단
            if (st.positionPhase == PositionPhase.NONE && !st.sellPending) {
                logger.warn("SELL_SKIP [{}] {} reason=PHANTOM_POSITION phase=NONE qty={} avgPrice={}",
                        market, normalized, currentQuantity, fmt(avgPrice));
                return Optional.empty();
            }

            refreshHoldingState(st, avgPrice, currentPrice, nowMs);

            if (st.sellPending) {
                long elapsed = nowMs - st.sellPendingSinceMs;
                if (elapsed < SELL_PENDING_TIMEOUT_MS) {
                    return Optional.empty();
                }
                // 타임아웃: 이전 주문 상태 정리 후 즉시 시장가 재매도 (이유 변경 조건 대기 없이)
                // forceMarketOnNextSell 방식은 가격 조건이 바뀌면 재매도 안 나갈 수 있어 위험
                if (avgPrice > 0.0) {
                    double tradePnlPct = (currentPrice * (1.0 - TIMEOUT_SLIPPAGE_BUFFER) - avgPrice) / avgPrice;
                    double soldNotional = avgPrice * currentQuantity;
                    double orderBase = st.buyAmountPerOrder > 0 ? st.buyAmountPerOrder : soldNotional;
                    st.pendingSellWeightedPnl = tradePnlPct * (soldNotional / orderBase);
                    st.pendingSellTradePnlPct = tradePnlPct;
                } else {
                    st.pendingSellWeightedPnl = 0.0;
                    st.pendingSellTradePnlPct = 0.0;
                }
                st.sellPending = false;
                st.sellPendingSinceMs = 0L;
                st.forceMarketOnNextSell = false;
                st.lastPendingSellReason = "SELL_TIMEOUT_MARKET_RETRY";
                st.pendingSellQty = currentQuantity;
                st.lastSellSignalMs = nowMs;
                st.sellPending = true;
                st.sellPendingSinceMs = nowMs;
                st.positionPhase = PositionPhase.EXITING;
                // US 해외주문은 정규장 중 시장가(ORD_DVSN=32) 불가 → 할인 지정가로 즉시 체결 시도
                double fallbackPrice;
                boolean fallbackMarket;
                if (market == Market.US && currentPrice > 0) {
                    fallbackPrice = roundToTickSize(currentPrice * (1.0 - TIMEOUT_SLIPPAGE_BUFFER), market);
                    fallbackMarket = false;
                } else {
                    fallbackPrice = 0.0;
                    fallbackMarket = true;
                }
                logger.warn("SELL timeout for {} — issuing immediate {} sell qty={} fallbackPrice={}",
                        normalized, fallbackMarket ? "MARKET" : "LIMIT", currentQuantity, fmt(fallbackPrice));
                return Optional.of(new OrderCommand(normalized, currentQuantity, "SELL", fallbackPrice, fallbackMarket, "SELL_TIMEOUT_MARKET_RETRY"));
            }

            if ((nowMs - st.lastSellSignalMs) < SELL_RETRY_COOLDOWN_MS) {
                return Optional.empty();
            }

            SellDecision sellDecision = evaluateSellDecision(st, market, normalized, currentPrice, currentQuantity, avgPrice, nowMs);

            if (!sellDecision.shouldSell) {
                return Optional.empty();
            }

            if (isProfitExitReason(sellDecision.reason)) {
                st.lastProfitExitTimeMs = nowMs;
            } else if (isTrailExitReason(sellDecision.reason)) {
                st.lastTrailExitTimeMs = nowMs;
            } else if (isStopLossExitReason(sellDecision.reason) || isTimeStopExitReason(sellDecision.reason)) {
                st.lastStopLossExitTimeMs = nowMs;
                st.lastStopLossExitReason = sellDecision.reason;
            }
            // 청산 이유를 enum으로 저장 (재진입 쿨다운 판단용)
            st.lastExitType = toExitType(sellDecision.reason);

            boolean forceMarket = false;
            if (st.forceMarketOnNextSell) {
                if (nowMs <= st.forceMarketUntilMs) {
                    forceMarket = true;
                }
                st.forceMarketOnNextSell = false;
                st.forceMarketUntilMs = 0L;
                st.forceMarketReason = null;
            }

            boolean marketOrder = sellDecision.marketOrder;
            if (!marketOrder && forceMarket && isTakeProfitReason(sellDecision.reason)) {
                marketOrder = true;
                logger.warn("SELL fallback to MARKET for {} reason={} (retry after unfilled LIMIT)",
                        normalized, sellDecision.reason);
            }

            // 여기서는 주문 의도만 저장
            st.lastPendingSellReason = sellDecision.reason;
            st.pendingSellQty = sellDecision.quantity;
            st.lastSellSignalMs = nowMs;
            st.sellPending = true;
            st.sellPendingSinceMs = nowMs;
            st.positionPhase = PositionPhase.EXITING;

            double orderPrice = marketOrder ? 0.0 : roundToTickSize(currentPrice, market);
            double logPrice = marketOrder ? currentPrice : orderPrice;

            double tradePnlPct = (currentPrice - avgPrice) / avgPrice;
            // 수량 가중 손익: 1주문 기준량(buyAmountPerOrder) 대비 실현 금액 비율로 스케일링
            double soldNotional = avgPrice * sellDecision.quantity;
            double orderBase = st.buyAmountPerOrder > 0 ? st.buyAmountPerOrder : soldNotional;
            double weightedPnl = tradePnlPct * (soldNotional / orderBase);
            // dailyPnl은 주문 생성 시점이 아닌 SELL_FILLED 수신 시점에 반영
            // (브로커 거절/타임아웃 시 롤백 불필요 — 미반영이므로)
            st.pendingSellWeightedPnl = weightedPnl;
            st.pendingSellTradePnlPct = tradePnlPct;
            long holdMs = st.entryTimeMs > 0 ? (nowMs - st.entryTimeMs) : 0L;
            double peakPnlRate = st.highestSinceEntry > 0.0 && avgPrice > 0.0
                    ? (st.highestSinceEntry - avgPrice) / avgPrice : 0.0;
            logger.info("SELL_EXECUTED [{}] {} mode={} phase={} exitType={} price={} qty={} reason={} pnl={} peakPnl={} holdMs={} high={} avgPrice={} weightedPnl={} (pending fill)",
                    market,
                    normalized,
                    st.entryMode,
                    st.positionPhase,
                    st.lastExitType,
                    fmt(logPrice),
                    sellDecision.quantity,
                    sellDecision.reason,
                    fmtPct(tradePnlPct),
                    fmtPct(peakPnlRate),
                    holdMs,
                    fmt(st.highestSinceEntry),
                    fmt(avgPrice),
                    fmtPct(weightedPnl));

            return Optional.of(new OrderCommand(
                    normalized,
                    sellDecision.quantity,
                    "SELL",
                    orderPrice,
                    marketOrder,
                    sellDecision.reason
            ));
        }
    }

    private BuySignal buildBuySignal(SymbolState st,
                                     Market market,
                                     double price,
                                     double volume,
                                     LocalDateTime now) {
        BuySignal signal = new BuySignal();
        signal.ticks = st.minuteHistory.size();
        signal.spanSeconds = historySpanSeconds(st.minuteHistory);
        signal.enoughHistory = st.minuteHistory.hasEnoughHistory(MIN_HISTORY_BARS, MIN_HISTORY_SPAN_SECONDS);

        signal.price = price;
        signal.volume = Math.max(0.0, volume);
        signal.averageVolume = st.minuteHistory.averageVolume(20);

        signal.velocity = st.minuteHistory.velocitySeconds(VELOCITY_WINDOW_MIN_SECONDS, VELOCITY_WINDOW_MAX_SECONDS);
        signal.velocityShort = st.minuteHistory.velocitySeconds(TREND_SHORT_MIN_SECONDS, TREND_SHORT_MAX_SECONDS);
        signal.velocityMid = st.minuteHistory.velocitySeconds(TREND_MID_MIN_SECONDS, TREND_MID_MAX_SECONDS);
        signal.velocityLong = st.minuteHistory.velocitySeconds(TREND_LONG_MIN_SECONDS, TREND_LONG_MAX_SECONDS);

        signal.shortUp = signal.velocityShort > 0.0;
        signal.midUp = signal.velocityMid > 0.0;
        signal.longUp = signal.velocityLong > 0.0;

        signal.trendScore = (signal.shortUp ? 1 : 0) + (signal.midUp ? 1 : 0) + (signal.longUp ? 1 : 0);
        signal.multiUptrend = signal.trendScore >= 2;

        signal.recentHigh = st.minuteHistory.highestHigh(BUY_RECENT_RANGE_BARS);
        signal.recentLow = st.minuteHistory.lowestLow(BUY_RECENT_RANGE_BARS);
        signal.latestTurnover = signal.price * signal.volume;
        signal.averageTurnover = st.minuteHistory.averageTurnover(20);

        signal.pullbackAvgShort = st.minuteHistory.averagePrice(3);
        signal.pullbackAvgLong = st.minuteHistory.averagePrice(6);

        signal.vwap = st.minuteHistory.sessionVwap();
        signal.prevVwap = st.minuteHistory.sessionVwapPrevious();
        signal.aboveVwap = signal.vwap > 0.0 && signal.price > signal.vwap;
        // VWAP slope: 단순히 같거나 미세하게 올라도 횡보 구간 — 최소 0.02% 이상 상승이어야 진짜 상승 VWAP
        signal.vwapSlopeUp = signal.vwap > 0.0 && signal.prevVwap > 0.0
                && ((signal.vwap - signal.prevVwap) / signal.prevVwap) >= VWAP_SLOPE_MIN_PCT;
        signal.vwapDistancePct = signal.vwap > 0.0 ? ((signal.price - signal.vwap) / signal.vwap) : 0.0;
        signal.nearVwap = signal.vwap > 0.0 && Math.abs(signal.vwapDistancePct) <= VWAP_NEAR_DISTANCE_PCT;
        // vwapTooFar: 8% 초과만 safety-net으로 차단. 실제 per-mode 상한은 isBuyCandidate에서 처리
        signal.vwapTooFar = signal.vwap > 0.0 && signal.vwapDistancePct > VWAP_TOO_FAR_HARD_LIMIT;

        signal.choppyMarket = Math.abs(signal.velocityShort) < 0.0005
                && Math.abs(signal.pullbackAvgShort - signal.pullbackAvgLong) < (signal.price * 0.001);
        signal.marketWeak = signal.velocityShort <= -0.001
                && signal.pullbackAvgShort < signal.pullbackAvgLong;

        if (!signal.enoughHistory) {
            signal.rejectReason = "NOT_ENOUGH_HISTORY";
            return signal;
        }

        signal.timeWindowBlocked = !passesTimeWindow(market, now);
        signal.slowModeTimeWindowBlocked = !passesSlowModeTimeWindow(market, now);
        // marketFilterPassed는 아래 컨텍스트 블록 끝에서 entryMode 확정 후 단 한 번 계산하여 할당
        // (여기서 true로 선초기화하면 중간 로직 추가 시 의도치 않게 true가 유지될 위험이 있음)
        signal.cheapStockBlocked = !passesCheapStockFilter(market, price);
        signal.turnoverFilterPassed = passesTurnoverFilter(signal, market);
        signal.absoluteLiquidityPassed = passesAbsoluteLiquidityFilter(signal, market);

        double lowVolumeSkipMult = lowVolumeSkipMult(market);
        signal.lowVolumeSkip = signal.averageVolume > 0.0
                && signal.volume < signal.averageVolume * lowVolumeSkipMult;

        // momentum
        signal.momentumNearHigh = signal.recentHigh > 0.0 && signal.price >= (signal.recentHigh * MOMENTUM_PRICE_NEAR_HIGH);
        signal.momentumVelocityOk = signal.velocityShort > 0.0 && signal.velocityMid >= 0.0;
        double effectiveVolMult = backtestConfig != null ? backtestConfig.volumeMult : MOMENTUM_VOLUME_MULT;
        signal.momentumVolumeOk = signal.averageVolume > 0.0 && signal.volume >= (signal.averageVolume * effectiveVolMult);
        signal.momentumBreakout = signal.momentumNearHigh && signal.momentumVelocityOk && signal.momentumVolumeOk;

        // 최근 3봉 양봉수 / 이전 2봉 고점 돌파 / 연속 상승 종가 계산
        {
            java.util.List<MinuteBarHistory.MinuteBar> recent3 = st.minuteHistory.latestBars(3);
            int bullish = 0;
            double prev2High = 0.0;
            for (int i = 0; i < recent3.size(); i++) {
                MinuteBarHistory.MinuteBar bar = recent3.get(i);
                if (bar.getClose() >= bar.getOpen()) bullish++;
                if (i < recent3.size() - 1) prev2High = Math.max(prev2High, bar.getHigh());
            }
            signal.recentBullishBars  = bullish;
            signal.recentHighBreakout = recent3.size() >= 3 && signal.price > prev2High;
            signal.consecutiveHigherClose = recent3.size() >= 3
                    && recent3.get(1).getClose() > recent3.get(0).getClose()
                    && recent3.get(2).getClose() > recent3.get(1).getClose();
        }
        signal.fromHighPct = signal.recentHigh > 0.0
                ? (signal.price / signal.recentHigh - 1.0) * 100.0 : 0.0;

        // volume breakout
        signal.volumeBreakNearHigh = signal.recentHigh > 0.0 && signal.price >= (signal.recentHigh * 0.9970);
        signal.volumeBreakVelocityOk = signal.velocityShort > 0.0 && signal.velocityMid >= 0.0;
        signal.volumeBreakVolumeOk = signal.averageVolume > 0.0 && signal.volume >= (signal.averageVolume * VOLUME_BREAKOUT_VOLUME_MULT);

        signal.strongBreakout =
                signal.volumeBreakNearHigh
                        && signal.volumeBreakVelocityOk
                        && signal.averageVolume > 0.0
                        && signal.volume >= (signal.averageVolume * STRONG_BREAKOUT_VOLUME_MULT);

        signal.volumeBreakout =
                signal.volumeBreakNearHigh
                        && signal.volumeBreakVelocityOk
                        && signal.volumeBreakVolumeOk;

        // breakout retest
        double breakoutRetestUpper = signal.recentHigh > 0.0 ? (signal.recentHigh * BREAKOUT_RETEST_UPPER) : 0.0;
        double breakoutRetestLower = signal.recentHigh > 0.0 ? (signal.recentHigh * BREAKOUT_RETEST_LOWER) : 0.0;
        signal.breakoutRetestReady =
                signal.recentHigh > 0.0
                        && signal.price >= breakoutRetestLower
                        && signal.price <= breakoutRetestUpper;

        signal.breakoutRetestRecovering =
                signal.breakoutRetestReady
                        && signal.aboveVwap
                        && signal.vwapSlopeUp
                        && signal.velocityShort > 0.0
                        && signal.pullbackAvgShort >= signal.pullbackAvgLong
                        && signal.price >= signal.pullbackAvgShort;

        // ── BREAKOUT 과열 차단용 BB/RSI 지표 계산 ────────────────────────
        {
            MinuteBarHistory.BollingerBands bb = st.minuteHistory.computeFiveMinuteBollingerBands(20, 2.0);
            MinuteBarHistory.RsiResult rsiR    = st.minuteHistory.computeFiveMinuteRsiSignal(14, 9);
            signal.bbUpper = bb.upper;
            signal.rsi     = rsiR.rsi;
        }

        // ── THIRTY_MIN_RSI_BB_CROSS 신호 계산 ─────────────────────────────────────────
        {
            // ← 하드코딩(14,9,20) 제거: KRX 하루 390봉→30분봉 13개 → 기존 기간으로는 valid=false 불가피
            // backtestConfig 값 사용, 기본값은 하루 내 산출 가능한 축소 기간(5,3,8)
            int rsiPeriod = backtestConfig != null ? backtestConfig.rsiBb30mRsiPeriod    : 5;
            int sigPeriod = backtestConfig != null ? backtestConfig.rsiBb30mSignalPeriod : 3;
            int bbPeriod  = backtestConfig != null ? backtestConfig.rsiBb30mBbPeriod     : 8;
            double bbMult = backtestConfig != null ? backtestConfig.rsiBb30mStdMult      : 2.0;
            int volBars   = backtestConfig != null ? backtestConfig.rsiBb30mVolBars      : 20;
            // minBars 체크 제거 — bb30.valid / rsi30.valid 이 데이터 부족 시 이미 false 반환

            MinuteBarHistory.BollingerBands bb30  = st.minuteHistory.computeThirtyMinuteBollingerBands(bbPeriod, bbMult);
            MinuteBarHistory.RsiResult      rsi30 = st.minuteHistory.computeThirtyMinuteRsiSignal(rsiPeriod, sigPeriod);

            signal.thirtyMinBbLower       = bb30.valid  ? bb30.lower       : 0;
            signal.thirtyMinBbMiddle      = bb30.valid  ? bb30.middle      : 0;
            signal.thirtyMinRsi           = rsi30.valid ? rsi30.rsi        : 0;
            signal.thirtyMinRsiSignal     = rsi30.valid ? rsi30.rsiSignal  : 0;
            // RSI가 신호선 위 + 건강한 범위 + 최소 격차(크로스 직후 확인용)
            signal.thirtyMinRsiGoldenCross = rsi30.valid
                    && rsi30.rsi > rsi30.rsiSignal
                    && (rsi30.rsi - rsi30.rsiSignal) >= 1.5
                    && rsi30.rsi >= 45.0 && rsi30.rsi <= 68.0; // (← 40~75: 과열/과매도 구간 제외)

            double curVol30 = st.minuteHistory.latestCompleteThirtyMinuteVolume();
            double avgVol30 = st.minuteHistory.thirtyMinuteAverageVolume(volBars);
            signal.thirtyMinCurrentVol = curVol30;
            signal.thirtyMinAvgVol     = avgVol30;

            boolean goldenCross  = rsi30.valid && rsi30.rsi > rsi30.rsiSignal
                    && (rsi30.rsi - rsi30.rsiSignal) >= 1.5
                    && rsi30.rsi >= 45.0 && rsi30.rsi <= 68.0; // (← 40~75: 과열/과매도 구간 제외)
            boolean bbLowerTouch = bb30.valid && price <= bb30.lower * 1.02;
            boolean volOk        = avgVol30 > 0 && curVol30 > avgVol30 * 0.8; // ← 1.0배: 평균의 80% 이상으로 완화

            signal.thirtyMinRsiBbCondsMet =
                    bb30.valid && rsi30.valid   // minBars 대신 .valid 로 충분성 검증
                    && goldenCross
                    && bbLowerTouch
                    && volOk;

            int sc30 = 0;
            if (goldenCross)   sc30 += 50;
            if (bbLowerTouch)  sc30 += 30;
            if (volOk)         sc30 += 20;
            signal.thirtyMinRsiBbScore = Math.min(sc30, 100);
        }

        // ── OPENING_RANGE_BREAKOUT 조건 계산 (우선순위 할당보다 먼저 수행) ───
        {
            LocalTime barTime = now.toLocalTime();
            boolean orbTimeOk  = market == Market.KRX
                    && st.openingRangeReady
                    && !barTime.isBefore(LocalTime.of(9, 30))   // (← 09:15: 장 초반 변동성 구간 09:15~09:30 제외)
                    && barTime.isBefore(LocalTime.of(10, 0));   // ← 10:30: ORB 09:30~10:00 전용
            boolean orbAbove   = st.openingRangeHigh > 0.0 && price > st.openingRangeHigh * 1.001; // (← 1.003)
            double inlineVolRatio = signal.averageVolume > 0.0 ? signal.volume / signal.averageVolume : 0.0;
            signal.orbBreakout = orbTimeOk
                    && orbAbove
                    && signal.aboveVwap
                    && signal.vwapSlopeUp
                    && inlineVolRatio >= 2.0              // (← 2.5)
                    && signal.trendScore >= 2
                    && signal.velocityShort > signal.velocityMid
                    && signal.recentBullishBars >= 2      // 직전 3봉 중 2봉 이상 양봉
                    && signal.vwapDistancePct <= 0.018;
        }

        // ── RED_TO_GREEN 신호 계산 ────────────────────────────────────────────
        {
            double prevClose = st.minuteHistory.prevDayLastClose();
            if (prevClose > 0.0) {
                double crossPct = (price - prevClose) / prevClose;
                boolean timeOk = market == Market.KRX
                        && !now.toLocalTime().isBefore(java.time.LocalTime.of(9, 10))
                        && now.toLocalTime().isBefore(java.time.LocalTime.of(11, 0)); // (← 13:00: 장 초반 전일종가 돌파만 유효)
                signal.r2gPrevClose = prevClose;
                signal.r2gCrossPct  = crossPct;
                // 진짜 R2G: 오늘 시가가 전일 종가 아래 (RED 출발) → 장중 전일 종가 돌파 (GREEN 전환)
                boolean cWasBelowPrevClose = st.dayOpenPrice > 0.0 && st.dayOpenPrice < prevClose;
                signal.r2gCross = timeOk
                        && cWasBelowPrevClose
                        && crossPct >= R2G_MIN_CROSS_PCT
                        && crossPct <= (backtestConfig != null ? backtestConfig.r2gMaxCrossPct : R2G_MAX_CROSS_PCT)
                        && signal.averageVolume > 0.0
                        && signal.volume >= signal.averageVolume * R2G_MIN_VOLUME_MULT
                        && signal.velocityShort > 0.0;
            }
        }

        // ── 1. 사전 점수 계산 (EntryMode 확정 이전 — modeScore() default 케이스가 사용) ──────────────
        // 순서 fix: 이전에는 chooseSignal() 이후에 계산되어 ORB/BREAKOUT 등 default 케이스가 0점을 반환했음

        // VWAP 영역 (max 20)
        signal.scoreVwap = 0;
        if (signal.aboveVwap)   signal.scoreVwap += 10;
        if (signal.vwapSlopeUp) signal.scoreVwap += 10;

        // 추세 영역 (max 25)
        signal.scoreTrend = 0;
        if (signal.velocityShort > 0.0)    signal.scoreTrend += 5;
        if (signal.velocityShort >= 0.002) signal.scoreTrend += 5;
        if (signal.velocityShort >= 0.004) signal.scoreTrend += 5;
        if (signal.trendScore >= 2)        signal.scoreTrend += 5;
        if (signal.trendScore == 3)        signal.scoreTrend += 5;

        // 거래량 영역 (max 15)
        signal.volumeRatio = signal.averageVolume > 0.0
                ? signal.volume / signal.averageVolume : 0.0;
        signal.scoreVolume = 0;
        if (signal.volumeRatio >= 1.2) signal.scoreVolume += 3;
        if (signal.volumeRatio >= 1.5) signal.scoreVolume += 3;
        if (signal.volumeRatio >= 2.0) signal.scoreVolume += 4;
        if (signal.volumeRatio >= 3.0) signal.scoreVolume += 5;

        // 거래대금 영역 (max 10)
        signal.turnoverRatio = signal.averageTurnover > 0.0
                ? signal.latestTurnover / signal.averageTurnover : 0.0;
        signal.scoreTurnover = 0;
        if (signal.turnoverRatio >= 1.2) signal.scoreTurnover += 3;
        if (signal.turnoverRatio >= 1.5) signal.scoreTurnover += 3;
        if (signal.turnoverRatio >= 2.0) signal.scoreTurnover += 4;

        // 고점 위치 영역 (max 15)
        signal.scoreHigh = 0;
        if (signal.recentHigh > 0.0) {
            if (signal.price >= signal.recentHigh * 0.980) signal.scoreHigh += 5;
            if (signal.price >= signal.recentHigh * 0.990) signal.scoreHigh += 5;
            if (signal.price >= signal.recentHigh * 0.995) signal.scoreHigh += 5;
        }

        // 패턴 보너스 (BREAKOUT+consecutiveHigherClose 보너스는 entryMode 확정 후 step 3에서 추가)
        signal.scorePattern = 0;
        if (signal.pullbackEntry)  signal.scorePattern += 5;
        if (signal.volumeBreakout) signal.scorePattern += 5;
        if (signal.strongBreakout) signal.scorePattern += 5;

        // 감점 (공통 — entryMode별 예외는 확정 후 step 3에서 환원)
        int deductions = 0;
        if (signal.marketWeak)   deductions += 15;
        if (signal.choppyMarket) deductions += 8;
        if (signal.vwapDistancePct > 0.020)      deductions += 30; // >2.0%: -30
        else if (signal.vwapDistancePct > 0.012) deductions += 10; // >1.2%: -10
        if (signal.recentHigh > 0.0 && signal.fromHighPct > -0.2) deductions += 10;
        if (signal.vwapDistancePct > 0.018) deductions += 10; // PULLBACK 면제는 step 3 환원
        {
            MinuteBarHistory.MinuteBar latestBar = st.minuteHistory.latest();
            if (latestBar != null) {
                double barHigh  = latestBar.getHigh();
                double barLow   = latestBar.getLow();
                double barClose = latestBar.getClose();
                if (barHigh > barLow && (barHigh - barClose) / (barHigh - barLow) > 0.4) {
                    deductions += 12; // EARLY_MOMENTUM 면제는 step 3 환원
                }
            }
        }
        if (signal.velocityMid > 0.0 && signal.velocityShort < signal.velocityMid * 0.5) {
            deductions += 8;
        }
        int rawScore = signal.scoreVwap + signal.scoreTrend + signal.scoreVolume
                + signal.scoreTurnover + signal.scoreHigh + signal.scorePattern - deductions;
        signal.signalScore = Math.min(Math.max(rawScore, 0), 100);

        // ── 2. EntryMode 확정 (전략 독립 평가 → 점수 기반 최적 신호 선택) ─────────────────────────
        {
            java.util.List<EntryMode> candidates = new java.util.ArrayList<>();
            if (candidateORB(signal, market, now))      candidates.add(EntryMode.OPENING_RANGE_BREAKOUT);
            if (candidateBreakout(signal, market, now)) candidates.add(EntryMode.BREAKOUT);
            if (candidate30M(signal, now))              candidates.add(EntryMode.THIRTY_MIN_RSI_BB_CROSS);
            if (candidateR2G(signal, market, now))      candidates.add(EntryMode.RED_TO_GREEN);
            signal.signalCount = candidates.size();
            signal.entryMode   = chooseSignal(candidates, signal);
        }

        // ── 3. EntryMode 확정 후 점수 보정 ───────────────────────────────────────────────────────
        // BREAKOUT: 연속 고가 마감 보너스 (entryMode 확정 후에만 의미 있음)
        if (signal.entryMode == EntryMode.BREAKOUT && signal.consecutiveHigherClose) {
            signal.scorePattern += 5;
            signal.signalScore = Math.min(signal.signalScore + 5, 100);
        }
        // ── 5. 시장 컨텍스트 필터 (entryMode 확정 후 단 한 번) ───────────────
        {
            MarketContext mCtx = marketContext.get(market);
            boolean ctxWeak   = mCtx != null && !isMarketContextExpired(mCtx) && mCtx.marketWeak;
            // 역추세 모드는 약세장이 진입 조건의 일부 — 시장 필터 면제
            boolean isCounterTrend = signal.entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS
                    || signal.entryMode == EntryMode.RED_TO_GREEN;
            boolean marketPass = !(ctxWeak && !isCounterTrend);
            signal.marketFilterPassed = marketPass;
        }

        if (!signal.enoughHistory) {
            signal.rejectReason = "NOT_ENOUGH_HISTORY";
        } else if (signal.effectiveTimeWindowBlocked()) {
            signal.rejectReason = "TIME_WINDOW_BLOCKED";
        } else if (!signal.marketFilterPassed) {
            signal.rejectReason = "MARKET_FILTER_BLOCKED";
        } else if (signal.cheapStockBlocked) {
            signal.rejectReason = "CHEAP_STOCK_BLOCKED";
        } else if (!signal.turnoverFilterPassed) {
            signal.rejectReason = "TURNOVER_FILTER_BLOCKED";
        } else if (!signal.absoluteLiquidityPassed) {
            signal.rejectReason = "ABSOLUTE_LIQUIDITY_BLOCKED";
        } else if (signal.lowVolumeSkip) {
            signal.rejectReason = "LOW_VOLUME_SKIP";
        } else if (!signal.aboveVwap
                && signal.entryMode != EntryMode.RED_TO_GREEN) {
            signal.rejectReason = "BELOW_VWAP";
        } else if (!signal.vwapSlopeUp
                && signal.entryMode != EntryMode.BREAKOUT
                && signal.entryMode != EntryMode.RED_TO_GREEN
                && signal.entryMode != EntryMode.THIRTY_MIN_RSI_BB_CROSS) {
            signal.rejectReason = "VWAP_SLOPE_DOWN";
        } else if (signal.vwapTooFar) {
            signal.rejectReason = "VWAP_TOO_FAR_EXTREME";
        } else if (signal.entryMode == EntryMode.NONE) {
            signal.rejectReason = "NO_ENTRY_MODE";
        } else {
            // BREAKOUT / ORB / 30M / R2G diagnosis
            boolean highConviction = signal.signalScore >= HIGH_CONVICTION_SCORE;
            if (signal.entryMode == EntryMode.BREAKOUT) {
                double effVolRatio      = VOLUME_RATIO_BREAKOUT * (highConviction ? HIGH_CONVICTION_VOLUME_MULT : 1.0);
                double effTurnoverRatio = TURNOVER_RATIO_BREAKOUT * (highConviction ? HIGH_CONVICTION_TURNOVER_MULT : 1.0);
                int effMinScore = backtestConfig != null ? backtestConfig.breakoutMinScore : 85;
                if (signal.vwapDistancePct > VWAP_MAX_GAP_BREAKOUT) {
                    signal.rejectReason = "VWAP_GAP_TOO_LARGE";
                } else if (signal.averageVolume > 0.0 && signal.volume < signal.averageVolume * effVolRatio) {
                    signal.rejectReason = "VOL_RATIO_LOW";
                } else if (signal.averageTurnover > 0.0 && signal.latestTurnover < signal.averageTurnover * effTurnoverRatio) {
                    signal.rejectReason = "TURNOVER_RATIO_LOW";
                } else if (signal.signalScore < effMinScore) {
                    signal.rejectReason = "SCORE_LOW";
                } else if (!signal.breakoutRetestReady || !signal.breakoutRetestRecovering) {
                    signal.rejectReason = "BREAKOUT_RETEST_FAIL";
                } else if ((backtestConfig == null || backtestConfig.breakoutRequireMultiUptrend) && !signal.multiUptrend) {
                    signal.rejectReason = "BREAKOUT_NO_MULTITREND";
                } else if (market == Market.KRX && signal.vwapDistancePct < BREAKOUT_KRX_MIN_VWAP_GAP) {
                    signal.rejectReason = "BREAKOUT_VWAP_GAP_LOW";
                } else if (market == Market.KRX && (signal.averageVolume <= 0.0 || signal.volume < signal.averageVolume * (backtestConfig != null ? backtestConfig.breakoutKrxVolMult : BREAKOUT_KRX_MIN_VOLUME_MULT))) {
                    signal.rejectReason = "BREAKOUT_VOLUME_LOW";
                } else {
                    signal.rejectReason = "FILTER_LOW";
                }
            } else {
                signal.rejectReason = "FILTER_LOW";
            }
        }

        String _timeSlot = now.toLocalTime().isBefore(LocalTime.of(11, 0)) ? "AM" : "PM";
        signal.patternKey = signal.entryMode.name() + "|" + _timeSlot + "|TREND=" + signal.trendScore;

        return signal;
    }

    // ── 전략 후보 선택 메서드 (각 전략이 독립적으로 조건 판단) ──────────────────────────
    private boolean candidateORB(BuySignal sig, Market market, LocalDateTime now) {
        boolean enabled = backtestConfig != null ? backtestConfig.enableOpeningRangeBreakout : ENABLE_OPENING_RANGE_BREAKOUT;
        return enabled && sig.orbBreakout;
    }

    private boolean candidateBreakout(BuySignal sig, Market market, LocalDateTime now) {
        boolean enabled = backtestConfig != null ? backtestConfig.enableBreakout : ENABLE_BREAKOUT_ENTRY;
        if (!enabled) return false;
        if (market == Market.KRX && now.toLocalTime().isBefore(LocalTime.of(9, 30))) return false; // (← 10:01: 핵심 돌파 구간 09:30~10:00 복구)
        boolean hasSignal = sig.volumeBreakout || sig.strongBreakout
                || (sig.breakoutRetestReady && sig.breakoutRetestRecovering);
        if (!hasSignal) return false;
        boolean cfgMultiUp = backtestConfig != null ? backtestConfig.breakoutRequireMultiUptrend : false;
        return cfgMultiUp ? sig.trendScore == 3 : sig.trendScore >= 2;
    }

    private boolean candidate30M(BuySignal sig, LocalDateTime now) {
        boolean enabled = backtestConfig != null ? backtestConfig.enable30mRsiBbCross : ENABLE_30M_RSI_BB_CROSS_ENTRY;
        if (!enabled) return false;
        // 10:00~13:30 시간창 (← 09:00~13:00: ORB 시간창 충돌 해소, 장 안정 후 RSI 패턴 유효)
        java.time.LocalTime t = now.toLocalTime();
        if (t.isBefore(java.time.LocalTime.of(10, 0)) || !t.isBefore(java.time.LocalTime.of(13, 30))) return false;
        int minSc = backtestConfig != null ? backtestConfig.rsiBb30mMinScore : THIRTY_MIN_RSI_BB_MIN_SCORE;
        return sig.thirtyMinRsiBbCondsMet && sig.thirtyMinRsiBbScore >= minSc;
    }

    private boolean candidateR2G(BuySignal sig, Market market, LocalDateTime now) {
        boolean enabled = backtestConfig != null ? backtestConfig.enableR2G : ENABLE_R2G_ENTRY;
        if (!enabled || !sig.r2gCross) return false;
        // VWAP 아래 진입 금지 — 전일종가 돌파했어도 당일 추세가 하락 중이면 제외
        if (sig.vwap > 0.0 && sig.price < sig.vwap * 0.998) return false;
        return true;
    }

    private EntryMode chooseSignal(java.util.List<EntryMode> candidates, BuySignal sig) {
        if (candidates.isEmpty()) return EntryMode.NONE;
        if (candidates.size() == 1) return candidates.get(0);
        return candidates.stream()
                .max(java.util.Comparator.comparingInt(m -> modeScore(m, sig)))
                .orElse(EntryMode.NONE);
    }

    private int modeScore(EntryMode mode, BuySignal sig) {
        switch (mode) {
            case THIRTY_MIN_RSI_BB_CROSS: return sig.thirtyMinRsiBbScore;
            case BREAKOUT:                return sig.signalScore + 3;
            default:                      return sig.signalScore;
        }
    }

    private boolean isEntryReady(BuySignal signal, Market market) {
        if (!signal.enoughHistory) return false;
        if (signal.effectiveTimeWindowBlocked()) return false;
        if (!signal.marketFilterPassed) return false; // buildBuySignal에서 모드별로 설정됨
        if (signal.cheapStockBlocked) return false;
        if (!signal.turnoverFilterPassed) return false;
        if (!signal.absoluteLiquidityPassed) return false;
        if (signal.lowVolumeSkip) return false;
        // 시장 컨텍스트 하드 차단 제거:
        //   - marketWeak: PULLBACK/STRONG_PULLBACK/VWAP_RECLAIM은 허용(감점만), BREAKOUT/EARLY_MOMENTUM은 차단
        //   - choppyMarket: 점수 패널티(-8)로 처리 (하드 차단 불필요)
        return signal.isBuyCandidate(market, backtestConfig);
    }

    // Avoid NO_ENTRY_READY spam when record() lags behind tick calls
    private boolean ensureEntryReady(SymbolState st,
                                     Market market,
                                     String symbol,
                                     long nowMs) {
        if (st.entrySignal == null) {
            MinuteBarHistory.MinuteBar latest = st.minuteHistory.latest();
            if (latest != null
                    && st.lastHistoryRebuildBarTs != null
                    && st.lastHistoryRebuildBarTs.equals(latest.getTimestamp())) {
                logger.debug("BUY_WAIT [{}] {} reason=REBUILD_ALREADY barTs={}", market, symbol, latest.getTimestamp());
                return false;
            }
            if (!tryBuildEntryReadyFromHistory(st, market, symbol, nowMs)) {
                return false;
            }
        }

        if (st.entryReadyUntilMs > 0 && nowMs > st.entryReadyUntilMs) {
            clearEntryReadyState(st);
            return false;
        }

        if (backtestNowMs <= 0 && nowMs < st.entryReadyAtMs + ENTRY_READY_MIN_DELAY_MS) {
            return false;
        }

        return true;
    }

    private boolean tryBuildEntryReadyFromHistory(SymbolState st,
                                                  Market market,
                                                  String symbol,
                                                  long nowMs) {
        MinuteBarHistory.MinuteBar latest = st.minuteHistory.latest();
        if (latest == null) {
            return false;
        }
        if (st.lastHistoryRebuildBarTs != null && st.lastHistoryRebuildBarTs.equals(latest.getTimestamp())) {
            return false;
        }
        st.lastHistoryRebuildBarTs = latest.getTimestamp();

        BuySignal signal = buildBuySignal(st, market, latest.getClose(), latest.getVolume(), latest.getTimestamp());
        if (!isEntryReady(signal, market)) {
            return false;
        }

        st.entrySignal = signal;
        st.entryReadyAtMs = nowMs;
        st.entryReadyUntilMs = nowMs + ENTRY_READY_TTL_MS;
        st.entryReadyClose = latest.getClose();
        st.entryReadyPatternKey = signal.patternKey;
        st.lastEntryReadyBarTs = latest.getTimestamp();

        return true;
    }

    private void clearEntryReadyState(SymbolState st) {
        st.entrySignal = null;
        st.entryReadyAtMs = 0L;
        st.entryReadyUntilMs = 0L;
        st.entryReadyClose = 0.0;
        st.entryReadyPatternKey = null;
        // lastEntryReadyBarTs is kept to avoid re-evaluating the same bar repeatedly
    }

    private boolean passesTickEntryGate(SymbolState st,
                                        String symbol,
                                        Market market,
                                        double currentPrice) {
        BuySignal signal = st.entrySignal;
        if (signal == null) {
            return false;
        }

        // 신호 발생 기준가(entryReadyClose) 대비 현재 가격 이탈 방지
        // - US 돌파는 -1.0%까지 재테스트 허용, 그 외 모드는 -0.2% 이상 밀리면 신호 무효
        // - BREAKOUT/VOLUME_BREAKOUT: +0.8% 허용 (강한 돌파 시 빠르게 이탈 → 못 잡는 문제 완화)
        // - 그 외 모드: +0.5% 허용 (기존 유지)
        if (st.entryReadyClose > 0.0) {
            double moveFromReady = (currentPrice - st.entryReadyClose) / st.entryReadyClose;
            double maxOvershoot = signal.entryMode == EntryMode.BREAKOUT ? 0.008 : 0.005;
            double maxPullback = -0.002;
            if (moveFromReady < maxPullback || moveFromReady > maxOvershoot) {
                return false;
            }
        }

        List<PriceHistory.Tick> recentTicks = st.tickHistory.latestTicks(3);
        if (recentTicks.size() < 2) {
            return false;
        }

        double p2 = recentTicks.get(recentTicks.size() - 2).getPrice();
        double p3 = recentTicks.get(recentTicks.size() - 1).getPrice();

        // Breakout / VolumeBreakout: 직전 틱 하락하지 않으면 진입 허용
        boolean twoTickOk = p2 <= p3;
        if (!twoTickOk) {
            return false;
        }

        if (signal.entryMode == EntryMode.BREAKOUT) {
            if (!signal.breakoutRetestReady || !signal.breakoutRetestRecovering) {
                return false;
            }
            return currentPrice >= signal.recentHigh * BREAKOUT_RETEST_LOWER
                    && currentPrice <= signal.recentHigh * BREAKOUT_RETEST_UPPER
                    && currentPrice >= p2;
        }

        if (signal.entryMode == EntryMode.OPENING_RANGE_BREAKOUT) {
            // ORB 돌파: 직전 틱 이상 유지 (강한 돌파이므로 조건 단순화)
            return currentPrice >= p2;
        }

        if (signal.entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS) {
            // 30분봉 RSI BB 크로스: 직전 틱 이상 유지
            return currentPrice >= p2;
        }

        if (signal.entryMode == EntryMode.RED_TO_GREEN) {
            // R2G: 전일종가 위 유지 + 직전 틱 이상
            return currentPrice >= p2
                    && signal.r2gPrevClose > 0.0
                    && currentPrice > signal.r2gPrevClose;
        }

        return false;
    }

    private boolean canBuy(SymbolState st,
                           String symbol,
                           Market market,
                           LocalDateTime now,
                           BuySignal signal) {
        if (!signal.enoughHistory) {
            return false;
        }

        long nowMs = nowMs();
        resetDailyEntryIfNeeded(st, now.toLocalDate());

        // 일일 손익 한도 초과 시 신규 진입 전면 차단
        double todayPnl = dailyPnlAccumulator.getOrDefault(now.toLocalDate(), 0.0);
        double effectiveDailyLossLimit  = backtestConfig != null ? -backtestConfig.maxDailyLossPct / 100.0  : -MAX_DAILY_LOSS_PCT;
        double effectiveDailyProfitGoal = backtestConfig != null ?  backtestConfig.maxDailyProfitPct / 100.0 : MAX_DAILY_PROFIT_PCT;
        if (todayPnl <= effectiveDailyLossLimit) {
            logger.warn("BUY_SKIP [{}] {} reason=DAILY_LOSS_LIMIT dailyPnl={}",
                    market, symbol, fmtPct(todayPnl));
            return false;
        }
        if (todayPnl >= effectiveDailyProfitGoal) {
            logger.warn("BUY_SKIP [{}] {} reason=DAILY_PROFIT_TARGET_HIT dailyPnl={}",
                    market, symbol, fmtPct(todayPnl));
            return false;
        }

        // STOP_LOSS / VWAP_BREAK / EMERGENCY_STOP 발생 당일 동일 종목 재진입 금지
        boolean sameDayBlock = backtestConfig != null ? backtestConfig.blockSameDayAfterStop : true;
        if (sameDayBlock && st.lastStopLossExitTimeMs > 0) {
            String exitRsn = st.lastStopLossExitReason;
            if ("VWAP_BREAK".equals(exitRsn) || "EMERGENCY_STOP".equals(exitRsn)
                    || (exitRsn != null && exitRsn.startsWith("STOP_LOSS_"))) {
                ZoneId zone = market == Market.KRX ? KST_ZONE : NY_ZONE;
                java.time.LocalDate exitDay = Instant.ofEpochMilli(st.lastStopLossExitTimeMs).atZone(zone).toLocalDate();
                if (exitDay.equals(now.toLocalDate())) {
                    logger.debug("BUY_SKIP [{}] {} reason=SAME_DAY_STOP_BLOCK exitReason={}", market, symbol, exitRsn);
                    return false;
                }
            }
        }

        int effectiveMaxDaily = backtestConfig != null ? backtestConfig.maxDailyEntryCount : MAX_DAILY_ENTRY_COUNT;
        if (st.dailyEntryCount >= effectiveMaxDaily) {
            return false;
        }

        if (st.lastTrailExitTimeMs > 0 && nowMs - st.lastTrailExitTimeMs < REENTER_TRAIL_COOLDOWN_MS) {
            return false;
        }

        if (st.lastProfitExitTimeMs > 0 && nowMs - st.lastProfitExitTimeMs < REENTER_PROFIT_COOLDOWN_MS) {
            return false;
        }

        if (st.lastStopLossExitTimeMs > 0) {
            long cooldown = resolveStopLossCooldown(st.lastStopLossExitReason);
            if (nowMs - st.lastStopLossExitTimeMs < cooldown) {
                return false;
            }
        }

        int effectiveMaxSame = backtestConfig != null ? backtestConfig.maxSamePatternEntry : MAX_SAME_PATTERN_ENTRY_COUNT;
        if (signal.patternKey != null
                && signal.patternKey.equals(st.lastEntryPatternKey)
                && st.samePatternEntryCount >= effectiveMaxSame) {
            return false;
        }

        if (st.buyPending) {
            long elapsed = nowMs - st.buyPendingSinceMs;
            if (elapsed < PENDING_TIMEOUT_MS) {
                return false;
            }
            st.buyPending = false;
            st.buyPendingSinceMs = 0L;
            logger.warn("BUY pending timeout cleared for {}", symbol);
        }

        long effectiveCooldownMs = backtestConfig != null ? backtestConfig.buyCooldownSec * 1000L : BUY_COOLDOWN_MS;
        long cooldownLeft = effectiveCooldownMs - (nowMs - st.lastBuySignalMs);
        if (cooldownLeft > 0) {
            return false;
        }

        // During KRX opening window (9:00~9:15), 강화 조건 적용
        if (market == Market.KRX && isKrMarketCautiousWindow(now)) {
            boolean cautiousPassed = (signal.signalCount >= 2 || signal.strongBreakout)
                    && signal.multiUptrend
                    && signal.velocity >= strongVelocityMin(market)
                    && signal.volume >= signal.averageVolume * STRONG_BREAKOUT_VOLUME_MULT;
            if (!cautiousPassed) return false;
        }

        if (!signal.marketFilterPassed) {
            return false;
        }

        if (signal.effectiveTimeWindowBlocked()) {
            return false;
        }

        if (signal.cheapStockBlocked) {
            return false;
        }

        if (!signal.turnoverFilterPassed) {
            return false;
        }

        if (!signal.absoluteLiquidityPassed) {
            return false;
        }

        if (signal.lowVolumeSkip) {
            return false;
        }

        if (!signal.isBuyCandidate(market, backtestConfig)) {
            return false;
        }

        return true;
    }

    private SellDecision evaluateSellDecision(SymbolState st,
                                              Market market,
                                              String symbol,
                                              double currentPrice,
                                              int currentQuantity,
                                              double avgPrice,
                                              long nowMs) {
        if (currentQuantity <= 0 || avgPrice <= 0.0 || currentPrice <= 0.0) {
            return SellDecision.none();
        }

        double pnlMult = currentPrice / avgPrice;
        double pnlRate = (currentPrice - avgPrice) / avgPrice;
        double sessionVwap = st.minuteHistory.sessionVwap();
        long holdMs = st.entryTimeMs > 0 ? (nowMs - st.entryTimeMs) : 0L;
        // 트레일 판단은 반드시 highestSinceEntry 기준 (현재 수익률 기준 금지)
        double peakProfitRate = st.highestSinceEntry > 0.0
                ? (st.highestSinceEntry - avgPrice) / avgPrice : 0.0;

        long effectiveVwapGraceMs = backtestConfig != null ? backtestConfig.vwapBreakGraceSec * 1000L : VWAP_BREAK_GRACE_MS;
        boolean vwapBreakEnabled = backtestConfig == null || backtestConfig.useVwapBreak;
        double effectiveVwapBreakBuffer = backtestConfig != null ? 1.0 - backtestConfig.vwapBreakBuffer / 100.0 : VWAP_BREAK_SELL_BUFFER;

        ExitProfile profile = exitProfileFor(st.entryMode, market);
        logger.debug("EXIT_PROFILE [{}] {} mode={} stop={} tp={} trailStart={} trailDrop={} useTrail={}",
                market, symbol, st.entryMode,
                fmt(profile.stopLossMult),
                profile.takeProfitMult  != null ? fmt(profile.takeProfitMult)    : "none",
                profile.trailStartProfit != null ? fmtPct(profile.trailStartProfit) : "none",
                profile.trailDropFromHigh != null ? fmtPct(profile.trailDropFromHigh) : "none",
                profile.useTrailing);

        // 1. Emergency stop (모든 모드 공통, 항상 적용)
        double effectiveEmStopMult = backtestConfig != null ? 1.0 - backtestConfig.emergencyStopPct / 100.0 : EMERGENCY_STOP_MULT;
        if (pnlMult <= effectiveEmStopMult) {
            return new SellDecision(true, currentQuantity, "EMERGENCY_STOP", true);
        }

        // RSI_BB 평균회귀 익절 비활성 — +2% 트레일 또는 -2% 손절만 사용
        // if (st.entryMode == EntryMode.RSI_BOLLINGER_REBOUND) { ... RSI_BB_MEAN_REVERSION_PROFIT ... }

        // 2. 장 마감 강제 청산 (KRX 15:25 / US 15:55~16:15)
        // US는 16:15 이후 EOD 트리거 중단 — 장 마감 후 MOC 확인 시간 15분 허용, 이후는 session gate가 차단
        boolean eodEnabled = backtestConfig == null || backtestConfig.useEodForceSell;
        LocalTime nowTime = nowByMarket(market).toLocalTime();
        boolean eodForce = eodEnabled && (
                (market == Market.KRX && nowTime.isAfter(LocalTime.of(15, 25)))
                || (market == Market.US && nowTime.isAfter(LocalTime.of(15, 55)) && nowTime.isBefore(LocalTime.of(16, 15))));
        if (eodForce) {
            logger.info("SELL [EOD_FORCE_SELL] {} pnl={} time={}", symbol, fmtPct(pnlRate), nowTime);
            return new SellDecision(true, currentQuantity, "EOD_FORCE_SELL", true);
        }

        // 2-0. ORB 타임스탑: 90분 경과 + 수익률 < +0.5% → 방향 없는 거래 조기 종료
        if (st.entryMode == EntryMode.OPENING_RANGE_BREAKOUT
                && holdMs >= 5_400_000L
                && pnlRate < 0.005) {
            logger.info("SELL [TIME_STOP_ORB] {} pnl={} holdMs={}", symbol, fmtPct(pnlRate), holdMs);
            return new SellDecision(true, currentQuantity, "TIME_STOP_ORB", true);
        }

        // 2-2. BREAKOUT 전용: VWAP 이탈 청산 (유예 + 버퍼는 설정값 사용)
        if (vwapBreakEnabled
                && st.entryMode == EntryMode.BREAKOUT
                && holdMs >= effectiveVwapGraceMs
                && sessionVwap > 0.0
                && currentPrice < sessionVwap * effectiveVwapBreakBuffer
                && pnlRate <= -0.003) {
            return new SellDecision(true, currentQuantity, "VWAP_BREAK", true);
        }

        // 3. Stop loss (entryMode별)
        if (pnlMult <= profile.stopLossMult) {
            String reason = "STOP_LOSS_" + profile.modeTag;
            return new SellDecision(true, currentQuantity, reason, true);
        }

        // 4. Trail 활성화 여부: useTrailing=true이고 peak profit이 trailStartProfit 이상 도달한 경우
        boolean trailActive = profile.useTrailing
                && profile.trailStartProfit != null
                && peakProfitRate >= profile.trailStartProfit;

        // 5. 고정 익절 (entryMode별): takeProfitMult != null이면 trail 활성 여부 무관하게 익절
        // BREAKOUT도 고정 익절을 사용함 (KRX +2.8%, US +3.0% 도달 시 익절).
        // trail 조건도 별도로 유지되므로 trail 발동이 먼저면 trail이 우선됨.
        if (profile.takeProfitMult != null && pnlMult >= profile.takeProfitMult) {
            String reason = "TAKE_PROFIT_" + profile.modeTag;
            return new SellDecision(true, currentQuantity, reason, false);
        }

        // 6. Trailing stop (entryMode별): trail 활성화 후 highestSinceEntry 대비 하락 청산
        if (trailActive && profile.trailDropFromHigh != null
                && currentPrice <= st.highestSinceEntry * (1.0 - profile.trailDropFromHigh)) {
            String reason = "TRAIL_" + profile.modeTag;
            logger.info("SELL [{}] {} pnl={} peakPnl={} high={} current={}",
                    reason, symbol, fmtPct(pnlRate), fmtPct(peakProfitRate),
                    fmt(st.highestSinceEntry), fmt(currentPrice));
            return new SellDecision(true, currentQuantity, reason, true);
        }

        // 7. Breakeven guard: peak≥1.3%, 손실≤-0.3%, hold≥3분, trail 미발동, EARLY_MOMENTUM 제외
        // EARLY_MOMENTUM은 EARLY_MOMENTUM_DEAD로 별도 조기 청산 — BREAKEVEN_GUARD 이유 오염 방지
        boolean bgeEnabled = backtestConfig == null || backtestConfig.useBreakevenGuard;
        double effectiveBgePeak = backtestConfig != null ? backtestConfig.breakevenPeak / 100.0 : BREAKEVEN_GUARD_PEAK_THRESHOLD;
        double effectiveBgeLoss = backtestConfig != null ? backtestConfig.breakevenLoss / 100.0 : BREAKEVEN_GUARD_LOSS_TRIGGER;
        if (bgeEnabled
                && !trailActive
                && holdMs >= BREAKEVEN_GUARD_MIN_HOLD_MS
                && peakProfitRate >= effectiveBgePeak
                && pnlRate <= effectiveBgeLoss) {
            logger.info("SELL [BREAKEVEN_GUARD] {} pnl={} peakPnl={} holdMs={}",
                    symbol, fmtPct(pnlRate), fmtPct(peakProfitRate), holdMs);
            return new SellDecision(true, currentQuantity, "BREAKEVEN_GUARD", true);
        }

        // 8. VWAP_BREAK: 매수 후 유예 + 손실 구간에서만 발동 (RSI_BB 제외 — mean-reversion은 VWAP 아래 진입이 기본)
        // trail 비활성, 현재 속도 < -0.001 (진입 당시 속도가 아닌 현재 재계산 — 0.0 기준은 너무 민감)
        double exitVelocityShort = st.minuteHistory.velocitySeconds(TREND_SHORT_MIN_SECONDS, TREND_SHORT_MAX_SECONDS);
        if (vwapBreakEnabled
                && st.entryMode != EntryMode.THIRTY_MIN_RSI_BB_CROSS
                && st.entryMode != EntryMode.OPENING_RANGE_BREAKOUT  // ORB: 자체 stop-loss로 관리
                && !trailActive
                && holdMs >= effectiveVwapGraceMs
                && sessionVwap > 0.0
                && currentPrice < (sessionVwap * effectiveVwapBreakBuffer)
                && pnlRate <= -0.004
                && exitVelocityShort < -0.001) {
            return new SellDecision(true, currentQuantity, "VWAP_BREAK", true);
        }

        // 8-1. BREAKOUT: 3~6분 사이 실패 돌파 감지
        // VWAP 이탈 + 속도 음전(-0.0025 이하) → 성공한 돌파는 이 구간에서 이미 위에 있어야 함
        boolean failedBreakoutEnabled = backtestConfig == null || backtestConfig.useFailedBreakout;

        // 8-1a. BREAKOUT: 5분 내 힘 못 쓰면 컷 (VWAP 이탈 + 수익률 +0.3% 미달)
        if (failedBreakoutEnabled
                && st.entryMode == EntryMode.BREAKOUT
                && holdMs < 300_000L
                && pnlRate < 0.003
                && sessionVwap > 0.0
                && currentPrice < sessionVwap) {
            return new SellDecision(true, currentQuantity, "FAILED_BREAKOUT", true);
        }

        // 8-1. BREAKOUT: 3~8분 사이 실패 돌파 감지
        // VWAP 이탈 + 속도 음전(-0.0025 이하) → 성공한 돌파는 이 구간에서 이미 위에 있어야 함
        if (failedBreakoutEnabled
                && st.entryMode == EntryMode.BREAKOUT
                && holdMs >= 180_000L
                && holdMs <= effectiveVwapGraceMs
                && exitVelocityShort < -0.0025
                && sessionVwap > 0.0
                && currentPrice < sessionVwap * 0.998) {
            return new SellDecision(true, currentQuantity, "FAILED_BREAKOUT", true);
        }

        return SellDecision.none();
    }

    private ExitProfile exitProfileFor(EntryMode mode, Market market) {
        // Backtest: per-mode stop/TP/trail override
        if (backtestConfig != null) {
            double stop, tp, tStart, tDrop;
            boolean useTrail;
            String label = mode != null ? mode.name() : "CUSTOM";
            switch (mode != null ? mode : EntryMode.BREAKOUT) {
                case BREAKOUT:
                    stop     = 1.0 - backtestConfig.breakoutStopPct   / 100.0;
                    tp       = 1.0 + backtestConfig.breakoutTpPct     / 100.0;
                    tStart   = backtestConfig.breakoutTrailSt   / 100.0;
                    tDrop    = backtestConfig.breakoutTrailDrop / 100.0;
                    useTrail = tStart > 0;
                    break;
                case OPENING_RANGE_BREAKOUT:
                    stop     = 1.0 - backtestConfig.orbStopPct   / 100.0;
                    tp       = backtestConfig.orbTpPct > 0 ? 1.0 + backtestConfig.orbTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.orbTrailSt   / 100.0;
                    tDrop    = backtestConfig.orbTrailDrop  / 100.0;
                    useTrail = tStart > 0;
                    break;
                case THIRTY_MIN_RSI_BB_CROSS:
                    stop     = 1.0 - backtestConfig.rsiBb30mStopPct   / 100.0;
                    tp       = backtestConfig.rsiBb30mTpPct > 0 ? 1.0 + backtestConfig.rsiBb30mTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.rsiBb30mTrailSt   / 100.0;
                    tDrop    = backtestConfig.rsiBb30mTrailDrop / 100.0;
                    useTrail = tStart > 0;
                    break;
                case RED_TO_GREEN:
                    stop     = 1.0 - backtestConfig.r2gStopPct   / 100.0;
                    tp       = backtestConfig.r2gTpPct > 0 ? 1.0 + backtestConfig.r2gTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.r2gTrailSt   / 100.0;
                    tDrop    = backtestConfig.r2gTrailDrop  / 100.0;
                    useTrail = tStart > 0;
                    break;
                default:
                    stop     = 1.0 - backtestConfig.breakoutStopPct / 100.0;
                    tp       = backtestConfig.breakoutTpPct > 0 ? 1.0 + backtestConfig.breakoutTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.breakoutTrailSt   / 100.0;
                    tDrop    = backtestConfig.breakoutTrailDrop / 100.0;
                    useTrail = tStart > 0;
                    break;
            }
            return new ExitProfile(stop, tp > 9990 ? null : tp, useTrail ? tStart : null, useTrail ? tDrop : null, useTrail, label);
        }
        switch (mode) {
            case BREAKOUT:
                return new ExitProfile(STOP_KRX_BREAKOUT, TP_KRX_BREAKOUT,
                        TRAIL_START_KRX_BREAKOUT, TRAIL_DROP_KRX_BREAKOUT, true, "BREAKOUT");
            case OPENING_RANGE_BREAKOUT:
                return new ExitProfile(STOP_KRX_ORB, TP_KRX_ORB,
                        TRAIL_ST_KRX_ORB, TRAIL_DR_KRX_ORB, true, "ORB");
            case THIRTY_MIN_RSI_BB_CROSS:
                return new ExitProfile(STOP_KRX_30M_RSI_BB, TP_KRX_30M_RSI_BB,
                        TRAIL_ST_KRX_30M_RSI_BB, TRAIL_DR_KRX_30M_RSI_BB, true, "30M_RSI_BB");
            case RED_TO_GREEN:
                return new ExitProfile(STOP_KRX_R2G, TP_KRX_R2G,
                        TRAIL_ST_KRX_R2G, TRAIL_DR_KRX_R2G, true, "R2G");
            default:
                return new ExitProfile(0.977, 1.017, 0.012, 0.006, true, "DEFAULT");
        }
    }

    private boolean passesTimeWindow(Market market, LocalDateTime now) {
        LocalTime t = now.toLocalTime();
        if (market == Market.KRX) {
            int endH = backtestConfig != null ? backtestConfig.entryEndHour   : 15;
            int endM = backtestConfig != null ? backtestConfig.entryEndMinute : 20;
            return t.isBefore(LocalTime.of(endH, endM));
        }
        if (market == Market.US)  return t.isBefore(LocalTime.of(15, 50));
        return true;
    }

    private boolean passesSlowModeTimeWindow(Market market, LocalDateTime now) {
        LocalTime t = now.toLocalTime();
        if (market == Market.KRX) {
            int endH = backtestConfig != null ? backtestConfig.slowModeEntryEndHour   : 15;
            int endM = backtestConfig != null ? backtestConfig.slowModeEntryEndMinute : 20;
            return t.isBefore(LocalTime.of(endH, endM));
        }
        if (market == Market.US)  return t.isBefore(LocalTime.of(15, 50));
        return true;
    }


    private boolean passesCheapStockFilter(Market market, double price) {
        if (market != Market.KRX) return true;
        double minP = backtestConfig != null ? backtestConfig.minPrice : MIN_KRX_PRICE;
        return price >= minP;
    }

    private boolean isMarketContextExpired(MarketContext ctx) {
        return ctx == null || (nowMs() - ctx.updatedAtMs > MARKET_CONTEXT_TTL_MS);
    }

    private boolean passesTurnoverFilter(BuySignal signal, Market market) {
        if (signal.averageTurnover <= 0.0) {
            return true;
        }
        return signal.latestTurnover >= signal.averageTurnover * 0.25;
    }

    private boolean passesAbsoluteLiquidityFilter(BuySignal signal, Market market) {
        if (market == Market.US) {
            double minTo    = backtestConfig != null ? backtestConfig.minTurnoverUs : MIN_US_LATEST_TURNOVER;
            double minToAvg = backtestConfig != null ? minTo * 0.6 : MIN_US_AVG_TURNOVER;
            return signal.latestTurnover >= minTo && signal.averageTurnover >= minToAvg;
        }
        double minTo    = backtestConfig != null ? backtestConfig.minTurnoverKrx    : MIN_KRX_LATEST_TURNOVER;
        double minAvgTo = backtestConfig != null ? backtestConfig.minAvgTurnoverKrx : MIN_KRX_AVG_TURNOVER;
        return signal.latestTurnover >= minTo
                && signal.averageTurnover >= minAvgTo;
    }


    private double determinePositionSize(BuySignal signal, Market market) {
        if (!signal.isBuyCandidate(market, backtestConfig)) {
            return 0.0;
        }

        if (signal.entryMode == EntryMode.BREAKOUT) {
            if (signal.strongBreakout
                    && signal.volume > 0.0
                    && signal.averageVolume > 0.0
                    && signal.volume >= signal.averageVolume * VOLUME_SURGE_MULT_FOR_SIZE_UP) {
                return 1.0;
            }
            return BASE_SIZE_BREAKOUT;
        }

        return BASE_SIZE_DEFAULT;
    }

    private int resolveBuyQuantity(SymbolState st, double orderPrice, double positionSize) {
        if (orderPrice <= 0.0) return 0;
        if (st.buyAmountPerOrder <= 0.0) return 0;
        // positionSize = notional multiplier (e.g. 1.20x PULLBACK, 0.80x BREAKOUT, 1.50x SIZE_UP)
        double targetNotional = st.buyAmountPerOrder * positionSize;
        int qty = (int) Math.floor(targetNotional / orderPrice);
        return qty >= 1 ? qty : 0;
    }

    private void refreshHoldingState(SymbolState st, double avgPrice, double currentPrice, long nowMs) {
        if (st.entryTimeMs == 0L) {
            st.entryTimeMs = nowMs;
        }
        if (st.entryPriceSnapshot <= 0.0) {
            st.entryPriceSnapshot = avgPrice;
        }
        if (currentPrice > st.highestSinceEntry) {
            st.highestSinceEntry = currentPrice;
        }
        if (avgPrice > 0.0) {
            st.lastKnownProfitRate = (currentPrice - avgPrice) / avgPrice;
        }
    }

    private void resetEntryState(SymbolState st) {
        st.entryTimeMs = 0L;
        st.highestSinceEntry = 0.0;
        st.entryPriceSnapshot = 0.0;
        st.lastKnownProfitRate = 0.0;
        st.entryMode = EntryMode.NONE;
        st.positionPhase = PositionPhase.NONE;
        st.lastPendingSellReason = null;
        st.pendingSellQty = 0;
        st.entrySignalScore   = 0;
        st.entrySignalGrade   = null;
        st.entryVwapDistPct   = 0.0;
        st.entryVelocityShort = 0.0;
        st.entryVolumeRatio   = 0.0;
        st.entryTurnoverRatio = 0.0;
        st.entryScoreVwap     = 0;
        st.entryScoreTrend    = 0;
        st.entryScoreVolume   = 0;
        st.entryScoreTurnover = 0;
        st.entryScoreHigh     = 0;
        st.entryScorePattern  = 0;
        st.entryVelocityMid   = 0.0;
        st.entryVelocityLong  = 0.0;
        st.entryTrendScore    = 0;
        st.entryFromHighPct   = 0.0;
        st.entryQty           = 0;
    }

    private void resetDailyEntryIfNeeded(SymbolState st, java.time.LocalDate today) {
        if (today == null) return;
        if (st.lastEntryDay == null || !st.lastEntryDay.equals(today)) {
            st.lastEntryDay = today;
            st.dailyEntryCount = 0;
            st.lastEntryPatternKey = null;
            st.samePatternEntryCount = 0;
            st.statsDailyEntryReadyCount = 0;
            st.statsDailyExecCount = 0;
            st.statsDailyExitReasonCounts.clear();
            st.statsDailyExitPnlSum = 0.0;
            dailyPnlAccumulator.keySet().removeIf(d -> d.isBefore(today));
        }
    }

    private void updateEntryCounters(SymbolState st, BuySignal signal, java.time.LocalDate entryDay) {
        resetDailyEntryIfNeeded(st, entryDay);
        st.dailyEntryCount++;

        if (signal == null || signal.patternKey == null) {
            st.lastEntryPatternKey = null;
            st.samePatternEntryCount = 0;
            return;
        }

        if (signal.patternKey.equals(st.lastEntryPatternKey)) {
            st.samePatternEntryCount++;
        } else {
            st.lastEntryPatternKey = signal.patternKey;
            st.samePatternEntryCount = 1;
        }
    }

    private SymbolState state(String symbol) {
        return states.computeIfAbsent(symbol, key -> new SymbolState());
    }

    private Market detectMarket(String symbol, SymbolState st) {
        if (st.market != null) {
            return st.market;
        }
        return symbol.matches("^\\d{5,6}$") ? Market.KRX : Market.US;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
    }

    private LocalDateTime nowByMarket(Market market) {
        if (backtestNowMs > 0L) {
            ZoneId zone = market == Market.KRX ? KST_ZONE : NY_ZONE;
            return Instant.ofEpochMilli(backtestNowMs).atZone(zone).toLocalDateTime();
        }
        if (market == Market.KRX) {
            return LocalDateTime.now(KST_ZONE);
        }
        return LocalDateTime.now(NY_ZONE); // US는 반드시 뉴욕 시간 기준
    }

    private long historySpanSeconds(MinuteBarHistory history) {
        return history.spanSeconds();
    }


    private boolean isKrMarketCautiousWindow(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return !time.isBefore(LocalTime.of(9, 5)) && time.isBefore(LocalTime.of(9, 15));
    }

    private double lowVolumeSkipMult(Market market) {
        // safety-net only: 5% 미만이면 무조건 차단. 실질 기준은 isBuyCandidate per-mode 체크
        return 0.05;
    }

    private double momentumVelocityMin(Market market) {
        return 0.006;
    }

    private double strongVelocityMin(Market market) {
        return 0.010;
    }


    private double roundToTickSize(double price, Market market) {
        if (market == Market.US) {
            if (price >= 1.0) {
                return Math.round(price * 100.0) / 100.0;
            }
            return Math.round(price * 10000.0) / 10000.0;
        }

        if (price < 1_000) return Math.round(price);
        if (price < 5_000) return Math.round(price / 5.0) * 5.0;
        if (price < 10_000) return Math.round(price / 10.0) * 10.0;
        if (price < 50_000) return Math.round(price / 50.0) * 50.0;
        if (price < 100_000) return Math.round(price / 100.0) * 100.0;
        if (price < 500_000) return Math.round(price / 500.0) * 500.0;
        return Math.round(price / 1000.0) * 1000.0;
    }

    private boolean isTakeProfitReason(String reason) {
        if (reason == null) return false;
        return reason.startsWith("TAKE_PROFIT_");
    }

    private boolean isProfitExitReason(String reason) {
        if (reason == null) return false;
        return reason.startsWith("TAKE_PROFIT_") || "RSI_BB_MEAN_REVERSION_PROFIT".equals(reason);
    }

    private boolean isTrailExitReason(String reason) {
        if (reason == null) return false;
        return reason.startsWith("TRAIL_")
                || "BREAKEVEN_GUARD".equals(reason);
    }

    private ExitType toExitType(String reason) {
        if (reason == null) return ExitType.NONE;
        if (reason.startsWith("TAKE_PROFIT_") || "RSI_BB_MEAN_REVERSION_PROFIT".equals(reason)) {
            return ExitType.PROFIT;
        }
        if (reason.startsWith("TRAIL_") || "BREAKEVEN_GUARD".equals(reason)) {
            return ExitType.TRAIL;
        }
        if (reason.startsWith("STOP_LOSS_")
                || "EMERGENCY_STOP".equals(reason)
                || "FAILED_BREAKOUT".equals(reason)) {
            return ExitType.STOPLOSS;
        }
        if ("TIME_STOP_SOFT".equals(reason)
                || "TIME_STOP_MID".equals(reason)
                || "TIME_STOP_HARD".equals(reason)
                || "TIME_STOP_ORB".equals(reason)
                || "EOD_FORCE_SELL".equals(reason)) {
            return ExitType.TIMESTOP;
        }
        if ("VWAP_BREAK".equals(reason)) {
            return ExitType.VWAP_BREAK;
        }
        return ExitType.NONE;
    }

    private long resolveStopLossCooldown(String reason) {
        if ("FAILED_BREAKOUT".equals(reason)) return REENTER_FAILED_BREAKOUT_COOLDOWN_MS;
        return REENTER_STOPLOSS_COOLDOWN_MS; // STOP_LOSS / VWAP_BREAK / EMERGENCY_STOP 등: 15분
    }


    private boolean isStopLossExitReason(String reason) {
        if (reason == null) return false;
        return reason.startsWith("STOP_LOSS_")
                || "EMERGENCY_STOP".equals(reason)
                || "FAILED_BREAKOUT".equals(reason)
                || "VWAP_BREAK".equals(reason);
    }

    private boolean isTimeStopExitReason(String reason) {
        return false; // TIME_STOP 이유는 현재 미사용 — dead code 제거
    }

    private String buildBuyWhyString(BuySignal signal) {
        StringBuilder sb = new StringBuilder();
        if (signal.entryMode == EntryMode.BREAKOUT) {
            if (signal.strongBreakout)           sb.append("strongBreakout");
            if (signal.volumeBreakout)           sb.append(" volBreakout");
            if (signal.breakoutRetestRecovering) sb.append(" retestOK");
            if (signal.multiUptrend)             sb.append(" multiUp");
            if (signal.averageVolume > 0.0)
                sb.append(" vol(").append(String.format("%.2f", signal.volume / signal.averageVolume)).append("x)");
        } else if (signal.entryMode == EntryMode.OPENING_RANGE_BREAKOUT) {
            sb.append(String.format("vwap=%.2f%%", signal.vwapDistancePct * 100.0));
            sb.append(String.format(" trend=%d", signal.trendScore));
            if (signal.averageVolume > 0.0)
                sb.append(String.format(" vol(%.2fx)", signal.volume / signal.averageVolume));
        } else if (signal.entryMode == EntryMode.THIRTY_MIN_RSI_BB_CROSS) {
            sb.append(String.format("rsi30=%.1f sig30=%.1f", signal.thirtyMinRsi, signal.thirtyMinRsiSignal));
            sb.append(String.format(" bbLow30=%.0f", signal.thirtyMinBbLower));
            sb.append(" sc=").append(signal.thirtyMinRsiBbScore);
        } else if (signal.entryMode == EntryMode.RED_TO_GREEN) {
            sb.append(String.format("crossPct=%.2f%%", signal.r2gCrossPct * 100.0));
            if (signal.averageVolume > 0.0)
                sb.append(String.format(" vol(%.2fx)", signal.volume / signal.averageVolume));
        }
        return sb.toString();
    }

    private String buildPatternKey(Market market, BuySignal signal, LocalDateTime barTime) {
        LocalDateTime ts = barTime != null ? barTime : LocalDateTime.now();
        String timeKey = ts.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        StringBuilder sb = new StringBuilder();
        sb.append(timeKey).append("|");
        if (signal.volumeBreakout) sb.append("V");
        if (signal.strongBreakout) sb.append("S");
        sb.append("|").append(signal.entryMode);
        sb.append("|").append(market);
        return sb.toString();
    }

    private String fmt(double value) {
        return String.format("%.4f", value);
    }

    private String fmtPct(double value) {
        return String.format("%.2f%%", value * 100.0);
    }
}
