package com.autotrading.market;

import com.autotrading.model.StockQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    private final KoreaInvestmentApiClient client;

    public MarketDataService(KoreaInvestmentApiClient client) {
        this.client = client;
    }

    public StockQuote fetchPrice(String symbol) {
        StockQuote quote;
        if (isOverseas(symbol)) {
            // 알파벳으로 시작하면 미국 종목 — 거래소는 NAS 기본값, 필요시 파라미터로 받을 수 있음
            String exchange = resolveExchange(symbol);
            quote = client.fetchOverseasCurrentPrice(symbol, exchange);
        } else {
            // 숫자로 시작하면 국내 종목
            quote = client.fetchCurrentMarketPrice(symbol);
        }
        logger.debug("Fetched market quote {} -> {}", symbol, quote.getPrice());
        return quote;
    }

    /**
     * 종목코드가 알파벳으로 시작하면 해외 종목으로 판단
     */
    private boolean isOverseas(String symbol) {
        if (symbol == null || symbol.isEmpty()) return false;
        return Character.isLetter(symbol.charAt(0));
    }

    /**
     * 심볼 기반 거래소 추정 (필요시 DB나 watchlist에서 가져오도록 확장 가능)
     * 현재는 NAS 기본값 사용
     */
    private String resolveExchange(String symbol) {
        // NYS 상장 종목 예시 (필요에 따라 확장)
        switch (symbol.toUpperCase()) {
            case "BRK.A": case "BRK.B":
            case "JPM": case "BAC": case "WMT":
                return "NYS";
            default:
                return "NAS"; // NASDAQ 기본값
        }
    }
}