package com.autotrading.order;

import com.autotrading.market.KoreaInvestmentApiClient;
import com.autotrading.model.OrderCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final KoreaInvestmentApiClient apiClient;

    public OrderService(KoreaInvestmentApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public Map<String, Object> placeOrder(OrderCommand command) {
        normalizeMarketOrderForReasons(command);
        validate(command);

        logger.info(
                "ORDER_REQUEST type={} symbol={} qty={} price={} exchange={} marketOrder={} reason={}",
                command.getType(),
                command.getSymbol(),
                command.getQuantity(),
                command.isMarketOrder() ? "MKT" : command.getPrice(),
                command.getExchange(),
                command.isMarketOrder(),
                command.getReason()
        );

        Map<String, Object> result;

        try {
            if (isOverseasExchange(command.getExchange())) {
                result = apiClient.placeOverseasOrder(command);
            } else if (command.isMarketOrder()) {
                result = placeMarketOrder(command);
            } else {
                result = placeLimitOrder(command);
            }

            logger.info(
                    "ORDER_RESPONSE type={} symbol={} qty={} marketOrder={} result={}",
                    command.getType(),
                    command.getSymbol(),
                    command.getQuantity(),
                    command.isMarketOrder(),
                    result
            );

            return result;
        } catch (Exception e) {
            logger.error(
                    "ORDER_FAILED type={} symbol={} qty={} price={} exchange={} marketOrder={} reason={} msg={}",
                    command.getType(),
                    command.getSymbol(),
                    command.getQuantity(),
                    command.getPrice(),
                    command.getExchange(),
                    command.isMarketOrder(),
                    command.getReason(),
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    private Map<String, Object> placeMarketOrder(OrderCommand command) {
        logger.info(
                "ORDER_EXECUTE MARKET type={} symbol={} qty={} exchange={} reason={}",
                command.getType(),
                command.getSymbol(),
                command.getQuantity(),
                command.getExchange(),
                command.getReason()
        );

        return apiClient.sendOrder(command);
    }

    private Map<String, Object> placeLimitOrder(OrderCommand command) {
        logger.info(
                "ORDER_EXECUTE LIMIT type={} symbol={} qty={} price={} exchange={} reason={}",
                command.getType(),
                command.getSymbol(),
                command.getQuantity(),
                command.getPrice(),
                command.getExchange(),
                command.getReason()
        );

        return apiClient.sendOrder(command);
    }

    public Map<String, Object> getOrderStatus(String orderId) {
        logger.info("ORDER_STATUS_REQUEST orderId={}", orderId);
        Map<String, Object> result = apiClient.checkOrderStatus(orderId);
        logger.info("ORDER_STATUS_RESPONSE orderId={} result={}", orderId, result);
        return result;
    }

    private void validate(OrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("OrderCommand is null.");
        }

        if (command.getSymbol() == null || command.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Order symbol is empty.");
        }

        if (command.getType() == null || command.getType().isBlank()) {
            throw new IllegalArgumentException("Order type is empty.");
        }

        String type = command.getType().trim().toUpperCase();
        if (!"BUY".equals(type) && !"SELL".equals(type)) {
            throw new IllegalArgumentException("Order type must be BUY or SELL.");
        }

        if (command.getQuantity() <= 0) {
            throw new IllegalArgumentException("Order quantity must be greater than 0.");
        }

        if (!command.isMarketOrder() && command.getPrice() <= 0.0) {
            throw new IllegalArgumentException("Limit order price must be greater than 0.");
        }
    }

    private void normalizeMarketOrderForReasons(OrderCommand command) {
        if (command == null) return;
        String type = command.getType();
        if (type == null) return;
        if (!"SELL".equalsIgnoreCase(type)) return;

        String reason = command.getReason();
        if (!isMarketSellReason(reason)) return;

        if (!command.isMarketOrder()) {
            command.setMarketOrder(true);
            command.setPrice(0.0);
            logger.info("ORDER market override for reason={} symbol={}", reason, command.getSymbol());
        }
    }

    private boolean isMarketSellReason(String reason) {
        if (reason == null) return false;
        return "TAKE_PROFIT_PARTIAL".equals(reason)
                || "TAKE_PROFIT_PARTIAL_FULL".equals(reason)
                || "TAKE_PROFIT_FINAL".equals(reason)
                || "TRAILING_STOP".equals(reason)
                || "STOP_LOSS".equals(reason)
                || "EMERGENCY_STOP".equals(reason);
    }

    private boolean isOverseasExchange(String exchange) {
        if (exchange == null || exchange.isBlank()) return false;
        String ex = exchange.trim().toUpperCase();
        if ("KRX".equals(ex) || "KR".equals(ex) || "KOSPI".equals(ex) || "KOSDAQ".equals(ex)) {
            return false;
        }
        return true;
    }
}
