package com.tradingsim;

import java.time.LocalDate;

public class MarketData {
    private LocalDate date;
    private String symbol;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    
    public MarketData(LocalDate date, String symbol, double open, double high, 
                     double low, double close, long volume) {
        this.date = date;
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
    
    public LocalDate getDate() { return date; }
    public String getSymbol() { return symbol; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }
} 