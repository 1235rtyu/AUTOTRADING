package com.autotrading.service.impl;

import com.autotrading.config.KisProperties;
import com.autotrading.mapper.MinuteBarMapper;
import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.market.MarketDataService;
import com.autotrading.model.MinuteBar;
import com.autotrading.model.OrderCommand;
import com.autotrading.service.BacktestService;
import com.autotrading.strategy.StrategyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.autotrading.model.BacktestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class BacktestServiceImpl implements BacktestService {

    private static final Logger logger = LoggerFactory.getLogger(BacktestServiceImpl.class);
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneId NY  = ZoneId.of("America/New_York");

    private final KoreaInvestmentApiClient apiClient;
    private final MinuteBarMapper minuteBarMapper;
    private final MarketDataService marketDataService;
    private final KisProperties kisProperties;

    // async job tracking
    private final Map<String, Map<String, Object>> jobs = new ConcurrentHashMap<>();
    // rate limiting
    private long lastApiCallMs = 0L;
    private final Object rateLock = new Object();

    public BacktestServiceImpl(KoreaInvestmentApiClient apiClient,
                                MinuteBarMapper minuteBarMapper,
                                MarketDataService marketDataService,
                                KisProperties kisProperties) {
        this.apiClient = apiClient;
        this.minuteBarMapper = minuteBarMapper;
        this.marketDataService = marketDataService;
        this.kisProperties = kisProperties;
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> startCollect(String market, String symbol, String startDate, String endDate) {
        if (symbol == null || symbol.isBlank()) return error("종목코드를 입력하세요.");
        LocalDate start, end;
        try {
            start = LocalDate.parse(startDate);
            end   = LocalDate.parse(endDate);
        } catch (Exception e) {
            return error("날짜 형식 오류 (YYYY-MM-DD)");
        }
        if (start.isAfter(end)) return error("시작일이 종료일보다 늦습니다.");

        String jobId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("state",   "RUNNING");
        status.put("jobId",   jobId);
        status.put("progress", 0);
        status.put("message", "데이터 수집 시작...");
        status.put("inserted", 0);
        status.put("totalDays", 0);
        jobs.put(jobId, status);

        final String sym = symbol.trim().toUpperCase();
        final String mkt = market.toUpperCase();
        CompletableFuture.runAsync(() -> {
            try {
                if ("KRX".equals(mkt)) {
                    collectKrx(sym, start, end, status);
                } else {
                    collectUs(sym, start, end, status);
                }
                collectProxyBars(mkt, sym, start, end, status);
                status.put("state", "DONE");
                if (!status.containsKey("messageOverride")) {
                    status.put("message", "수집 완료: " + status.get("inserted") + "개 봉");
                }
            } catch (Exception e) {
                logger.error("Collect job {} failed", jobId, e);
                status.put("state", "ERROR");
                status.put("message", e.getMessage());
            }
        });

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "STARTED");
        res.put("jobId", jobId);
        return res;
    }

    @Override
    public Map<String, Object> getCollectStatus(String jobId) {
        Map<String, Object> status = jobs.get(jobId);
        if (status == null) return error("잡을 찾을 수 없습니다: " + jobId);
        return new LinkedHashMap<>(status);
    }

    @Override
    public Map<String, Object> runBacktest(String market, String symbol,
                                            String startDate, String endDate,
                                            double buyAmount, BacktestConfig config) {
        if (symbol == null || symbol.isBlank()) return error("종목코드를 입력하세요.");
        LocalDate start, end;
        try {
            start = LocalDate.parse(startDate);
            end   = LocalDate.parse(endDate);
        } catch (Exception e) {
            return error("날짜 형식 오류 (YYYY-MM-DD)");
        }

        ZoneId zone = "KRX".equalsIgnoreCase(market) ? KST : NY;
        LocalDateTime startDt = start.atTime(0, 0);
        LocalDateTime endDt   = end.atTime(23, 59, 59);

        List<MinuteBar> bars = minuteBarMapper.findByRange(
                market.toUpperCase(), symbol.trim().toUpperCase(), startDt, endDt);
        if (bars.isEmpty()) {
            return error("저장된 분봉 데이터가 없습니다. 먼저 [분봉 데이터 가져오기]를 실행하세요.");
        }

        String normalizedMarket = market.toUpperCase();
        String proxySymbol = proxySymbol(normalizedMarket);
        List<MinuteBar> proxyBars = minuteBarMapper.findByRange(
                normalizedMarket, proxySymbol, startDt, endDt);

        return simulate(normalizedMarket, symbol.trim().toUpperCase(),
                startDate, endDate, bars, proxySymbol, proxyBars, zone,
                buyAmount > 0 ? buyAmount : 600_000.0, config);
    }

    // ─────────────────────────────────────────────────────────
    // KRX 수집
    // ─────────────────────────────────────────────────────────

    private void collectKrx(String symbol, LocalDate start, LocalDate end,
                              Map<String, Object> status) {
        // Work out total business days for progress
        int totalDays = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) totalDays++;
        }
        status.put("totalDays", totalDays);

        int done = 0;
        int inserted = 0;
        List<String> failures = new ArrayList<>();
        List<MinuteBar> prevDayBars = Collections.emptyList(); // 직전 거래일 데이터 (휴장일 탐지용)
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            status.put("message", d + " 분봉 수집 중...");
            try {
                List<MinuteBar> dayBars = fetchKrxDay(symbol, d);
                if (!dayBars.isEmpty()) {
                    if (looksLikeHolidayData(dayBars, prevDayBars)) {
                        // KIS API가 휴장일 조회 시 직전 거래일 데이터를 반환하는 경우 차단
                        logger.info("KRX holiday skip: {} {} bars match previous trading day (market closed?)",
                                symbol, d);
                    } else {
                        doBatchInsert(dayBars);
                        inserted += dayBars.size();
                        prevDayBars = dayBars;
                    }
                }
            } catch (Exception e) {
                logger.warn("KRX bar fetch failed {} {}: {}", symbol, d, e.getMessage());
                failures.add(d + ": " + e.getMessage());
            }
            done++;
            status.put("progress", totalDays > 0 ? (int)(done * 100.0 / totalDays) : 100);
            status.put("inserted", inserted);
        }
        if (!failures.isEmpty()) {
            throw new RuntimeException("KRX 분봉 수집 실패 (" + failures.size() + "일): " + failures.get(0));
        }
        if (inserted == 0) {
            throw new RuntimeException("KRX 분봉 데이터가 없습니다. 종목코드와 거래일을 확인하세요.");
        }
    }

    /**
     * KIS API는 휴장일 조회 시 직전 거래일 데이터를 그대로 반환하는 경우가 있다.
     * 새로 수집한 bars가 직전 거래일 bars와 시간·종가·거래량이 동일하면 휴장일 데이터로 판단.
     */
    private boolean looksLikeHolidayData(List<MinuteBar> newBars, List<MinuteBar> prevBars) {
        if (prevBars.isEmpty() || newBars.size() != prevBars.size()) return false;

        // 직전 거래일 bars를 시간(LocalTime) 기준으로 빠르게 조회
        Map<LocalTime, MinuteBar> prevByTime = prevBars.stream()
                .collect(Collectors.toMap(b -> b.getBarTime().toLocalTime(), b -> b, (a, b) -> a));

        int checks = 0;
        int matches = 0;
        for (MinuteBar bar : newBars) {
            MinuteBar prev = prevByTime.get(bar.getBarTime().toLocalTime());
            if (prev == null) continue;
            checks++;
            if (Math.abs(bar.getClosePrice() - prev.getClosePrice()) < 0.01
                    && Math.abs(bar.getVolume() - prev.getVolume()) < 0.01) {
                matches++;
            }
            if (checks >= 10) break; // 10봉 샘플로 충분
        }
        // 5봉 이상 확인되고 전부 일치하면 휴장일 데이터로 판단
        return checks >= 5 && matches == checks;
    }

    /** Fetch all 1-minute bars for a single KRX trading day via FHKST03010230. */
    @SuppressWarnings("unchecked")
    private List<MinuteBar> fetchKrxDay(String symbol, LocalDate date) {
        String dateStr = date.format(YYYYMMDD);
        List<MinuteBar> dayBars = new ArrayList<>();
        Set<LocalDateTime> seen = new HashSet<>();

        String fromTime = "153000";
        for (int page = 0; page < 15; page++) {
            throttle();
            Map<String, Object> result = apiClient.fetchDailyMinuteChart(symbol, dateStr, fromTime);

            Object rtCd = result.get("rt_cd");
            if (!"0".equals(String.valueOf(rtCd))) {
                throw new RuntimeException("KIS 오류: " + safeStr(result, "msg1"));
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("output2");
            if (rows == null || rows.isEmpty()) break;

            boolean anyNew = false;
            String earliest = null;
            for (Map<String, Object> row : rows) {
                String timeStr = safeStr(row, "stck_cntg_hour");
                if (timeStr == null || timeStr.length() < 4) continue;
                String normalizedTime = timeStr.replaceAll("[^0-9]", "");
                if (normalizedTime.length() < 6) continue;
                if (earliest == null || normalizedTime.compareTo(earliest) < 0) {
                    earliest = normalizedTime;
                }

                int hour, min;
                try {
                    hour = Integer.parseInt(normalizedTime.substring(0, 2));
                    min  = Integer.parseInt(normalizedTime.substring(2, 4));
                } catch (NumberFormatException e) { continue; }

                // KRX session 09:00~15:30
                if (hour < 9 || (hour == 15 && min > 30) || hour > 15) continue;

                LocalDateTime barTime = LocalDateTime.of(date, LocalTime.of(hour, min));
                if (seen.contains(barTime)) continue;
                seen.add(barTime);
                anyNew = true;

                double close = safeDouble(row, "stck_prpr");
                if (close <= 0) continue;
                double open  = orElse(safeDouble(row, "stck_oprc"), close);
                double high  = orElse(safeDouble(row, "stck_hgpr"), close);
                double low   = orElse(safeDouble(row, "stck_lwpr"), close);
                double vol   = safeDouble(row, "cntg_vol");
                double to    = safeDouble(row, "acml_tr_pbmn");
                if (to <= 0) to = close * vol;

                MinuteBar bar = new MinuteBar();
                bar.setMarket("KRX");
                bar.setSymbol(symbol);
                bar.setBarTime(barTime);
                bar.setOpenPrice(open);
                bar.setHighPrice(high);
                bar.setLowPrice(low);
                bar.setClosePrice(close);
                bar.setVolume(vol);
                bar.setTurnover(to);
                dayBars.add(bar);
            }
            if (!anyNew) break; // no new bars – reached beginning of session
            if (earliest == null || earliest.compareTo("090000") <= 0) break;
            fromTime = decrementMinute(earliest);
        }

        dayBars.sort(Comparator.comparing(MinuteBar::getBarTime));
        return dayBars;
    }

    // ─────────────────────────────────────────────────────────
    // US 수집 — Yahoo Finance 1분봉
    // ─────────────────────────────────────────────────────────

    private static final String YF_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    /**
     * Yahoo Finance 인증에 필요한 [cookie, crumb] 쌍을 반환합니다.
     * finance.yahoo.com 홈에서 쿠키를 얻은 뒤 query1/query2로 crumb 획득.
     * 실패 시 ["", ""] 반환.
     */
    private String[] fetchYahooCrumb() {
        // Step 1: finance.yahoo.com 홈에서 쿠키 획득
        String cookie = "";
        try {
            HttpURLConnection home = (HttpURLConnection)
                    new URL("https://finance.yahoo.com").openConnection();
            home.setRequestProperty("User-Agent", YF_UA);
            home.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            home.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            home.setConnectTimeout(12_000);
            home.setReadTimeout(15_000);
            home.setInstanceFollowRedirects(true);

            int homeCode = home.getResponseCode();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<String>> h : home.getHeaderFields().entrySet()) {
                if ("Set-Cookie".equalsIgnoreCase(h.getKey())) {
                    for (String v : h.getValue()) {
                        String kv = v.split(";")[0].trim();
                        if (kv.isEmpty()) continue;
                        if (sb.length() > 0) sb.append("; ");
                        sb.append(kv);
                    }
                }
            }
            try { home.getInputStream().skip(Long.MAX_VALUE); } catch (Exception ignored) {}
            home.disconnect();
            cookie = sb.toString();
            logger.debug("Yahoo Finance 홈 쿠키 획득 (code={}, cookieLen={})", homeCode, cookie.length());
        } catch (Exception e) {
            logger.warn("Yahoo Finance 홈 쿠키 획득 실패: {}", e.getMessage());
        }

        // Step 2: query1 / query2 순서로 crumb 획득 시도
        for (String host : new String[]{"query1", "query2"}) {
            try {
                HttpURLConnection cc = (HttpURLConnection)
                        new URL("https://" + host + ".finance.yahoo.com/v1/test/getcrumb").openConnection();
                cc.setRequestProperty("User-Agent", YF_UA);
                cc.setRequestProperty("Accept", "application/json, text/plain, */*");
                cc.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                cc.setRequestProperty("Referer", "https://finance.yahoo.com/");
                if (!cookie.isEmpty()) cc.setRequestProperty("Cookie", cookie);
                cc.setConnectTimeout(8_000);
                cc.setReadTimeout(8_000);

                int crumbCode = cc.getResponseCode();
                String crumb = "";
                if (crumbCode == 200) {
                    crumb = new String(cc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                } else {
                    try { if (cc.getErrorStream() != null) cc.getErrorStream().skip(Long.MAX_VALUE); }
                    catch (Exception ignored) {}
                }
                cc.disconnect();

                String preview = crumb.length() > 40 ? crumb.substring(0, 40) + "..." : crumb;
                logger.debug("Yahoo Finance crumb ({}) code={}, value='{}'", host, crumbCode, preview);

                if (!crumb.isEmpty() && !crumb.startsWith("{") && !crumb.startsWith("<")) {
                    logger.info("Yahoo Finance crumb 획득 성공 (host={}, len={})", host, crumb.length());
                    return new String[]{cookie, crumb};
                }
            } catch (Exception e) {
                logger.warn("Yahoo Finance crumb ({}) 실패: {}", host, e.getMessage());
            }
        }
        logger.error("Yahoo Finance crumb 획득 최종 실패 — cookieLen={}", cookie.length());
        return new String[]{"", ""};
    }

    private void collectUs(String symbol, LocalDate start, LocalDate end,
                            Map<String, Object> status) {
        // Yahoo Finance 1분봉 최대 ~30일
        LocalDate maxStart      = LocalDate.now(NY).minusDays(29);
        final LocalDate effStart = start.isBefore(maxStart) ? maxStart : start;
        final LocalDate effEnd   = end;
        if (!effStart.equals(start)) {
            status.put("messageOverride", true);
            status.put("message", "Yahoo Finance 제한으로 " + effStart + " 이후 1분봉만 수집했습니다.");
        }

        status.put("message", "Yahoo Finance 1분봉 수집 중 (" + effStart + " ~ " + effEnd + ")...");

        long period1 = effStart.atStartOfDay(NY).toInstant().getEpochSecond();
        long period2 = effEnd.atTime(23, 59, 59).atZone(NY).toInstant().getEpochSecond();

        // 2024년부터 Yahoo Finance v8 API는 cookie + crumb 인증 필요
        String[] cookieCrumb = fetchYahooCrumb();
        String cookie = cookieCrumb[0];
        String crumb  = cookieCrumb[1];
        if (crumb.isEmpty()) {
            throw new RuntimeException(
                "Yahoo Finance 인증 실패 — crumb 획득 불가 (로그에서 원인 확인)");
        }

        String encodedCrumb;
        try { encodedCrumb = URLEncoder.encode(crumb, StandardCharsets.UTF_8); }
        catch (Exception e) { encodedCrumb = crumb; }

        String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol
                + "?interval=1m&period1=" + period1 + "&period2=" + period2
                + "&includePrePost=false&crumb=" + encodedCrumb;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", YF_UA);
            conn.setRequestProperty("Accept", "application/json");
            if (!cookie.isEmpty()) conn.setRequestProperty("Cookie", cookie);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            int httpCode = conn.getResponseCode();
            if (httpCode != 200) {
                String errBody = "";
                try {
                    java.io.InputStream es = conn.getErrorStream();
                    if (es != null) errBody = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
                logger.error("Yahoo Finance {} for {} | URL={} | body={}", httpCode, symbol, urlStr, errBody.length() > 300 ? errBody.substring(0, 300) : errBody);
                throw new RuntimeException("Yahoo Finance HTTP " + httpCode + " for " + symbol);
            }

            Map<?, ?> root    = new ObjectMapper().readValue(conn.getInputStream(), Map.class);
            Map<?, ?> chart   = (Map<?, ?>) root.get("chart");
            List<?>   resList = (List<?>) chart.get("result");
            if (resList == null || resList.isEmpty()) {
                status.put("inserted", 0); status.put("progress", 100);
                status.put("message", "Yahoo Finance 데이터 없음 (종목코드 확인)");
                return;
            }

            Map<?, ?> res        = (Map<?, ?>) resList.get(0);
            List<?>   tsList     = (List<?>) res.get("timestamp");
            Map<?, ?> indicators = (Map<?, ?>) res.get("indicators");
            Map<?, ?> quote      = (Map<?, ?>) ((List<?>) indicators.get("quote")).get(0);
            List<?>   oList = (List<?>) quote.get("open"),  hList = (List<?>) quote.get("high"),
                      lList = (List<?>) quote.get("low"),   cList = (List<?>) quote.get("close"),
                      vList = (List<?>) quote.get("volume");

            List<MinuteBar> bars = new ArrayList<>();
            for (int i = 0; i < tsList.size(); i++) {
                if (cList.get(i) == null) continue;
                long epochSec = ((Number) tsList.get(i)).longValue();
                ZonedDateTime zdt = Instant.ofEpochSecond(epochSec).atZone(NY);
                int h = zdt.getHour(), m = zdt.getMinute();
                // Regular session only: 09:30–15:59
                if (h < 9 || (h == 9 && m < 30) || h >= 16) continue;
                LocalDate barDate = zdt.toLocalDate();
                if (barDate.isBefore(effStart) || barDate.isAfter(effEnd)) continue;

                double close = ((Number) cList.get(i)).doubleValue();
                double open  = oList.get(i) != null ? ((Number) oList.get(i)).doubleValue() : close;
                double high  = hList.get(i) != null ? ((Number) hList.get(i)).doubleValue() : close;
                double low   = lList.get(i) != null ? ((Number) lList.get(i)).doubleValue() : close;
                double vol   = vList.get(i) != null ? ((Number) vList.get(i)).doubleValue() : 0.0;

                MinuteBar bar = new MinuteBar();
                bar.setMarket("US"); bar.setSymbol(symbol);
                bar.setBarTime(zdt.toLocalDateTime());
                bar.setOpenPrice(open); bar.setHighPrice(high); bar.setLowPrice(low);
                bar.setClosePrice(close); bar.setVolume(vol); bar.setTurnover(close * vol);
                bars.add(bar);
            }

            bars.sort(Comparator.comparing(MinuteBar::getBarTime));
            if (!bars.isEmpty()) doBatchInsert(bars);
            status.put("inserted", bars.size());
            status.put("progress", 100);
            status.put("message", "US 1분봉 " + bars.size() + "개 수집 완료 (Yahoo Finance)");

        } catch (Exception e) {
            logger.error("Yahoo Finance fetch failed for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Yahoo Finance 수집 실패: " + e.getMessage(), e);
        }
    }

    private void collectProxyBars(String market, String symbol, LocalDate start, LocalDate end,
                                  Map<String, Object> status) {
        String proxy = proxySymbol(market);
        if (proxy.equalsIgnoreCase(symbol)) return;

        Map<String, Object> proxyStatus = new ConcurrentHashMap<>();
        try {
            if ("KRX".equals(market)) {
                collectKrx(proxy, start, end, proxyStatus);
            } else {
                collectUs(proxy, start, end, proxyStatus);
            }
            status.put("proxySymbol", proxy);
            status.put("proxyInserted", proxyStatus.getOrDefault("inserted", 0));
        } catch (Exception e) {
            logger.warn("Proxy bar fetch failed {} {}: {}", market, proxy, e.getMessage());
            status.put("proxySymbol", proxy);
            status.put("proxyWarning", "시장 프록시 " + proxy + " 수집 실패: " + e.getMessage());
        }
    }

    private String proxySymbol(String market) {
        return "KRX".equalsIgnoreCase(market) ? "229200" : "QQQ";
    }

    // ─────────────────────────────────────────────────────────
    // BACKTEST SIMULATION
    // ─────────────────────────────────────────────────────────

    private Map<String, Object> simulate(String market, String symbol,
                                          String startDate, String endDate,
                                          List<MinuteBar> bars, String proxySymbol,
                                          List<MinuteBar> proxyBars, ZoneId zone,
                                          double buyAmount, BacktestConfig cfg) {
        StrategyEngine engine = new StrategyEngine(marketDataService);
        StrategyEngine.Market mkt = "KRX".equals(market)
                ? StrategyEngine.Market.KRX : StrategyEngine.Market.US;
        engine.setMarket(symbol, mkt);
        engine.setBuyAmount(symbol, buyAmount);
        engine.setBacktestConfig(cfg);
        engine.setMarket(proxySymbol, mkt);

        Map<LocalDateTime, MinuteBar> proxyByTime = new HashMap<>();
        for (MinuteBar proxyBar : proxyBars) {
            proxyByTime.put(proxyBar.getBarTime(), proxyBar);
        }

        int currentQty    = 0;
        double avgPrice   = 0.0;
        LocalDate lastDay = null;
        Map<String, Integer> rejectCounts = new LinkedHashMap<>();

        // Entry info captured at BUY time
        LocalDateTime entryBarTime    = null;
        double        entryClose      = 0.0;
        String        entryMode       = "UNKNOWN";
        int           entryScore      = 0;
        String        entryGrade      = "D";
        double        entryVwap       = 0.0;
        double        entryVel        = 0.0;
        double        entryVolRatio   = 0.0;
        double        entryToRatio    = 0.0;
        int           entryScVwap     = 0;
        int           entryScTrend    = 0;
        int           entryScVol      = 0;
        int           entryScTo       = 0;
        int           entryScHigh     = 0;
        int           entryScPat      = 0;
        double        entryVelMid     = 0.0;
        double        entryVelLong    = 0.0;
        int           entryTrendSc    = 0;
        double        entryFromHigh   = 0.0;
        boolean       snapCaptured    = false;

        List<Map<String, Object>> trades = new ArrayList<>();

        for (MinuteBar bar : bars) {
            LocalDate barDay = bar.getBarTime().toLocalDate();

            // Reset daily counters on new trading day (always, regardless of position)
            if (lastDay != null && !barDay.equals(lastDay)) {
                engine.advanceBacktestDay(symbol);
                engine.advanceBacktestDay(proxySymbol);
            }
            lastDay = barDay;

            long barMs = bar.getBarTime().atZone(zone).toInstant().toEpochMilli();
            engine.setBacktestNowMs(barMs);
            MinuteBar proxyBar = proxyByTime.get(bar.getBarTime());
            if (proxyBar != null) {
                engine.record(proxySymbol,
                        proxyBar.getOpenPrice(), proxyBar.getHighPrice(), proxyBar.getLowPrice(),
                        proxyBar.getClosePrice(), proxyBar.getVolume(), barMs);
                engine.updateMarketContextFromSymbol(proxySymbol, mkt);
            }
            engine.record(symbol,
                    bar.getOpenPrice(), bar.getHighPrice(), bar.getLowPrice(),
                    bar.getClosePrice(), bar.getVolume(), barMs);

            double price = bar.getClosePrice();

            if (currentQty == 0) {
                Optional<OrderCommand> cmd = engine.decide(symbol, price, bar.getVolume(), 0, 0.0);
                if (cmd.isPresent() && "BUY".equals(cmd.get().getType())) {
                    currentQty  = cmd.get().getQuantity();
                    avgPrice    = price;
                    entryBarTime = bar.getBarTime();
                    entryClose  = price;
                    snapCaptured = false;
                    engine.notifyBuyFilled(symbol);
                } else {
                    String rej = engine.getLastRejectReason(symbol);
                    if (rej != null && !"NONE".equals(rej)) {
                        rejectCounts.merge(rej, 1, (a, b) -> a + b);
                    }
                }
            } else {
                Optional<OrderCommand> cmd = engine.decide(symbol, price, bar.getVolume(), currentQty, avgPrice);

                // Capture entry snapshot once refreshHoldingState sets entryPriceSnapshot
                if (!snapCaptured) {
                    StrategyEngine.EntrySnapshot snap = engine.getEntrySnapshot(symbol);
                    if (snap != null) {
                        entryMode    = snap.entryMode;
                        entryScore   = snap.signalScore;
                        entryGrade   = snap.signalGrade;
                        entryVwap    = snap.vwapDistPct;
                        entryVel     = snap.velocityShort;
                        entryVolRatio = snap.volumeRatio;
                        entryToRatio  = snap.turnoverRatio;
                        entryScVwap   = snap.scoreVwap;
                        entryScTrend  = snap.scoreTrend;
                        entryScVol    = snap.scoreVolume;
                        entryScTo     = snap.scoreTurnover;
                        entryScHigh   = snap.scoreHigh;
                        entryScPat    = snap.scorePattern;
                        entryVelMid   = snap.velocityMid;
                        entryVelLong  = snap.velocityLong;
                        entryTrendSc  = snap.trendScore;
                        entryFromHigh = snap.fromHighPct;
                        snapCaptured  = true;
                    }
                }

                if (cmd.isPresent() && "SELL".equals(cmd.get().getType())) {
                    if (!snapCaptured) {
                        StrategyEngine.EntrySnapshot snap = engine.getEntrySnapshot(symbol);
                        if (snap != null) {
                            entryMode    = snap.entryMode;
                            entryScore   = snap.signalScore;
                            entryGrade   = snap.signalGrade;
                            entryVwap    = snap.vwapDistPct;
                            entryVel     = snap.velocityShort;
                            entryVolRatio = snap.volumeRatio;
                            entryToRatio  = snap.turnoverRatio;
                            entryScVwap   = snap.scoreVwap;
                            entryScTrend  = snap.scoreTrend;
                            entryScVol    = snap.scoreVolume;
                            entryScTo     = snap.scoreTurnover;
                            entryScHigh   = snap.scoreHigh;
                            entryScPat    = snap.scorePattern;
                            entryVelMid   = snap.velocityMid;
                            entryVelLong  = snap.velocityLong;
                            entryTrendSc  = snap.trendScore;
                            entryFromHigh = snap.fromHighPct;
                        }
                    }

                    double rawPnl = avgPrice > 0 ? (price - avgPrice) / avgPrice : 0.0;
                    double pnlPct = rawPnl - (cfg != null ? (cfg.feePct + cfg.slippagePct + (rawPnl > 0 ? cfg.taxPct : 0.0)) / 100.0 : 0.0);
                    long holdSec  = entryBarTime != null
                            ? Duration.between(entryBarTime, bar.getBarTime()).getSeconds() : 0L;
                    double actualEntryAmt = entryClose * currentQty;

                    trades.add(buildTrade(symbol, entryBarTime, bar.getBarTime(), entryMode,
                            entryClose, price, pnlPct, cmd.get().getReason(),
                            holdSec, entryScore, entryGrade, entryVwap, entryVel,
                            entryVolRatio, entryToRatio,
                            entryScVwap, entryScTrend, entryScVol, entryScTo, entryScHigh, entryScPat,
                            entryVelMid, entryVelLong, entryTrendSc, entryFromHigh, actualEntryAmt));

                    engine.notifySellFilled(symbol, 0);
                    currentQty = 0; avgPrice = 0.0; entryBarTime = null;
                    entryMode = "UNKNOWN"; entryGrade = "D"; snapCaptured = false;
                }
            }
        }

        // Force-close any open position at the last bar
        if (currentQty > 0 && !bars.isEmpty()) {
            MinuteBar last  = bars.get(bars.size() - 1);
            double lastPrice = last.getClosePrice();
            double rawPnl    = avgPrice > 0 ? (lastPrice - avgPrice) / avgPrice : 0.0;
            double pnlPct    = rawPnl - (cfg != null ? (cfg.feePct + cfg.slippagePct + (rawPnl > 0 ? cfg.taxPct : 0.0)) / 100.0 : 0.0);
            long holdSec     = entryBarTime != null
                    ? Duration.between(entryBarTime, last.getBarTime()).getSeconds() : 0L;
            double actualEntryAmt = entryClose * currentQty;
            trades.add(buildTrade(symbol, entryBarTime, last.getBarTime(), entryMode,
                    entryClose, lastPrice, pnlPct, "BACKTEST_END",
                    holdSec, entryScore, entryGrade, entryVwap, entryVel,
                    entryVolRatio, entryToRatio,
                    entryScVwap, entryScTrend, entryScVol, entryScTo, entryScHigh, entryScPat,
                    entryVelMid, entryVelLong, entryTrendSc, entryFromHigh, actualEntryAmt));
        }

        Map<String, Object> result = buildResult(market, symbol, startDate, endDate, bars.size(), buyAmount, trades,
                !proxyByTime.isEmpty());

        // 거절 사유 상위 10개 내림차순 정렬
        List<Map<String, Object>> rejectList = new ArrayList<>();
        rejectCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("reason", e.getKey());
                    item.put("count", e.getValue());
                    rejectList.add(item);
                });
        result.put("rejectReasonSummary", rejectList);
        return result;
    }

    private Map<String, Object> buildTrade(String symbol,
                                            LocalDateTime entryTime, LocalDateTime exitTime,
                                            String mode, double entryPrice, double exitPrice,
                                            double pnlPct, String exitReason, long holdSec,
                                            int score, String grade, double vwap, double vel,
                                            double volRatio, double toRatio,
                                            int scVwap, int scTrend, int scVol, int scTo, int scHigh, int scPat,
                                            double velMid, double velLong, int trendSc, double fromHigh,
                                            double actualEntryAmt) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("symbol",         symbol != null ? symbol : "");
        t.put("entryTime",      entryTime  != null ? entryTime.toString()  : "");
        t.put("exitTime",       exitTime   != null ? exitTime.toString()   : "");
        t.put("entryMode",      mode);
        t.put("entryPrice",     entryPrice);
        t.put("exitPrice",      exitPrice);
        t.put("pnlPct",         pnlPct);
        t.put("actualEntryAmt", actualEntryAmt);
        t.put("exitReason",     exitReason != null ? exitReason : "");
        t.put("holdSeconds",    holdSec);
        t.put("signalScore",    score);
        t.put("signalGrade",    grade != null ? grade : "D");
        t.put("vwapDistPct",    vwap);
        t.put("velocityShort",  vel);
        t.put("velocityMid",    velMid);
        t.put("velocityLong",   velLong);
        t.put("trendScore",     trendSc);
        t.put("fromHighPct",    fromHigh);
        t.put("volumeRatio",    volRatio);
        t.put("turnoverRatio",  toRatio);
        t.put("scoreVwap",      scVwap);
        t.put("scoreTrend",     scTrend);
        t.put("scoreVolume",    scVol);
        t.put("scoreTurnover",  scTo);
        t.put("scoreHigh",      scHigh);
        t.put("scorePattern",   scPat);
        return t;
    }

    private Map<String, Object> buildResult(String market, String symbol,
                                             String startDate, String endDate,
                                             int totalBars, double buyAmount,
                                             List<Map<String, Object>> trades,
                                             boolean proxyReplayEnabled) {
        int total = trades.size();
        int wins  = 0;
        double sumPnl = 0, sumWin = 0, sumLoss = 0;
        double cumPnl = 1.0, peak = 1.0, maxDD = 0.0;

        Map<String, List<Double>> byMode = new LinkedHashMap<>();
        Map<String, List<Double>> byExit = new LinkedHashMap<>();

        for (Map<String, Object> t : trades) {
            double p = (double) t.get("pnlPct");
            sumPnl += p;
            if (p > 0) { wins++; sumWin  += p; }
            else        {         sumLoss += Math.abs(p); }
            cumPnl *= (1 + p);
            if (cumPnl > peak) peak = cumPnl;
            double dd = (peak - cumPnl) / peak;
            if (dd > maxDD) maxDD = dd;

            byMode.computeIfAbsent((String) t.get("entryMode"),  k -> new ArrayList<>()).add(p);
            byExit.computeIfAbsent(exitGroup((String) t.get("exitReason")), k -> new ArrayList<>()).add(p);
        }

        double wr   = total > 0 ? (double) wins / total : 0.0;
        double avgP = total > 0 ? sumPnl / total : 0.0;
        double pf   = sumLoss > 0 ? sumWin / sumLoss : (sumWin > 0 ? 9999 : 0);
        int losses  = total - wins;
        double exp  = wr * (wins > 0 ? sumWin / wins : 0)
                    - (1 - wr) * (losses > 0 ? sumLoss / losses : 0);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status",          "OK");
        r.put("market",          market);
        r.put("symbol",          symbol);
        r.put("startDate",       startDate);
        r.put("endDate",         endDate);
        r.put("totalBars",       totalBars);
        r.put("buyAmount",       buyAmount);
        r.put("totalTrades",     total);
        r.put("wins",            wins);
        r.put("winRate",         wr);
        r.put("avgPnlPct",       avgP);
        r.put("cumulativePnlPct", cumPnl - 1.0);
        r.put("maxDrawdown",     maxDD);
        r.put("profitFactor",    pf);
        r.put("expectancy",      exp);
        List<String> warnings = new ArrayList<>();
        warnings.add("분봉 종가 기반 근사 테스트입니다. 실전의 5초 틱 진입 확인은 완전히 재현되지 않습니다.");
        if (!proxyReplayEnabled) {
            warnings.add("저장된 시장 프록시 분봉이 없어 실전의 시장 약세 필터는 결과에 반영되지 않았습니다.");
        }
        r.put("proxyReplayEnabled", proxyReplayEnabled);
        r.put("warnings", warnings);

        List<Map<String, Object>> modeList = new ArrayList<>();
        byMode.forEach((k, v) -> {
            Map<String, Object> sm = statMap(k, v);
            double modeWin = 0, modeLoss = 0;
            for (double p : v) { if (p > 0) modeWin += p; else modeLoss += Math.abs(p); }
            sm.put("profitFactor", modeLoss > 0 ? modeWin / modeLoss : (modeWin > 0 ? 9999.0 : 0.0));
            int mw = (int) v.stream().filter(p -> p > 0).count();
            int ml = v.size() - mw;
            double mWr  = v.size() > 0 ? (double) mw / v.size() : 0.0;
            double avgW = mw > 0 ? modeWin  / mw : 0.0;
            double avgL = ml > 0 ? modeLoss / ml : 0.0;
            sm.put("expectancy", mWr * avgW - (1 - mWr) * avgL);
            sm.put("mdd", computeMdd(v));
            modeList.add(sm);
        });
        r.put("modeStats", modeList);

        List<Map<String, Object>> exitList = new ArrayList<>();
        byExit.forEach((k, v) -> exitList.add(statMap(k, v)));
        r.put("exitStats", exitList);

        // ── Score range stats ──────────────────────────────────────────────
        String[][] scoreRanges = {{"95-100","95","100"},{"90-94","90","94"},{"85-89","85","89"},
                                   {"80-84","80","84"},{"75-79","75","79"},{"<75","0","74"}};
        List<Map<String, Object>> scoreRangeList = new ArrayList<>();
        for (String[] sr : scoreRanges) {
            int lo = Integer.parseInt(sr[1]), hi = Integer.parseInt(sr[2]);
            List<Double> pnls = new ArrayList<>();
            for (Map<String, Object> t : trades) {
                int sc = t.get("signalScore") instanceof Integer ? (Integer) t.get("signalScore") : 0;
                if (sc >= lo && sc <= hi) pnls.add((Double) t.get("pnlPct"));
            }
            Map<String, Object> m = statMap(sr[0], pnls);
            m.put("profitFactor", pfFromPnls(pnls));
            scoreRangeList.add(m);
        }
        r.put("scoreRangeStats", scoreRangeList);

        // ── Grade stats ────────────────────────────────────────────────────
        String[] grades = {"SS", "S", "A", "B", "C", "D"};
        List<Map<String, Object>> gradeList = new ArrayList<>();
        for (String g : grades) {
            List<Double> pnls = new ArrayList<>();
            for (Map<String, Object> t : trades) {
                if (g.equals(t.get("signalGrade"))) pnls.add((Double) t.get("pnlPct"));
            }
            Map<String, Object> m = statMap(g, pnls);
            m.put("profitFactor", pfFromPnls(pnls));
            gradeList.add(m);
        }
        r.put("gradeStats", gradeList);

        r.put("trades", trades);
        return r;
    }

    private double computeMdd(List<Double> pnls) {
        double cum = 1.0, peak = 1.0, mdd = 0.0;
        for (double p : pnls) {
            cum *= (1 + p);
            if (cum > peak) peak = cum;
            double dd = (peak - cum) / peak;
            if (dd > mdd) mdd = dd;
        }
        return mdd;
    }

    private double pfFromPnls(List<Double> pnls) {
        double win = 0, loss = 0;
        for (double p : pnls) { if (p > 0) win += p; else loss += Math.abs(p); }
        return loss > 0 ? win / loss : (win > 0 ? 9999.0 : 0.0);
    }

    private Map<String, Object> statMap(String label, List<Double> pnls) {
        int cnt  = pnls.size();
        int w    = (int) pnls.stream().filter(p -> p > 0).count();
        double s = pnls.stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label",            label);
        m.put("count",            cnt);
        m.put("winRate",          cnt > 0 ? (double) w / cnt : 0.0);
        m.put("avgPnlPct",        cnt > 0 ? s / cnt : 0.0);
        m.put("cumulativePnlPct", pnls.stream().mapToDouble(p -> Math.log1p(p)).sum());
        return m;
    }

    private String exitGroup(String reason) {
        if (reason == null) return "UNKNOWN";
        if (reason.startsWith("TAKE_PROFIT")) return "TAKE_PROFIT";
        if (reason.startsWith("TRAIL") || "BREAKEVEN_GUARD".equals(reason)) return "TRAIL";
        if (reason.startsWith("STOP_LOSS") || "EMERGENCY_STOP".equals(reason)
                || "FAILED_BREAKOUT".equals(reason) || "FAILED_PULLBACK".equals(reason)
                || "EARLY_MOMENTUM_DEAD".equals(reason) || "VWAP_BREAK".equals(reason)) return "STOPLOSS";
        if (reason.startsWith("TIME_STOP") || "EOD_FORCE_SELL".equals(reason)) return "TIME_STOP";
        if ("BACKTEST_END".equals(reason)) return "BACKTEST_END";
        return reason;
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private void doBatchInsert(List<MinuteBar> bars) {
        int batchSize = 500;
        for (int i = 0; i < bars.size(); i += batchSize) {
            minuteBarMapper.batchInsert(bars.subList(i, Math.min(i + batchSize, bars.size())));
        }
    }

    private void throttle() {
        long interval = kisProperties.isDemo() ? 1_200L : 100L;
        synchronized (rateLock) {
            long now     = System.currentTimeMillis();
            long elapsed = now - lastApiCallMs;
            if (elapsed < interval) {
                try { Thread.sleep(interval - elapsed); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            lastApiCallMs = System.currentTimeMillis();
        }
    }

    private String safeStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private double safeDouble(Map<String, Object> m, String key) {
        String s = safeStr(m, key);
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s.replace(",", "")); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private double orElse(double v, double fallback) {
        return v > 0 ? v : fallback;
    }

    private String decrementMinute(String hhmmss) {
        try {
            LocalTime time = LocalTime.parse(hhmmss, DateTimeFormatter.ofPattern("HHmmss"));
            return time.minusMinutes(1).format(DateTimeFormatter.ofPattern("HHmmss"));
        } catch (Exception e) {
            throw new RuntimeException("잘못된 KIS 분봉 시간: " + hhmmss, e);
        }
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status",  "ERROR");
        m.put("message", msg);
        return m;
    }
}
