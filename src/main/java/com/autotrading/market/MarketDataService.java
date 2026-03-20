package com.autotrading.market;

import com.autotrading.model.StockQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");
    private static final int OVERSEAS_TIME_FALLBACK_MAX_STALE_MINUTES = 2;
    private static final boolean ALLOW_CROSS_EXCHANGE_FALLBACK = false;
    private static final double OVERSEAS_MAX_PRICE_JUMP_PCT = 0.10; // +-10% in short interval is treated as suspicious.
    private static final int PRICE_JUMP_GUARD_RESET_MINUTES = 30;

    private final KoreaInvestmentApiClient client;
    private final Map<String, String> exchangeCache = new ConcurrentHashMap<>();
    private final Map<String, PriceSnapshot> overseasLastPriceCache = new ConcurrentHashMap<>();

    private static class PriceSnapshot {
        final double price;
        final LocalDateTime timestamp;

        PriceSnapshot(double price, LocalDateTime timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    public MarketDataService(KoreaInvestmentApiClient client) {
        this.client = client;
    }

    public StockQuote fetchPrice(String symbol) {
        return fetchPrice(symbol, null);
    }

    public StockQuote fetchPrice(String symbol, String exchangeHint) {
        StockQuote quote;
        if (isOverseas(symbol, exchangeHint)) {
            String preferred = resolveExchange(symbol, exchangeHint);
            quote = fetchOverseasWithFallback(symbol, preferred);
        } else {
            quote = client.fetchCurrentMarketPrice(symbol);
        }
        logger.debug("Fetched market quote {} -> {}", symbol, quote.getPrice());
        return quote;
    }

    public String resolveExchangeForOrder(String symbol) {
        if (!isOverseas(symbol, null)) {
            return null;
        }
        String cached = exchangeCache.get(symbol);
        if (cached != null && !cached.isBlank()) {
            return toOrderExchange(cached);
        }
        return toOrderExchange(resolveExchange(symbol, null));
    }

    private StockQuote fetchOverseasWithFallback(String symbol, String preferred) {
        List<String> candidates = buildCandidateExchanges(symbol, preferred);
        IllegalStateException last = null;
        for (String exchange : candidates) {
            try {
                StockQuote quote = client.fetchOverseasCurrentPrice(symbol, exchange);
                if (!validateOverseasQuote(symbol, quote, "PRICE_DETAIL")) {
                    throw new IllegalStateException("Price-detail validation failed.");
                }
                String preferredQuoteExchange = StringUtils.hasText(preferred) ? toQuoteExchange(preferred) : null;
                if (StringUtils.hasText(preferredQuoteExchange)
                        && !preferredQuoteExchange.equalsIgnoreCase(exchange)) {
                    logger.info("Overseas exchange corrected for {}: {} -> {}", symbol, preferredQuoteExchange, exchange);
                }
                exchangeCache.put(symbol, exchange);
                return quote;
            } catch (IllegalStateException e) {
                last = e;
                logger.debug("Overseas quote retry {} on {} failed: {}", symbol, exchange, e.getMessage());

                // price-detail 응답이 비어 있는 종목은 1분봉/일봉으로 마지막 가격을 보정 시도
                StockQuote fallback = fallbackOverseasQuoteFromCharts(symbol, exchange);
                if (fallback != null && validateOverseasQuote(symbol, fallback, "TIME_CHART_FALLBACK")) {
                    // Do not cache exchange from fallback path.
                    // Fallback data can be partial and should not override primary exchange routing.
                    logger.warn("Overseas quote fallback from chart used for {} on {}", symbol, exchange);
                    return fallback;
                }
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("Overseas market price fetch failed.");
    }

    private StockQuote fallbackOverseasQuoteFromCharts(String symbol, String exchange) {
        StockQuote minute = fromOverseasTimeChart(symbol, exchange);
        if (minute != null) {
            return minute;
        }
        // Daily fallback is intentionally disabled for live entry decisions.
        // Using daily close as "now" price can lead to severe mis-trades.
        return null;
    }

    private StockQuote fromOverseasTimeChart(String symbol, String exchange) {
        Map<String, Object> raw = client.fetchOverseasTimeChart(symbol, exchange, "1");
        if (!"OK".equals(String.valueOf(raw.getOrDefault("status", "ERROR")))) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw.get("output2");
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        rows.sort(Comparator.comparingLong(this::timeSortKey));
        for (int i = rows.size() - 1; i >= 0; i--) {
            Map<String, Object> row = rows.get(i);
            LocalDateTime rowTs = parseOverseasRowDateTime(row);
            if (rowTs == null || !isFreshOverseasRow(rowTs, OVERSEAS_TIME_FALLBACK_MAX_STALE_MINUTES)) {
                continue;
            }
            Double price = pickDouble(row, "last", "stck_prpr", "clos", "close", "ovrs_prpr");
            if (price == null || price <= 0) {
                continue;
            }
            Double volParsed = pickDouble(row, "tvol", "evol", "acml_vol", "cntg_vol", "volume");
            double vol = volParsed != null ? volParsed : 0d;
            return new StockQuote(symbol, resolveQuoteName(raw, symbol), price, vol, rowTs);
        }
        logger.warn("Overseas time-chart fallback stale/invalid for {} on {}", symbol, exchange);
        return null;
    }

    private StockQuote fromOverseasDaily(String symbol, String exchange) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Map<String, Object> raw = client.fetchOverseasDailyPrice(symbol, exchange, "0", today);
        if (!"OK".equals(String.valueOf(raw.getOrDefault("status", "ERROR")))) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) raw.get("output2");
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Map<String, Object> first = rows.get(0);
        Double price = pickDouble(first, "clos", "close", "last", "stck_prpr", "ovrs_prpr", "base");
        if (price == null || price <= 0) {
            return null;
        }
        Double vol = pickDouble(first, "tvol", "evol", "acml_vol", "volume");
        return new StockQuote(symbol, resolveQuoteName(raw, symbol), price, vol != null ? vol : 0d, LocalDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private String resolveQuoteName(Map<String, Object> raw, String defaultName) {
        if (raw == null) {
            return defaultName;
        }
        Object output1 = raw.get("output1");
        if (output1 instanceof Map) {
            String name = pickString((Map<String, Object>) output1,
                    "ovrs_item_name", "ovrs_item_kor_name", "hts_kor_isnm", "stck_isnm", "prdt_name", "name");
            if (StringUtils.hasText(name)) {
                return name;
            }
        }
        return defaultName;
    }

    private long timeSortKey(Map<String, Object> row) {
        String d = pickString(row, "xymd", "date", "stck_bsop_date");
        String t = pickString(row, "xhms", "time", "stck_cntg_hour");
        String digits = (d + t).replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String pickString(Map<String, Object> row, String... keys) {
        if (row == null) {
            return "";
        }
        for (String key : keys) {
            Object v = row.get(key);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (StringUtils.hasText(s) && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return "";
    }

    private Double pickDouble(Map<String, Object> row, String... keys) {
        String s = pickString(row, keys);
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Double.parseDouble(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> buildCandidateExchanges(String symbol, String preferred) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (preferred != null && !preferred.isBlank()) {
            set.add(toQuoteExchange(preferred));
        }
        // Safe correction path: if an explicit hint is wrong, retry once using internal symbol mapping.
        // This avoids broad cross-exchange fallback while recovering common NAS/NYS mismatches.
        String mapped = resolveExchange(symbol, null);
        if (mapped != null && !mapped.isBlank()) {
            set.add(toQuoteExchange(mapped));
        }
        if (ALLOW_CROSS_EXCHANGE_FALLBACK) {
            set.add("NAS");
            set.add("AMS");
            set.add("NYS");
        } else if (set.isEmpty()) {
            // Keep a single default exchange only when absolutely no hint exists.
            set.add("NAS");
        }
        return new ArrayList<>(set);
    }

    private LocalDateTime parseOverseasRowDateTime(Map<String, Object> row) {
        String d = pickString(row, "xymd", "date", "stck_bsop_date");
        String t = pickString(row, "xhms", "time", "stck_cntg_hour");
        String dDigits = d.replaceAll("[^0-9]", "");
        String tDigits = t.replaceAll("[^0-9]", "");
        if (dDigits.length() < 8) {
            return null;
        }
        String ymd = dDigits.substring(0, 8);
        String hms;
        if (tDigits.length() >= 6) {
            hms = tDigits.substring(0, 6);
        } else if (tDigits.length() == 4) {
            hms = tDigits + "00";
        } else if (tDigits.length() == 2) {
            hms = tDigits + "0000";
        } else {
            return null;
        }
        try {
            return LocalDateTime.parse(ymd + hms, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isFreshOverseasRow(LocalDateTime rowTs, int maxStaleMinutes) {
        ZonedDateTime rowNy = rowTs.atZone(NY_ZONE);
        long diffNy = Math.abs(Duration.between(rowNy, ZonedDateTime.now(NY_ZONE)).toMinutes());
        return diffNy <= maxStaleMinutes;
    }

    private boolean isOverseas(String symbol, String exchangeHint) {
        if (symbol == null || symbol.isEmpty()) {
            return false;
        }
        if (StringUtils.hasText(exchangeHint)) {
            String ex = exchangeHint.trim().toUpperCase();
            if ("KRX".equals(ex) || "KR".equals(ex) || "KOSPI".equals(ex) || "KOSDAQ".equals(ex)) {
                return false;
            }
            if ("NAS".equals(ex) || "NASD".equals(ex) || "NYS".equals(ex) || "NYSE".equals(ex)
                    || "AMS".equals(ex) || "AMEX".equals(ex)) {
                return true;
            }
        }
        return !symbol.matches("^\\d{5,6}$");
    }

    private boolean validateOverseasQuote(String symbol, StockQuote quote, String source) {
        if (quote == null) {
            return false;
        }
        double price = quote.getPrice();
        if (!Double.isFinite(price) || price <= 0d) {
            logger.warn("Invalid overseas quote [{}] for {}: price={}", source, symbol, price);
            return false;
        }
        double volume = quote.getVolume();
        if (!Double.isFinite(volume) || volume < 0d) {
            logger.warn("Invalid overseas quote [{}] for {}: volume={}", source, symbol, volume);
            return false;
        }
        LocalDateTime ts = quote.getTimestamp() != null ? quote.getTimestamp() : LocalDateTime.now(NY_ZONE);
        if (!passesPriceJumpGuard(symbol, price, ts, source)) {
            return false;
        }
        return true;
    }

    private boolean passesPriceJumpGuard(String symbol, double currentPrice, LocalDateTime currentTs, String source) {
        PriceSnapshot prev = overseasLastPriceCache.get(symbol);
        if (prev == null || prev.price <= 0d || prev.timestamp == null) {
            overseasLastPriceCache.put(symbol, new PriceSnapshot(currentPrice, currentTs));
            return true;
        }

        long gapMinutes = Math.abs(Duration.between(prev.timestamp, currentTs).toMinutes());
        if (gapMinutes > PRICE_JUMP_GUARD_RESET_MINUTES) {
            overseasLastPriceCache.put(symbol, new PriceSnapshot(currentPrice, currentTs));
            return true;
        }

        double jumpPct = Math.abs(currentPrice - prev.price) / prev.price;
        if (jumpPct > OVERSEAS_MAX_PRICE_JUMP_PCT) {
            logger.warn("Suspicious overseas quote [{}] rejected for {}: prev={} now={} jumpPct={}",
                    source, symbol, prev.price, currentPrice, String.format("%.2f", jumpPct * 100d));
            return false;
        }

        overseasLastPriceCache.put(symbol, new PriceSnapshot(currentPrice, currentTs));
        return true;
    }

    private String resolveExchange(String symbol, String exchangeHint) {
        if (exchangeHint != null && !exchangeHint.isBlank()) {
            return toQuoteExchange(exchangeHint);
        }
        String cached = exchangeCache.get(symbol);
        if (cached != null && !cached.isBlank()) {
            return toQuoteExchange(cached);
        }
        switch (symbol.toUpperCase()) {
            case "SOXL":
            case "ETHU":
            case "SMU":
                return "AMS";
            case "BRK.A":
            case "BRK.B":
            case "SMR":
            case "JPM":
            case "BAC":
            case "WMT":
            case "KO":
            case "DIS":
            case "MCD":
            case "JNJ":
            case "V":
            case "MA":
            case "XOM":
                return "NYS";
            default:
                return "NAS";
        }
    }

    private String toQuoteExchange(String exchange) {
        if (exchange == null) {
            return "NAS";
        }
        String upper = exchange.trim().toUpperCase();
        switch (upper) {
            case "NASD":
            case "NASDAQ":
            case "NAS":
                return "NAS";
            case "NYSE":
            case "NYS":
                return "NYS";
            case "AMEX":
            case "AMS":
                return "AMS";
            default:
                return upper;
        }
    }

    private String toOrderExchange(String exchange) {
        String upper = toQuoteExchange(exchange);
        switch (upper) {
            case "NAS":
                return "NASD";
            case "NYS":
                return "NYSE";
            case "AMS":
                return "AMEX";
            default:
                return upper;
        }
    }

    
}
