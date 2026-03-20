package com.autotrading.model;

public class OrderCommand {
    private String symbol;
    private int quantity;
    private String type; // BUY or SELL
    private double price;
    private String exchange;

    public OrderCommand() {}

    public OrderCommand(String symbol, int quantity, String type, double price) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.price = price;
    }

    public OrderCommand(String symbol, int quantity, String type, double price, String exchange) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.price = price;
        this.exchange = exchange;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}
