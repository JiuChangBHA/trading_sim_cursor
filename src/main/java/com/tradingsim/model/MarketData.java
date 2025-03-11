package com.tradingsim.model;

import java.time.LocalDate;

/**
 * Represents daily market data for a financial instrument.
 */
public class MarketData {
    private final LocalDate date;
    private final String symbol;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;

    public MarketData(LocalDate date, String symbol, double open, double high, double low, double close, long volume) {
        this.date = date;
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public String getSymbol() { return symbol; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }

    @Override
    public String toString() {
        return String.format("%s: %s O:%.2f H:%.2f L:%.2f C:%.2f V:%d", 
            date, symbol, open, high, low, close, volume);
    }
}
