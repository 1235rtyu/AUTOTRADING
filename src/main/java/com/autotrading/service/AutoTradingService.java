package com.autotrading.service;

import java.util.List;
import java.util.Map;

public interface AutoTradingService {
    String start(String symbol);
    String start(String symbol, String exchange);
    String start(String symbol, String exchange, Double buyAmount);
    String stopSymbol(String symbol);
    String stop();
    String status();
    List<Map<String, String>> runningSymbols();
}
