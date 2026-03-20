package com.autotrading.service.impl;

import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.service.MarketInsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashSet;

@Service
public class MarketInsightServiceImpl implements MarketInsightService {
    private static final Logger logger = LoggerFactory.getLogger(MarketInsightServiceImpl.class);
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
            List<String> exchanges = buildUsExchangeCandidates(exchange);
            List<String> diagnostics = new ArrayList<>();
            for (String exch : exchanges) {
                Map<String, Object> raw = apiClient.fetchOverseasTradeVolume(exch);
                List<Map<String, Object>> rows = asList(raw.get("output2"));
                if (rows == null || rows.isEmpty()) {
                    rows = asList(raw.get("data"));
                }
                if (rows == null || rows.isEmpty()) {
                    diagnostics.add(exch + ": status=" + raw.getOrDefault("status", "?")
                            + ", msg=" + raw.getOrDefault("message", "")
                            + ", nday=" + raw.getOrDefault("nday", "?"));
                    continue;
                }

                List<Map<String, Object>> normalized = normalizeUsRankingRows(rows);
                if (normalized.isEmpty()) {
                    diagnostics.add(exch + ": rows=" + rows.size() + ", normalized=0, nday=" + raw.getOrDefault("nday", "?"));
                    continue;
                }

                Map<String, Object> result = new HashMap<>();
                result.put("status", "OK");
                result.put("message", raw.getOrDefault("message", "OK"));
                result.put("exchange", exch);
                result.put("data", normalized);
                return result;
            }

            return Map.of(
                    "status", "ERROR",
                    "message", diagnostics.isEmpty()
                            ? "해외 랭킹 데이터가 없습니다."
                            : "해외 랭킹 데이터가 없습니다. " + String.join(" | ", diagnostics),
                    "exchange", normalizeExchange(exchange),
                    "data", List.of()
            );
        }

        Map<String, Object> raw = apiClient.fetchVolumeRanking();
        List<Map<String, Object>> rows = asList(raw.get("output"));
        if (rows == null || rows.isEmpty()) {
            return Map.of("status", "ERROR", "message", "국내 랭킹 데이터가 없습니다.", "data", List.of());
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> item = new HashMap<>();
            String price = pickString(r, "stck_prpr");
            String volume = pickString(r, "acml_vol");
            double tradeValue = resolveTradeValue(
                    pickDouble(r, "acml_tr_pbmn", "stck_tr_pbmn", "tr_pbmn", "trde_amt"),
                    asDouble(price, 0d),
                    asDouble(volume, 0d)
            );
            item.put("symbol", pickString(r, "mksc_shrn_iscd"));
            item.put("name", pickString(r, "hts_kor_isnm"));
            item.put("stck_prpr", price);
            item.put("prdy_ctrt", pickString(r, "prdy_ctrt"));
            item.put("prdy_vrss_sign", pickString(r, "prdy_vrss_sign"));
            // Keep legacy key(acml_vol) for existing screens, but rank/display by traded value.
            item.put("acml_tr_pbmn", formatRankMetric(tradeValue));
            item.put("acml_vol", formatRankMetric(tradeValue));
            item.put("acml_vol_raw", volume);
            normalized.add(item);
        }
        normalized.sort(Comparator.comparingDouble(this::rankingAmount).reversed());

        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("message", raw.getOrDefault("message", "정상처리 되었습니다."));
        result.put("exchange", "KRX");
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
    public Map<String, Object> getMarketIndex() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            Map<String, Object> kospi = asMap(apiClient.fetchDomesticIndex("0001").get("output"));
            appendIndex(result, "KOSPI", kospi,
                    new String[]{"bstp_nmix_prpr", "stck_prpr"},
                    new String[]{"bstp_nmix_prdy_ctrt", "prdy_ctrt"},
                    new String[]{"bstp_nmix_prdy_vrss", "prdy_vrss"});
        } catch (Exception e) {
            logger.warn("Failed to fetch KOSPI index", e);
            appendIndex(result, "KOSPI", null, new String[0], new String[0], new String[0]);
        }

        try {
            Map<String, Object> kosdaq = asMap(apiClient.fetchDomesticIndex("1001").get("output"));
            appendIndex(result, "KOSDAQ", kosdaq,
                    new String[]{"bstp_nmix_prpr", "stck_prpr"},
                    new String[]{"bstp_nmix_prdy_ctrt", "prdy_ctrt"},
                    new String[]{"bstp_nmix_prdy_vrss", "prdy_vrss"});
        } catch (Exception e) {
            logger.warn("Failed to fetch KOSDAQ index", e);
            appendIndex(result, "KOSDAQ", null, new String[0], new String[0], new String[0]);
        }

        String[][] usIndices = {{"S", "S&P 500"}, {"N", "NASDAQ"}, {"D", "DOW"}};
        for (String[] idx : usIndices) {
            try {
                Map<String, Object> out = asMap(apiClient.fetchOverseasMajorIndex(idx[0]).get("output"));
                appendIndex(result, idx[1], out,
                        new String[]{"ovrs_nmix_prpr", "last", "stck_prpr", "price"},
                        new String[]{"prdy_ctrt", "diff_rate"},
                        new String[]{"ovrs_nmix_prdy_vrss", "prdy_vrss", "point"});
            } catch (Exception e) {
                logger.warn("Failed to fetch overseas index {}", idx[1], e);
                appendIndex(result, idx[1], null, new String[0], new String[0], new String[0]);
            }
        }

        try {
            Map<String, Object> fx = asMap(apiClient.fetchUsdKrwExchange().get("output"));
            appendIndex(result, "USD/KRW", fx,
                    new String[]{"bstp_nmix_prpr", "stck_prpr", "price"},
                    new String[]{"bstp_nmix_prdy_ctrt", "prdy_ctrt", "change"},
                    new String[]{"bstp_nmix_prdy_vrss", "prdy_vrss", "point"});
        } catch (Exception e) {
            logger.warn("Failed to fetch USD/KRW", e);
            appendIndex(result, "USD/KRW", null, new String[0], new String[0], new String[0]);
        }

        return Map.of("status", "OK", "data", result);
    }

    @Override
    public Map<String, Object> getDomesticBalance() {
        return apiClient.fetchDomesticBalance();
    }

    @Override
    public Map<String, Object> getDomesticHoldings() {
        return apiClient.fetchDomesticHoldings();
    }

    @Override
    public Map<String, Object> getDomesticDailyCcld(String startDate,
                                                    String endDate,
                                                    String sideCode,
                                                    String ccldDvsn,
                                                    String exchangeId) {
        String safeSide = normalizeListCode(sideCode, "00", "00", "01", "02");
        String safeCcld = normalizeListCode(ccldDvsn, "01", "00", "01", "02");
        String safeExchange = normalizeListCode(exchangeId, "KRX", "KRX", "NXT", "SOR", "ALL");
        return apiClient.fetchDomesticDailyCcld(
                startDate,
                endDate,
                safeSide,
                safeCcld,
                "00",
                "",
                "00",
                safeExchange,
                20
        );
    }

    @Override
    public Map<String, Object> getOverseasBalance(String exchange, String currency) {
        String exch = normalizeExchange(exchange);
        String cur = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD";
        return apiClient.fetchOverseasBalance(exch, cur);
    }

    @Override
    public Map<String, Object> getOverseasHoldings(String exchange, String currency) {
        String exch = StringUtils.hasText(exchange) ? exchange.trim().toUpperCase() : "NASD";
        String cur = StringUtils.hasText(currency) ? currency.trim().toUpperCase() : "USD";
        return apiClient.fetchOverseasHoldings(exch, cur);
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

    private String normalizeListCode(String value, String defaultValue, String... allowed) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String safe = value.trim().toUpperCase();
        for (String allow : allowed) {
            if (safe.equals(allow)) {
                return safe;
            }
        }
        return defaultValue;
    }

    private List<String> buildUsExchangeCandidates(String exchange) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addUsExchangeCandidate(candidates, exchange);
        addUsExchangeCandidate(candidates, "NAS");
        addUsExchangeCandidate(candidates, "NYS");
        addUsExchangeCandidate(candidates, "AMS");
        return new ArrayList<>(candidates);
    }

    private void addUsExchangeCandidate(LinkedHashSet<String> candidates, String exchange) {
        if (!StringUtils.hasText(exchange)) {
            return;
        }
        String upper = exchange.trim().toUpperCase();
        switch (upper) {
            case "NAS":
            case "NASD":
            case "NASDAQ":
                candidates.add("NAS");
                candidates.add("NASD");
                break;
            case "NYS":
            case "NYSE":
                candidates.add("NYS");
                candidates.add("NYSE");
                break;
            case "AMS":
            case "AMEX":
                candidates.add("AMS");
                candidates.add("AMEX");
                break;
            default:
                candidates.add(upper);
                break;
        }
    }

    private List<Map<String, Object>> normalizeUsRankingRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }

            String symbol = pickString(row, "symbol", "symb", "ovrs_pdno", "pdno", "mksc_shrn_iscd");
            if (!StringUtils.hasText(symbol)) {
                String rawSymbol = pickString(row, "rsym");
                if (StringUtils.hasText(rawSymbol)) {
                    symbol = rawSymbol.length() > 4 ? rawSymbol.substring(4) : rawSymbol;
                }
            }
            if (!StringUtils.hasText(symbol)) {
                continue;
            }
            symbol = symbol.trim().toUpperCase();
            if (!seen.add(symbol)) {
                continue;
            }

            String name = pickString(row, "name", "ovrs_item_name", "ovrs_item_kor_name", "hts_kor_isnm");
            String price = pickString(row, "stck_prpr", "last", "ovrs_prpr", "close", "clos", "prpr");
            String rate = pickString(row, "prdy_ctrt", "diff_rate", "rate", "prdy_vrss_rt");
            String sign = pickString(row, "prdy_vrss_sign", "diff_sign", "sign");
            String volume = pickString(row, "acml_vol", "tvol", "evol", "vol", "avol", "volume");
            double tradeValue = resolveTradeValue(
                    pickDouble(row, "tr_pbmn", "acml_tr_pbmn", "ovrs_tr_pbmn", "trade_pbmn", "trade_value", "trde_amt", "tot_amt"),
                    asDouble(price, 0d),
                    asDouble(volume, 0d)
            );

            if (!StringUtils.hasText(rate)) {
                rate = "0";
            }
            if (!StringUtils.hasText(sign)) {
                sign = "3";
                try {
                    double parsedRate = Double.parseDouble(rate.replace(",", "").replace("%", ""));
                    sign = parsedRate > 0 ? "2" : (parsedRate < 0 ? "5" : "3");
                } catch (NumberFormatException ignored) {
                }
            }
            if (!StringUtils.hasText(volume)) {
                volume = "0";
            }

            Map<String, Object> item = new HashMap<>();
            item.put("symbol", symbol);
            item.put("name", name);
            item.put("stck_prpr", price);
            item.put("prdy_ctrt", rate);
            item.put("prdy_vrss_sign", sign);
            // Keep legacy key(acml_vol) for existing screens, but rank/display by traded value.
            item.put("acml_tr_pbmn", formatRankMetric(tradeValue));
            item.put("acml_vol", formatRankMetric(tradeValue));
            item.put("acml_vol_raw", volume);
            normalized.add(item);
        }

        normalized.sort(Comparator.comparingDouble(this::rankingAmount).reversed());
        return normalized;
    }

    private double resolveTradeValue(Double apiTradeValue, double price, double volume) {
        if (apiTradeValue != null && apiTradeValue > 0) {
            return apiTradeValue;
        }
        if (price > 0 && volume > 0) {
            return price * volume;
        }
        return 0d;
    }

    private String formatRankMetric(double value) {
        return String.valueOf(Math.round(Math.max(0d, value)));
    }

    private double rankingAmount(Map<String, Object> row) {
        if (row == null) {
            return 0d;
        }
        return asDouble(row.get("acml_tr_pbmn"), asDouble(row.get("acml_vol"), 0d));
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
            Double close = pickDouble(row,
                    "stck_prpr", "stck_clpr", "prpr", "last", "last_pr", "last_prpr",
                    "close", "clos", "ovrs_prpr");
            if (close == null) {
                continue;
            }

            Double open = pickDouble(row, "stck_oprc", "open", "ovrs_oprc", "oprc");
            Double high = pickDouble(row, "stck_hgpr", "high", "ovrs_hgpr", "hgpr");
            Double low = pickDouble(row, "stck_lwpr", "low", "ovrs_lwpr", "lwpr");
            Double volume = pickDouble(row, "cntg_vol", "trde_qty", "vol", "volume", "acml_vol", "tvol", "evol");

            double o = open != null ? open : close;
            double h = high != null ? high : close;
            double l = low != null ? low : close;
            double c = close;
            double v = volume != null ? volume : 0d;

            Map<String, Object> point = new HashMap<>();
            point.put("time", display);
            point.put("open", o);
            point.put("high", h);
            point.put("low", l);
            point.put("close", c);
            point.put("price", c);
            point.put("volume", v);
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
            int end = Math.min(i + step, points.size());
            Map<String, Object> first = points.get(i);
            Map<String, Object> last = points.get(end - 1);

            double open = asDouble(first.get("open"), asDouble(first.get("price"), 0d));
            double close = asDouble(last.get("close"), asDouble(last.get("price"), open));
            double high = open;
            double low = open;
            double volume = 0d;

            for (int j = i; j < end; j++) {
                Map<String, Object> p = points.get(j);
                double ph = asDouble(p.get("high"), asDouble(p.get("price"), close));
                double pl = asDouble(p.get("low"), asDouble(p.get("price"), close));
                high = Math.max(high, ph);
                low = Math.min(low, pl);
                volume += Math.max(0d, asDouble(p.get("volume"), 0d));
            }

            Map<String, Object> bar = new HashMap<>();
            bar.put("time", first.get("time"));
            bar.put("ts", first.get("ts"));
            bar.put("open", open);
            bar.put("high", high);
            bar.put("low", low);
            bar.put("close", close);
            bar.put("price", close);
            bar.put("volume", volume);
            sampled.add(bar);
        }
        return sampled;
    }

    private double asDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(value).replace(",", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void appendIndex(List<Map<String, Object>> result,
                             String name,
                             Map<String, Object> row,
                             String[] priceKeys,
                             String[] changeKeys,
                             String[] pointKeys) {
        double price = 0d;
        double change = 0d;
        double point = 0d;
        if (row != null && !row.isEmpty()) {
            Double p = pickDouble(row, priceKeys);
            Double c = pickDouble(row, changeKeys);
            Double pt = pickDouble(row, pointKeys);
            price = p != null ? p : 0d;
            change = c != null ? c : 0d;
            point = pt != null ? pt : 0d;
        }

        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("price", price);
        item.put("change", change);
        item.put("point", point);
        result.add(item);
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
