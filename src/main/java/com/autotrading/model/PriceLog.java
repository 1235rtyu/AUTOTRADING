package com.autotrading.model;

import java.time.LocalDateTime;

public class PriceLog {
    private Integer id;
    private String symbol;
    private String symbolName;
    private double price;
    private double volume;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
