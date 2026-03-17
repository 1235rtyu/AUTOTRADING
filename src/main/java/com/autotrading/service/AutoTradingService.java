package com.autotrading.service;

public interface AutoTradingService {
    String start(String symbol);
    String stop();
    String status();
}
