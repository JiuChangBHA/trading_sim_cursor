package com.tradingsim.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a trading position in a financial instrument.
 * Tracks all trades that make up the position for accurate P&L calculation.
 */
public class Position {
    private final String symbol;
    private double quantity;
    private double averageEntryPrice;
    private LocalDate entryDate;
    private double currentPrice;
    
    // Track individual trades for more accurate P&L calculation
    private final List<Trade> trades;
    
    /**
     * Represents an individual trade that contributes to a position
     */
    private static class Trade {
        private final double quantity;
        private final double price;
        private final LocalDate date;
        
        public Trade(double quantity, double price, LocalDate date) {
            this.quantity = quantity;
            this.price = price;
            this.date = date;
        }
        
        public double getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public LocalDate getDate() { return date; }
    }

    public Position(String symbol, double quantity, double entryPrice, LocalDate entryDate) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageEntryPrice = entryPrice;
        this.entryDate = entryDate;
        this.currentPrice = entryPrice;
        this.trades = new ArrayList<>();
        
        // Record the initial trade
        if (quantity != 0) {
            this.trades.add(new Trade(quantity, entryPrice, entryDate));
        }
    }

    // Getters
    public String getSymbol() { return symbol; }
    public double getQuantity() { return quantity; }
    public double getAverageEntryPrice() { return averageEntryPrice; }
    public LocalDate getEntryDate() { return entryDate; }
    public double getCurrentPrice() { return currentPrice; }
    
    // Setters
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    
    /**
     * Update the position with a new trade
     * @param quantity Quantity of the trade (positive for buy, negative for sell)
     * @param price Price of the trade
     * @param date Date of the trade
     * @return Realized profit/loss from this trade, if any
     */
    public double updatePosition(double quantity, double price, LocalDate date) {
        double realizedPnL = 0.0;
        
        if (this.quantity == 0) {
            // New position
            this.quantity = quantity;
            this.averageEntryPrice = price;
            this.entryDate = date;
            this.trades.add(new Trade(quantity, price, date));
        } else if ((this.quantity > 0 && quantity > 0) || (this.quantity < 0 && quantity < 0)) {
            // Adding to existing position (same direction)
            double totalCost = this.quantity * this.averageEntryPrice + quantity * price;
            this.quantity += quantity;
            this.averageEntryPrice = totalCost / this.quantity;
            this.trades.add(new Trade(quantity, price, date));
        } else if (Math.abs(quantity) < Math.abs(this.quantity)) {
            // Reducing position (partial close)
            // Calculate realized P&L using FIFO method
            realizedPnL = calculateRealizedPnL(quantity, price);
            this.quantity += quantity;
            this.trades.add(new Trade(quantity, price, date));
        } else {
            // Closing position and possibly opening in opposite direction
            double remainingQuantity = this.quantity + quantity;
            
            // Calculate realized P&L for the closed portion
            realizedPnL = calculateRealizedPnL(this.quantity * -1, price);
            
            if (remainingQuantity != 0) {
                // Opening a new position in the opposite direction
                this.quantity = remainingQuantity;
                this.averageEntryPrice = price;
                this.entryDate = date;
                this.trades.clear(); // Clear old trades
                this.trades.add(new Trade(remainingQuantity, price, date));
            } else {
                // Position fully closed
                this.quantity = 0;
                this.trades.clear();
            }
        }
        
        this.currentPrice = price;
        return realizedPnL;
    }
    
    /**
     * Calculate realized profit/loss for a partial or full position close
     * Uses FIFO (First In, First Out) accounting method
     * 
     * @param closeQuantity Quantity being closed (negative for long positions, positive for short positions)
     * @param closePrice Price at which the position is being closed
     * @return Realized profit/loss
     */
    private double calculateRealizedPnL(double closeQuantity, double closePrice) {
        if (trades.isEmpty() || closeQuantity == 0) {
            return 0.0;
        }
        
        double remainingCloseQty = Math.abs(closeQuantity);
        double realizedPnL = 0.0;
        List<Trade> tradesToRemove = new ArrayList<>();
        
        // Process trades in FIFO order
        for (Trade trade : trades) {
            if (remainingCloseQty <= 0) break;
            
            double tradeQty = Math.abs(trade.getQuantity());
            double qtyToClose = Math.min(tradeQty, remainingCloseQty);
            
            // Calculate P&L for this portion
            if (this.quantity > 0) {
                // Long position
                realizedPnL += qtyToClose * (closePrice - trade.getPrice());
            } else {
                // Short position
                realizedPnL += qtyToClose * (trade.getPrice() - closePrice);
            }
            
            remainingCloseQty -= qtyToClose;
            
            if (qtyToClose >= tradeQty) {
                // This trade is fully closed
                tradesToRemove.add(trade);
            }
        }
        
        // Remove fully closed trades
        trades.removeAll(tradesToRemove);
        
        return realizedPnL;
    }
    
    /**
     * Calculate the current market value of the position
     * @return Market value
     */
    public double getMarketValue() {
        return quantity * currentPrice;
    }
    
    /**
     * Calculate the unrealized profit/loss of the position
     * @return Unrealized profit/loss
     */
    public double getUnrealizedPnL() {
        if (quantity == 0) return 0;
        
        if (quantity > 0) {
            // Long position
            return quantity * (currentPrice - averageEntryPrice);
        } else {
            // Short position
            return quantity * (averageEntryPrice - currentPrice);
        }
    }
    
    /**
     * Calculate the percentage return of the position
     * @return Percentage return
     */
    public double getPercentageReturn() {
        if (quantity == 0 || averageEntryPrice == 0) return 0;
        
        if (quantity > 0) {
            // Long position
            return ((currentPrice / averageEntryPrice) - 1) * 100;
        } else {
            // Short position
            return ((averageEntryPrice / currentPrice) - 1) * 100;
        }
    }
} 