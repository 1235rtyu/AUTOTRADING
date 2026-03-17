package com.autotrading.market;

import com.autotrading.config.KisProperties;
import com.autotrading.model.AccountCredential;
import com.autotrading.model.OrderCommand;
import com.autotrading.model.StockQuote;
import com.autotrading.util.AccountCredentialStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KoreaInvestmentApiClient {
    private static final Logger logger = LoggerFactory.getLogger(KoreaInvestmentApiClient.class);
    private static final DateTimeFormatter TOKEN_EXPIRY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KisProperties properties;
    private final AccountCredentialStore credentialStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt;

    public KoreaInvestmentApiClient(KisProperties properties, AccountCredentialStore credentialStore) {
        this.properties = properties;
        this.credentialStore = credentialStore;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public synchronized String generateAccessToken() {
        if (!properties.isConfigured()) {
            logger.error("KIS appKey/appSecret not configured. Check kis.properties.");
            return null;
        }
        Instant now = Instant.now();
        if (StringUtils.hasText(accessToken) && accessTokenExpiresAt != null && accessTokenExpiresAt.isAfter(now.plusSeconds(30))) {
            return accessToken;
        }

        String url = properties.getBaseUrl() + "/oauth2/tokenP";
        Map<String, String> payload = new HashMap<>();
        payload.put("grant_type", "client_credentials");
        payload.put("appkey", properties.getAppKey());
        payload.put("appsecret", properties.getAppSecret());

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.error("Token request failed. status={} body={}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String token = root.path("access_token").asText(null);
            if (!StringUtils.hasText(token)) {
                logger.error("Token response missing access_token. body={}", response.body());
                return null;
            }
            accessToken = token;
            accessTokenExpiresAt = resolveTokenExpiry(root);
            logger.info("KIS access token refreshed. expiresAt={}", accessTokenExpiresAt);
            return accessToken;
        } catch (Exception e) {
            logger.error("Token request failed", e);
            return null;
        }
    }

    public StockQuote fetchCurrentMarketPrice(String symbol) {
        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("KIS access token unavailable.");
        }
        String baseUrl = properties.getBaseUrl();
        String query = "FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price?" + query;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "FHKST01010100")
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Price request failed. status=" + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode output = root.path("output");
            String priceText = output.path("stck_prpr").asText();
            if (!StringUtils.hasText(priceText)) {
                throw new IllegalStateException("Price response missing stck_prpr.");
            }
            double price = Double.parseDouble(priceText.replace(",", ""));
            double volume = 0;
            String volText = output.path("acml_vol").asText();
            if (StringUtils.hasText(volText)) {
                try { volume = Double.parseDouble(volText.replace(",", "")); } catch (NumberFormatException ignored) {}
            }
            return new StockQuote(symbol, price, volume, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Failed to fetch market price for {}", symbol, e);
            throw new IllegalStateException("Market price fetch failed.", e);
        }
    }

    public Map<String, Object> fetchHtsTopView() {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }
        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String url = properties.getBaseUrl() + "/uapi/domestic-stock/v1/ranking/hts-top-view";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "HHMCM000100C0")
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            List<Map<String, Object>> data = objectMapper.convertValue(root.path("output1"), List.class);
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("data", data);
            return result;
        } catch (Exception e) {
            logger.error("HTS top view request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Ranking request failed: " + e.getMessage());
            return result;
        }
    }


public StockQuote fetchOverseasCurrentPrice(String symbol, String exchange) {
    String token = generateAccessToken();
    if (!StringUtils.hasText(token)) {
        throw new IllegalStateException("KIS access token unavailable.");
    }

    String query = "AUTH="
            + "&EXCD=" + URLEncoder.encode(exchange, StandardCharsets.UTF_8)
            + "&SYMB=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
    String url = properties.getBaseUrl()
            + "/uapi/overseas-price/v1/quotations/price-detail?" + query;

    try {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("authorization", "Bearer " + token)
                .header("appkey",    properties.getAppKey())
                .header("appsecret", properties.getAppSecret())
                .header("tr_id",     "HHDFS76200200")
                .header("custtype",  properties.getCustomerType())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode output = root.path("output");

        // 시간외 가격 우선, 없으면 정규장 가격
        String priceText = output.path("e_last").asText("");
        if (!StringUtils.hasText(priceText) || "0".equals(priceText) || "0.00".equals(priceText)) {
            priceText = output.path("last").asText("");
        }
        if (!StringUtils.hasText(priceText)) {
            throw new IllegalStateException("Price response missing price field.");
        }

        double price = Double.parseDouble(priceText.replace(",", ""));
        double volume = 0;
        String volText = output.path("tvol").asText("");
        if (!StringUtils.hasText(volText)) {
            volText = output.path("atota").asText(""); // fallback if 필드명 상이
        }
        if (StringUtils.hasText(volText)) {
            try { volume = Double.parseDouble(volText.replace(",", "")); } catch (NumberFormatException ignored) {}
        }
        return new StockQuote(symbol, price, volume, java.time.LocalDateTime.now());
    } catch (Exception e) {
        logger.error("Failed to fetch overseas price for {}", symbol, e);
        throw new IllegalStateException("Overseas market price fetch failed.", e);
    }
}




    /**
     * 거래량 상위 랭킹 (간단 래퍼)
     * KIS에 별도 엔드포인트가 없다면 HTS Top View 결과를 그대로 반환해
     * 서비스 단에서 공통 포맷으로 소비할 수 있게 한다.
     */
// 수정 — 거래량순위 전용 API 직접 호출
public Map<String, Object> fetchVolumeRanking() {
    Map<String, Object> result = new HashMap<>();
    if (!properties.isConfigured()) {
        result.put("status", "ERROR");
        result.put("message", "KIS appKey/appSecret not configured.");
        return result;
    }
    String token = generateAccessToken();
    if (!StringUtils.hasText(token)) {
        result.put("status", "ERROR");
        result.put("message", "KIS access token unavailable.");
        return result;
    }

    String query = "FID_COND_MRKT_DIV_CODE=J"
            + "&FID_COND_SCR_DIV_CODE=20171"
            + "&FID_INPUT_ISCD=0000"
            + "&FID_DIV_CLS_CODE=0"
            + "&FID_BLNG_CLS_CODE=0"
            + "&FID_TRGT_CLS_CODE=111111111"
            + "&FID_TRGT_EXLS_CLS_CODE=0000000000"
            + "&FID_INPUT_PRICE_1="
            + "&FID_INPUT_PRICE_2="
            + "&FID_VOL_CNT="
            + "&FID_INPUT_DATE_1=";

    String url = properties.getBaseUrl()
            + "/uapi/domestic-stock/v1/quotations/volume-rank?" + query;
    try {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("authorization", "Bearer " + token)
                .header("appkey",    properties.getAppKey())
                .header("appsecret", properties.getAppSecret())
                .header("tr_id",     "FHPST01710000")
                .header("custtype",  properties.getCustomerType())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        String rtCd = root.path("rt_cd").asText();

        // KIS 거래량순위는 output 배열로 반환
        List<Map<String, Object>> output = objectMapper.convertValue(
                root.path("output"), List.class);

        result.put("status",  "0".equals(rtCd) ? "OK" : "ERROR");
        result.put("message", root.path("msg1").asText(""));
        result.put("output",  output != null ? output : List.of());
        return result;
    } catch (Exception e) {
        logger.error("Volume ranking request failed", e);
        result.put("status",  "ERROR");
        result.put("message", "Volume ranking request failed: " + e.getMessage());
        result.put("output",  List.of());
        return result;
    }
}

    public Map<String, Object> fetchTimeChart(String symbol, String fromTime) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }
        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String query = "FID_COND_MRKT_DIV_CODE=J" +
                "&FID_INPUT_ISCD=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
                "&FID_INPUT_HOUR_1=" + URLEncoder.encode(fromTime, StandardCharsets.UTF_8) +
                "&FID_PW_DATA_INCU_YN=Y" +
                "&FID_ETC_CLS_CODE=0";
        String url = properties.getBaseUrl() + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice?" + query;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "FHKST03010200")
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            Map<String, Object> output1 = objectMapper.convertValue(root.path("output1"), Map.class);
            List<Map<String, Object>> output2 = objectMapper.convertValue(root.path("output2"), List.class);
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("output1", output1);
            result.put("output2", output2);
            return result;
        } catch (Exception e) {
            logger.error("Time chart request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Chart request failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> fetchDomesticDailyChart(String symbol, String periodCode, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }
        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String query = "FID_COND_MRKT_DIV_CODE=J" +
                "&FID_INPUT_ISCD=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
                "&FID_INPUT_DATE_1=" + URLEncoder.encode(startDate, StandardCharsets.UTF_8) +
                "&FID_INPUT_DATE_2=" + URLEncoder.encode(endDate, StandardCharsets.UTF_8) +
                "&FID_PERIOD_DIV_CODE=" + URLEncoder.encode(periodCode, StandardCharsets.UTF_8) +
                "&FID_ORG_ADJ_PRC=1";
        String url = properties.getBaseUrl() + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice?" + query;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "FHKST03010100")
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            Map<String, Object> output1 = objectMapper.convertValue(root.path("output1"), Map.class);
            List<Map<String, Object>> output2 = objectMapper.convertValue(root.path("output2"), List.class);
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("output1", output1);
            result.put("output2", output2);
            return result;
        } catch (Exception e) {
            logger.error("Daily chart request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Daily chart request failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> fetchOverseasTimeChart(String symbol, String exchange, String minutes) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }
        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String query = "AUTH=" +
                "&EXCD=" + URLEncoder.encode(exchange, StandardCharsets.UTF_8) +
                "&SYMB=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
                "&NMIN=" + URLEncoder.encode(minutes, StandardCharsets.UTF_8) +
                "&PINC=0" +
                "&NEXT=" +
                "&NREC=120" +
                "&FILL=" +
                "&KEYB=";
        String url = properties.getBaseUrl() + "/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice?" + query;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "HHDFS76950200")
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            Map<String, Object> output1 = objectMapper.convertValue(root.path("output1"), Map.class);
            List<Map<String, Object>> output2 = objectMapper.convertValue(root.path("output2"), List.class);
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("output1", output1);
            result.put("output2", output2);
            return result;
        } catch (Exception e) {
            logger.error("Overseas time chart request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Overseas time chart request failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> fetchOverseasDailyPrice(String symbol, String exchange, String gubn, String baseDate) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }
        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String query = "AUTH=" +
                "&EXCD=" + URLEncoder.encode(exchange, StandardCharsets.UTF_8) +
                "&SYMB=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
                "&GUBN=" + URLEncoder.encode(gubn, StandardCharsets.UTF_8) +
                "&BYMD=" + URLEncoder.encode(baseDate, StandardCharsets.UTF_8) +
                "&MODP=1";
        String url = properties.getBaseUrl() + "/uapi/overseas-price/v1/quotations/dailyprice?" + query;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "HHDFS76240000")
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            Map<String, Object> output1 = objectMapper.convertValue(root.path("output1"), Map.class);
            List<Map<String, Object>> output2 = objectMapper.convertValue(root.path("output2"), List.class);
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("output1", output1);
            result.put("output2", output2);
            return result;
        } catch (Exception e) {
            logger.error("Overseas daily price request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Overseas daily price request failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> fetchOverseasTradeVolume(String exchange) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }
        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String query = "EXCD=" + URLEncoder.encode(exchange, StandardCharsets.UTF_8) +
                "&NDAY=0" +
                "&VOL_RANG=0" +
                "&PRC1=" +
                "&PRC2=" +
                "&KEYB=" +
                "&AUTH=";
        String url = properties.getBaseUrl() + "/uapi/overseas-stock/v1/ranking/trade-pbmn?" + query;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", "HHDFS76320010")
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            List<Map<String, Object>> output2 = objectMapper.convertValue(root.path("output2"), List.class);
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("data", output2);
            result.put("output2", output2);
            return result;
        } catch (Exception e) {
            logger.error("Overseas trade volume ranking failed", e);
            result.put("status", "ERROR");
            result.put("message", "Overseas ranking request failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> sendOrder(OrderCommand command) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }

        AccountInfo accountInfo = resolveAccountInfo();
        if (!accountInfo.isValid()) {
            result.put("status", "ERROR");
            result.put("message", "Account number/product code missing. Login or set kis.accountNo and kis.accountProductCode.");
            return result;
        }

        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        boolean isBuy = "BUY".equalsIgnoreCase(command.getType());
        String trId = properties.isDemo()
                ? (isBuy ? "VTTC0012U" : "VTTC0011U")
                : (isBuy ? "TTTC0012U" : "TTTC0011U");

        String baseUrl = properties.getBaseUrl();
        String url = baseUrl + "/uapi/domestic-stock/v1/trading/order-cash";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("CANO", accountInfo.cano);
        payload.put("ACNT_PRDT_CD", accountInfo.productCode);
        payload.put("PDNO", command.getSymbol());
        payload.put("ORD_DVSN", "00");
        payload.put("ORD_QTY", String.valueOf(command.getQuantity()));
        payload.put("ORD_UNPR", formatOrderPrice(command.getPrice()));

        try {
            String hashKey = properties.isRequireHashKey() ? createHashKey(payload) : null;
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", trId)
                    .header("custtype", properties.getCustomerType())
                    .header("content-type", "application/json");
            if (StringUtils.hasText(hashKey)) {
                requestBuilder.header("hashkey", hashKey);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            String orderNo = root.path("output").path("ODNO").asText("");
            String message = root.path("msg1").asText("");

            result.put("status", "0".equals(rtCd) ? "ACCEPTED" : "REJECTED");
            result.put("orderId", orderNo);
            result.put("message", message);
            result.put("symbol", command.getSymbol());
            result.put("side", command.getType());
            result.put("price", command.getPrice());
            return result;
        } catch (Exception e) {
            logger.error("Order request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Order request failed: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> checkOrderStatus(String orderId) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }

        AccountInfo accountInfo = resolveAccountInfo();
        if (!accountInfo.isValid()) {
            result.put("status", "ERROR");
            result.put("message", "Account number/product code missing.");
            return result;
        }

        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String trId = properties.isDemo() ? "VTTC0081R" : "TTTC0081R";
        LocalDate today = LocalDate.now();
        String date = today.format(DateTimeFormatter.BASIC_ISO_DATE);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("CANO", accountInfo.cano);
        params.put("ACNT_PRDT_CD", accountInfo.productCode);
        params.put("INQR_STRT_DT", date);
        params.put("INQR_END_DT", date);
        params.put("SLL_BUY_DVSN_CD", "00");
        params.put("INQR_DVSN", "00");
        params.put("PDNO", "");
        params.put("CCLD_DVSN", "00");
        params.put("ORD_GNO_BRNO", "");
        params.put("ODNO", orderId == null ? "" : orderId);
        params.put("INQR_DVSN_3", "00");
        params.put("INQR_DVSN_1", "");
        params.put("CTX_AREA_FK100", "");
        params.put("CTX_AREA_NK100", "");
        params.put("EXCG_ID_DVSN_CD", "KRX");

        String url = properties.getBaseUrl() + "/uapi/domestic-stock/v1/trading/inquire-daily-ccld?" + buildQuery(params);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", trId)
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("data", objectMapper.convertValue(root, Map.class));
            return result;
        } catch (Exception e) {
            logger.error("Order status request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Order status request failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * 국내 주식 잔고 조회 (TTTC8434R / VTTC8434R)
     */
    public Map<String, Object> fetchDomesticBalance() {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }

        AccountInfo accountInfo = resolveAccountInfo();
        if (!accountInfo.isValid()) {
            result.put("status", "ERROR");
            result.put("message", "Account number/product code missing. Login first.");
            return result;
        }

        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String trId = properties.isDemo() ? "VTTC8434R" : "TTTC8434R";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("CANO", accountInfo.cano);
        params.put("ACNT_PRDT_CD", accountInfo.productCode);
        params.put("AFHR_FLPR_YN", "N");
        params.put("OFL_YN", "");
        params.put("INQR_DVSN", "01");
        params.put("UNPR_DVSN", "01");
        params.put("FUND_STTL_ICLD_YN", "N");
        params.put("FNCG_AMT_AUTO_RDPT_YN", "N");
        params.put("PRCS_DVSN", "00");
        params.put("CTX_AREA_FK100", "");
        params.put("CTX_AREA_NK100", "");

        String url = properties.getBaseUrl() + "/uapi/domestic-stock/v1/trading/inquire-balance?" + buildQuery(params);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", trId)
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("output1", objectMapper.convertValue(root.path("output1"), List.class));
            // output2는 예수금/총자산 등을 담은 단일 오브젝트이지만 응답이 배열로 올 수 있어 List로 파싱
            result.put("output2", objectMapper.convertValue(root.path("output2"), List.class));
            return result;
        } catch (Exception e) {
            logger.error("Domestic balance request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Domestic balance request failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * 해외 주식 잔고 조회 (TTTS3012R / VTTS3012R)
     * @param exchange NASD/NAS/NYSE/AMEX/SEHK/...
     * @param currency USD/HKD/JPY/CNY/VND
     */
    public Map<String, Object> fetchOverseasBalance(String exchange, String currency) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }

        AccountInfo accountInfo = resolveAccountInfo();
        if (!accountInfo.isValid()) {
            result.put("status", "ERROR");
            result.put("message", "Account number/product code missing. Login first.");
            return result;
        }

        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String trId = properties.isDemo() ? "VTTS3012R" : "TTTS3012R";
        String ex = StringUtils.hasText(exchange) ? exchange : "NASD";
        String cur = StringUtils.hasText(currency) ? currency : "USD";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("CANO", accountInfo.cano);
        params.put("ACNT_PRDT_CD", accountInfo.productCode);
        params.put("OVRS_EXCG_CD", ex);
        params.put("TR_CRCY_CD", cur);
        params.put("CTX_AREA_FK200", "");
        params.put("CTX_AREA_NK200", "");

        String url = properties.getBaseUrl() + "/uapi/overseas-stock/v1/trading/inquire-balance?" + buildQuery(params);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", trId)
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));
            result.put("output1", objectMapper.convertValue(root.path("output1"), List.class));
            result.put("output2", objectMapper.convertValue(root.path("output2"), Map.class));
            return result;
        } catch (Exception e) {
            logger.error("Overseas balance request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Overseas balance request failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * 해외 예수금(외화 현금) 조회 (TTTS3016R / VTTS3016R)
     */
    public Map<String, Object> fetchOverseasCash(String currency) {
        Map<String, Object> result = new HashMap<>();
        if (!properties.isConfigured()) {
            result.put("status", "ERROR");
            result.put("message", "KIS appKey/appSecret not configured.");
            return result;
        }

        AccountInfo accountInfo = resolveAccountInfo();
        if (!accountInfo.isValid()) {
            result.put("status", "ERROR");
            result.put("message", "Account number/product code missing. Login first.");
            return result;
        }

        String token = generateAccessToken();
        if (!StringUtils.hasText(token)) {
            result.put("status", "ERROR");
            result.put("message", "KIS access token unavailable.");
            return result;
        }

        String trId = properties.isDemo() ? "VTTS3016R" : "TTTS3016R";
        String cur = StringUtils.hasText(currency) ? currency : "USD";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("CANO", accountInfo.cano);
        params.put("ACNT_PRDT_CD", accountInfo.productCode);
        params.put("TR_CRCY_CD", cur);
        params.put("CTX_AREA_FK200", "");
        params.put("CTX_AREA_NK200", "");

        String url = properties.getBaseUrl() + "/uapi/overseas-stock/v1/trading/inquire-foreign-deposit?" + buildQuery(params);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "Bearer " + token)
                    .header("appkey", properties.getAppKey())
                    .header("appsecret", properties.getAppSecret())
                    .header("tr_id", trId)
                    .header("custtype", properties.getCustomerType())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String rtCd = root.path("rt_cd").asText();
            result.put("status", "0".equals(rtCd) ? "OK" : "ERROR");
            result.put("message", root.path("msg1").asText(""));

            JsonNode out1 = root.path("output1");
            JsonNode out2 = root.path("output2");
            Object o1 = out1.isArray() ? objectMapper.convertValue(out1, List.class)
                                       : objectMapper.convertValue(out1, Map.class);
            Object o2 = out2.isArray() ? objectMapper.convertValue(out2, List.class)
                                       : objectMapper.convertValue(out2, Map.class);
            result.put("output1", o1);
            result.put("output2", o2);
            return result;
        } catch (Exception e) {
            logger.error("Overseas cash request failed", e);
            result.put("status", "ERROR");
            result.put("message", "Overseas cash request failed: " + e.getMessage());
            return result;
        }
    }

    private Instant resolveTokenExpiry(JsonNode root) {
        String expiredText = root.path("access_token_token_expired").asText(null);
        if (StringUtils.hasText(expiredText)) {
            try {
                LocalDateTime time = LocalDateTime.parse(expiredText, TOKEN_EXPIRY_FORMAT);
                return time.atZone(ZoneId.of("Asia/Seoul")).toInstant();
            } catch (Exception ignored) {
            }
        }
        long expiresIn = root.path("expires_in").asLong(0);
        if (expiresIn <= 0) {
            expiresIn = 60 * 30;
        }
        return Instant.now().plusSeconds(expiresIn);
    }

    private String createHashKey(Map<String, Object> payload) throws Exception {
        String url = properties.getBaseUrl() + "/uapi/hashkey";
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("content-type", "application/json")
                .header("appkey", properties.getAppKey())
                .header("appsecret", properties.getAppSecret())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        String hashKey = root.path("HASH").asText();
        if (!StringUtils.hasText(hashKey)) {
            hashKey = root.path("hashkey").asText();
        }
        return hashKey;
    }

    private String buildQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    private AccountInfo resolveAccountInfo() {
        String cano = null;
        String productCode = null;

        Optional<AccountCredential> stored = credentialStore.get();
        if (stored.isPresent()) {
            AccountCredential credential = stored.get();
            cano = credential.getCano();
            productCode = credential.getAccountProductCode();
        }

        AccountInfo propInfo = parseAccount(properties.getAccountNo());
        if (!StringUtils.hasText(cano)) {
            cano = propInfo.cano;
        }
        if (!StringUtils.hasText(productCode)) {
            productCode = StringUtils.hasText(propInfo.productCode) ? propInfo.productCode : properties.getAccountProductCode();
        }

        AccountInfo info = new AccountInfo(cano, productCode);
        if (!info.isValid()) {
            logger.warn("Account info missing. sessionCano={}, sessionProd={}, propCano={}, propProd={}",
                    stored.map(AccountCredential::getCano).orElse(""),
                    stored.map(AccountCredential::getAccountProductCode).orElse(""),
                    propInfo.cano,
                    StringUtils.hasText(propInfo.productCode) ? propInfo.productCode : properties.getAccountProductCode());
        }
        return info;
    }

    private AccountInfo parseAccount(String accountNo) {
        if (!StringUtils.hasText(accountNo)) {
            return new AccountInfo("", "");
        }
        String normalized = accountNo.trim().replace(" ", "");
        if (normalized.contains("-")) {
            String[] parts = normalized.split("-", 2);
            String cano = parts.length > 0 ? parts[0] : "";
            String productCode = parts.length > 1 ? parts[1] : "";
            return new AccountInfo(cano, productCode);
        }
        if (normalized.length() >= 10) {
            return new AccountInfo(normalized.substring(0, 8), normalized.substring(8, 10));
        }
        if (normalized.length() == 8) {
            return new AccountInfo(normalized, "");
        }
        return new AccountInfo("", "");
    }

    private String formatOrderPrice(double price) {
        long rounded = Math.round(price);
        if (rounded < 0) {
            rounded = 0;
        }
        return String.valueOf(rounded);
    }

    private static class AccountInfo {
        private final String cano;
        private final String productCode;

        private AccountInfo(String cano, String productCode) {
            this.cano = cano;
            this.productCode = productCode;
        }

        private boolean isValid() {
            return StringUtils.hasText(cano) && StringUtils.hasText(productCode);
        }

        private String getCano() {
            return cano;
        }

        private String getProductCode() {
            return productCode;
        }
    }
}
