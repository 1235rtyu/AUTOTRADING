package com.autotrading.service.impl;

import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.service.MarketInsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class MarketInsightServiceImpl implements MarketInsightService {

    private static final Logger logger = LoggerFactory.getLogger(MarketInsightServiceImpl.class);

    private final KoreaInvestmentApiClient apiClient;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    /**
     * KIS 국내 분봉 API 한 번 호출당 최대 반환 건수.
     *   - 당일봉  (FHKST03010200): 30건
     *   - 일별봉  (FHKST03010230): 120건
     */
    private static final int TODAY_MINUTE_LIMIT   = 30;
    private static final int HISTORY_MINUTE_LIMIT = 120;

    /**
     * 분봉 페이지 조회 최대 반복 횟수.
     * 예) 30분봉 전체 세션(09:00~15:30 = 13회 기준 약 400분 / 30 = 14봉) → 최대 5페이지면 충분
     * 1분봉은 390봉 / 30 = 13페이지 필요 → 15로 설정
     */
    private static final int MAX_MINUTE_PAGES = 15;

    public MarketInsightServiceImpl(KoreaInvestmentApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /* ════════════════════════════════════════════
       Public API
    ════════════════════════════════════════════ */

    @Override
    public Map<String, Object> getHtsTopView() {
        return apiClient.fetchHtsTopView();
    }

    @Override
    public Map<String, Object> getIntradayChart(String symbol, String fromTime) {
        String safeSymbol = StringUtils.hasText(symbol) ? symbol.trim() : "005930";
        String safeFrom   = normalizeTime(fromTime);
        return apiClient.fetchTimeChart(safeSymbol, safeFrom);
    }

    @Override
    public Map<String, Object> getRanking(String market, String exchange) {
        String safeMarket = normalizeMarket(market);

        if ("US".equals(safeMarket)) {
            List<String> exchanges  = buildUsExchangeCandidates(exchange);
            List<String> diagnostics = new ArrayList<>();
            for (String exch : exchanges) {
                Map<String, Object> raw  = apiClient.fetchOverseasTradeVolume(exch);
                List<Map<String, Object>> rows = asList(raw.get("output2"));
                if (rows == null || rows.isEmpty()) rows = asList(raw.get("data"));
                if (rows == null || rows.isEmpty()) {
                    diagnostics.add(exch + ": status=" + raw.getOrDefault("status", "?")
                            + ", msg=" + raw.getOrDefault("message", "")
                            + ", nday=" + raw.getOrDefault("nday", "?"));
                    continue;
                }
                List<Map<String, Object>> normalized = normalizeUsRankingRows(rows);
                if (normalized.isEmpty()) {
                    diagnostics.add(exch + ": rows=" + rows.size() + ", normalized=0");
                    continue;
                }
                Map<String, Object> result = new HashMap<>();
                result.put("status",   "OK");
                result.put("message",  raw.getOrDefault("message", "OK"));
                result.put("exchange", exch);
                result.put("data",     normalized);
                return result;
            }
            return Map.of(
                "status",   "ERROR",
                "message",  diagnostics.isEmpty() ? "해외 랭킹 데이터가 없습니다."
                                                  : "해외 랭킹 데이터가 없습니다. " + String.join(" | ", diagnostics),
                "exchange", normalizeExchange(exchange),
                "data",     List.of()
            );
        }

        // 코스피(1) + 코스닥(2) 각각 30개씩 조회 후 합산 → 최대 60개 확보
        List<Map<String, Object>> allRawRows = new ArrayList<>();
        String lastMessage = "";
        for (int blng : new int[]{1, 2}) {
            try {
                Map<String, Object> raw2 = apiClient.fetchVolumeRanking(blng);
                List<Map<String, Object>> r2 = asList(raw2.get("output"));
                if (r2 != null) allRawRows.addAll(r2);
                if (StringUtils.hasText((String) raw2.get("message"))) lastMessage = (String) raw2.get("message");
            } catch (Exception ignored) {}
        }
        // 합산 결과가 없으면 전체(0) 로 재시도
        if (allRawRows.isEmpty()) {
            Map<String, Object> raw = apiClient.fetchVolumeRanking(0);
            List<Map<String, Object>> r0 = asList(raw.get("output"));
            if (r0 != null) allRawRows.addAll(r0);
            lastMessage = (String) raw.getOrDefault("message", "");
        }
        List<Map<String, Object>> rows = allRawRows;
        if (rows.isEmpty()) {
            return Map.of("status", "ERROR", "message", "국내 랭킹 데이터가 없습니다.", "data", List.of());
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> item = new HashMap<>();
            String price  = pickString(r, "stck_prpr");
            String volume = pickString(r, "acml_vol");
            double tradeValue = resolveTradeValue(
                    pickDouble(r, "acml_tr_pbmn", "stck_tr_pbmn", "tr_pbmn", "trde_amt"),
                    asDouble(price, 0d), asDouble(volume, 0d));
            item.put("symbol",         pickString(r, "mksc_shrn_iscd"));
            item.put("name",           pickString(r, "hts_kor_isnm"));
            item.put("stck_prpr",      price);
            item.put("prdy_ctrt",      pickString(r, "prdy_ctrt"));
            item.put("prdy_vrss_sign", pickString(r, "prdy_vrss_sign"));
           item.put("acml_tr_pbmn", formatRankMetric(tradeValue));
            item.put("acml_vol",     formatRankMetric(asDouble(volume, 0d)));
            item.put("acml_vol_raw", volume);
            normalized.add(item);
        }
        normalized.sort(Comparator.comparingDouble(this::rankingAmount).reversed());

        Map<String, Object> result = new HashMap<>();
        result.put("status",   "OK");
        result.put("message",  StringUtils.hasText(lastMessage) ? lastMessage : "정상처리 되었습니다.");
        result.put("exchange", "KRX");
        result.put("data",     normalized);
        return result;
    }

    /**
     * 차트 조회. date 파라미터로 KR 분봉의 과거 조회를 지원.
     *
     * 분봉 조회 전략:
     *  - date == null (당일): fetchTimeChart() 반복 호출 (30건씩, 최대 15페이지)
     *  - date != null (과거): fetchDailyMinuteChart() 반복 호출 (120건씩, 최대 15페이지)
     *  - 5m/15m/30m/60m: 1분봉으로 전체 조회 후 다운샘플 (정확도 최대)
     *    단, apiClient에 직접 해당 분봉 API가 있으면 그쪽 우선 사용 가능
     */
    @Override
    public Map<String, Object> getChart(String market, String symbol,
                                        String timeframe, String exchange,
                                        String date) {
        String safeMarket = normalizeMarket(market);
        String safeSymbol = StringUtils.hasText(symbol) ? symbol.trim()
                : ("US".equals(safeMarket) ? "AAPL" : "005930");
        String safeTf   = normalizeTimeframe(timeframe);
        String exch     = normalizeExchange(exchange);

        /* ── 날짜 정규화 (YYYYMMDD) ── */
        String safeDate = normalizeDateParam(date);  /* null이면 당일 */
        boolean isToday = isToday(safeDate);

        Map<String, Object> raw;

        if ("US".equals(safeMarket)) {
            raw = fetchUsChart(safeSymbol, exch, safeTf, safeDate);
        } else {
            raw = fetchKrChart(safeSymbol, safeTf, safeDate, isToday);
        }

        /* ── 응답 조립 ── */
        Map<String, Object> response = new HashMap<>();
        response.put("status",    raw.getOrDefault("status", "ERROR"));
        response.put("message",   raw.getOrDefault("message", ""));
        response.put("market",    safeMarket);
        response.put("symbol",    safeSymbol);
        response.put("timeframe", safeTf);
        response.put("exchange",  "US".equals(safeMarket) ? exch : "KRX");
        if (safeDate != null) response.put("date", safeDate);

        Map<String, Object> output1 = asMap(raw.get("output1"));
        if (output1 != null) {
            String name = pickString(output1,
                    "hts_kor_isnm", "stck_nm", "stck_isnm", "prdt_name",
                    "kor_isnm", "isu_nm", "name");
            if (StringUtils.hasText(name)) response.put("name", name);
        }

        List<Map<String, Object>> output2 = asList(raw.get("output2"));
        List<Map<String, Object>> points   = buildPoints(output2);
        points.sort(Comparator.comparingLong(p -> ((Number) p.getOrDefault("ts", 0L)).longValue()));

        /* ── 다운샘플: 1분봉으로 가져온 KR 데이터를 N분봉으로 집계 ── */
        if ("KR".equals(safeMarket) && isMinuteTf(safeTf) && !"1m".equals(safeTf)) {
            int step = Integer.parseInt(safeTf.replace("m", ""));
            points = downsample(points, step);
        }

        response.put("points", points);
        return response;
    }

    /* ════════════════════════════════════════════
       chart 내부 로직
    ════════════════════════════════════════════ */

    /**
     * KR 차트 조회.
     * 분봉의 경우 전체 세션을 커버할 때까지 페이지를 반복 조회하여 이어 붙임.
     */
    private Map<String, Object> fetchKrChart(String symbol, String tf,
                                             String date, boolean isToday) {
        if (!isMinuteTf(tf)) {
            /* 일봉/주봉/월봉 */
            String period  = mapPeriod(tf);
            LocalDate end   = LocalDate.now();
            LocalDate start = end.minusMonths(period.equals("D") ? 3 : 24);
            return apiClient.fetchDomesticDailyChart(
                    symbol, period,
                    start.format(DATE_FORMAT), end.format(DATE_FORMAT));
        }

        /* ── 분봉 페이지 반복 조회 ── */
        /* 항상 1분봉으로 가져와서 필요 시 다운샘플 (정확도 ↑) */
        List<Map<String, Object>> allOutput2 = new ArrayList<>();
        Map<String, Object> firstRaw = null;

        if (isToday) {
            /*
             * 당일 분봉: fetchTimeChart() 반복
             * 마지막으로 수신한 봉의 시간을 다음 요청의 fromTime으로 사용.
             * 역순(최신→과거)으로 오는 경우도 고려해서 처음 봉의 시간을 추적.
             */
            String fromTime = "153000";  /* 장 마감 시간부터 역방향 조회 */
            for (int page = 0; page < MAX_MINUTE_PAGES; page++) {
                Map<String, Object> raw = apiClient.fetchTimeChart(symbol, fromTime);
                if (firstRaw == null) firstRaw = raw;

                List<Map<String, Object>> chunk = asList(raw.get("output2"));
                if (chunk == null || chunk.isEmpty()) break;

                allOutput2.addAll(chunk);

                /* 다음 페이지 fromTime: 수신한 봉 중 가장 이른 시간 */
                String earliest = findEarliestTime(chunk);
                if (earliest == null || earliest.compareTo("090100") <= 0) break;

                /* 이미 장 시작 구간에 도달하면 종료 */
                fromTime = decrementTime(earliest, 1);
            }
        } else {
            /*
             * 과거 분봉: fetchDailyMinuteChart() 반복
             * 동일하게 역방향으로 조회.
             */
            String safeDate = date != null ? date : LocalDate.now().format(DATE_FORMAT);
            String fromTime = "153000";
            for (int page = 0; page < MAX_MINUTE_PAGES; page++) {
                Map<String, Object> raw = apiClient.fetchDailyMinuteChart(symbol, safeDate, fromTime);
                if (firstRaw == null) firstRaw = raw;

                List<Map<String, Object>> chunk = asList(raw.get("output2"));
                if (chunk == null || chunk.isEmpty()) break;

                allOutput2.addAll(chunk);

                String earliest = findEarliestTime(chunk);
                if (earliest == null || earliest.compareTo("090100") <= 0) break;

                fromTime = decrementTime(earliest, 1);
            }
        }

        if (firstRaw == null) {
            return Map.of("status", "ERROR", "message", "분봉 데이터 없음");
        }

        /* output2를 수집된 전체 리스트로 교체 */
        Map<String, Object> merged = new HashMap<>(firstRaw);
        /* 중복 제거: stck_cntg_hour 기준 */
        allOutput2 = deduplicateMinutes(allOutput2);
        merged.put("output2", allOutput2);
        return merged;
    }

    /**
     * US 차트 조회.
     */
    private Map<String, Object> fetchUsChart(String symbol, String exch,
                                             String tf, String date) {
        if (isMinuteTf(tf)) {
            String minutes = tf.replace("m", "");
            return apiClient.fetchOverseasTimeChart(symbol, exch, minutes);
        }
        String gubn    = mapGubn(tf);
        String endDate = date != null ? date : LocalDate.now().format(DATE_FORMAT);
        return apiClient.fetchOverseasDailyPrice(symbol, exch, gubn, endDate);
    }

    /* ════════════════════════════════════════════
       분봉 페이지 조회 헬퍼
    ════════════════════════════════════════════ */

    /**
     * 분봉 청크에서 가장 이른 체결시간(HHMMSS)을 반환.
     * KIS output2는 최신→과거 역순이므로 마지막 원소가 가장 이름.
     */
    private String findEarliestTime(List<Map<String, Object>> chunk) {
        if (chunk == null || chunk.isEmpty()) return null;
        /* 마지막 원소(가장 오래된 봉)의 체결시간 */
        Map<String, Object> oldest = chunk.get(chunk.size() - 1);
        String t = pickString(oldest, "stck_cntg_hour", "cntg_hour", "time");
        return StringUtils.hasText(t) ? t.replaceAll("[^0-9]", "") : null;
    }

    /**
     * HHMMSS 시간 문자열에서 minutes분을 차감한 시간 반환.
     * 예) decrementTime("093000", 1) → "092900"
     */
    private String decrementTime(String hhmmss, int minutes) {
        try {
            int total = Integer.parseInt(hhmmss);
            int h = total / 10000;
            int m = (total / 100) % 100;
            int s = total % 100;
            int totalMins = h * 60 + m - minutes;
            if (totalMins < 0) totalMins = 0;
            int nh = totalMins / 60;
            int nm = totalMins % 60;
            return String.format("%02d%02d%02d", nh, nm, s);
        } catch (NumberFormatException e) {
            return "090000";
        }
    }

    /**
     * stck_cntg_hour 기준 중복 봉 제거.
     */
    private List<Map<String, Object>> deduplicateMinutes(List<Map<String, Object>> rows) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String key = pickString(row, "stck_bsop_date", "xymd") +
                         pickString(row, "stck_cntg_hour", "xhms");
            if (seen.add(key)) out.add(row);
        }
        return out;
    }

    /* ════════════════════════════════════════════
       Market Index
    ════════════════════════════════════════════ */

    @Override
    public Map<String, Object> getMarketIndex() {
        List<Map<String, Object>> result = new ArrayList<>();

        tryAppendIndex(result, "KOSPI",   () -> asMap(apiClient.fetchDomesticIndex("0001").get("output")),
            new String[]{"bstp_nmix_prpr","stck_prpr"},
            new String[]{"bstp_nmix_prdy_ctrt","prdy_ctrt"},
            new String[]{"bstp_nmix_prdy_vrss","prdy_vrss"});

        tryAppendIndex(result, "KOSDAQ",  () -> asMap(apiClient.fetchDomesticIndex("1001").get("output")),
            new String[]{"bstp_nmix_prpr","stck_prpr"},
            new String[]{"bstp_nmix_prdy_ctrt","prdy_ctrt"},
            new String[]{"bstp_nmix_prdy_vrss","prdy_vrss"});

        for (String[] idx : new String[][]{{"S","S&P 500"},{"N","NASDAQ"},{"D","DOW"}}) {
            final String code = idx[0], name = idx[1];
            tryAppendIndex(result, name, () -> asMap(apiClient.fetchOverseasMajorIndex(code).get("output")),
                new String[]{"ovrs_nmix_prpr","last","stck_prpr","price"},
                new String[]{"prdy_ctrt","diff_rate"},
                new String[]{"ovrs_nmix_prdy_vrss","prdy_vrss","point"});
        }

        tryAppendIndex(result, "USD/KRW", () -> asMap(apiClient.fetchUsdKrwExchange().get("output")),
            new String[]{"bstp_nmix_prpr","stck_prpr","price"},
            new String[]{"bstp_nmix_prdy_ctrt","prdy_ctrt","change"},
            new String[]{"bstp_nmix_prdy_vrss","prdy_vrss","point"});

        return Map.of("status", "OK", "data", result);
    }

    /* ════════════════════════════════════════════
       Balance / Holdings / Cash
    ════════════════════════════════════════════ */

    @Override public Map<String, Object> getDomesticBalance()   { return apiClient.fetchDomesticBalance(); }
    @Override public Map<String, Object> getDomesticHoldings()  { return apiClient.fetchDomesticHoldings(); }

    @Override
    public Map<String, Object> getDomesticDailyCcld(String startDate, String endDate,
                                                    String sideCode, String ccldDvsn,
                                                    String exchangeId) {
        String safeSide     = normalizeListCode(sideCode,   "00", "00","01","02");
        String safeCcld     = normalizeListCode(ccldDvsn,   "01", "00","01","02");
        String safeExchange = normalizeListCode(exchangeId, "KRX","KRX","NXT","SOR","ALL");
        return apiClient.fetchDomesticDailyCcld(
                startDate, endDate, safeSide, safeCcld, "00", "", "00", safeExchange, 20);
    }

    @Override
    public Map<String, Object> getOverseasBalance(String exchange, String currency) {
        return apiClient.fetchOverseasBalance(normalizeExchange(exchange),
                StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD");
    }

    @Override
    public Map<String, Object> getOverseasHoldings(String exchange, String currency) {
        return apiClient.fetchOverseasHoldings(
                StringUtils.hasText(exchange) ? exchange.trim().toUpperCase() : "NASD",
                StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD");
    }

    @Override
    public Map<String, Object> getOverseasCash(String currency) {
        return apiClient.fetchOverseasCash(
                StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD");
    }

    /* ════════════════════════════════════════════
       downsample: 1분봉 → N분봉 OHLCV 집계
    ════════════════════════════════════════════ */

    /**
     * 1분봉 포인트 리스트를 step분 단위로 집계.
     *
     * 개선된 방식:
     *  - 단순 인덱스 슬라이싱(i+=step) 대신 타임스탬프 기반 시간 버킷으로 집계
     *  - 09:00 기준 step분 단위 버킷 경계를 계산하므로 09:05, 09:10... 정렬 보장
     *  - KRX 거래 시간(09:00~15:30) 내 버킷만 처리
     */
    private List<Map<String, Object>> downsample(List<Map<String, Object>> points, int step) {
        if (points == null || points.isEmpty() || step <= 1) return points;

        Map<Integer, List<Map<String, Object>>> buckets = new java.util.TreeMap<>();

        for (Map<String, Object> p : points) {
            /* time 형식: "YYYY-MM-DD HH:mm" */
            String time = String.valueOf(p.getOrDefault("time", ""));
            int bucketKey = timeToBucket(time, step);
            if (bucketKey < 0) continue;  /* 거래 시간 외 무시 */
            buckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(p);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : buckets.entrySet()) {
            List<Map<String, Object>> bars = entry.getValue();
            if (bars.isEmpty()) continue;

            Map<String, Object> first = bars.get(0);
            Map<String, Object> last  = bars.get(bars.size() - 1);

            double open   = asDouble(first.get("open"),  asDouble(first.get("close"), 0d));
            double close  = asDouble(last.get("close"),  asDouble(last.get("open"),   open));
            double high   = open;
            double low    = open;
            double volume = 0d;

            for (Map<String, Object> b : bars) {
                double h = asDouble(b.get("high"),   asDouble(b.get("close"), close));
                double l = asDouble(b.get("low"),    asDouble(b.get("close"), close));
                high   = Math.max(high, h);
                low    = Math.min(low,  l);
                volume += Math.max(0d, asDouble(b.get("volume"), 0d));
            }

            /* 버킷의 시작 시각을 time 레이블로 사용 */
            String timeLabel = bucketKeyToTimeLabel(first, entry.getKey(), step);

            Map<String, Object> bar = new HashMap<>();
            bar.put("time",   timeLabel);
            bar.put("ts",     first.getOrDefault("ts", 0L));
            bar.put("open",   open);
            bar.put("high",   high);
            bar.put("low",    low);
            bar.put("close",  close);
            bar.put("price",  close);
            bar.put("volume", volume);
            result.add(bar);
        }
        return result;
    }

    /**
     * 시간 문자열을 step분 단위 버킷 키(정수)로 변환.
     * 버킷 키 = 하루 시작부터의 분 수 // step * step
     * 거래 시간(540~930분, 09:00~15:30) 외의 봉은 -1 반환.
     */
    private int timeToBucket(String time, int step) {
        /* time 형식: "YYYY-MM-DD HH:mm" 또는 "YYYY-MM-DD HH:MM:SS" */
        try {
            String[] parts = time.split("[ T]");
            if (parts.length < 2) return -1;
            String[] hm = parts[1].split(":");
            if (hm.length < 2) return -1;
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            int totalMin = h * 60 + m;
            /* KRX 09:00~15:30 = 540~930분 */
            if (totalMin < 540 || totalMin > 930) return -1;
            return (totalMin / step) * step;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 버킷 키(분 수)를 time 레이블 문자열로 변환.
     * 기존 봉의 날짜 부분을 재사용.
     */
    private String bucketKeyToTimeLabel(Map<String, Object> firstBar, int bucketKey, int step) {
        String orig = String.valueOf(firstBar.getOrDefault("time", ""));
        String datePart = "";
        if (orig.length() >= 10) datePart = orig.substring(0, 10);
        int bh = bucketKey / 60;
        int bm = bucketKey % 60;
        return datePart + " " + String.format("%02d:%02d", bh, bm);
    }

    /* ════════════════════════════════════════════
       normalize helpers
    ════════════════════════════════════════════ */

    private String normalizeDateParam(String date) {
        if (!StringUtils.hasText(date)) return null;
        String digits = date.trim().replaceAll("[^0-9]", "");
        return digits.length() == 8 ? digits : null;
    }

    private boolean isToday(String dateStr) {
        if (dateStr == null) return true;
        String today = LocalDate.now().format(DATE_FORMAT);
        return today.equals(dateStr);
    }

    private String normalizeTime(String fromTime) {
        if (!StringUtils.hasText(fromTime)) return "090000";
        String digits = fromTime.replaceAll("[^0-9]", "");
        if (digits.length() == 4) return digits + "00";
        if (digits.length() >= 6) return digits.substring(0, 6);
        return "090000";
    }

    private String normalizeMarket(String market) {
        if (!StringUtils.hasText(market)) return "KR";
        return market.trim().toUpperCase().startsWith("US") ? "US" : "KR";
    }

    private String normalizeExchange(String exchange) {
        if (!StringUtils.hasText(exchange)) return "NAS";
        String safe = exchange.trim().toUpperCase();
        if (safe.startsWith("NY")) return "NYS";
        if (safe.startsWith("AM")) return "AMS";
        return "NAS";
    }

    /**
     * 타임프레임 정규화.
     * KIS 분봉 API는 1분봉(FHKST03010200) 기반이므로
     * 5m/15m/30m/60m 모두 수용 후 downsample.
     */
    private String normalizeTimeframe(String timeframe) {
        if (!StringUtils.hasText(timeframe)) return "1d";
        switch (timeframe.trim().toLowerCase()) {
            case "1m":  return "1m";
            case "3m":  return "3m";
            case "5m":  return "5m";
            case "10m": return "10m";
            case "15m": return "15m";
            case "30m": return "30m";
            case "60m": return "60m";
            case "1d":  return "1d";
            case "1w":  return "1w";
            case "1mon":
            case "1mo":
            case "1mth":return "1mo";
            default:    return "1d";
        }
    }

    private boolean isMinuteTf(String tf) {
        if (!StringUtils.hasText(tf)) return false;
        String lower = tf.toLowerCase();
        return lower.endsWith("m")
            && !lower.equals("1mon") && !lower.equals("1mo") && !lower.equals("1mth");
    }

    private String mapPeriod(String tf) {
        if ("1w".equals(tf)) return "W";
        if ("1mo".equals(tf) || "1mon".equals(tf) || "1mth".equals(tf)) return "M";
        return "D";
    }

    private String mapGubn(String tf) {
        if ("1w".equals(tf)) return "1";
        if ("1mo".equals(tf) || "1mon".equals(tf) || "1mth".equals(tf)) return "2";
        return "0";
    }

    private String normalizeListCode(String value, String defaultValue, String... allowed) {
        if (!StringUtils.hasText(value)) return defaultValue;
        String safe = value.trim().toUpperCase();
        for (String a : allowed) if (safe.equals(a)) return safe;
        return defaultValue;
    }

    /* ════════════════════════════════════════════
       buildPoints — output2 → chart points
    ════════════════════════════════════════════ */

    private List<Map<String, Object>> buildPoints(List<Map<String, Object>> rows) {
        List<Map<String, Object>> points = new ArrayList<>();
        if (rows == null) return points;

        for (Map<String, Object> row : rows) {
            if (row == null) continue;
            String date    = pickString(row, "xymd", "stck_bsop_date", "bsop_date", "date");
            String time    = pickString(row, "xhms", "stck_cntg_hour", "cntg_hour", "time");
            String display = buildDisplayTime(date, time);
            long   ts      = buildSortKey(date, time);

            Double close = pickDouble(row,
                "stck_prpr", "stck_clpr", "prpr", "last", "last_pr", "last_prpr",
                "close", "clos", "ovrs_prpr");
            if (close == null || close <= 0) continue;

            Double open   = pickDouble(row, "stck_oprc", "open", "ovrs_oprc", "oprc");
            Double high   = pickDouble(row, "stck_hgpr", "high", "ovrs_hgpr", "hgpr");
            Double low    = pickDouble(row, "stck_lwpr", "low",  "ovrs_lwpr", "lwpr");
            Double volume = pickDouble(row, "cntg_vol", "trde_qty", "vol", "volume",
                                           "acml_vol", "tvol", "evol");

            Map<String, Object> point = new HashMap<>();
            point.put("time",   display);
            point.put("open",   open   != null ? open   : close);
            point.put("high",   high   != null ? high   : close);
            point.put("low",    low    != null ? low    : close);
            point.put("close",  close);
            point.put("price",  close);
            point.put("volume", volume != null ? volume : 0d);
            point.put("ts",     ts);
            points.add(point);
        }
        return points;
    }

    /* ════════════════════════════════════════════
       index 조회 헬퍼
    ════════════════════════════════════════════ */

    @FunctionalInterface
    private interface RowSupplier {
        Map<String, Object> get() throws Exception;
    }

    private void tryAppendIndex(List<Map<String, Object>> result,
                                String name, RowSupplier supplier,
                                String[] priceKeys, String[] changeKeys, String[] pointKeys) {
        try {
            appendIndex(result, name, supplier.get(), priceKeys, changeKeys, pointKeys);
        } catch (Exception e) {
            logger.warn("Failed to fetch index {}", name, e);
            appendIndex(result, name, null, priceKeys, changeKeys, pointKeys);
        }
    }

    private void appendIndex(List<Map<String, Object>> result, String name,
                             Map<String, Object> row,
                             String[] priceKeys, String[] changeKeys, String[] pointKeys) {
        double price  = 0d, change = 0d, point = 0d;
        if (row != null && !row.isEmpty()) {
            Double p  = pickDouble(row, priceKeys);
            Double c  = pickDouble(row, changeKeys);
            Double pt = pickDouble(row, pointKeys);
            price  = p  != null ? p  : 0d;
            change = c  != null ? c  : 0d;
            point  = pt != null ? pt : 0d;
        }
        Map<String, Object> item = new HashMap<>();
        item.put("name",   name);
        item.put("price",  price);
        item.put("change", change);
        item.put("point",  point);
        result.add(item);
    }

    /* ════════════════════════════════════════════
       US ranking helpers
    ════════════════════════════════════════════ */

    private List<String> buildUsExchangeCandidates(String exchange) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addUsExchangeCandidate(candidates, exchange);
        addUsExchangeCandidate(candidates, "NAS");
        addUsExchangeCandidate(candidates, "NYS");
        addUsExchangeCandidate(candidates, "AMS");
        return new ArrayList<>(candidates);
    }

    private void addUsExchangeCandidate(LinkedHashSet<String> candidates, String exchange) {
        if (!StringUtils.hasText(exchange)) return;
        switch (exchange.trim().toUpperCase()) {
            case "NAS": case "NASD": case "NASDAQ":
                candidates.add("NAS"); candidates.add("NASD"); break;
            case "NYS": case "NYSE":
                candidates.add("NYS"); candidates.add("NYSE"); break;
            case "AMS": case "AMEX":
                candidates.add("AMS"); candidates.add("AMEX"); break;
            default:
                candidates.add(exchange.trim().toUpperCase());
        }
    }

    private List<Map<String, Object>> normalizeUsRankingRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (Map<String, Object> row : rows) {
            if (row == null) continue;
            String symbol = pickString(row, "symbol","symb","ovrs_pdno","pdno","mksc_shrn_iscd");
            if (!StringUtils.hasText(symbol)) {
                String rsym = pickString(row, "rsym");
                if (StringUtils.hasText(rsym)) symbol = rsym.length() > 4 ? rsym.substring(4) : rsym;
            }
            if (!StringUtils.hasText(symbol)) continue;
            symbol = symbol.trim().toUpperCase();
            if (!seen.add(symbol)) continue;

            String name   = pickString(row, "name","ovrs_item_name","ovrs_item_kor_name","hts_kor_isnm");
            String price  = pickString(row, "stck_prpr","last","ovrs_prpr","close","clos","prpr");
            String rate   = pickString(row, "prdy_ctrt","diff_rate","rate","prdy_vrss_rt");
            String sign   = pickString(row, "prdy_vrss_sign","diff_sign","sign");
            String volume = pickString(row, "acml_vol","tvol","evol","vol","avol","volume");
            double tradeValue = resolveTradeValue(
                    pickDouble(row, "tr_pbmn","acml_tr_pbmn","ovrs_tr_pbmn","trade_pbmn","trade_value","trde_amt","tot_amt"),
                    asDouble(price, 0d), asDouble(volume, 0d));

            if (!StringUtils.hasText(rate)) rate = "0";
            if (!StringUtils.hasText(sign)) {
                sign = "3";
                try {
                    double r = Double.parseDouble(rate.replace(",","").replace("%",""));
                    sign = r > 0 ? "2" : (r < 0 ? "5" : "3");
                } catch (NumberFormatException ignored) {}
            }
            if (!StringUtils.hasText(volume)) volume = "0";

            Map<String, Object> item = new HashMap<>();
            item.put("symbol",          symbol);
            item.put("name",            name);
            item.put("stck_prpr",       price);
            item.put("prdy_ctrt",       rate);
            item.put("prdy_vrss_sign",  sign);
            item.put("acml_tr_pbmn", formatRankMetric(tradeValue));
            item.put("acml_vol",     formatRankMetric(asDouble(volume, 0d)));
            item.put("acml_vol_raw", volume);
            normalized.add(item);
        }
        normalized.sort(Comparator.comparingDouble(this::rankingAmount).reversed());
        return normalized;
    }

    /* ════════════════════════════════════════════
       공통 유틸
    ════════════════════════════════════════════ */

    private double resolveTradeValue(Double apiValue, double price, double volume) {
        if (apiValue != null && apiValue > 0) return apiValue;
        if (price > 0 && volume > 0) return price * volume;
        return 0d;
    }

    private String formatRankMetric(double value) {
        return String.valueOf(Math.round(Math.max(0d, value)));
    }

    private double rankingAmount(Map<String, Object> row) {
        if (row == null) return 0d;
        return asDouble(row.get("acml_tr_pbmn"), asDouble(row.get("acml_vol"), 0d));
    }

    private String buildDisplayTime(String date, String time) {
        if (StringUtils.hasText(date) && StringUtils.hasText(time))
            return formatDate(date) + " " + formatTime(time);
        if (StringUtils.hasText(time))  return formatTime(time);
        if (StringUtils.hasText(date))  return formatDate(date);
        return "";
    }

    private long buildSortKey(String date, String time) {
        String d = StringUtils.hasText(date) ? date.replaceAll("[^0-9]","") : "";
        String t = StringUtils.hasText(time) ? time.replaceAll("[^0-9]","") : "";
        String key = d + t;
        if (key.isEmpty()) return 0L;
        try { return Long.parseLong(key); } catch (NumberFormatException e) { return 0L; }
    }

    private String formatDate(String raw) {
        String digits = raw.replaceAll("[^0-9]","");
        if (digits.length() >= 8)
            return digits.substring(0,4)+"-"+digits.substring(4,6)+"-"+digits.substring(6,8);
        return raw;
    }

    private String formatTime(String raw) {
        String digits = raw.replaceAll("[^0-9]","");
        if (digits.length() >= 4) return digits.substring(0,2)+":"+digits.substring(2,4);
        return raw;
    }

    private String pickString(Map<String, Object> row, String... keys) {
        if (row == null) return "";
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null) { String s = v.toString(); if (StringUtils.hasText(s)) return s; }
        }
        return "";
    }

    private Double pickDouble(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String key : keys) {
            Object v = row.get(key);
            if (v == null) continue;
            try {
                String t = v.toString().replace(",","");
                if (StringUtils.hasText(t)) return Double.parseDouble(t);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private double asDouble(Object value, double fallback) {
        if (value == null) return fallback;
        try { return Double.parseDouble(String.valueOf(value).replace(",","")); }
        catch (NumberFormatException e) { return fallback; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : null;
    }
}