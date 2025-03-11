package com.tradingsim.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;

/**
 * A strategy that generates buy/sell signals based on mean reversion principles.
 */
public class MeanReversionStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(MeanReversionStrategy.class.getName());
    private int lookbackPeriod = 20;
    private double entryThreshold = 2.0; // Standard deviations
    private double exitThreshold = 0.5; // Standard deviations
    private List<Double> prices = new ArrayList<>();
    private double lastZScore;
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("lookbackPeriod")) {
            this.lookbackPeriod = (int) parameters.get("lookbackPeriod");
        }
        if (parameters.containsKey("entryThreshold")) {
            this.entryThreshold = (double) parameters.get("entryThreshold");
        }
        if (parameters.containsKey("exitThreshold")) {
            this.exitThreshold = (double) parameters.get("exitThreshold");
        }
        this.prices.clear();
        this.lastZScore = 0.0;
        LOGGER.info("Strategy initialized with lookbackPeriod: " + lookbackPeriod + 
                   ", entryThreshold: " + entryThreshold + 
                   ", exitThreshold: " + exitThreshold);
    }
    
    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        // Add the latest price to our list
        prices.add(marketData.getClose());
        
        // We need at least lookbackPeriod prices to calculate mean and standard deviation
        if (prices.size() < lookbackPeriod) {
            return null;
        }
        
        // Calculate mean and standard deviation
        double mean = calculateMean();
        double stdDev = calculateStdDev(mean);
        
        // Calculate z-score (how many standard deviations away from the mean)
        double currentPrice = prices.get(prices.size() - 1);
        double zScore = (currentPrice - mean) / stdDev;
        this.lastZScore = zScore;
        
        // Check if we have a position in this symbol
        boolean hasPosition = positions.containsKey(marketData.getSymbol()) && 
                             positions.get(marketData.getSymbol()).getQuantity() != 0;
        
        // Generate signals based on z-score
        if (zScore < -entryThreshold && !hasPosition) {
            // Price is significantly below mean - buy signal (expecting reversion to mean)
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
        } else if (zScore > entryThreshold && !hasPosition) {
            // Price is significantly above mean - sell signal (expecting reversion to mean)
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
        } else if (Math.abs(zScore) < exitThreshold && hasPosition) {
            // Price has reverted to mean - exit position
            Position position = positions.get(marketData.getSymbol());
            Order.OrderSide exitSide = position.getQuantity() > 0 ? OrderSide.SELL : OrderSide.BUY;
            return new Order(marketData.getSymbol(), OrderType.MARKET, exitSide, Math.abs(position.getQuantity()));
        }
        
        return null;
    }
    
    private double calculateMean() {
        int startIdx = prices.size() - lookbackPeriod;
        double sum = 0;
        for (int i = startIdx; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / lookbackPeriod;
    }
    
    private double calculateStdDev(double mean) {
        int startIdx = prices.size() - lookbackPeriod;
        double sumSquaredDiff = 0;
        for (int i = startIdx; i < prices.size(); i++) {
            double diff = prices.get(i) - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / lookbackPeriod);
    }
    
    @Override
    public String getName() {
        return "Mean Reversion Strategy";
    }
    
    @Override
    public String getDescription() {
        return "Generates signals based on mean reversion principles, buying when prices are significantly below the mean and selling when they are significantly above.";
    }
    
    @Override
    public Map<String, Object> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("lookbackPeriod", lookbackPeriod);
        state.put("entryThreshold", entryThreshold);
        state.put("exitThreshold", exitThreshold);
        state.put("lastZScore", lastZScore);
        return state;
    }
    
    @Override
    public int getMinIndex() {
        return lookbackPeriod;
    }
    
    @Override
    public void reset() {
        prices.clear();
        lastZScore = 0.0;
        LOGGER.info("Strategy reset");
    }
} 