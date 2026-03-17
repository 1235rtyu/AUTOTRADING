package com.autotrading.service.impl;

import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.service.MarketInsightService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

@Service
public class MarketInsightServiceImpl implements MarketInsightService {
    private final KoreaInvestmentApiClient apiClient;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public MarketInsightServiceImpl(KoreaInvestmentApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Map<String, Object> getHtsTopView() {
        return apiClient.fetchHtsTopView();
    }

    @Override
    public Map<String, Object> getIntradayChart(String symbol, String fromTime) {
        String safeSymbol = StringUtils.hasText(symbol) ? symbol.trim() : "005930";
        String safeFrom = normalizeTime(fromTime);
        return apiClient.fetchTimeChart(safeSymbol, safeFrom);
    }

@Override
public Map<String, Object> getRanking(String market, String exchange) {
    String safeMarket = normalizeMarket(market);

    if ("US".equals(safeMarket)) {
        // US 로직은 기존 그대로
        String exch = normalizeExchange(exchange);
        Map<String, Object> raw = apiClient.fetchOverseasTradeVolume(exch);
        List<Map<String, Object>> output2 = asList(raw.get("output2"));
        if (output2 == null || output2.isEmpty()) {
            return Map.of("status", "ERROR", "message", "해외 랭킹 데이터 없음", "data", List.of());
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> r : output2) {
            Map<String, Object> item = new HashMap<>();
            String symb = pickString(r, "symb");
            String rsym = pickString(r, "rsym");
            if (!StringUtils.hasText(symb) && rsym.length() > 4) {
                symb = rsym.substring(4);
            }
            item.put("symbol",           symb);
            item.put("name",             pickString(r, "name", "ename", symb));
            item.put("stck_prpr",        pickString(r, "last"));
            item.put("prdy_ctrt",        pickString(r, "rate"));
            item.put("prdy_vrss_sign",   pickString(r, "sign"));
            item.put("acml_vol",         pickString(r, "tvol"));
            normalized.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("message", "정상처리 되었습니다.");
        result.put("data", normalized);
        return result;
    }

    // ★ KR: fetchHtsTopView() 대신 fetchVolumeRanking() 사용
    Map<String, Object> raw = apiClient.fetchVolumeRanking();

    List<Map<String, Object>> rows = asList(raw.get("output"));
    if (rows == null || rows.isEmpty()) {
        return Map.of("status", "ERROR", "message", "국내 랭킹 데이터 없음", "data", List.of());
    }

    List<Map<String, Object>> normalized = new ArrayList<>();
    for (Map<String, Object> r : rows) {
        Map<String, Object> item = new HashMap<>();
        item.put("symbol",           pickString(r, "mksc_shrn_iscd"));
        item.put("name",             pickString(r, "hts_kor_isnm"));
        item.put("stck_prpr",        pickString(r, "stck_prpr"));        // 현재가
        item.put("prdy_ctrt",        pickString(r, "prdy_ctrt"));        // 등락률
        item.put("prdy_vrss_sign",   pickString(r, "prdy_vrss_sign"));   // 등락 기호
        item.put("acml_vol",         pickString(r, "acml_vol"));         // 거래량
        normalized.add(item);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("status", "OK");
    result.put("message", "정상처리 되었습니다.");
    result.put("data", normalized);
    return result;
}

    @Override
    public Map<String, Object> getChart(String market, String symbol, String timeframe, String exchange) {
        String safeMarket = normalizeMarket(market);
        String safeSymbol = StringUtils.hasText(symbol) ? symbol.trim() : ("US".equals(safeMarket) ? "AAPL" : "005930");
        String safeTf = normalizeTimeframe(timeframe);
        String exch = normalizeExchange(exchange);

        Map<String, Object> raw;
        if ("US".equals(safeMarket)) {
            if (isMinuteTf(safeTf)) {
                String minutes = safeTf.replace("m", "");
                raw = apiClient.fetchOverseasTimeChart(safeSymbol, exch, minutes);
            } else {
                String gubn = mapGubn(safeTf);
                raw = apiClient.fetchOverseasDailyPrice(safeSymbol, exch, gubn, LocalDate.now().format(DATE_FORMAT));
            }
        } else {
            if (isMinuteTf(safeTf)) {
                raw = apiClient.fetchTimeChart(safeSymbol, "090000");
            } else {
                String period = mapPeriod(safeTf);
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusMonths(period.equals("D") ? 3 : 24);
                raw = apiClient.fetchDomesticDailyChart(safeSymbol, period, start.format(DATE_FORMAT), end.format(DATE_FORMAT));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", raw.getOrDefault("status", "ERROR"));
        response.put("message", raw.getOrDefault("message", ""));
        response.put("market", safeMarket);
        response.put("symbol", safeSymbol);
        response.put("timeframe", safeTf);
        response.put("exchange", "US".equals(safeMarket) ? exch : "KRX");

        Map<String, Object> output1 = asMap(raw.get("output1"));
        List<Map<String, Object>> output2 = asList(raw.get("output2"));
        if (output1 != null) {
            String name = pickString(output1, "hts_kor_isnm", "stck_nm", "stck_isnm", "prdt_name", "kor_isnm", "isu_nm", "name");
            if (StringUtils.hasText(name)) {
                response.put("name", name);
            }
        }

        List<Map<String, Object>> points = buildPoints(output2);
        points.sort(Comparator.comparingLong(p -> ((Number) p.getOrDefault("ts", 0L)).longValue()));
        if ("KR".equals(safeMarket) && isMinuteTf(safeTf) && !"1m".equals(safeTf)) {
            int step = Integer.parseInt(safeTf.replace("m", ""));
            points = downsample(points, step);
        }

        response.put("points", points);
        return response;
    }

    @Override
    public Map<String, Object> getDomesticBalance() {
        return apiClient.fetchDomesticBalance();
    }

    @Override
    public Map<String, Object> getOverseasBalance(String exchange, String currency) {
        String exch = normalizeExchange(exchange);
        String cur = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD";
        return apiClient.fetchOverseasBalance(exch, cur);
    }

    @Override
    public Map<String, Object> getOverseasCash(String currency) {
        String cur = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD";
        return apiClient.fetchOverseasCash(cur);
    }

    private String normalizeTime(String fromTime) {
        if (!StringUtils.hasText(fromTime)) {
            return "090000";
        }
        String digits = fromTime.replaceAll("[^0-9]", "");
        if (digits.length() == 4) {
            return digits + "00";
        }
        if (digits.length() >= 6) {
            return digits.substring(0, 6);
        }
        return "090000";
    }

    private String normalizeMarket(String market) {
        if (!StringUtils.hasText(market)) {
            return "KR";
        }
        String safe = market.trim().toUpperCase();
        if (safe.startsWith("US")) {
            return "US";
        }
        return "KR";
    }

    private String normalizeExchange(String exchange) {
        if (!StringUtils.hasText(exchange)) {
            return "NAS";
        }
        String safe = exchange.trim().toUpperCase();
        if (safe.startsWith("NY")) {
            return "NYS";
        }
        if (safe.startsWith("AM")) {
            return "AMS";
        }
        return "NAS";
    }

    private String normalizeTimeframe(String timeframe) {
        if (!StringUtils.hasText(timeframe)) {
            return "1m";
        }
        String safe = timeframe.trim().toLowerCase();
        switch (safe) {
            case "1m":
            case "5m":
            case "15m":
            case "30m":
            case "60m":
            case "1d":
            case "1w":
            case "1mon":
            case "1mth":
            case "1mo":
                return safe;
            default:
                return "1m";
        }
    }

    private boolean isMinuteTf(String tf) {
        return tf.endsWith("m") && !tf.equals("1mon") && !tf.equals("1mo") && !tf.equals("1mth");
    }

    private String mapPeriod(String tf) {
        if ("1w".equals(tf)) {
            return "W";
        }
        if ("1mon".equals(tf) || "1mo".equals(tf) || "1mth".equals(tf)) {
            return "M";
        }
        return "D";
    }

    private String mapGubn(String tf) {
        if ("1w".equals(tf)) {
            return "1";
        }
        if ("1mon".equals(tf) || "1mo".equals(tf) || "1mth".equals(tf)) {
            return "2";
        }
        return "0";
    }

    private List<Map<String, Object>> buildPoints(List<Map<String, Object>> rows) {
        List<Map<String, Object>> points = new ArrayList<>();
        if (rows == null) {
            return points;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            String date = pickString(row, "xymd", "stck_bsop_date", "bsop_date", "date");
            String time = pickString(row, "xhms", "stck_cntg_hour", "cntg_hour", "time");
            String display = buildDisplayTime(date, time);
            long ts = buildSortKey(date, time);
            Double price = pickDouble(row, "stck_prpr", "stck_clpr", "prpr", "last", "last_pr", "last_prpr", "close", "clos", "ovrs_prpr");
            if (price == null) {
                continue;
            }
            Map<String, Object> point = new HashMap<>();
            point.put("time", display);
            point.put("price", price);
            point.put("ts", ts);
            points.add(point);
        }
        return points;
    }

    private List<Map<String, Object>> downsample(List<Map<String, Object>> points, int step) {
        if (points == null || points.isEmpty() || step <= 1) {
            return points;
        }
        List<Map<String, Object>> sampled = new ArrayList<>();
        for (int i = 0; i < points.size(); i += step) {
            sampled.add(points.get(i));
        }
        return sampled;
    }

    private String buildDisplayTime(String date, String time) {
        if (StringUtils.hasText(date) && StringUtils.hasText(time)) {
            return formatDate(date) + " " + formatTime(time);
        }
        if (StringUtils.hasText(time)) {
            return formatTime(time);
        }
        if (StringUtils.hasText(date)) {
            return formatDate(date);
        }
        return "";
    }

    private long buildSortKey(String date, String time) {
        String d = StringUtils.hasText(date) ? date.replaceAll("[^0-9]", "") : "";
        String t = StringUtils.hasText(time) ? time.replaceAll("[^0-9]", "") : "";
        String key = d + t;
        if (key.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(key);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String formatDate(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
        }
        return raw;
    }

    private String formatTime(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) {
            return digits.substring(0, 2) + ":" + digits.substring(2, 4);
        }
        return raw;
    }

    private String pickString(Map<String, Object> row, String... keys) {
        if (row == null) {
            return "";
        }
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                String text = value.toString();
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return "";
    }

    private Double pickDouble(Map<String, Object> row, String... keys) {
        if (row == null) {
            return null;
        }
        for (String key : keys) {
            Object value = row.get(key);
            if (value == null) {
                continue;
            }
            try {
                String text = value.toString().replace(",", "");
                if (!StringUtils.hasText(text)) {
                    continue;
                }
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return null;
    }
}
