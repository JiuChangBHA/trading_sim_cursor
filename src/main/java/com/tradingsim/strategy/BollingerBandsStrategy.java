package com.tradingsim.strategy;

import java.util.*;
import com.tradingsim.model.*;

/**
 * A strategy that generates buy/sell signals based on Bollinger Bands
 */
public class BollingerBandsStrategy extends BaseStrategy {
    private List<Double> prices = new ArrayList<>();
    private double sma = 0.0;
    private double upperBand = 0.0;
    private double lowerBand = 0.0;
    private double currentPrice = 0.0;
    
    @Override
    public String getName() {
        return "Bollinger Bands";
    }
    
    @Override
    public String getDescription() {
        return "Generates signals when price touches or crosses Bollinger Bands";
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        prices.clear();
        sma = 0.0;
        upperBand = 0.0;
        lowerBand = 0.0;
        currentPrice = 0.0;
    }
    
    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        currentPrice = marketData.getClose();
        String symbol = marketData.getSymbol();
        
        // Add price to list
        prices.add(currentPrice);
        
        // Get parameters (default values if not specified)
        int period = parameters.containsKey("period") ? 
                    (int) parameters.get("period") : 20;
        double stdDevMultiplier = parameters.containsKey("stdDevMultiplier") ? 
                                 (double) parameters.get("stdDevMultiplier") : 2.0;
        
        // Need at least period prices
        if (prices.size() < period) {
            return null;
        }
        
        // Keep only necessary prices
        while (prices.size() > period) {
            prices.remove(0);
        }
        
        // Calculate SMA
        double sum = 0.0;
        for (double price : prices) {
            sum += price;
        }
        sma = sum / period;
        
        // Calculate standard deviation
        double sumSquaredDiff = 0.0;
        for (double price : prices) {
            double diff = price - sma;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSquaredDiff / period);
        
        // Calculate Bollinger Bands
        upperBand = sma + (stdDevMultiplier * stdDev);
        lowerBand = sma - (stdDevMultiplier * stdDev);
        
        // Update state
        state.put("currentPrice", currentPrice);
        state.put("sma", sma);
        state.put("upperBand", upperBand);
        state.put("lowerBand", lowerBand);
        
        // Generate signals based on price relative to bands
        if (currentPrice >= upperBand) {
            // Price at or above upper band - SELL
            Position position = positions.get(symbol);
            if (position != null && position.getQuantity() > 0) {
                return createSellOrder(symbol, position.getQuantity());
            }
        } else if (currentPrice <= lowerBand) {
            // Price at or below lower band - BUY
            return createBuyOrder(symbol, 1.0);
        }
        
        return null;
    }
    
    @Override
    public int getMinIndex() {
        int period = parameters.containsKey("period") ? 
                    (int) parameters.get("period") : 20;
        return period;
    }
    
    @Override
    public void reset() {
        super.reset();
        prices.clear();
        sma = 0.0;
        upperBand = 0.0;
        lowerBand = 0.0;
        currentPrice = 0.0;
    }
} 