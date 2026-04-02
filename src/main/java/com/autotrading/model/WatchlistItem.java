package com.autotrading.model;

import java.time.LocalDateTime;

public class WatchlistItem {
    private Integer id;
    private String symbol;
    private String exchange;
    private String folder;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
