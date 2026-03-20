package com.autotrading.model;

import java.time.LocalDateTime;

public class AutoPosition {
    private Integer id;
    private String symbol;
    private String symbolName;
    private int quantity;
    private double avgPrice;
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getAvgPrice() { return avgPrice; }
    public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
