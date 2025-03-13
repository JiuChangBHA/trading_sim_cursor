package com.tradingsim.strategy;

import java.util.*;
import com.tradingsim.model.*;

/**
 * A simple moving average strategy that generates buy/sell signals based on price crossing the moving average
 */
public class SimpleMovingAverageStrategy extends BaseStrategy {
    private List<Double> prices = new ArrayList<>();
    private double movingAverage = 0.0;
    private double currentPrice = 0.0;
    private boolean isAboveMA = false;
    private boolean initialized = false;
    
    @Override
    public String getName() {
        return "Simple Moving Average";
    }
    
    @Override
    public String getDescription() {
        return "A basic moving average crossover strategy";
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        prices.clear();
        movingAverage = 0.0;
        currentPrice = 0.0;
        isAboveMA = false;
        initialized = false;
    }
    
    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        currentPrice = marketData.getClose();
        String symbol = marketData.getSymbol();
        
        // Add price to list
        prices.add(currentPrice);
        
        // Get window size from parameters (default to 20)
        int windowSize = parameters.containsKey("windowSize") ? 
                        (int) parameters.get("windowSize") : 20;
        
        // Keep only the most recent prices based on window size
        if (prices.size() > windowSize) {
            prices.remove(0);
        }
        
        // Need at least windowSize prices to calculate MA
        if (prices.size() < windowSize) {
            return null;
        }
        
        // Calculate simple moving average
        double sum = 0.0;
        for (double price : prices) {
            sum += price;
        }
        movingAverage = sum / prices.size();
        
        // Check for crossover
        boolean wasAboveMA = isAboveMA;
        isAboveMA = currentPrice > movingAverage;
        
        if (!initialized) {
            initialized = true;
            return null;
        }
        
        // Generate signals on crossover
        if (wasAboveMA != isAboveMA) {
            if (isAboveMA) {
                // Price crossed above MA - BUY
                Position position = positions.get(symbol);
                if (position == null || position.getQuantity() <= 0) {
                    return createBuyOrder(symbol, 1.0);
                }
            } else {
                // Price crossed below MA - SELL
                Position position = positions.get(symbol);
                if (position != null && position.getQuantity() > 0) {
                    return createSellOrder(symbol, position.getQuantity());
                }
            }
        }
        
        return null;
    }
    
    @Override
    public void reset() {
        super.reset();
        prices.clear();
        movingAverage = 0.0;
        currentPrice = 0.0;
        isAboveMA = false;
        initialized = false;
    }

    @Override
    public boolean isValidParameters() {
        boolean validWindowSize = true;
        if (parameters.containsKey("windowSize")) {
            if (!(parameters.get("windowSize") instanceof Integer)) {
                return false;
            }
            validWindowSize = (int) parameters.get("windowSize") > 0;
        }
        return validWindowSize;
    }
    

    @Override
    public int getMinIndex() {
        return (int) parameters.get("windowSize");
    }
} 