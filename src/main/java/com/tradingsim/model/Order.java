package com.tradingsim.model;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Represents a trading order with its properties and execution details.
 */
public class Order {
    public enum OrderType {
        MARKET,
        LIMIT,
        STOP,
        STOP_LIMIT
    }

    public enum OrderSide {
        BUY,
        SELL,
        HOLD
    }

    private String symbol;
    private OrderType type;
    private OrderSide side;
    private double quantity;
    private double limitPrice;
    private double stopPrice;
    private LocalDateTime timestamp;
    private String id;
    private boolean isActive;
    
    // Execution details (previously in TradeExecuted)
    private LocalDate executionDate;
    private double executionPrice;
    private double profitLoss;

    public Order(String symbol, OrderType type, OrderSide side, double quantity) {
        this.symbol = symbol;
        this.type = type;
        this.side = side;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
        this.id = generateOrderId();
        this.isActive = true;
    }

    private String generateOrderId() {
        return String.format("%s-%s-%d", 
            symbol, 
            side.toString(), 
            System.currentTimeMillis());
    }

    // Getters
    public String getSymbol() { return symbol; }
    public OrderType getType() { return type; }
    public OrderSide getSide() { return side; }
    public double getQuantity() { return quantity; }
    public double getLimitPrice() { return limitPrice; }
    public double getStopPrice() { return stopPrice; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getId() { return id; }
    public boolean isActive() { return isActive; }
    
    // Execution details getters
    public LocalDate getExecutionDate() { return executionDate; }
    public double getExecutionPrice() { return executionPrice; }
    public double getProfitLoss() { return profitLoss; }

    // Setters
    public void setLimitPrice(double limitPrice) { this.limitPrice = limitPrice; }
    public void setStopPrice(double stopPrice) { this.stopPrice = stopPrice; }
    public void setActive(boolean active) { isActive = active; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    
    /**
     * Execute the order at the specified price and record profit/loss
     * @param price The execution price
     * @param profitLoss The profit or loss from the trade
     */
    public void execute(double price, double profitLoss) {
        this.executionPrice = price;
        this.profitLoss = profitLoss;
        this.executionDate = LocalDate.now();
        this.isActive = false;
    }
    
    /**
     * Execute the order at the specified price and record profit/loss on a specific date
     * @param date The execution date
     * @param price The execution price
     * @param profitLoss The profit or loss from the trade
     */
    public void execute(LocalDate date, double price, double profitLoss) {
        this.executionDate = date;
        this.executionPrice = price;
        this.profitLoss = profitLoss;
        this.isActive = false;
    }
} 