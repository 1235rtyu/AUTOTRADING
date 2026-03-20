package com.autotrading.model;

import java.time.LocalDateTime;

public class StockQuote {
    private String symbol;
    private String name;
    private double price;
    private double volume;
    private LocalDateTime timestamp;

    public StockQuote() {}

    public StockQuote(String symbol, double price, LocalDateTime timestamp) {
        this(symbol, price, 0, timestamp);
    }

    public StockQuote(String symbol, double price, double volume, LocalDateTime timestamp) {
        this(symbol, null, price, volume, timestamp);
    }

    public StockQuote(String symbol, String name, double price, double volume, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
