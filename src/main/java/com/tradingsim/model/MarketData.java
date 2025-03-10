package com.tradingsim.model;

import java.time.LocalDateTime;

/**
 * Represents market data for a financial instrument at a specific point in time.
 */
public class MarketData {
    private String symbol;
    private double price;
    private double volume;
    private LocalDateTime timestamp;
    private double bid;
    private double ask;

    public MarketData(String symbol, double price, double volume, LocalDateTime timestamp, double bid, double ask) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
        this.bid = bid;
        this.ask = ask;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getVolume() { return volume; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getBid() { return bid; }
    public double getAsk() { return ask; }

    // Setters
    public void setPrice(double price) { this.price = price; }
    public void setVolume(double volume) { this.volume = volume; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setBid(double bid) { this.bid = bid; }
    public void setAsk(double ask) { this.ask = ask; }
} 