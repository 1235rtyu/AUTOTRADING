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
        logger.info("Placing order: {} {} @ {}", command.getType(), command.getSymbol(), command.getPrice());
        return apiClient.sendOrder(command);
    }

    public Map<String, Object> getOrderStatus(String orderId) {
        return apiClient.checkOrderStatus(orderId);
    }
}
