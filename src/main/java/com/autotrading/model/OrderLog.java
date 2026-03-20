package com.autotrading.model;

import java.time.LocalDateTime;

public class OrderLog {
    private Integer id;
    private String symbol;
    private String symbolName;
    private String side;
    private int quantity;
    private double price;
    private String reason;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
