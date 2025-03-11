package com.tradingsim;

import java.time.LocalDate;

public class TradeExecuted {
    private LocalDate date;
    private Signal signal;
    private double price;
    private double profitLoss;

    public TradeExecuted(LocalDate date, Signal signal, double price, double profitLoss) {
        this.date = date;
        this.signal = signal;
        this.price = price;
        this.profitLoss = profitLoss;
    }

    public LocalDate getDate() {
        return date;
    }

    public Signal getSignal() {
        return signal;
    }

    public double getPrice() {
        return price;
    }

    public double getProfitLoss() {
        return profitLoss;
    }
} 