package com.autotrading.scheduler;

import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.market.MarketDataService;
import com.autotrading.order.OrderService;
import com.autotrading.position.PositionService;
import com.autotrading.dao.AutoTradingDao;
import com.autotrading.dao.WatchlistDao;
import com.autotrading.model.AutoPosition;
import com.autotrading.model.StockQuote;
import com.autotrading.model.WatchlistItem;
import com.autotrading.service.RiskManager;
import com.autotrading.strategy.StrategyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SchedulerService v3.6
 *
 * [v3.5 → v3.6 수정 내역]
 *
 * 수정 A (High). exchange 파라미터 무시 문제
 *   기존: start(symbol, exchange)가 exchange를 무시하고 start(symbol)과 동일하게 동작.
 *         isOverseasSymbol()로만 시장을 감지하여 KRX 심볼도 OK지만
 *         exchange가 명시된 경우에는 그대로 사용되어야 하는데 무시되는 버그.
 *   수정: start(symbol, exchange)에서 exchange를 StrategyEngine.Market으로 변환하여
 *         strategyEngine.setMarket()으로 명시적으로 등록.
 *         exchange가 null이면 기존처럼 자동 감지에 위임.
 *
 * 수정 B (High). buyPending이 placeOrder 직전에만 설정되도록 수정
 *   기존: decide()가 BUY OrderCommand 반환 직후 buyPending=true 설정.
 *         그 사이 타이머 재진입 시 pending 30초 대기.
 *   수정: decide()에서 pending을 설정하지 않음.
 *         execute()에서 실제 주문 직전(placeOrder 직전)에만
 *         strategyEngine.markBuyPending()을 호출.
 *         재진입 시 markBuyPending() 이전에는 pending이 설정되지 않으므로 안전.
 *
 * 수정 C (High). SELL 후 clearPositionState 타이밍 문제
 *   기존: decide() 내부에서 SELL 신호 생성 직후 clearPositionState() 호출.
 *         이후 REJECT/ERROR 시에도 상태가 초기화되는 버그.
 *   수정: handleAccepted(SELL)에서 strategyEngine.notifySellAccepted()를 호출.
 *         REJECT/ERROR 시에는 호출하지 않아 상태가 유지되어 재시도 가능.
 *
 * 수정 D (Medium). stopSymbol() 개별 종목 중지 기능 추가
 *   기존: stop()이 전체 종목을 중지. toggle(enable=false)이 stop()을 호출하여
 *         다른 종목도 함께 중지되는 문제.
 *   수정: stopSymbol(symbol)을 추가하여 특정 종목만 중지하고 해당 상태만 초기화.
 *         toggle(enable=false)은 별도로 ApiController에서 호출 방식 변경 필요.
 *         stop()은 전체 중지 전용으로 유지.
 *
 * v3.5에서 유지되는 사항:
 *   BarAccumulator startNewBar 중복 시작 방지 (타이밍 문제, 분봉 정확성)
 *   REJECTED/ERROR 케이스 처리 분리
 *   stop() 전체 상태 clear
 *   해외 심볼 DB fallback
 */
@Service
public class SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final MarketDataService        marketDataService;
    private final StrategyEngine           strategyEngine;
    private final OrderService             orderService;
    private final PositionService          positionService;
    private final AutoTradingDao           autoTradingDao;
    private final WatchlistDao             watchlistDao;
    private final RiskManager              riskManager;
    private final KoreaInvestmentApiClient kisApiClient;

    private final Map<String, Timer>          timers          = new ConcurrentHashMap<>();
    private final Map<String, Double>         prevVolume      = new ConcurrentHashMap<>();
    private final Map<String, Instant>        lastSellTime    = new ConcurrentHashMap<>();
    private final Map<String, Instant>        stopLossTime    = new ConcurrentHashMap<>();
    private final Map<String, BarAccumulator> barAccumMap     = new ConcurrentHashMap<>();
    private final Map<String, String>         symbolExchange  = new ConcurrentHashMap<>();
    private final Map<String, Integer>        quoteFailCount  = new ConcurrentHashMap<>();
    private final Map<String, String>         symbolNameCache = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> buyFillConfirmTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService buyFillConfirmExecutor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "buy-fill-confirm");
                    t.setDaemon(true);
                    return t;
                }
            });

    // ── 1분봉 누적기 ────────────────────────────────────────────────────────────
    private static class BarAccumulator {
        double open       = 0;
        double high       = 0;
        double low        = Double.MAX_VALUE;
        double close      = 0;
        double volAccum   = 0;
        long   barStartMs = 0;

        boolean isEmpty() { return barStartMs == 0; }

        void update(double price, double deltaVol) {
            if (isEmpty()) {
                open = price; high = price; low = price;
            } else {
                high = Math.max(high, price);
                low  = Math.min(low,  price);
            }
            close    = price;
            volAccum += deltaVol;
        }

        void startNewBar(long newBarStartMs, double firstPrice, double firstDeltaVol) {
            open       = firstPrice;
            high       = firstPrice;
            low        = firstPrice;
            close      = firstPrice;
            volAccum   = firstDeltaVol;
            barStartMs = newBarStartMs;
        }

        static long bucketTs(long epochMs) {
            return (epochMs / 60_000L) * 60_000L;
        }
    }

    private static final long COOLDOWN_NORMAL_SEC   = 120;
    private static final long COOLDOWN_STOPLOSS_SEC = 300;

    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_ERROR    = "ERROR";
    private static final int BUY_FILL_CONFIRM_MAX_ATTEMPTS = 5;
    private static final long BUY_FILL_CONFIRM_INTERVAL_MS = 700L;

    public SchedulerService(MarketDataService marketDataService,
                            StrategyEngine strategyEngine,
                            OrderService orderService,
                            PositionService positionService,
                            AutoTradingDao autoTradingDao,
                            WatchlistDao watchlistDao,
                            RiskManager riskManager,
                            KoreaInvestmentApiClient kisApiClient) {
        this.marketDataService = marketDataService;
        this.strategyEngine    = strategyEngine;
        this.orderService      = orderService;
        this.positionService   = positionService;
        this.autoTradingDao    = autoTradingDao;
        this.watchlistDao      = watchlistDao;
        this.riskManager       = riskManager;
        this.kisApiClient      = kisApiClient;
    }

    public synchronized String start(String symbol) {
        return start(symbol, null, null);
    }

    /**
     * [수정 A] exchange를 strategyEngine.setMarket()으로 명시적으로 등록.
     *
     * exchange 값 → Market 매핑:
     *   "KRX", "KR", "KOSPI", "KOSDAQ" → Market.KRX
     *   "NAS", "NASD", "NYSE", "AMEX"  → Market.US
     *   null (KRX 심볼 = 숫자로 시작)   → Market.KRX (자동 감지)
     *   null && 영문 심볼               → Market.US  (기존처럼 자동 감지에 위임)
     *
     * setMarket()을 호출하면 detectMarket()에서 st.market이 null이 아니므로
     * 자동 감지 없이 명시적으로 등록된 값을 사용한다.
     */
    public synchronized String start(String symbol, String exchange) {
        return start(symbol, exchange, null);
    }

    public synchronized String start(String symbol, String exchange, Double buyAmount) {
        if (symbol == null || symbol.isBlank()) return "Invalid symbol";
        String sym = symbol.trim().toUpperCase();
        if (buyAmount != null) {
            strategyEngine.setBuyAmount(sym, buyAmount);
        }
        if (timers.containsKey(sym)) {
            if (buyAmount != null) {
                return "Already Running (buy amount updated): " + sym;
            }
            return "Already Running: " + sym;
        }

        // [수정 A] Market 등록
        String orderExchange = resolveOrderExchangeForStart(sym, exchange);
        symbolExchange.put(sym, orderExchange);

        StrategyEngine.Market market = resolveMarket(orderExchange);
        strategyEngine.setMarket(sym, market);
        logger.info("Market set for {} : exchange={} -> orderExchange={} -> {}",
                sym, exchange, orderExchange, market);
        // exchange == null이면 StrategyEngine.detectMarket()이 자동 감지에 위임

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                try { execute(sym); }
                catch (Exception e) {
                    logger.error("Strategy error for {}", sym, e);
                }
            }
        }, 0, 5_000);
        timers.put(sym, timer);
        logger.info("Auto-trading scheduler started for {}", sym);
        return "Started " + sym;
    }

    /**
     * [수정 A] exchange 문자열을 StrategyEngine.Market으로 변환
     */
    private StrategyEngine.Market resolveMarket(String exchUpper) {
        if (exchUpper == null) {
            return StrategyEngine.Market.KRX;
        }
        switch (exchUpper) {
            case "KRX":
            case "KR":
            case "KOSPI":
            case "KOSDAQ":
                return StrategyEngine.Market.KRX;
            default:
                return StrategyEngine.Market.US;
        }
    }

    private void execute(String symbol) {
        if (riskManager.hasHitLossLimit()) {
            logger.warn("Daily loss limit reached; stopping all trading");
            stop();
            return;
        }

        // ── 1. 현재가 시세 조회 ──────────────────────────────────────────────────
        String orderExchange = resolveOrderExchangeForRuntime(symbol);
        String quoteExchangeHint = toQuoteExchangeHint(orderExchange);
        var quote = safeFetchQuote(symbol, quoteExchangeHint);
        if (quote == null) {
            return;
        }
        quoteFailCount.remove(symbol);
        orderExchange = syncOrderExchangeWithQuote(symbol, orderExchange);
        double price = quote.getPrice();
        double rawVol = quote.getVolume();
        String symbolName = resolveSymbolName(symbol, quote);

        // ── 2. delta 거래량 계산 ─────────────────────────────────────────────────
        double prevVol     = prevVolume.getOrDefault(symbol, rawVol);
        double deltaVolume = Math.max(0, rawVol - prevVol);
        prevVolume.put(symbol, rawVol);

        // ── 3. 1분봉 누적 처리 ───────────────────────────────────────────────────
        long nowMs    = System.currentTimeMillis();
        long bucketTs = BarAccumulator.bucketTs(nowMs);

        BarAccumulator accum = barAccumMap.computeIfAbsent(symbol, k -> new BarAccumulator());

        if (!accum.isEmpty() && accum.barStartMs != bucketTs) {
            logger.debug("1m bar completed for {} @ {} o={} h={} l={} c={} vol={}",
                    symbol, accum.barStartMs,
                    accum.open, accum.high, accum.low, accum.close, accum.volAccum);
            // 5초 틱마다 저장하지 않고 완성된 1분봉 단위로 저장
            autoTradingDao.savePriceLog(symbol, symbolName, accum.close, accum.volAccum, quote.getTimestamp());
            strategyEngine.record(
                    symbol,
                    accum.open, accum.high, accum.low, accum.close,
                    accum.volAccum, accum.barStartMs
            );
            accum.startNewBar(bucketTs, price, deltaVolume);
        } else if (accum.isEmpty()) {
            accum.startNewBar(bucketTs, price, deltaVolume);
        } else {
            accum.update(price, deltaVolume);
        }

        double currentVolume1m = accum.volAccum;

        // ── 4. 보유 포지션 조회 ──────────────────────────────────────────────────
        int    quantity = 0;
        double avgPrice = 0;

        if (isOverseasSymbol(symbol)) {
            logger.debug("Overseas symbol {} → skipping fetchDomesticBalance, using DB", symbol);
            AutoPosition dbPos = positionService.getPosition(symbol);
            quantity = dbPos != null ? dbPos.getQuantity() : 0;
            avgPrice = dbPos != null ? dbPos.getAvgPrice()  : 0;
        } else {
            RealPosition real = fetchRealPosition(symbol);
            if (real != null) {
                quantity = real.quantity;
                avgPrice = real.avgPrice;
                AutoPosition dbPos = positionService.getPosition(symbol);
                int dbQty = dbPos != null ? dbPos.getQuantity() : 0;
                if (dbQty != quantity) {
                    logger.info("Position sync {} : DB qty={} → Real qty={} avgPrice={}",
                            symbol, dbQty, quantity, avgPrice);
                    positionService.updatePosition(symbol, quantity, avgPrice);
                }
            } else {
                logger.warn("Real position fetch failed for {}, falling back to DB", symbol);
                AutoPosition dbPos = positionService.getPosition(symbol);
                quantity = dbPos != null ? dbPos.getQuantity() : 0;
                avgPrice = dbPos != null ? dbPos.getAvgPrice()  : 0;
            }
        }

        // If a live position exists, treat BUY as filled and stop async confirm task.
        if (quantity > 0) {
            cancelBuyFillConfirmTask(symbol);
            strategyEngine.notifyBuyFilled(symbol);
        }

        // ── 5. 전략 판단 ─────────────────────────────────────────────────────────
        var orderOpt = strategyEngine.decide(symbol, price, currentVolume1m, quantity, avgPrice);
        if (orderOpt.isEmpty()) return;

        var command = orderOpt.get();
        if (isOverseasSymbol(symbol)) {
            command.setExchange(orderExchange);
        }

        // ── 6. 매수/매도 쿨다운 체크 ────────────────────────────────────────────
        if ("BUY".equals(command.getType())) {
            Instant slTime = stopLossTime.get(symbol);
            if (slTime != null && Instant.now().isBefore(slTime.plusSeconds(COOLDOWN_STOPLOSS_SEC))) {
                long rem = COOLDOWN_STOPLOSS_SEC
                        - (Instant.now().getEpochSecond() - slTime.getEpochSecond());
                logger.info("BUY blocked (stop-loss cooldown) for {} ({}s remaining)", symbol, rem);
                // [수정 B] markBuyPending 이전에 리턴하므로 pending 설정 없음. 안전.
                return;
            }
            Instant sellTime = lastSellTime.get(symbol);
            if (sellTime != null && Instant.now().isBefore(sellTime.plusSeconds(COOLDOWN_NORMAL_SEC))) {
                long rem = COOLDOWN_NORMAL_SEC
                        - (Instant.now().getEpochSecond() - sellTime.getEpochSecond());
                logger.info("BUY blocked (sell cooldown) for {} ({}s remaining)", symbol, rem);
                return;
            }
        }

        // ── 7. 주문 실행 전 계좌 사전체크 ────────────────────────────────────────
        // [수정 B] 실제 주문 직전(placeOrder 직전)에만 BUY pending 설정.
        //          이 시점 이전에는 pending이 설정되지 않으므로 타이머 재진입 시 안전.
        // BUY 직전 실계좌 포지션 재확인으로 중복 매수 방지
        if ("BUY".equals(command.getType())) {
            RealPosition livePos = isOverseasSymbol(symbol)
                    ? fetchOverseasRealPosition(symbol, orderExchange)
                    : fetchRealPosition(symbol);

            AutoPosition dbPosNow = positionService.getPosition(symbol);
            int dbQtyNow = dbPosNow != null ? dbPosNow.getQuantity() : 0;
            double dbAvgNow = dbPosNow != null ? dbPosNow.getAvgPrice() : 0.0;

            if (livePos != null && (livePos.quantity != dbQtyNow
                    || Math.abs(livePos.avgPrice - dbAvgNow) > 0.0001)) {
                logger.info("Pre-order position sync {} : DB qty={} -> Real qty={} avgPrice={}",
                        symbol, dbQtyNow, livePos.quantity, livePos.avgPrice);
                positionService.updatePosition(symbol, livePos.quantity, livePos.avgPrice);
                dbQtyNow = livePos.quantity;
            }

            int effectiveQty = livePos != null ? Math.max(livePos.quantity, dbQtyNow) : dbQtyNow;
            if (effectiveQty > 0) {
                logger.info("BUY blocked (pre-check holding) for {} : qty={}", symbol, effectiveQty);
                return;
            }

            if (livePos == null) {
                logger.warn("BUY pre-check position unavailable for {} -> proceeding with snapshot qty={}",
                        symbol, quantity);
            }
        }

        // Re-check live account position right before SELL to avoid repeated rejects.
        if ("SELL".equals(command.getType())) {
            RealPosition livePos = isOverseasSymbol(symbol)
                    ? fetchOverseasRealPosition(symbol, orderExchange)
                    : fetchRealPosition(symbol);

            AutoPosition dbPosNow = positionService.getPosition(symbol);
            int dbQtyNow = dbPosNow != null ? dbPosNow.getQuantity() : 0;
            double dbAvgNow = dbPosNow != null ? dbPosNow.getAvgPrice() : 0.0;

            if (livePos != null && (livePos.quantity != dbQtyNow
                    || Math.abs(livePos.avgPrice - dbAvgNow) > 0.0001)) {
                logger.info("Pre-order position sync {} : DB qty={} -> Real qty={} avgPrice={}",
                        symbol, dbQtyNow, livePos.quantity, livePos.avgPrice);
                positionService.updatePosition(symbol, livePos.quantity, livePos.avgPrice);
                dbQtyNow = livePos.quantity;
                quantity = livePos.quantity;
                avgPrice = livePos.avgPrice;
            }

            int availableQty = livePos != null ? livePos.quantity : dbQtyNow;
            if (availableQty <= 0) {
                logger.info("SELL blocked (pre-check no holding) for {}", symbol);
                // Clear stale-hold state only (do not reuse SELL-accepted semantic path).
                strategyEngine.clearStaleHoldState(symbol);
                return;
            }

            if (command.getQuantity() > availableQty) {
                logger.info("SELL quantity adjusted for {} : requested={} -> available={}",
                        symbol, command.getQuantity(), availableQty);
                command.setQuantity(availableQty);
            }

            if (livePos == null) {
                logger.warn("SELL pre-check position unavailable for {} -> proceeding with snapshot qty={}",
                        symbol, quantity);
            }
        }

        // ── 8. 리스크 매니저 체크 (실주문 직전) ──────────────────────────────────
        if (!riskManager.allowOrder(symbol)) {
            logger.warn("Order blocked by risk manager for {}", symbol);
            return;
        }

        if ("BUY".equals(command.getType())) {
            strategyEngine.markBuyPending(symbol);
        }

        Map<String, Object> resp;
        try {
            resp = orderService.placeOrder(command);
        } catch (Exception e) {
            if ("BUY".equals(command.getType())) {
                strategyEngine.notifyBuyRejected(symbol);
            } else {
                strategyEngine.notifySellRejected(symbol);
            }
            riskManager.orderFailed(symbol);
            logger.error("Order ERROR (EXCEPTION) for {} side={} -> treated as failed. msg={}",
                    symbol, command.getType(), e.getMessage());
            return;
        }
        String respStatus = normalizeStatus(resp.getOrDefault("status", ""));
        String respMsg    = String.valueOf(resp.getOrDefault("message", ""));

        logger.info("Order response: symbol={} side={} price={} qty={} status={} session=- message={}",
                symbol, command.getType(), command.getPrice(),
                command.getQuantity(), respStatus, respMsg);

        String logReason = "Strategy | " + respStatus;
        if (respMsg != null && !respMsg.isBlank()) {
            String cleanMsg = respMsg.replaceAll("[\\r\\n]+", " ").trim();
            if (cleanMsg.length() > 180) {
                cleanMsg = cleanMsg.substring(0, 180);
            }
            logReason = logReason + " | " + cleanMsg;
        }
        autoTradingDao.saveOrderLog(
                symbol, command.getType(), command.getQuantity(), command.getPrice(),
                logReason);

        // ── 9. 주문 결과 처리 ────────────────────────────────────────────────────
        if (STATUS_ACCEPTED.equals(respStatus)) {
            handleAccepted(symbol, command, quantity, avgPrice);
            riskManager.orderSucceeded(symbol);
        } else if (STATUS_REJECTED.equals(respStatus)) {
            handleRejected(symbol, command, respMsg);
            riskManager.orderFailed(symbol);
        } else {
            handleError(symbol, command, respStatus, respMsg);
            riskManager.orderFailed(symbol);
        }
    }

    // ── 주문 결과 처리 메서드 ────────────────────────────────────────────────────

    /**
     * ACCEPTED = 주문 접수 완료 (체결 완료 아님).
     *
     * BUY: 포지션 낙관적 업데이트.
     *      pending은 유지하고 실제 체결은 confirmBuyFilled() / 다음 주기 실계좌 sync로 확정.
     *
     * SELL: 포지션 업데이트 후 notifySellAccepted()로 전략 상태 정리.
     *       [수정 C] 기존에는 decide() 내부에서 clearPositionState()를 신호 생성과 동시에 호출했으나
     *               REJECT/ERROR 시에도 상태가 초기화되는 버그 존재.
     *               실제 ACCEPTED 확인 후에만 상태를 정리하도록 변경.
     */
    private void handleAccepted(String symbol,
                                com.autotrading.model.OrderCommand command,
                                int quantity, double avgPrice) {
        if ("BUY".equals(command.getType())) {
            int expectedQty = quantity + command.getQuantity();
            logger.info("BUY accepted, waiting fill sync: symbol={} expectedQty={} orderPrice={}",
                    symbol, expectedQty, command.getPrice());
            // Do not optimistic-update position on BUY ACCEPTED.
            // Position will be updated by async fill confirm or next execute-cycle live sync.
            confirmBuyFilled(symbol, command.getExchange(), expectedQty);

        } else if ("SELL".equals(command.getType())) {
            int newQty = Math.max(0, quantity - command.getQuantity());
            positionService.updatePosition(symbol, newQty, newQty == 0 ? 0 : avgPrice);

            double pnl    = (command.getPrice() - avgPrice) * command.getQuantity();
            double pnlPct = avgPrice > 0 ? pnl / (avgPrice * command.getQuantity()) * 100 : 0;
            logger.info("Position updated SELL: symbol={} qty={} pnl={} ({}%)",
                    symbol, newQty,
                    String.format("%.0f", pnl),
                    String.format("%.2f", pnlPct));

            // [수정 C] ACCEPTED 확인 후에만 전략 포지션 상태 정리
            strategyEngine.notifySellAccepted(symbol);

            if (pnl < 0) {
                stopLossTime.put(symbol, Instant.now());
                riskManager.addLoss(Math.abs(pnl));
                logger.info("Stop-loss cooldown started for {} ({}s)", symbol, COOLDOWN_STOPLOSS_SEC);
            } else {
                lastSellTime.put(symbol, Instant.now());
                logger.info("Sell cooldown started for {} ({}s)", symbol, COOLDOWN_NORMAL_SEC);
            }
        }
    }

    // ── REJECTED 처리 ────────────────────────────────────────────────────────────

    private void handleRejected(String symbol,
                                com.autotrading.model.OrderCommand command,
                                String respMsg) {
        logger.warn("Order REJECTED for {} side={} → position NOT updated. msg={}",
                symbol, command.getType(), respMsg);

        if ("BUY".equals(command.getType())) {
            cancelBuyFillConfirmTask(symbol);
            strategyEngine.notifyBuyRejected(symbol);
        } else if ("SELL".equals(command.getType())) {
            // [수정 C] 포지션 상태를 초기화하지 않음. 다음 사이클에서 notifySellRejected()로 재시도 가능.
            strategyEngine.notifySellRejected(symbol);

            // Refresh live position on SELL reject to reduce stale-state retry loops.
            String orderExchange = resolveOrderExchangeForRuntime(symbol);
            RealPosition livePos = isOverseasSymbol(symbol)
                    ? fetchOverseasRealPosition(symbol, orderExchange)
                    : fetchRealPosition(symbol);

            if (livePos != null) {
                AutoPosition dbPos = positionService.getPosition(symbol);
                int dbQty = dbPos != null ? dbPos.getQuantity() : 0;
                double dbAvg = dbPos != null ? dbPos.getAvgPrice() : 0.0;

                if (livePos.quantity != dbQty || Math.abs(livePos.avgPrice - dbAvg) > 0.0001) {
                    logger.info("Post-reject position sync {} : DB qty={} -> Real qty={} avgPrice={}",
                            symbol, dbQty, livePos.quantity, livePos.avgPrice);
                    positionService.updatePosition(symbol, livePos.quantity, livePos.avgPrice);
                }

                if (livePos.quantity <= 0) {
                    logger.warn("SELL reject but no live holding for {} -> clearing stale hold state", symbol);
                    strategyEngine.clearStaleHoldState(symbol);
                }
            } else {
                logger.warn("SELL reject sync skipped for {} (live position unavailable)", symbol);
            }
        }
    }

    // ── ERROR 처리 ───────────────────────────────────────────────────────────────

    private void handleError(String symbol,
                             com.autotrading.model.OrderCommand command,
                             String respStatus, String respMsg) {
        // [수정 B] BUY ERROR: markBuyPending()이 이미 호출된 이후이므로 주문이 실패하더라도
        //          pending을 해제. notifyBuyRejected()가 동일한 역할로 처리.
        if ("BUY".equals(command.getType())) {
            cancelBuyFillConfirmTask(symbol);
            strategyEngine.notifyBuyRejected(symbol);
            logger.error("Order ERROR (BUY) for {} status={} → pending cleared. msg={}",
                    symbol, respStatus, respMsg);
        } else {
            // SELL ERROR: 포지션 상태 유지, 다음 사이클에서 재시도
            logger.error("Order ERROR (SELL) for {} status={} → position state kept. msg={}",
                    symbol, respStatus, respMsg);
        }
    }

    // ── KIS API 잔고 조회 ────────────────────────────────────────────────────────

    private RealPosition fetchRealPosition(String symbol) {
        try {
            Map<String, Object> balResp = kisApiClient.fetchDomesticBalance();
            if (!"OK".equals(balResp.get("status"))) {
                logger.warn("fetchDomesticBalance non-OK: {}", balResp.get("message"));
                return null;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output1 =
                    (List<Map<String, Object>>) balResp.get("output1");
            if (output1 == null) return new RealPosition(0, 0.0);

            for (Map<String, Object> item : output1) {
                String pdno = String.valueOf(item.getOrDefault("pdno", ""));
                if (!symbol.equalsIgnoreCase(pdno)) continue;
                int    qty = parseIntSafe(item.get("hldg_qty"));
                double avg = parseDoubleSafe(item.get("pchs_avg_pric"));
                logger.debug("Real position {} : qty={} avgPrice={}", symbol, qty, avg);
                return new RealPosition(qty, avg);
            }
            return new RealPosition(0, 0.0);

        } catch (Exception e) {
            logger.warn("fetchRealPosition failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    // ── 시세 조회 ────────────────────────────────────────────────────────────────

    private com.autotrading.model.StockQuote safeFetchQuote(String symbol, String quoteExchangeHint) {
        try {
            return marketDataService.fetchPrice(symbol, quoteExchangeHint);
        } catch (IllegalStateException e) {
            int fail = quoteFailCount.merge(symbol, 1, Integer::sum);
            String msg = String.valueOf(e.getMessage());

            // 동일 오류 반복 시 로그 양을 줄이기 위해 초기/주기/임계 시점에만 경고 출력
            if (fail == 1 || fail % 12 == 0 || fail == 24) {
                logger.warn("Quote fetch failed for {} (count={}) : {}", symbol, fail, msg);
            }

            // 해외 종목이 계속 시세를 받지 못하면 자동 중지 (약 2분 = 24회 * 5초)
            if (isOverseasSymbol(symbol) && fail >= 24) {
                logger.error("Auto-stop {} due to repeated overseas quote failures. Check symbol/exchange.", symbol);
                stopSymbol(symbol);
            }
            return null;
        }
    }

    private String normalizeStatus(Object rawStatus) {
        if (rawStatus == null) return STATUS_ERROR;
        String s = rawStatus.toString().trim().toUpperCase();
        if (s.equals("ACCEPTED") || s.equals("0") || s.equals("00") || s.equals("SUCCESS"))
            return STATUS_ACCEPTED;
        if (s.equals("REJECTED") || s.equals("1") || s.equals("REJECT"))
            return STATUS_REJECTED;
        return STATUS_ERROR;
    }

    private int parseIntSafe(Object val) {
        if (val == null) return 0;
        try { return (int) Double.parseDouble(val.toString().replace(",", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    private double parseDoubleSafe(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString().replace(",", "")); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private String resolveSymbolName(String symbol, StockQuote quote) {
        String fromQuote = quote != null ? quote.getName() : null;
        if (StringUtils.hasText(fromQuote)) {
            String normalized = fromQuote.trim();
            symbolNameCache.put(symbol, normalized);
            return normalized;
        }

        String cached = symbolNameCache.get(symbol);
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        AutoPosition position = positionService.getPosition(symbol);
        if (position != null && StringUtils.hasText(position.getSymbolName())) {
            String normalized = position.getSymbolName().trim();
            symbolNameCache.put(symbol, normalized);
            return normalized;
        }

        return symbol;
    }

    private static class RealPosition {
        final int    quantity;
        final double avgPrice;
        RealPosition(int q, double a) { quantity=q; avgPrice=a; }
    }

    private boolean isOverseasSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return false;
        }
        String normalized = symbol.trim().toUpperCase();

        // If we already know the symbol exchange, trust it first.
        String cachedExchange = symbolExchange.get(normalized);
        if (StringUtils.hasText(cachedExchange)) {
            String ex = cachedExchange.trim().toUpperCase();
            if ("KRX".equals(ex) || "KR".equals(ex) || "KOSPI".equals(ex) || "KOSDAQ".equals(ex)) {
                return false;
            }
            if ("NASD".equals(ex) || "NASDAQ".equals(ex) || "NAS".equals(ex)
                    || "NYSE".equals(ex) || "NYS".equals(ex)
                    || "AMEX".equals(ex) || "AMS".equals(ex)) {
                return true;
            }
        }

        // Fallback heuristic only.
        return !normalized.matches("^\\d{5,6}$");
    }

    private void cancelBuyFillConfirmTask(String symbol) {
        ScheduledFuture<?> task = buyFillConfirmTasks.remove(symbol);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void confirmBuyFilled(String symbol, String exchange, int expectedQty) {
        cancelBuyFillConfirmTask(symbol);

        final AtomicInteger attempts = new AtomicInteger(0);
        final String exchangeForTask = exchange;

        Runnable confirmTask = () -> {
            int attempt = attempts.incrementAndGet();
            RealPosition real = isOverseasSymbol(symbol)
                    ? fetchOverseasRealPosition(symbol, exchangeForTask)
                    : fetchRealPosition(symbol);

            if (real != null && real.quantity >= expectedQty) {
                AutoPosition dbPos = positionService.getPosition(symbol);
                int dbQty = dbPos != null ? dbPos.getQuantity() : 0;
                double dbAvg = dbPos != null ? dbPos.getAvgPrice() : 0.0;

                if (dbQty != real.quantity || Math.abs(dbAvg - real.avgPrice) > 0.0001) {
                    positionService.updatePosition(symbol, real.quantity, real.avgPrice);
                    logger.info("BUY fill sync applied: symbol={} qty={} avgPrice={}",
                            symbol, real.quantity, real.avgPrice);
                }

                strategyEngine.notifyBuyFilled(symbol);
                cancelBuyFillConfirmTask(symbol);
                logger.info("BUY fill confirmed async for {} (qty={} expected={})",
                        symbol, real.quantity, expectedQty);
                return;
            }

            if (attempt >= BUY_FILL_CONFIRM_MAX_ATTEMPTS) {
                // Keep pending state; next execute-cycle live sync can still confirm.
                cancelBuyFillConfirmTask(symbol);
                logger.warn("BUY fill not confirmed async for {} (expectedQty={}) -> waiting for next live sync",
                        symbol, expectedQty);
            }
        };

        ScheduledFuture<?> future = buyFillConfirmExecutor.scheduleAtFixedRate(
                confirmTask,
                BUY_FILL_CONFIRM_INTERVAL_MS,
                BUY_FILL_CONFIRM_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        buyFillConfirmTasks.put(symbol, future);
    }

    private RealPosition fetchOverseasRealPosition(String symbol, String exchange) {
        try {
            String ex = normalizeOverseasOrderExchange(exchange);
            Map<String, Object> balResp = kisApiClient.fetchOverseasBalance(ex, "USD");
            if (!"OK".equals(balResp.get("status"))) {
                logger.warn("fetchOverseasBalance non-OK for {}({}): {}", symbol, ex, balResp.get("message"));
                return null;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> output1 =
                    (List<Map<String, Object>>) balResp.get("output1");
            if (output1 == null) return new RealPosition(0, 0.0);

            for (Map<String, Object> item : output1) {
                String pdno = pickString(item, "ovrs_pdno", "pdno", "symbol", "item_cd");
                if (!symbol.equalsIgnoreCase(pdno)) continue;
                int qty = pickInt(item, "ovrs_cblc_qty", "hldg_qty", "hold_qty", "cblc_qty");
                double avg = pickDouble(item, "pchs_avg_pric", "avg_unpr", "pchs_unpr", "avg_pric");
                logger.debug("Overseas real position {}({}) : qty={} avgPrice={}", symbol, ex, qty, avg);
                return new RealPosition(qty, avg);
            }
            return new RealPosition(0, 0.0);
        } catch (Exception e) {
            logger.warn("fetchOverseasRealPosition failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private String resolveOrderExchangeForStart(String symbol, String requestedExchange) {
        if (!isOverseasSymbol(symbol)) {
            return "KRX";
        }
        if (StringUtils.hasText(requestedExchange)) {
            return normalizeOverseasOrderExchange(requestedExchange);
        }

        WatchlistItem watch = watchlistDao.findBySymbol(symbol);
        if (watch != null && StringUtils.hasText(watch.getExchange())) {
            return normalizeOverseasOrderExchange(watch.getExchange());
        }
        return resolveOrderExchangeForRuntime(symbol);
    }

    private String resolveOrderExchangeForRuntime(String symbol) {
        if (!isOverseasSymbol(symbol)) {
            return "KRX";
        }
        String cached = symbolExchange.get(symbol);
        if (StringUtils.hasText(cached)) {
            return normalizeOverseasOrderExchange(cached);
        }
        String resolved = marketDataService.resolveExchangeForOrder(symbol);
        String normalized = normalizeOverseasOrderExchange(resolved);
        symbolExchange.put(symbol, normalized);
        return normalized;
    }

    private String syncOrderExchangeWithQuote(String symbol, String currentOrderExchange) {
        if (!isOverseasSymbol(symbol)) {
            return currentOrderExchange;
        }
        String resolved = normalizeOverseasOrderExchange(marketDataService.resolveExchangeForOrder(symbol));
        if (!resolved.equals(currentOrderExchange)) {
            logger.info("Exchange auto-corrected for {}: {} -> {}", symbol, currentOrderExchange, resolved);
            symbolExchange.put(symbol, resolved);
            return resolved;
        }
        return currentOrderExchange;
    }

    private String normalizeOverseasOrderExchange(String exchange) {
        if (!StringUtils.hasText(exchange)) return "NASD";
        String upper = exchange.trim().toUpperCase();
        switch (upper) {
            case "NAS":
            case "NASDAQ":
            case "NASD":
                return "NASD";
            case "NYS":
            case "NYSE":
                return "NYSE";
            case "AMS":
            case "AMEX":
                return "AMEX";
            default:
                return upper;
        }
    }

    private String toQuoteExchangeHint(String orderExchange) {
        if (!StringUtils.hasText(orderExchange)) return null;
        switch (normalizeOverseasOrderExchange(orderExchange)) {
            case "NASD":
                return "NAS";
            case "NYSE":
                return "NYS";
            case "AMEX":
                return "AMS";
            default:
                return orderExchange;
        }
    }

    private String pickString(Map<String, Object> row, String... keys) {
        if (row == null) return "";
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) return s;
            }
        }
        return "";
    }

    private int pickInt(Map<String, Object> row, String... keys) {
        return parseIntSafe(pickString(row, keys));
    }

    private double pickDouble(Map<String, Object> row, String... keys) {
        return parseDoubleSafe(pickString(row, keys));
    }

    // ── 스케줄러 시작/중지 ───────────────────────────────────────────────────────

    /**
     * [수정 D] 개별 종목 중지 기능 추가.
     *
     * 기존 stop()은 전체 종목을 중지하므로 toggle(enable=false)에서 호출하면
     * 다른 종목도 함께 중지되는 문제가 있었음.
     * 이 메서드는 특정 종목만 중지하고 해당 상태만 초기화한다.
     *
     * ApiController.toggleSymbol(enable=false)에서 stop() 대신 이 메서드를 호출하도록
     * AutoTradingService 등에서 연결 필요.
     * stop()은 전체 중지 전용으로 유지.
     */
    public synchronized String stopSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) return "Invalid symbol";
        String sym = symbol.trim().toUpperCase();

        Timer t = timers.remove(sym);
        if (t != null) {
            t.cancel();
        }

        cancelBuyFillConfirmTask(sym);
        strategyEngine.resetSymbol(sym);

        barAccumMap.remove(sym);
        prevVolume.remove(sym);
        lastSellTime.remove(sym);
        stopLossTime.remove(sym);
        symbolExchange.remove(sym);
        quoteFailCount.remove(sym);
        symbolNameCache.remove(sym);

        if (t == null) return "Not Running: " + sym;

        logger.info("Auto-trading scheduler stopped for {} → symbol state cleared", sym);
        return "Stopped " + sym;
    }

    public synchronized String stop() {
        if (timers.isEmpty()) {
            buyFillConfirmTasks.values().forEach(f -> f.cancel(false));
            buyFillConfirmTasks.clear();
            return "Not Running";
        }
        List<String> running = new ArrayList<>(timers.keySet());
        timers.values().forEach(Timer::cancel);
        timers.clear();
        for (String sym : running) {
            cancelBuyFillConfirmTask(sym);
            strategyEngine.resetSymbol(sym);
        }
        buyFillConfirmTasks.clear();
        barAccumMap.clear();
        prevVolume.clear();
        lastSellTime.clear();
        stopLossTime.clear();
        symbolExchange.clear();
        quoteFailCount.clear();
        symbolNameCache.clear();
        logger.info("Auto-trading scheduler stopped (all symbols) → all state cleared");
        return "Stopped";
    }

    public String status() {
        return timers.isEmpty()
                ? "STOPPED"
                : "RUNNING (" + String.join(",", timers.keySet()) + ")";
    }

    public List<Map<String, String>> runningSymbols() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (String symbol : timers.keySet()) {
            Map<String, String> row = new HashMap<>();
            row.put("symbol", symbol);
            row.put("exchange", resolveOrderExchangeForRuntime(symbol));
            rows.add(row);
        }
        rows.sort((a, b) -> a.get("symbol").compareTo(b.get("symbol")));
        return rows;
    }
}
