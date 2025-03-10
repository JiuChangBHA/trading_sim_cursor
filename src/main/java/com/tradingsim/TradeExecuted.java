package com.tradingsim;

import java.time.LocalDate;

public class TradeExecuted {
    private LocalDate date;
    private TradingSignal signal;
    private double shares;
    private double price;
    private double value;
    
    public TradeExecuted(LocalDate date, TradingSignal signal, double shares, 
                        double price, double value) {
        this.date = date;
        this.signal = signal;
        this.shares = shares;
        this.price = price;
        this.value = value;
    }
    
    public LocalDate getDate() { return date; }
    public TradingSignal getSignal() { return signal; }
    public double getShares() { return shares; }
    public double getPrice() { return price; }
    public double getValue() { return value; }
} 