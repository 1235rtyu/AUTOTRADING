package com.autotrading.service;

import com.autotrading.model.OrderLog;

import java.util.List;

public interface HistoryService {
    List<OrderLog> getRecentOrders(int limit);
}
