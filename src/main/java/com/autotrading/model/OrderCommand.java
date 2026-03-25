package com.autotrading.model;

public class OrderCommand {
    private String symbol;
    private int quantity;
    private String type; // BUY or SELL
    private double price;
    private String exchange;

    // 추가
    private boolean marketOrder; // true면 시장가
    private String reason;       // ENTRY, STOP_LOSS, TAKE_PROFIT 등

    public OrderCommand() {}

    public OrderCommand(String symbol, int quantity, String type, double price) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.price = price;
        this.marketOrder = false;
    }

    public OrderCommand(String symbol, int quantity, String type, double price, String exchange) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.price = price;
        this.exchange = exchange;
        this.marketOrder = false;
    }

    // 추천 생성자
    public OrderCommand(String symbol, int quantity, String type, double price, boolean marketOrder, String reason) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.price = price;
        this.marketOrder = marketOrder;
        this.reason = reason;
    }

    public OrderCommand(String symbol, int quantity, String type, double price,
                        String exchange, boolean marketOrder, String reason) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.type = type;
        this.price = price;
        this.exchange = exchange;
        this.marketOrder = marketOrder;
        this.reason = reason;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public boolean isMarketOrder() {
        return marketOrder;
    }

    public void setMarketOrder(boolean marketOrder) {
        this.marketOrder = marketOrder;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "OrderCommand{" +
                "symbol='" + symbol + '\'' +
                ", quantity=" + quantity +
                ", type='" + type + '\'' +
                ", price=" + price +
                ", exchange='" + exchange + '\'' +
                ", marketOrder=" + marketOrder +
                ", reason='" + reason + '\'' +
                '}';
    }
}