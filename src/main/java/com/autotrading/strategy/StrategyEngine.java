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

    private enum EntryMode { NONE, PULLBACK, BREAKOUT, EARLY_MOMENTUM, STRONG_PULLBACK, VWAP_RECLAIM, RSI_BOLLINGER_REBOUND, OPENING_RANGE_BREAKOUT }

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
    private static final int MINUTE_HISTORY_CAPACITY = 150;     // 분봉 히스토리 최대 보관 수
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
    private static final double BREAKOUT_RETEST_LOWER = 0.9960; // KRX 돌파 재테스트 하단 (고점 대비 -0.4%)
    private static final double BREAKOUT_RETEST_UPPER = 1.0040; // KRX 돌파 재테스트 상단 (고점 대비 +0.4%)
    private static final double MOMENTUM_VOLUME_MULT = 1.5;          // 모멘텀 진입 최소 거래량 (평균의 1.5배)
    private static final double STRONG_BREAKOUT_VOLUME_MULT = 1.5;   // 강한 돌파 최소 거래량 (평균의 1.5배)
    private static final double VOLUME_BREAKOUT_VOLUME_MULT = 1.6;   // 거래량 돌파 최소 거래량 (평균의 1.6배)
    private static final double VOLUME_SURGE_MULT_FOR_SIZE_UP = 2.3; // 사이즈 업 조건: 거래량 평균 2.3배 이상

    // KRX BREAKOUT 추가 진입 조건 (진짜 돌파만 허용)
    private static final double BREAKOUT_KRX_MIN_VWAP_GAP       = 0.003; // VWAP 대비 최소 +0.3% 이상
    private static final double BREAKOUT_KRX_MIN_VOLUME_MULT    = 1.8;  // 거래량 평균 1.8배 이상 (고정)

    // =========================
    // Pullback zone (shared for KRX/US)
    // =========================
    private static final double PULLBACK_UPPER_FROM_HIGH = 0.990; // 눌림 진입 허용 상단: 고점 대비 -1.0%
    private static final double PULLBACK_LOWER_FROM_HIGH = 0.980; // 눌림 진입 허용 하단: 고점 대비 -2.0%

    private static final double PULLBACK_VOLUME_MULT = 1.00; // 눌림 진입 최소 거래량 (평균의 100% 이상)

    // LOW_VOLUME_SKIP_MULT removed: per-mode volume ratio used in isBuyCandidate

    // =========================
    // VWAP filter
    // =========================
    private static final double VWAP_NEAR_DISTANCE_PCT  = 0.0040; // pullback nearVwap 판단 기준
    private static final double VWAP_BREAK_SELL_BUFFER  = 0.9900; // VWAP -1.0% (← 0.9950: 정상 되돌림 손절 오인 방지)
    private static final double VWAP_SLOPE_MIN_PCT       = 0.0002; // VWAP slope 최소 상승폭
    // 극단적 과열만 safety-net으로 차단 — 실제 per-mode 상한은 isBuyCandidate에서 체크
    private static final double VWAP_TOO_FAR_HARD_LIMIT   = 0.080;  // 8% 초과만 즉시 차단
    // Pullback은 VWAP 대비 최대 -0.4% 아래까지 진입 허용 (회복 확인 조건 필요)


    // =========================
    // Per-mode entry thresholds (진입 모드별 필터 기준)
    // =========================
    // VWAP 이격 허용 상한 (price above VWAP 기준)
    private static final double VWAP_MAX_GAP_BREAKOUT        = 0.015; // 1.5% (← 2.2%에서 하향: VWAP 이격 클수록 되돌림 위험)
    private static final double VWAP_MAX_GAP_EARLY_MOMENTUM  = 0.012; // 1.2%
    private static final double VWAP_MAX_GAP_PULLBACK        = 0.010; // 1.0%
    private static final boolean ENABLE_EARLY_MOMENTUM_ENTRY  = false;
    private static final boolean ENABLE_PULLBACK_ENTRY         = false; // PULLBACK 전패(0% 승률) → 비활성화
    private static final boolean ENABLE_BREAKOUT_ENTRY         = true;  // BREAKOUT PF 1.23 (2026-06 백테스트 재확인 → 재활성)
    private static final boolean ENABLE_STRONG_PULLBACK_ENTRY  = true;
    private static final boolean ENABLE_VWAP_RECLAIM_ENTRY     = true;
    private static final boolean ENABLE_RSI_BB_REBOUND_ENTRY   = false; // PF 0.62 → 비활성

    // ── RSI_BOLLINGER_REBOUND 상수 ────────────────────────────────────────
    private static final double STOP_KRX_RSI_BB      = 0.988; // -1.2% (← 0.990: 더 좁은 손절)
    private static final double TP_KRX_RSI_BB        = 1.016; // +1.6% (← 1.018: 빠른 익절)
    private static final double TRAIL_ST_KRX_RSI_BB  = 0.012; // +1.2% (← 0.014)
    private static final double TRAIL_DR_KRX_RSI_BB  = 0.006; // -0.6% (← 0.007)

    // ── OPENING_RANGE_BREAKOUT 상수 ───────────────────────────────────────────
    private static final boolean ENABLE_OPENING_RANGE_BREAKOUT = false; // 신규 모드 — 백테스트 검증 후 활성화
    private static final double STOP_KRX_ORB       = 0.982; // -1.8%
    private static final double TP_KRX_ORB         = 1.022; // +2.2%
    private static final double TRAIL_ST_KRX_ORB   = 0.018; // +1.8%
    private static final double TRAIL_DR_KRX_ORB   = 0.010; // -1.0%

    // ── STRONG_PULLBACK 상수 ──────────────────────────────────────────────
    private static final double SP_VWAP_MIN_ABOVE      = 0.002;
    private static final double SP_PULLBACK_MIN_PCT    = 0.8;   // ← 1.2: 진입 폭 확대
    private static final double SP_PULLBACK_MAX_PCT    = 3.8;   // ← 3.5: 진입 폭 확대
    private static final double SP_VOL3_RATIO_MAX      = 0.7;   // ← 0.8: 거래량 감소 완화
    private static final double SP_BODY_RATIO_MIN      = 0.4;   // ← 0.5: 양봉 조건 완화
    private static final int    SP_MIN_SCORE           = 83;    // ← 85: 최소 점수 완화

    private static final double STOP_KRX_STRONG_PB    = 0.982;
    private static final double TP_KRX_STRONG_PB      = 1.030;
    private static final double TRAIL_ST_KRX_STRONG_PB = 0.020;
    private static final double TRAIL_DR_KRX_STRONG_PB = 0.008;

    // ── VWAP_RECLAIM 상수 ─────────────────────────────────────────────────
    private static final int    VR_LOOKBACK_BARS       = 12;
    private static final double VR_VOL_MULT            = 2.0;
    private static final int    VR_MIN_ABOVE_BARS      = 5;
    private static final int    VR_MIN_SCORE           = 87;
    private static final int    VR_MAX_SCORE           = 88;   // 89점 VR PF 0.45 → 과열 직전 차단

    private static final double STOP_KRX_VR           = 0.990; // ← 0.988: 손절 -1.0% (설정-실제 편차 축소)
    private static final double TP_KRX_VR             = 1.022; // ← 1.020: 익절 +2.2%
    private static final double TRAIL_ST_KRX_VR        = 0.018; // ← 0.015: 이익 조기 보호 강화
    private static final double TRAIL_DR_KRX_VR        = 0.010; // ← 0.013: 수익 반납 방지

    // PULLBACK 최소 진입 속도 (0.15%/s 이상 필수)
    private static final double PULLBACK_MIN_VELOCITY_SHORT  = 0.0010;

    // 거래량 비율 (volume / avgVolume 최소값)
    private static final double VOLUME_RATIO_BREAKOUT        = 1.20;
    private static final double VOLUME_RATIO_EARLY_MOMENTUM  = 0.12;
    private static final double VOLUME_RATIO_PULLBACK        = 0.80;

    // 거래대금 비율 (latestTurnover / avgTurnover 최소값)
    private static final double TURNOVER_RATIO_BREAKOUT      = 1.00;
    private static final double TURNOVER_RATIO_EARLY_MOMENTUM = 0.18;
    private static final double TURNOVER_RATIO_PULLBACK      = 0.70;

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
    private static final double BREAKEVEN_GUARD_LOSS_TRIGGER   = -0.001; // 최소 -0.1% 손실 이후에만 발동 (← -0.3%: 손실 축소)

    // =========================
    // Risk / Exit (KRX — entryMode별)
    // =========================
    // PULLBACK: 손절 -2.3%, 익절 +3.2%, 트레일 시작 +2.2%, 고점 하락 -1.6%
    private static final double STOP_KRX_PULLBACK              = 0.977;
    private static final double TP_KRX_PULLBACK                = 1.032;
    private static final double TRAIL_START_KRX_PULLBACK       = 0.022;
    private static final double TRAIL_DROP_KRX_PULLBACK        = 0.016;

    // BREAKOUT: 손절 -2.0%, 익절 +2.0%(← 3.0%), 트레일 시작 +1.8%(← 2.3%), 고점 하락 -1.5%
    private static final double STOP_KRX_BREAKOUT              = 0.980;
    private static final double TP_KRX_BREAKOUT                = 1.020; // ← 1.025: 2.5% 도달 전 역전 多, 2.0%로 복원
    private static final double TRAIL_START_KRX_BREAKOUT       = 0.018; // 이익 조기 보호 강화
    private static final double TRAIL_DROP_KRX_BREAKOUT        = 0.015;

    // EARLY_MOMENTUM: 손절 -2.0%, 익절 +2.2%, 트레일 없음 (모멘텀 소멸 즉시 컷)
    private static final double STOP_KRX_EARLY_MOMENTUM        = 0.980;
    private static final double TP_KRX_EARLY_MOMENTUM          = 1.022;


    // =========================
    // Sizing
    // =========================
    private static final double BASE_SIZE_PULLBACK = 1.00;
    private static final double BASE_SIZE_BREAKOUT = 0.80;
    private static final double BASE_SIZE_EARLY_MOMENTUM = 0.50; // 승률 낮은 모드 → 사이즈 축소
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
    private static final long REENTER_EARLY_MOMENTUM_DEAD_COOLDOWN_MS = 480_000L; // EARLY_MOMENTUM_DEAD: 8분
    private static final long REENTER_FAILED_PULLBACK_COOLDOWN_MS     = 480_000L; // FAILED_PULLBACK: 8분

    private static final int MAX_DAILY_ENTRY_COUNT = 2;           // 종목당 일일 최대 진입 횟수
    private static final int MAX_SAME_PATTERN_ENTRY_COUNT = 1;    // 동일 패턴 연속 진입 최대 횟수 (중복 추격 방지)

    // =========================
    private static final long VWAP_BREAK_GRACE_MS = 480_000L; // 매수 후 8분간 VWAP_BREAK 유예 (← 360_000L: 조기 청산 방지)

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

        // STRONG_PULLBACK / VWAP_RECLAIM 신호용 당일 상태 추적
        double dayOpenPrice = 0.0;
        java.time.LocalDate dayOpenDate = null;
        int consecutiveAboveVwapBars = 0;
        int barsSinceLastBelowVwap   = 99; // 99 = 오래됨, 0 = 직전봉이 VWAP 아래
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
        String signalGrade;
        double volumeRatio;
        double turnoverRatio;
        int    scoreVwap;
        int    scoreTrend;
        int    scoreVolume;
        int    scoreTurnover;
        int    scoreHigh;
        int    scorePattern;

        // ── STRONG_PULLBACK / VWAP_RECLAIM 전용 신호 ─────────────────────
        double  dayChangePct;         // 당일 등락률 (시가 대비 %)
        double  vol3;                 // 최근 3봉 평균 거래량
        double  vol5;                 // 최근 5봉 평균 거래량
        double  vol10;                // 최근 10봉 평균 거래량
        double  bodyRatio;            // 현재봉 몸통비율 (close-open)/(high-low)
        int     aboveVwapBars;        // 연속 VWAP 위 봉 수
        boolean hadVwapBelow;         // 최근 N봉 내 VWAP 이탈 발생 여부
        boolean spCondsMet;           // STRONG_PULLBACK 핵심 조건 충족
        int     spScore;              // STRONG_PULLBACK 점수 (0~100)
        boolean vrCondsMet;           // VWAP_RECLAIM 핵심 조건 충족
        int     vrScore;              // VWAP_RECLAIM 점수 (0~100)

        // ── RSI_BOLLINGER_REBOUND 전용 신호 ──────────────────────────────
        boolean rsiBbCondsMet;
        int     rsiBbScore;
        double  bbMiddle;
        double  bbUpper;
        double  bbLower;
        double  bbBandwidth;
        double  rsi;
        double  rsiSignal;
        double  prevRsi;
        double  prevRsiSignal;
        boolean rsiCrossedUp;
        boolean bbLowerTouchedBy5m;
        double  fiveMinLow;
        double  fiveMinClose;

        // ── OPENING_RANGE_BREAKOUT 전용 신호 ─────────────────────────────
        boolean orbBreakout;    // ORB 조건 충족

        boolean timeWindowBlocked;
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
            if (timeWindowBlocked) return false;
            if (!marketFilterPassed) return false;
            if (cheapStockBlocked) return false;
            if (!absoluteLiquidityPassed) return false; // 절대 유동성 (50M KRX)
            if (vwapTooFar) return false;               // safety-net: 8% 초과
            if (entryMode == EntryMode.NONE) return false;

            // 모드 활성화 플래그 (실전: 상수 기반, 백테스트: cfg 기반)
            {
                boolean enablePB = cfg != null ? cfg.enablePullback       : ENABLE_PULLBACK_ENTRY;
                boolean enableBO = cfg != null ? cfg.enableBreakout       : ENABLE_BREAKOUT_ENTRY;
                boolean enableEM = cfg != null ? cfg.enableEarlyMomentum  : ENABLE_EARLY_MOMENTUM_ENTRY;
                boolean enableSP = cfg != null ? cfg.enableStrongPullback : ENABLE_STRONG_PULLBACK_ENTRY;
                boolean enableVR = cfg != null ? cfg.enableVwapReclaim    : ENABLE_VWAP_RECLAIM_ENTRY;
                if (entryMode == EntryMode.PULLBACK        && !enablePB) return false;
                if (entryMode == EntryMode.BREAKOUT        && !enableBO) return false;
                if (entryMode == EntryMode.EARLY_MOMENTUM  && !enableEM) return false;
                if (entryMode == EntryMode.STRONG_PULLBACK && !enableSP) return false;
                if (entryMode == EntryMode.VWAP_RECLAIM    && !enableVR) return false;
            }
            boolean enableRsiBb = cfg != null ? cfg.enableRsiBbRebound : ENABLE_RSI_BB_REBOUND_ENTRY;
            if (entryMode == EntryMode.RSI_BOLLINGER_REBOUND && !enableRsiBb) return false;
            boolean enableOrb = cfg != null ? cfg.enableOpeningRangeBreakout : ENABLE_OPENING_RANGE_BREAKOUT;
            if (entryMode == EntryMode.OPENING_RANGE_BREAKOUT && !enableOrb) return false;

            // --- VWAP 포지션: RSI_BB는 mean-reversion — VWAP 아래 진입 허용 ---
            if (entryMode != EntryMode.RSI_BOLLINGER_REBOUND && !aboveVwap) return false;

            // RSI_BOLLINGER_REBOUND: 독립 조건으로 판단
            if (entryMode == EntryMode.RSI_BOLLINGER_REBOUND) {
                int minScore = cfg != null ? cfg.rsiBbMinScore : 75;
                if (!rsiBbCondsMet) return false;
                if (rsiBbScore < minScore) return false;
                if (velocityShort < -0.0015) return false; // ← -0.003: 하락 중 반등 시도 차단 강화
                if (averageVolume > 0.0 && volume < averageVolume * 0.7) return false;
                // BB 상단 추격 차단: 현재가가 BB 중단선 이상이면 평균회귀 대상 아님
                if (bbMiddle > 0.0 && price >= bbMiddle) return false;
                // BB 상단 근접 차단: 상단 3% 이내면 추격 매수로 간주
                if (bbUpper > 0.0 && price >= bbUpper * 0.97) return false;
                return true;
            }

            // OPENING_RANGE_BREAKOUT: ORB 독립 조건으로 판단
            if (entryMode == EntryMode.OPENING_RANGE_BREAKOUT) {
                return orbBreakout;
            }

            // STRONG_PULLBACK / VWAP_RECLAIM: 자체 조건으로 판단, 공통 필터 생략
            if (entryMode == EntryMode.STRONG_PULLBACK || entryMode == EntryMode.VWAP_RECLAIM) {
                // 최소 유동성 안전망 (spCondsMet/vrCondsMet은 거래대금 절대 최솟값을 검증하지 않음)
                if (averageTurnover > 0.0 && latestTurnover < averageTurnover * 0.8) return false;
                // SP: 눌림목이라 거래량 잠시 감소 허용 (0.7x), VR: 회복 돌파이므로 거래량 강해야 함 (0.9x)
                if (entryMode == EntryMode.STRONG_PULLBACK) {
                    if (averageVolume > 0.0 && volume < averageVolume * 0.7) return false;
                } else {
                    if (averageVolume > 0.0 && volume < averageVolume * 0.9) return false;
                }
                boolean blockSS = (cfg == null) || cfg.blockSSGrade;
                boolean blockS  = (cfg == null) || cfg.blockSGrade;
                boolean blockA  = cfg != null && cfg.blockAGrade;    // A(85-89): 기본 허용, BacktestConfig에서만 차단
                boolean blockB  = (cfg == null) || cfg.blockBGrade;  // B(80-84): 기본 차단 (PF 0.29)
                if (entryMode == EntryMode.STRONG_PULLBACK) {
                    int minSp = cfg != null ? cfg.spMinScore : SP_MIN_SCORE;
                    if (blockSS && spScore >= 95) return false;
                    if (blockS  && spScore >= 90 && spScore < 95) return false;
                    if (blockA  && spScore >= 85 && spScore < 90) return false;
                    if (blockB  && spScore >= 80 && spScore < 85) return false;
                    if (velocityShort <= velocityMid) return false;
                    return spCondsMet && spScore >= minSp;
                } else {
                    int minVr = cfg != null ? cfg.vrMinScore : VR_MIN_SCORE;
                    int maxVr = cfg != null && cfg.vrMaxScore > 0 ? cfg.vrMaxScore : VR_MAX_SCORE;
                    if (blockSS && vrScore >= 95) return false;
                    if (blockS  && vrScore >= 90 && vrScore < 95) return false;
                    if (blockA  && vrScore >= 85 && vrScore < 90) return false;
                    if (blockB  && vrScore >= 80 && vrScore < 85) return false;
                    if (vrScore > maxVr) return false; // 89점 과열 직전 차단 (PF 0.45)
                    if (velocityShort <= 0.0015) return false;
                    if (velocityShort <= velocityMid) return false; // VR: 단기 속도 가속 필수
                    if (dayChangePct < -3.0) return false; // 당일 -3% 이상 하락 종목 VR 차단
                    return vrCondsMet && vrScore >= minVr;
                }
            }

            // 기존 모드: VWAP slope 추가 체크
            if (!vwapSlopeUp) return false;

            // --- per-mode 기준값 결정 ---
            boolean highConviction = entryMode == EntryMode.EARLY_MOMENTUM
                    ? signalScore >= 88
                    : signalScore >= HIGH_CONVICTION_SCORE;

            double allowedVwapGap;
            double requiredVolumeRatio;
            double requiredTurnoverRatio;
            int minScore;

            if (entryMode == EntryMode.BREAKOUT) {
                allowedVwapGap        = VWAP_MAX_GAP_BREAKOUT;
                requiredVolumeRatio   = VOLUME_RATIO_BREAKOUT;
                requiredTurnoverRatio = TURNOVER_RATIO_BREAKOUT;
                minScore              = 83;
            } else if (entryMode == EntryMode.EARLY_MOMENTUM) {
                allowedVwapGap        = VWAP_MAX_GAP_EARLY_MOMENTUM;
                requiredVolumeRatio   = VOLUME_RATIO_EARLY_MOMENTUM;
                requiredTurnoverRatio = TURNOVER_RATIO_EARLY_MOMENTUM;
                minScore              = 70;
            } else { // PULLBACK
                allowedVwapGap        = VWAP_MAX_GAP_PULLBACK;
                requiredVolumeRatio   = VOLUME_RATIO_PULLBACK;
                requiredTurnoverRatio = TURNOVER_RATIO_PULLBACK;
                minScore              = 80;
            }

            // Backtest config: score threshold & VWAP gap override
            if (cfg != null) {
                if (entryMode == EntryMode.PULLBACK) {
                    allowedVwapGap = cfg.vwapMaxGapPullbackPct / 100.0;
                    minScore       = cfg.pullbackMinScore;
                } else if (entryMode == EntryMode.BREAKOUT) {
                    allowedVwapGap = cfg.vwapMaxGapBreakoutPct / 100.0;
                    minScore       = cfg.breakoutMinScore;
                }
            }

            // 고득점이면 일부 기준 완화
            if (highConviction) {
                requiredVolumeRatio   *= HIGH_CONVICTION_VOLUME_MULT;
                requiredTurnoverRatio *= HIGH_CONVICTION_TURNOVER_MULT;
            }

            // --- per-mode 필터 ---
            if (vwapDistancePct > allowedVwapGap) return false;
            if (averageVolume > 0.0 && volume < averageVolume * requiredVolumeRatio) return false;
            if (averageTurnover > 0.0 && latestTurnover < averageTurnover * requiredTurnoverRatio) return false;
            if (signalScore < minScore) return false;
            // SS/S/A/B 등급 차단: A(85-89) 허용, B(80-84) 기본 차단 (PF 0.29)
            boolean blockSS = (cfg == null) || cfg.blockSSGrade;
            boolean blockS  = (cfg == null) || cfg.blockSGrade;
            boolean blockA  = cfg != null && cfg.blockAGrade;    // A(85-89): 기본 허용, BacktestConfig에서만 차단
            boolean blockB  = (cfg == null) || cfg.blockBGrade;  // B(80-84): 기본 차단
            if (blockSS && signalScore >= 95) return false;
            if (blockS  && signalScore >= 90 && signalScore < 95) return false;
            if (blockA  && signalScore >= 85 && signalScore < 90) return false;
            if (blockB  && signalScore >= 80 && signalScore < 85) return false;

            // --- 모드별 진입 조건 ---
            if (entryMode == EntryMode.PULLBACK) {
                int  reqBullish     = cfg != null ? cfg.pullbackRequiredBullishBars       : 2;
                boolean reqHighBreak = cfg == null || cfg.pullbackRequireRecentHighBreakout;
                if (recentBullishBars < reqBullish) return false;
                if (reqHighBreak && !recentHighBreakout) return false;
                return pullbackEntry;
            }
            if (entryMode == EntryMode.BREAKOUT) {
                boolean cfgOverheat = cfg == null || cfg.breakoutOverheatBlock;
                boolean cfgAccel    = cfg == null || cfg.breakoutRequireAcceleration;
                boolean cfgMultiUp  = cfg == null || cfg.breakoutRequireMultiUptrend;

                // 과열 차단 (백테스트에서 비활성화 가능)
                if (cfgOverheat && recentHigh > 0.0 && velocityShort >= 0.009 && vwapDistancePct >= 0.015) {
                    boolean veryNearHigh = price >= recentHigh * 0.9995;
                    boolean nearHighVolumeSurge = price >= recentHigh * 0.998
                            && averageVolume > 0.0
                            && volume >= averageVolume * 2.5;
                    if (veryNearHigh || nearHighVolumeSurge) return false;
                }
                // 가속도 조건 (백테스트에서 비활성화 가능)
                if (cfgAccel && velocityShort <= velocityMid) return false;
                double cfgVelMid      = cfg != null ? cfg.breakoutMinVelocityMid      : 0.001;
                double cfgVelLong     = cfg != null ? cfg.breakoutMinVelocityLong     : 0.0005;
                int    cfgBullishBars = cfg != null ? cfg.breakoutRequiredBullishBars : 2;
                if (velocityMid < cfgVelMid) return false;
                if (velocityLong < cfgVelLong) return false;
                if (recentBullishBars < cfgBullishBars) return false;
                // MultiUptrend: 활성화 시 3개 MA 모두 상승, 비활성화 시 2개 이상 상승
                boolean trendOk = cfgMultiUp ? trendScore == 3 : trendScore >= 2;
                return (volumeBreakout || strongBreakout)
                        && breakoutRetestReady
                        && breakoutRetestRecovering
                        && trendOk
                        && vwapDistancePct >= BREAKOUT_KRX_MIN_VWAP_GAP
                        && averageVolume > 0.0 && volume >= averageVolume * (cfg != null ? cfg.breakoutKrxVolMult : BREAKOUT_KRX_MIN_VOLUME_MULT);
            }
            if (entryMode == EntryMode.EARLY_MOMENTUM) {
                return earlyMomentum && multiUptrend;
            }

            return false;
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
            st.minuteHistory.clear();
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
            st.consecutiveAboveVwapBars = 0;
            st.barsSinceLastBelowVwap   = 99;
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
            // 날짜 경계 감지: addBar 전에 히스토리 초기화 (VWAP에 전날 봉 포함되는 버그 방지)
            java.time.LocalDate barDate = ts.toLocalDate();
            if (st.dayOpenDate != null && !barDate.equals(st.dayOpenDate)) {
                st.minuteHistory.clear();
                st.consecutiveAboveVwapBars = 0;
                st.barsSinceLastBelowVwap   = 99;
                st.openingRangeHigh  = 0.0;
                st.openingRangeLow   = 0.0;
                st.openingRangeReady = false;
            }
            if (st.dayOpenDate == null || !barDate.equals(st.dayOpenDate)) {
                st.dayOpenDate  = barDate;
                st.dayOpenPrice = open > 0.0 ? open : close;
            }

            st.minuteHistory.addBar(open, high, low, close, Math.max(0.0, volume), ts);

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

            // VWAP 연속봉 상태 업데이트 (VWAP_RECLAIM 감지용)
            double vwapNow = st.minuteHistory.sessionVwap();
            if (vwapNow > 0.0 && close > vwapNow) {
                st.consecutiveAboveVwapBars = Math.min(st.consecutiveAboveVwapBars + 1, 999);
                st.barsSinceLastBelowVwap   = Math.min(st.barsSinceLastBelowVwap   + 1, 999);
            } else {
                st.consecutiveAboveVwapBars = 0;
                st.barsSinceLastBelowVwap   = 0;
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
                if (signal.entryMode == EntryMode.STRONG_PULLBACK) {
                    st.entrySignalScore = signal.spScore;
                } else if (signal.entryMode == EntryMode.VWAP_RECLAIM) {
                    st.entrySignalScore = signal.vrScore;
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

                boolean logHighConviction = signal.entryMode == EntryMode.EARLY_MOMENTUM
                        ? signal.signalScore >= 88 : signal.signalScore >= HIGH_CONVICTION_SCORE;
                boolean logBreakoutAccel = signal.velocityShort > signal.velocityMid;
                double logFromHighPct = signal.recentHigh > 0.0
                        ? (signal.price - signal.recentHigh) / signal.recentHigh : 0.0;
                double logAllowedVwapGap = signal.entryMode == EntryMode.BREAKOUT ? VWAP_MAX_GAP_BREAKOUT
                        : signal.entryMode == EntryMode.EARLY_MOMENTUM ? VWAP_MAX_GAP_EARLY_MOMENTUM
                        : signal.entryMode == EntryMode.STRONG_PULLBACK ? 0.05
                        : signal.entryMode == EntryMode.VWAP_RECLAIM    ? 0.03
                        : signal.entryMode == EntryMode.RSI_BOLLINGER_REBOUND ? 0.05
                        : VWAP_MAX_GAP_PULLBACK;
                String spVrLog = "";
                if (signal.entryMode == EntryMode.STRONG_PULLBACK) {
                    spVrLog = String.format(" | SP: score=%d dayChg=%.2f%% fromHigh=%.2f%% vol3/vol10=%.2fx body=%.2f",
                            signal.spScore, signal.dayChangePct, signal.fromHighPct,
                            signal.vol10 > 0.0 ? signal.vol3 / signal.vol10 : 0.0,
                            signal.bodyRatio);
                } else if (signal.entryMode == EntryMode.VWAP_RECLAIM) {
                    spVrLog = String.format(" | VR: score=%d aboveBars=%d hadBelow=%b vol/vol5=%.2fx",
                            signal.vrScore, signal.aboveVwapBars, signal.hadVwapBelow,
                            signal.vol5 > 0.0 ? signal.volume / signal.vol5 : 0.0);
                } else if (signal.entryMode == EntryMode.RSI_BOLLINGER_REBOUND) {
                    spVrLog = String.format(" | RSI_BB: score=%d rsi=%.1f sig=%.1f bbLow=%.0f 5mLow=%.0f cross=%b",
                            signal.rsiBbScore, signal.rsi, signal.rsiSignal,
                            signal.bbLower, signal.fiveMinLow, signal.rsiCrossedUp);
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
                        signal.signalScore,
                        "[" + signal.signalGrade + "]" + (logHighConviction ? "(CONV)" : ""),
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

                if (signal.entryMode == EntryMode.EARLY_MOMENTUM && logFromHighPct < -0.002) {
                    logger.warn("EARLY_MOMENTUM_PAST_PEAK {} fromHigh={}% — 고점 이탈 진입, 즉시 역전 위험",
                            normalized, fmtPct(logFromHighPct));
                }

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

        // pullback
        signal.pullbackDepthFromHigh = signal.recentHigh > 0.0
                ? ((signal.price - signal.recentHigh) / signal.recentHigh)
                : 0.0;

        double fromHighRatio = signal.recentHigh > 0.0 ? (signal.price / signal.recentHigh) : 0.0;
        double effectivePbUpper = backtestConfig != null ? 1.0 - backtestConfig.pullbackUpperPct / 100.0 : PULLBACK_UPPER_FROM_HIGH;
        double effectivePbLower = backtestConfig != null ? 1.0 - backtestConfig.pullbackLowerPct / 100.0 : PULLBACK_LOWER_FROM_HIGH;
        signal.pullbackZone = fromHighRatio >= effectivePbLower && fromHighRatio <= effectivePbUpper;

        // AND 구조: PULLBACK_MIN_VELOCITY_SHORT 이상 + 단기이평 회복 모두 필수
        // isBuyCandidate의 recoveryConfirmed 조건을 이 블록으로 흡수 (이중 검증 제거)
        double effectivePbVelShort = backtestConfig != null ? backtestConfig.pullbackVelocityShort : PULLBACK_MIN_VELOCITY_SHORT;
        signal.pullbackRecovering =
                signal.pullbackAvgShort > 0.0
                        && signal.pullbackAvgLong > 0.0
                        && signal.velocityShort >= effectivePbVelShort
                        && signal.velocityMid >= 0.0
                        && signal.price >= signal.pullbackAvgShort;

        // pullbackVelocityOk는 로깅/스코어링용으로만 유지
        signal.pullbackVelocityOk = signal.velocityShort > 0.0;
        double effectivePbVolMult = backtestConfig != null ? backtestConfig.pullbackVolumeMult : PULLBACK_VOLUME_MULT;
        signal.pullbackVolumeOk = signal.averageVolume > 0.0 && signal.volume >= (signal.averageVolume * effectivePbVolMult);

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

        // ── STRONG_PULLBACK / VWAP_RECLAIM 공통 보조 지표 ─────────────────
        signal.dayChangePct = st.dayOpenPrice > 0.0
                ? (price - st.dayOpenPrice) / st.dayOpenPrice * 100.0 : 0.0;
        signal.vol3  = st.minuteHistory.averageVolume(3);
        signal.vol5  = st.minuteHistory.averageVolume(5);
        signal.vol10 = st.minuteHistory.averageVolume(10);
        {
            MinuteBarHistory.MinuteBar lb = st.minuteHistory.latest();
            if (lb != null) {
                double barRange = lb.getHigh() - lb.getLow();
                signal.bodyRatio = barRange > 0.0
                        ? (lb.getClose() - lb.getOpen()) / barRange : 0.0;
            }
        }
        signal.aboveVwapBars = st.consecutiveAboveVwapBars;
        signal.hadVwapBelow  = st.barsSinceLastBelowVwap <= VR_LOOKBACK_BARS;

        // ── STRONG_PULLBACK 점수 ──────────────────────────────────────────
        {
            double spPbMin      = backtestConfig != null ? backtestConfig.spPullbackMinPct  : SP_PULLBACK_MIN_PCT;
            double spPbMax      = backtestConfig != null ? backtestConfig.spPullbackMaxPct  : SP_PULLBACK_MAX_PCT;
            double spVwapMinAb  = backtestConfig != null ? backtestConfig.spVwapMinAbovePct / 100.0 : SP_VWAP_MIN_ABOVE;
            double spVol3Max    = backtestConfig != null ? backtestConfig.spVol3RatioMax    : SP_VOL3_RATIO_MAX;
            double spBodyMin    = backtestConfig != null ? backtestConfig.spBodyRatioMin    : SP_BODY_RATIO_MIN;

            double pullbackPct  = -signal.fromHighPct; // 양수: 고점 아래로 눌림 %
            boolean cPullback   = pullbackPct >= spPbMin && pullbackPct <= spPbMax;
            boolean cVwapAbove  = signal.vwapDistancePct >= spVwapMinAb;
            boolean cVolDecrease= signal.vol3 > 0.0 && signal.vol10 > 0.0
                                   && signal.vol3 < signal.vol10 * spVol3Max;
            boolean cBullishBar = signal.bodyRatio >= spBodyMin;

            // 연속값 점수: 조건을 얼마나 잘 충족하는지 측정 (binary 아님)
            double spOptimal   = (spPbMin + spPbMax) / 2.0; // 최적 눌림 = 중간값
            double spHalfRange = (spPbMax - spPbMin) / 2.0;
            int sc1 = cPullback
                ? (int) Math.round(25.0 * Math.max(0.0, 1.0 - Math.abs(pullbackPct - spOptimal) / spHalfRange))
                : 0;  // 최적 눌림일수록 25점, 경계값에 가까울수록 0점
            int sc2 = cVwapAbove
                ? (int) Math.round(25.0 * Math.min(signal.vwapDistancePct / 0.008, 1.0))
                : 0;  // VWAP 0.2% → ~6점, 0.8%+ → 25점
            double vol3to10 = (signal.vol3 > 0.0 && signal.vol10 > 0.0) ? signal.vol3 / signal.vol10 : 1.0;
            int sc3 = cVolDecrease
                ? (int) Math.round(25.0 * Math.max(0.0, 1.0 - vol3to10 / spVol3Max))
                : 0;  // vol3/vol10=0 → 25점, 0.6(한계) → 0점
            int sc4 = cBullishBar
                ? (int) Math.round(25.0 * Math.min((signal.bodyRatio - spBodyMin) / Math.max(1.0 - spBodyMin, 0.01), 1.0))
                : 0;  // bodyRatio=0.6 → 0점, 1.0 → 25점
            signal.spScore    = sc1 + sc2 + sc3 + sc4;
            // 필수 조건: VWAP 위 + 눌림 범위 + 거래량 감소 + 양봉 재상승 확인
            signal.spCondsMet = cVwapAbove && cPullback && cVolDecrease && cBullishBar;
        }

        // ── VWAP_RECLAIM 점수 ─────────────────────────────────────────────
        {
            double vrVolM        = backtestConfig != null ? backtestConfig.vrVolMult         : VR_VOL_MULT;
            int    vrMinAbove    = backtestConfig != null ? backtestConfig.vrMinAboveVwapBars : VR_MIN_ABOVE_BARS;

            boolean cVwapReclaim = signal.aboveVwap && signal.hadVwapBelow;
            boolean cVolSurge   = signal.vol5 > 0.0 && signal.volume >= signal.vol5 * vrVolM;
            boolean cAboveBars  = signal.aboveVwapBars >= vrMinAbove;

            // 연속값 점수: 기준값 대비 얼마나 강한지 측정
            double volRatioVR = signal.vol5 > 0.0 ? signal.volume / signal.vol5 : 0.0;
            int vr1 = (int) Math.round(25.0 * Math.min(volRatioVR / (vrVolM * 1.6), 1.0));
            // vol=vrVolM(기준) → 15점, vrVolM*1.6x+ → 25점
            int vr2 = (int) Math.round(25.0 * Math.min((double) signal.aboveVwapBars / ((double) vrMinAbove * 1.5), 1.0));
            // bars=vrMinAbove(기준) → 16점, vrMinAbove*1.5봉+ → 25점
            int vr3 = (int) Math.round(25.0 * Math.min(signal.vwapDistancePct / 0.010, 1.0));
            // VWAP 이격 0% → 0점, 1.0%+ → 25점
            int vr4 = (int) Math.round(25.0 * Math.min(Math.max(signal.velocityShort, 0.0) / 0.006, 1.0));
            // 단기 속도 0 → 0점, 0.6%/s+ → 25점
            signal.vrScore    = vr1 + vr2 + vr3 + vr4;
            // 필수 조건: VWAP 재돌파 + 거래량 + 안착봉
            signal.vrCondsMet = cVwapReclaim && cVolSurge && cAboveBars;
        }

        boolean effectiveEnablePullback = backtestConfig != null ? backtestConfig.enablePullback : ENABLE_PULLBACK_ENTRY;
        signal.pullbackEntry = effectiveEnablePullback
                && signal.pullbackZone
                && signal.pullbackRecovering
                && signal.pullbackVolumeOk;

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

        // early momentum: 속도 기반 초기 급등 포착 (고점 근처 불필요, 거래량 조건 완화)
        // earlyMomentum: 초기 급등 포착 — 실패율 높으므로 조건 강화
        // fromHigh >= -0.2% 조건: 고점에서 0.2% 이상 이탈 시 이미 모멘텀 소진으로 판단
        double emFromHighRatio = signal.recentHigh > 0.0
                ? (signal.price - signal.recentHigh) / signal.recentHigh : 0.0;
        signal.earlyMomentum = ENABLE_EARLY_MOMENTUM_ENTRY
                && signal.velocityShort >= 0.006
                && signal.velocityMid >= 0.002
                && signal.trendScore == 3               // 단기/중기/장기 모두 상승 필수
                && signal.vwapDistancePct <= VWAP_MAX_GAP_EARLY_MOMENTUM  // VWAP 이격 상한 (상수 통일)
                && signal.vwapSlopeUp                   // VWAP slope 상승 필수
                && signal.averageTurnover > 0.0
                && signal.latestTurnover >= signal.averageTurnover * 1.5  // 거래대금 평균 1.5배 이상
                && signal.averageVolume > 0.0
                && signal.volume >= signal.averageVolume * 2.0  // 평균 2배 이상 거래량
                && signal.aboveVwap
                && !signal.choppyMarket
                && !signal.marketWeak
                && (signal.recentHigh <= 0.0 || emFromHighRatio >= -0.002); // 고점 -0.2% 이내

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

        // ── RSI_BOLLINGER_REBOUND 지표 계산 ──────────────────────────────
        {
            int bbPeriod    = backtestConfig != null ? backtestConfig.rsiBbPeriod              : 20;
            double bbMult   = backtestConfig != null ? backtestConfig.rsiBbStdMult             : 2.0;
            int rsiPeriod   = backtestConfig != null ? backtestConfig.rsiBbRsiPeriod           : 14;
            int sigPeriod   = backtestConfig != null ? backtestConfig.rsiBbSignalPeriod        : 9;
            double touchBuf = backtestConfig != null ? backtestConfig.rsiBbLowerTouchBufferPct / 100.0 : 0.002;
            double breakDwn = backtestConfig != null ? backtestConfig.rsiBbMaxBreakdownPct     / 100.0 : 0.005;
            // minVwap 설정값은 0.998 고정으로 대체 (VWAP -0.2% 이내: 하락추세 물림 방지)
            double rsiLowThr= backtestConfig != null ? backtestConfig.rsiBbRsiLowThreshold            : 45.0;

            MinuteBarHistory.BollingerBands bb   = st.minuteHistory.computeBollingerBands(bbPeriod, bbMult);
            MinuteBarHistory.RsiResult      rsiR = st.minuteHistory.computeRsiSignal(rsiPeriod, sigPeriod);
            MinuteBarHistory.FiveMinuteBar  five = st.minuteHistory.latestFiveMinuteBar();

            signal.bbMiddle    = bb.middle;
            signal.bbUpper     = bb.upper;
            signal.bbLower     = bb.lower;
            signal.bbBandwidth = bb.bandwidth;

            signal.rsi           = rsiR.rsi;
            signal.rsiSignal     = rsiR.rsiSignal;
            signal.prevRsi       = rsiR.prevRsi;
            signal.prevRsiSignal = rsiR.prevRsiSignal;
            signal.rsiCrossedUp  = rsiR.crossedUp;

            signal.fiveMinLow   = five.valid ? five.low   : 0.0;
            signal.fiveMinClose = five.valid ? five.close : 0.0;

            // BB 하단 터치: 5분 봉 low ≤ BB하단 × (1 + 버퍼)
            signal.bbLowerTouchedBy5m = bb.valid && five.valid
                    && five.low <= bb.lower * (1.0 + touchBuf);

            boolean rsiLowZone      = rsiR.valid && (rsiR.rsi <= rsiLowThr || rsiR.rsiSignal <= rsiLowThr);
            boolean rsiAscending    = rsiR.valid && rsiR.prevRsi < rsiR.rsi; // RSI 상향 전환 (이전봉보다 상승)
            boolean notBrokenDown   = bb.valid && price >= bb.lower * (1.0 - breakDwn);
            boolean notTooFarBelowVwap = signal.vwap <= 0.0 || price >= signal.vwap * 0.998; // VWAP -0.2% 이내

            signal.rsiBbCondsMet =
                    signal.enoughHistory
                    && st.minuteHistory.size() >= 30
                    && bb.valid
                    && rsiR.valid
                    && signal.bbLowerTouchedBy5m   // 5분 봉 BB 하단 터치
                    && rsiLowZone                  // RSI 과매도 구간
                    && rsiAscending                // RSI 상향 전환 (이전봉보다 RSI 증가)
                    && rsiR.crossedUp              // RSI 시그널 상향돌파
                    && notBrokenDown               // BB 하단 과하방 이탈 없음
                    && notTooFarBelowVwap;         // VWAP -0.2% 이내 (← -0.5%: 하락추세 물림 방지)

            // RSI_BB 점수 (0~100)
            int rsiBbSc = 0;
            if (signal.bbLowerTouchedBy5m)                                    rsiBbSc += 25;
            if (rsiR.crossedUp)                                               rsiBbSc += 25;
            if (rsiR.rsi <= 35.0 || rsiR.rsiSignal <= 35.0)                  rsiBbSc += 20;
            else if (rsiR.rsi <= rsiLowThr || rsiR.rsiSignal <= rsiLowThr)   rsiBbSc += 10;
            if (five.valid && price > five.close * 0.998)                     rsiBbSc += 10;
            double rsiBbVolRatio = signal.averageVolume > 0.0 ? signal.volume / signal.averageVolume : 0.0;
            if (rsiBbVolRatio >= 1.0)                                         rsiBbSc += 10;
            if (signal.dayChangePct > -3.0)                                   rsiBbSc += 10;
            signal.rsiBbScore = Math.min(rsiBbSc, 100);
        }

        // ── 1. EntryMode 확정 (우선순위: SP > VR > PULLBACK > BREAKOUT > EARLY_MOMENTUM > RSI_BB) ─
        signal.signalCount = 0;
        signal.entryMode   = EntryMode.NONE;

        boolean effectiveEnableSP = backtestConfig != null ? backtestConfig.enableStrongPullback : ENABLE_STRONG_PULLBACK_ENTRY;
        if (effectiveEnableSP && signal.spCondsMet) {
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.STRONG_PULLBACK;
        }
        boolean effectiveEnableVR = backtestConfig != null ? backtestConfig.enableVwapReclaim : ENABLE_VWAP_RECLAIM_ENTRY;
        if (effectiveEnableVR && signal.vrCondsMet) {
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.VWAP_RECLAIM;
        }
        if (signal.pullbackEntry) {
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.PULLBACK;
        }
        if (signal.volumeBreakout) {
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.BREAKOUT;
        }
        if (signal.strongBreakout) {
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.BREAKOUT;
        }
        if (signal.breakoutRetestReady && signal.breakoutRetestRecovering) {
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) {
                signal.entryMode = EntryMode.BREAKOUT;
            }
        }
        if (signal.earlyMomentum) {
            signal.signalCount++;
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.EARLY_MOMENTUM;
        }
        // RSI_BB: 최하위 우선순위 (다른 모드 신호 없을 때만 할당)
        boolean effectiveEnableRsiBb = backtestConfig != null ? backtestConfig.enableRsiBbRebound : ENABLE_RSI_BB_REBOUND_ENTRY;
        if (effectiveEnableRsiBb && signal.rsiBbCondsMet) {
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.RSI_BOLLINGER_REBOUND;
        }

        // ── OPENING_RANGE_BREAKOUT 조건 계산 ─────────────────────────────
        {
            LocalTime barTime = now.toLocalTime();
            boolean orbTimeOk  = market == Market.KRX
                    && st.openingRangeReady
                    && !barTime.isBefore(LocalTime.of(9, 10))
                    && barTime.isBefore(LocalTime.of(10, 30));
            boolean orbAbove   = st.openingRangeHigh > 0.0 && price > st.openingRangeHigh * 1.001;
            double inlineVolRatio = signal.averageVolume > 0.0 ? signal.volume / signal.averageVolume : 0.0;
            signal.orbBreakout = orbTimeOk
                    && orbAbove
                    && signal.aboveVwap
                    && signal.vwapSlopeUp
                    && inlineVolRatio >= 2.0
                    && signal.trendScore >= 2
                    && signal.velocityShort > signal.velocityMid
                    && signal.vwapDistancePct <= 0.018;
        }
        // ORB: RSI_BB와 동등 우선순위 (낮음), 다른 모드 없을 때 할당
        boolean effectiveEnableOrb = backtestConfig != null ? backtestConfig.enableOpeningRangeBreakout : ENABLE_OPENING_RANGE_BREAKOUT;
        if (effectiveEnableOrb && signal.orbBreakout) {
            if (signal.entryMode == EntryMode.NONE) signal.entryMode = EntryMode.OPENING_RANGE_BREAKOUT;
        }

        // ── 2. 계단식 세분화 점수 ─────────────────────────────────────────

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

        // 패턴 보너스 (max 20)
        signal.scorePattern = 0;
        if (signal.pullbackEntry)  signal.scorePattern += 5;
        if (signal.volumeBreakout) signal.scorePattern += 5;
        if (signal.strongBreakout) signal.scorePattern += 5;
        if (signal.entryMode == EntryMode.BREAKOUT && signal.consecutiveHigherClose) signal.scorePattern += 5;

        // ── 3. 감점 ──────────────────────────────────────────────────────
        int deductions = 0;
        if (signal.marketWeak)   deductions += 15;
        if (signal.choppyMarket) deductions += 8;

        // VWAP 과열 감점 (누적)
        if (signal.vwapDistancePct > 0.020)      deductions += 30; // >2.0%: -30
        else if (signal.vwapDistancePct > 0.012) deductions += 10; // >1.2%: -10

        // 고점 추격 감점: 고점 -0.2% 이내 = 실패 돌파 빈번
        if (signal.recentHigh > 0.0 && signal.fromHighPct > -0.2) deductions += 10;

        if (signal.entryMode != EntryMode.PULLBACK) {
            if (signal.vwapDistancePct > 0.018) deductions += 10;
            if (signal.entryMode != EntryMode.EARLY_MOMENTUM) {
                MinuteBarHistory.MinuteBar latestBar = st.minuteHistory.latest();
                if (latestBar != null) {
                    double barHigh  = latestBar.getHigh();
                    double barLow   = latestBar.getLow();
                    double barClose = latestBar.getClose();
                    if (barHigh > barLow
                            && (barHigh - barClose) / (barHigh - barLow) > 0.4) {
                        deductions += 12;
                    }
                }
            }
        }
        if (signal.velocityMid > 0.0 && signal.velocityShort < signal.velocityMid * 0.5) {
            deductions += 8;
        }

        // ── 4. 최종 점수 & Grade ─────────────────────────────────────────
        int rawScore = signal.scoreVwap + signal.scoreTrend + signal.scoreVolume
                + signal.scoreTurnover + signal.scoreHigh + signal.scorePattern - deductions;
        signal.signalScore = Math.min(Math.max(rawScore, 0), 100);

        if      (signal.signalScore >= 95) signal.signalGrade = "SS";
        else if (signal.signalScore >= 90) signal.signalGrade = "S";
        else if (signal.signalScore >= 85) signal.signalGrade = "A";
        else if (signal.signalScore >= 80) signal.signalGrade = "B";
        else if (signal.signalScore >= 75) signal.signalGrade = "C";
        else                               signal.signalGrade = "D";

        // ── 5. 시장 컨텍스트 필터 (entryMode 확정 후 단 한 번) ───────────────
        {
            MarketContext mCtx = marketContext.get(market);
            boolean ctxWeak   = mCtx != null && !isMarketContextExpired(mCtx) && mCtx.marketWeak;
            // useMarketFilter=true → 시장 약세 시 전 모드 차단 (8연패 방지)
            // useMarketFilter=false → BREAKOUT/EM만 차단 (기존 동작)
            boolean useFullFilter = backtestConfig != null ? backtestConfig.useMarketFilter : true;
            boolean marketPass = true;
            if (ctxWeak) {
                if (useFullFilter) {
                    marketPass = false;
                } else if (signal.entryMode != EntryMode.PULLBACK
                        && signal.entryMode != EntryMode.STRONG_PULLBACK
                        && signal.entryMode != EntryMode.VWAP_RECLAIM
                        && signal.entryMode != EntryMode.RSI_BOLLINGER_REBOUND) {
                    marketPass = false;
                }
            }
            signal.marketFilterPassed = marketPass;
        }

        if (!signal.enoughHistory) {
            signal.rejectReason = "NOT_ENOUGH_HISTORY";
        } else if (signal.timeWindowBlocked) {
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
        } else if (!signal.aboveVwap && signal.entryMode != EntryMode.RSI_BOLLINGER_REBOUND) {
            signal.rejectReason = "BELOW_VWAP";
        } else if (!signal.vwapSlopeUp
                && signal.entryMode != EntryMode.STRONG_PULLBACK
                && signal.entryMode != EntryMode.VWAP_RECLAIM
                && signal.entryMode != EntryMode.RSI_BOLLINGER_REBOUND) {
            signal.rejectReason = "VWAP_SLOPE_DOWN";
        } else if (signal.vwapTooFar) {
            signal.rejectReason = "VWAP_TOO_FAR_EXTREME";
        } else if (signal.entryMode == EntryMode.NONE) {
            signal.rejectReason = "NO_ENTRY_MODE";
        } else if (signal.entryMode == EntryMode.RSI_BOLLINGER_REBOUND) {
            int minRsiBbScore = backtestConfig != null ? backtestConfig.rsiBbMinScore : 75;
            if (!signal.enoughHistory || st.minuteHistory.size() < 30) {
                signal.rejectReason = "RSI_BB_NOT_ENOUGH_HISTORY";
            } else if (signal.bbLower <= 0.0) {
                signal.rejectReason = "RSI_BB_BB_INVALID";
            } else if (signal.rsi <= 0.0) {
                signal.rejectReason = "RSI_BB_RSI_INVALID";
            } else if (!signal.bbLowerTouchedBy5m) {
                signal.rejectReason = "RSI_BB_NO_LOWER_TOUCH";
            } else if (signal.rsi > 45.0 && signal.rsiSignal > 45.0) {
                signal.rejectReason = "RSI_BB_RSI_NOT_LOW";
            } else if (!signal.rsiCrossedUp) {
                signal.rejectReason = "RSI_BB_NO_RSI_CROSS";
            } else if (signal.bbLower > 0.0 && signal.price < signal.bbLower * 0.995) {
                signal.rejectReason = "RSI_BB_BROKEN_DOWN";
            } else if (signal.vwap > 0.0 && signal.price < signal.vwap * 0.985) {
                signal.rejectReason = "RSI_BB_TOO_FAR_BELOW_VWAP";
            } else if (signal.rsiBbScore < minRsiBbScore) {
                signal.rejectReason = "RSI_BB_SCORE_LOW";
            } else if (signal.averageVolume > 0.0 && signal.volume < signal.averageVolume * 0.7) {
                signal.rejectReason = "RSI_BB_VOLUME_LOW";
            } else {
                signal.rejectReason = "FILTER_LOW";
            }
        } else if (signal.entryMode == EntryMode.STRONG_PULLBACK) {
            int minSp = backtestConfig != null ? backtestConfig.spMinScore : SP_MIN_SCORE;
            if (!signal.spCondsMet) {
                signal.rejectReason = "SP_COND_FAIL";
            } else if (signal.velocityShort <= signal.velocityMid) {
                signal.rejectReason = "SP_ACCEL_FAIL";
            } else if (signal.spScore < minSp) {
                signal.rejectReason = "SP_SCORE_LOW";
            } else {
                signal.rejectReason = "FILTER_LOW";
            }
        } else if (signal.entryMode == EntryMode.VWAP_RECLAIM) {
            int minVr = backtestConfig != null ? backtestConfig.vrMinScore : VR_MIN_SCORE;
            int maxVr = backtestConfig != null && backtestConfig.vrMaxScore > 0 ? backtestConfig.vrMaxScore : VR_MAX_SCORE;
            if (!signal.vrCondsMet) {
                signal.rejectReason = "VR_COND_FAIL";
            } else if (signal.dayChangePct < -3.0) {
                signal.rejectReason = "VR_DAY_DOWN_BLOCKED";
            } else if (signal.vrScore > maxVr) {
                signal.rejectReason = "VR_SCORE_HIGH";
            } else if (signal.vrScore < minVr) {
                signal.rejectReason = "VR_SCORE_LOW";
            } else {
                signal.rejectReason = "FILTER_LOW";
            }
        } else {
            // Diagnose the specific per-mode gate (PULLBACK / BREAKOUT / EARLY_MOMENTUM)
            boolean highConviction = signal.signalScore >= HIGH_CONVICTION_SCORE;
            double effVwapGap;
            double effVolRatio;
            double effTurnoverRatio;
            if (signal.entryMode == EntryMode.BREAKOUT) {
                effVwapGap       = VWAP_MAX_GAP_BREAKOUT;
                effVolRatio      = VOLUME_RATIO_BREAKOUT        * (highConviction ? HIGH_CONVICTION_VOLUME_MULT : 1.0);
                effTurnoverRatio = TURNOVER_RATIO_BREAKOUT      * (highConviction ? HIGH_CONVICTION_TURNOVER_MULT : 1.0);
            } else if (signal.entryMode == EntryMode.EARLY_MOMENTUM) {
                effVwapGap       = VWAP_MAX_GAP_EARLY_MOMENTUM;
                effVolRatio      = VOLUME_RATIO_EARLY_MOMENTUM  * (highConviction ? HIGH_CONVICTION_VOLUME_MULT : 1.0);
                effTurnoverRatio = TURNOVER_RATIO_EARLY_MOMENTUM * (highConviction ? HIGH_CONVICTION_TURNOVER_MULT : 1.0);
            } else { // PULLBACK
                effVwapGap       = VWAP_MAX_GAP_PULLBACK;
                effVolRatio      = VOLUME_RATIO_PULLBACK        * (highConviction ? HIGH_CONVICTION_VOLUME_MULT : 1.0);
                effTurnoverRatio = TURNOVER_RATIO_PULLBACK      * (highConviction ? HIGH_CONVICTION_TURNOVER_MULT : 1.0);
            }

            int effMinScore;
            if (signal.entryMode == EntryMode.BREAKOUT) {
                effMinScore = backtestConfig != null ? backtestConfig.breakoutMinScore : 83;
            } else if (signal.entryMode == EntryMode.EARLY_MOMENTUM) {
                effMinScore = 70;
            } else {
                effMinScore = backtestConfig != null ? backtestConfig.pullbackMinScore : 80;
            }
            if (signal.vwapDistancePct > effVwapGap) {
                signal.rejectReason = "VWAP_GAP_TOO_LARGE";
            } else if (signal.averageVolume > 0.0 && signal.volume < signal.averageVolume * effVolRatio) {
                signal.rejectReason = "VOL_RATIO_LOW";
            } else if (signal.averageTurnover > 0.0 && signal.latestTurnover < signal.averageTurnover * effTurnoverRatio) {
                signal.rejectReason = "TURNOVER_RATIO_LOW";
            } else if (signal.signalScore < effMinScore) {
                signal.rejectReason = "SCORE_LOW";
            } else if (signal.entryMode == EntryMode.PULLBACK && !signal.pullbackEntry) {
                signal.rejectReason = "PULLBACK_COND_FAIL";
            } else if (signal.entryMode == EntryMode.BREAKOUT
                    && (!signal.breakoutRetestReady || !signal.breakoutRetestRecovering)) {
                signal.rejectReason = "BREAKOUT_RETEST_FAIL";
            } else if (signal.entryMode == EntryMode.BREAKOUT
                    && (backtestConfig == null || backtestConfig.breakoutRequireMultiUptrend)
                    && !signal.multiUptrend) {
                signal.rejectReason = "BREAKOUT_NO_MULTITREND";
            } else if (signal.entryMode == EntryMode.BREAKOUT && market == Market.KRX
                    && signal.vwapDistancePct < BREAKOUT_KRX_MIN_VWAP_GAP) {
                signal.rejectReason = "BREAKOUT_VWAP_GAP_LOW";
            } else if (signal.entryMode == EntryMode.BREAKOUT && market == Market.KRX
                    && (signal.averageVolume <= 0.0 || signal.volume < signal.averageVolume * (backtestConfig != null ? backtestConfig.breakoutKrxVolMult : BREAKOUT_KRX_MIN_VOLUME_MULT))) {
                signal.rejectReason = "BREAKOUT_VOLUME_LOW";
            } else if (signal.entryMode == EntryMode.EARLY_MOMENTUM && !signal.multiUptrend) {
                signal.rejectReason = "MOMENTUM_NO_MULTITREND";
            } else {
                signal.rejectReason = "FILTER_LOW";
            }
        }

        String _timeSlot = now.toLocalTime().isBefore(LocalTime.of(11, 0)) ? "AM" : "PM";
        signal.patternKey = signal.entryMode.name() + "|" + _timeSlot + "|TREND=" + signal.trendScore;

        return signal;
    }

    private boolean isEntryReady(BuySignal signal, Market market) {
        if (!signal.enoughHistory) return false;
        if (signal.timeWindowBlocked) return false;
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

        if (signal.entryMode == EntryMode.PULLBACK) {
            if (recentTicks.size() < 3) return false;
            double p1 = recentTicks.get(recentTicks.size() - 3).getPrice();
            if (!(p1 <= p2 && p2 <= p3)) return false;
        } else if (signal.entryMode != EntryMode.EARLY_MOMENTUM) {
            // Breakout / VolumeBreakout: 직전 틱 하락하지 않으면 진입 허용
            boolean twoTickOk = p2 <= p3;
            if (!twoTickOk) {
                return false;
            }
        }

        if (signal.pullbackEntry) {
            return currentPrice >= p2;
        }

        if (signal.entryMode == EntryMode.STRONG_PULLBACK) {
            // 눌림 회복 확인: 직전 틱 이상 + 거래량 감소 구간이므로 조건 완화
            return currentPrice >= p2;
        }

        if (signal.entryMode == EntryMode.VWAP_RECLAIM) {
            // VWAP 위 안착 확인: 직전 틱 이상
            return currentPrice >= p2 && signal.vwap > 0.0 && currentPrice > signal.vwap;
        }

        if (signal.entryMode == EntryMode.BREAKOUT) {
            if (!signal.breakoutRetestReady || !signal.breakoutRetestRecovering) {
                return false;
            }
            return currentPrice >= signal.recentHigh * BREAKOUT_RETEST_LOWER
                    && currentPrice <= signal.recentHigh * BREAKOUT_RETEST_UPPER
                    && currentPrice >= p2;
        }

        if (signal.entryMode == EntryMode.EARLY_MOMENTUM) {
            boolean ok = currentPrice >= p2
                    && signal.velocityShort > 0.0
                    && signal.multiUptrend;
            if (!ok) {
                return false;
            }
            return true;
        }

        if (signal.entryMode == EntryMode.RSI_BOLLINGER_REBOUND) {
            // 직전 틱 대비 상승 + BB 하단 이탈 미발생 + BB 중단선 미달성(상단 추격 금지)
            return currentPrice >= p2
                    && signal.bbLower > 0.0
                    && currentPrice >= signal.bbLower * 0.995
                    && currentPrice <= signal.bbMiddle;
        }

        if (signal.entryMode == EntryMode.OPENING_RANGE_BREAKOUT) {
            // ORB 돌파: 직전 틱 이상 유지 (강한 돌파이므로 조건 단순화)
            return currentPrice >= p2;
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

        // During KRX opening window (9:00~9:05), entryMode별 강화 조건 적용
        if (market == Market.KRX && isKrMarketCautiousWindow(now)) {
            if (signal.entryMode == EntryMode.PULLBACK) {
                boolean pullbackOpeningOk = signal.aboveVwap
                        && signal.velocityShort >= 0.0020
                        && signal.multiUptrend;
                if (!pullbackOpeningOk) return false;
            } else if (signal.entryMode == EntryMode.STRONG_PULLBACK) {
                if (signal.ticks < 10) return false; // 개장 초반 데이터 부족 시 SP 차단
            } else if (signal.entryMode == EntryMode.VWAP_RECLAIM) {
                // 개장 직후 VWAP_RECLAIM: 개장 직후에는 VWAP 자체가 불안정하므로 차단
                return false;
            } else {
                boolean cautiousPassed = (signal.signalCount >= 2 || signal.strongBreakout)
                        && signal.multiUptrend
                        && signal.velocity >= strongVelocityMin(market)
                        && signal.volume >= signal.averageVolume * STRONG_BREAKOUT_VOLUME_MULT;
                if (!cautiousPassed) return false;
            }
        }

        if (!signal.marketFilterPassed) {
            return false;
        }

        if (signal.timeWindowBlocked) {
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

        // 1-1. RSI_BB 평균회귀 익절: BB 중단선 회복 + RSI≥60 → 목표 달성
        if (st.entryMode == EntryMode.RSI_BOLLINGER_REBOUND) {
            int rsiP = backtestConfig != null ? backtestConfig.rsiBbRsiPeriod    : 14;
            int sigP = backtestConfig != null ? backtestConfig.rsiBbSignalPeriod : 9;
            int bbP  = backtestConfig != null ? backtestConfig.rsiBbPeriod       : 20;
            double bbM = backtestConfig != null ? backtestConfig.rsiBbStdMult    : 2.0;
            MinuteBarHistory.RsiResult rsiExit = st.minuteHistory.computeRsiSignal(rsiP, sigP);
            double bbMidExit = st.minuteHistory.computeBollingerBands(bbP, bbM).middle;
            if (rsiExit.valid && rsiExit.rsi >= 60.0 && bbMidExit > 0.0 && currentPrice >= bbMidExit) {
                return new SellDecision(true, currentQuantity, "RSI_BB_MEAN_REVERSION_PROFIT", false);
            }
        }

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

        // 2-1. VWAP_RECLAIM 전용: VWAP 재이탈 청산 (유예 + 버퍼는 설정값 사용)
        if (st.entryMode == EntryMode.VWAP_RECLAIM
                && holdMs >= effectiveVwapGraceMs
                && sessionVwap > 0.0
                && currentPrice < sessionVwap * effectiveVwapBreakBuffer
                && pnlRate <= -0.003) {
            return new SellDecision(true, currentQuantity, "VWAP_BREAK", true);
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
                && st.entryMode != EntryMode.EARLY_MOMENTUM
                && holdMs >= BREAKEVEN_GUARD_MIN_HOLD_MS
                && peakProfitRate >= effectiveBgePeak
                && pnlRate <= effectiveBgeLoss) {
            logger.info("SELL [BREAKEVEN_GUARD] {} pnl={} peakPnl={} holdMs={}",
                    symbol, fmtPct(pnlRate), fmtPct(peakProfitRate), holdMs);
            return new SellDecision(true, currentQuantity, "BREAKEVEN_GUARD", true);
        }

        // 8. VWAP_BREAK: 매수 후 유예 + 손실 구간에서만 발동
        // trail 비활성, 현재 속도 < -0.001 (진입 당시 속도가 아닌 현재 재계산 — 0.0 기준은 너무 민감)
        double exitVelocityShort = st.minuteHistory.velocitySeconds(TREND_SHORT_MIN_SECONDS, TREND_SHORT_MAX_SECONDS);
        double pullbackAvgShort = st.minuteHistory.averagePrice(3);
        if (vwapBreakEnabled
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
        if (failedBreakoutEnabled
                && st.entryMode == EntryMode.BREAKOUT
                && holdMs >= 180_000L
                && holdMs <= effectiveVwapGraceMs
                && exitVelocityShort < -0.0025
                && sessionVwap > 0.0
                && currentPrice < sessionVwap * 0.998) {
            return new SellDecision(true, currentQuantity, "FAILED_BREAKOUT", true);
        }

        // 8-2. PULLBACK / STRONG_PULLBACK: 1~3분 사이 실패 감지
        boolean failedPullbackEnabled = backtestConfig == null || backtestConfig.useFailedPullback;
        if (failedPullbackEnabled
                && (st.entryMode == EntryMode.PULLBACK || st.entryMode == EntryMode.STRONG_PULLBACK)
                && holdMs >= 60_000L
                && holdMs <= 180_000L
                && exitVelocityShort < -0.001
                && sessionVwap > 0.0
                && currentPrice < sessionVwap * 0.999) {
            return new SellDecision(true, currentQuantity, "FAILED_PULLBACK", true);
        }

        // 8-3. EARLY_MOMENTUM 전용: 1분~7분 사이 모멘텀 소멸 감지
        // velocity 음전 + avgShort 이탈 → 초기 추세 종료로 판단
        if (st.entryMode == EntryMode.EARLY_MOMENTUM
                && holdMs >= 60_000L
                && holdMs <= 420_000L
                && exitVelocityShort < -0.001
                && currentPrice < pullbackAvgShort) {
            return new SellDecision(true, currentQuantity, "EARLY_MOMENTUM_DEAD", true);
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
                case PULLBACK:
                    stop     = 1.0 - backtestConfig.pullbackStopPct   / 100.0;
                    tp       = 1.0 + backtestConfig.pullbackTpPct     / 100.0;
                    tStart   = backtestConfig.pullbackTrailSt   / 100.0;
                    tDrop    = backtestConfig.pullbackTrailDrop / 100.0;
                    useTrail = tStart > 0;
                    break;
                case BREAKOUT:
                    stop     = 1.0 - backtestConfig.breakoutStopPct   / 100.0;
                    tp       = 1.0 + backtestConfig.breakoutTpPct     / 100.0;
                    tStart   = backtestConfig.breakoutTrailSt   / 100.0;
                    tDrop    = backtestConfig.breakoutTrailDrop / 100.0;
                    useTrail = tStart > 0;
                    break;
                case EARLY_MOMENTUM:
                    stop     = 1.0 - backtestConfig.emStopPct / 100.0;
                    tp       = 1.0 + backtestConfig.emTpPct   / 100.0;
                    tStart   = 0; tDrop = 0; useTrail = false;
                    break;
                case STRONG_PULLBACK:
                    stop     = 1.0 - backtestConfig.spStopPct  / 100.0;
                    tp       = backtestConfig.spTpPct > 0 ? 1.0 + backtestConfig.spTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.spTrailSt   / 100.0;
                    tDrop    = backtestConfig.spTrailDrop  / 100.0;
                    useTrail = tStart > 0;
                    break;
                case VWAP_RECLAIM:
                    stop     = 1.0 - backtestConfig.vrStopPct  / 100.0;
                    tp       = backtestConfig.vrTpPct > 0 ? 1.0 + backtestConfig.vrTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.vrTrailSt   / 100.0;
                    tDrop    = backtestConfig.vrTrailDrop  / 100.0;
                    useTrail = tStart > 0;
                    break;
                case OPENING_RANGE_BREAKOUT:
                    stop     = 1.0 - backtestConfig.orbStopPct   / 100.0;
                    tp       = backtestConfig.orbTpPct > 0 ? 1.0 + backtestConfig.orbTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.orbTrailSt   / 100.0;
                    tDrop    = backtestConfig.orbTrailDrop  / 100.0;
                    useTrail = tStart > 0;
                    break;
                default: // RSI_BOLLINGER_REBOUND
                    stop     = 1.0 - backtestConfig.rsiBbStopPct  / 100.0;
                    tp       = backtestConfig.rsiBbTpPct > 0 ? 1.0 + backtestConfig.rsiBbTpPct / 100.0 : 9999.0;
                    tStart   = backtestConfig.rsiBbTrailSt   / 100.0;
                    tDrop    = backtestConfig.rsiBbTrailDrop / 100.0;
                    useTrail = tStart > 0;
                    break;
            }
            return new ExitProfile(stop, tp > 9990 ? null : tp, useTrail ? tStart : null, useTrail ? tDrop : null, useTrail, label);
        }
        switch (mode) {
            case PULLBACK:
                return new ExitProfile(STOP_KRX_PULLBACK, TP_KRX_PULLBACK,
                        TRAIL_START_KRX_PULLBACK, TRAIL_DROP_KRX_PULLBACK, true, "PULLBACK");
            case BREAKOUT:
                return new ExitProfile(STOP_KRX_BREAKOUT, TP_KRX_BREAKOUT,
                        TRAIL_START_KRX_BREAKOUT, TRAIL_DROP_KRX_BREAKOUT, true, "BREAKOUT");
            case EARLY_MOMENTUM:
                return new ExitProfile(STOP_KRX_EARLY_MOMENTUM, TP_KRX_EARLY_MOMENTUM,
                        null, null, false, "EARLY_MOMENTUM");
            case STRONG_PULLBACK:
                return new ExitProfile(STOP_KRX_STRONG_PB, TP_KRX_STRONG_PB,
                        TRAIL_ST_KRX_STRONG_PB, TRAIL_DR_KRX_STRONG_PB, true, "STRONG_PULLBACK");
            case VWAP_RECLAIM:
                return new ExitProfile(STOP_KRX_VR, TP_KRX_VR,
                        TRAIL_ST_KRX_VR, TRAIL_DR_KRX_VR, true, "VWAP_RECLAIM");
            case RSI_BOLLINGER_REBOUND:
                return new ExitProfile(STOP_KRX_RSI_BB, TP_KRX_RSI_BB,
                        TRAIL_ST_KRX_RSI_BB, TRAIL_DR_KRX_RSI_BB, true, "RSI_BB");
            case OPENING_RANGE_BREAKOUT:
                return new ExitProfile(STOP_KRX_ORB, TP_KRX_ORB,
                        TRAIL_ST_KRX_ORB, TRAIL_DR_KRX_ORB, true, "ORB");
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

        if (signal.entryMode == EntryMode.PULLBACK) {
            if (signal.volume > 0.0
                    && signal.averageVolume > 0.0
                    && signal.volume >= signal.averageVolume * VOLUME_SURGE_MULT_FOR_SIZE_UP) {
                return SIZE_UP_MULT;
            }
            return BASE_SIZE_PULLBACK;
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

        if (signal.entryMode == EntryMode.EARLY_MOMENTUM) {
            return BASE_SIZE_EARLY_MOMENTUM;
        }

        if (signal.entryMode == EntryMode.STRONG_PULLBACK || signal.entryMode == EntryMode.VWAP_RECLAIM
                || signal.entryMode == EntryMode.RSI_BOLLINGER_REBOUND
                || signal.entryMode == EntryMode.OPENING_RANGE_BREAKOUT) {
            return BASE_SIZE_PULLBACK;
        }

        return 0.0;
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
                || "FAILED_BREAKOUT".equals(reason)
                || "FAILED_PULLBACK".equals(reason)
                || "EARLY_MOMENTUM_DEAD".equals(reason)) {
            return ExitType.STOPLOSS;
        }
        if ("TIME_STOP_SOFT".equals(reason)
                || "TIME_STOP_MID".equals(reason)
                || "TIME_STOP_HARD".equals(reason)
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
        if ("FAILED_PULLBACK".equals(reason)) return REENTER_FAILED_PULLBACK_COOLDOWN_MS;
        if ("EARLY_MOMENTUM_DEAD".equals(reason)) return REENTER_EARLY_MOMENTUM_DEAD_COOLDOWN_MS;
        return REENTER_STOPLOSS_COOLDOWN_MS; // STOP_LOSS / VWAP_BREAK / EMERGENCY_STOP 등: 15분
    }


    private boolean isStopLossExitReason(String reason) {
        if (reason == null) return false;
        return reason.startsWith("STOP_LOSS_")
                || "EMERGENCY_STOP".equals(reason)
                || "FAILED_BREAKOUT".equals(reason)
                || "FAILED_PULLBACK".equals(reason)
                || "EARLY_MOMENTUM_DEAD".equals(reason)
                || "VWAP_BREAK".equals(reason);
    }

    private boolean isTimeStopExitReason(String reason) {
        return false; // TIME_STOP 이유는 현재 미사용 — dead code 제거
    }

    private String buildBuyWhyString(BuySignal signal) {
        StringBuilder sb = new StringBuilder();
        if (signal.entryMode == EntryMode.PULLBACK) {
            sb.append("zone(").append(fmtPct(signal.pullbackDepthFromHigh)).append("fromHigh)");
            if (signal.velocityShort > 0.0)                            sb.append(" vel+");
            if (signal.pullbackAvgShort >= signal.pullbackAvgLong)     sb.append(" avgCross");
            if (signal.price >= signal.pullbackAvgShort)               sb.append(" priceAboveAvg");
            if (signal.averageVolume > 0.0)
                sb.append(" vol(").append(String.format("%.2f", signal.volume / signal.averageVolume)).append("x)");
        } else if (signal.entryMode == EntryMode.BREAKOUT) {
            if (signal.strongBreakout)           sb.append("strongBreakout");
            if (signal.volumeBreakout)           sb.append(" volBreakout");
            if (signal.breakoutRetestRecovering) sb.append(" retestOK");
            if (signal.multiUptrend)             sb.append(" multiUp");
            if (signal.averageVolume > 0.0)
                sb.append(" vol(").append(String.format("%.2f", signal.volume / signal.averageVolume)).append("x)");
        } else if (signal.entryMode == EntryMode.EARLY_MOMENTUM) {
            sb.append("velShort=").append(fmtPct(signal.velocityShort));
            sb.append(" velMid=").append(fmtPct(signal.velocityMid));
            if (signal.aboveVwap) sb.append(" aboveVwap");
        } else if (signal.entryMode == EntryMode.STRONG_PULLBACK) {
            sb.append(String.format("dayChg=%.2f%%", signal.dayChangePct));
            sb.append(String.format(" fromHigh=%.2f%%", signal.fromHighPct));
            sb.append(" spScore=").append(signal.spScore);
            if (signal.vol10 > 0.0)
                sb.append(String.format(" vol3/vol10=%.2fx", signal.vol3 / signal.vol10));
            sb.append(String.format(" body=%.2f", signal.bodyRatio));
        } else if (signal.entryMode == EntryMode.VWAP_RECLAIM) {
            sb.append(String.format("dayChg=%.2f%%", signal.dayChangePct));
            sb.append(" vrScore=").append(signal.vrScore);
            sb.append(" aboveBars=").append(signal.aboveVwapBars);
            sb.append(" hadBelow=").append(signal.hadVwapBelow);
            if (signal.vol5 > 0.0)
                sb.append(String.format(" vol/vol5=%.2fx", signal.volume / signal.vol5));
        } else if (signal.entryMode == EntryMode.RSI_BOLLINGER_REBOUND) {
            sb.append(String.format("rsi=%.1f sig=%.1f", signal.rsi, signal.rsiSignal));
            sb.append(String.format(" bbLow=%.0f", signal.bbLower));
            sb.append(String.format(" 5mLow=%.0f", signal.fiveMinLow));
            sb.append(" rsiBbScore=").append(signal.rsiBbScore);
            if (signal.averageVolume > 0.0)
                sb.append(String.format(" vol(%.2fx)", signal.volume / signal.averageVolume));
        } else if (signal.entryMode == EntryMode.OPENING_RANGE_BREAKOUT) {
            sb.append(String.format("vwap=%.2f%%", signal.vwapDistancePct * 100.0));
            sb.append(String.format(" trend=%d", signal.trendScore));
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
        if (signal.pullbackEntry) sb.append("P");
        if (signal.momentumBreakout) sb.append("M");
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
