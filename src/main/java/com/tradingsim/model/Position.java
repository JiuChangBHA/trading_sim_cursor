package com.tradingsim.model;

/**
 * Represents a trading position for a specific financial instrument.
 */
public class Position {
    private String symbol;
    private double quantity;
    private double averageEntryPrice;
    private double currentPrice;
    private double unrealizedPnL;
    private double realizedPnL;

    public Position(String symbol, double quantity, double averageEntryPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageEntryPrice = averageEntryPrice;
        this.currentPrice = averageEntryPrice;
        this.unrealizedPnL = 0.0;
        this.realizedPnL = 0.0;
    }

    public void updateMarketPrice(double newPrice) {
        this.currentPrice = newPrice;
        this.unrealizedPnL = (currentPrice - averageEntryPrice) * quantity;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public double getQuantity() { return quantity; }
    public double getAverageEntryPrice() { return averageEntryPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public double getUnrealizedPnL() { return unrealizedPnL; }
    public double getRealizedPnL() { return realizedPnL; }

    // Setters
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setAverageEntryPrice(double price) { this.averageEntryPrice = price; }
    public void setRealizedPnL(double pnl) { this.realizedPnL = pnl; }
} 