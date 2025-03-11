package com.tradingsim.strategy;

import java.util.*;
import com.tradingsim.model.*;
import java.util.logging.Logger;


/**
 * A strategy that generates buy/sell signals based on fast MA crossing slow MA
 */
public class MovingAverageCrossoverStrategy extends BaseStrategy {
    private static final Logger LOGGER = Logger.getLogger(MovingAverageCrossoverStrategy.class.getName());
    private List<Double> prices = new ArrayList<>();
    private double fastMA = 0.0;
    private double slowMA = 0.0;
    private double currentPrice = 0.0;
    private boolean fastAboveSlow = false;
    private boolean initialized = false;
    
    @Override
    public String getName() {
        return "Moving Average Crossover";
    }
    
    @Override
    public String getDescription() {
        return "Generates signals when fast moving average crosses slow moving average";
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        prices.clear();
        fastMA = 0.0;
        slowMA = 0.0;
        currentPrice = 0.0;
        fastAboveSlow = false;
        initialized = false;
    }
    
    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        currentPrice = marketData.getClose();
        String symbol = marketData.getSymbol();
        
        // Add price to list
        prices.add(currentPrice);
        
        // Get parameters (default values if not specified)
        int fastPeriod = parameters.containsKey("fastPeriod") ? 
                        (int) parameters.get("fastPeriod") : 10;
        int slowPeriod = parameters.containsKey("slowPeriod") ? 
                        (int) parameters.get("slowPeriod") : 30;
        
        // Need at least slowPeriod prices
        if (prices.size() < slowPeriod) {
            return null;
        }
        
        // Keep only necessary prices
        while (prices.size() > slowPeriod) {
            prices.remove(0);
        }
        
        // Calculate fast MA
        double fastSum = 0.0;
        for (int i = prices.size() - fastPeriod; i < prices.size(); i++) {
            fastSum += prices.get(i);
        }
        fastMA = fastSum / fastPeriod;
        
        // Calculate slow MA
        double slowSum = 0.0;
        for (double price : prices) {
            slowSum += price;
        }
        slowMA = slowSum / slowPeriod;
        
        // Check for crossover
        boolean wasFastAboveSlow = fastAboveSlow;
        fastAboveSlow = fastMA > slowMA;
        
        // If not initialized, just set the initial state
        if (!initialized) {
            initialized = true;
            return null;
        }
        
        // Generate signals on crossover
        if (wasFastAboveSlow != fastAboveSlow) {
            if (fastAboveSlow) {
                // Fast MA crossed above slow MA - BUY
                return createBuyOrder(symbol, 1.0);
            } else {
                // Fast MA crossed below slow MA - SELL
                Position position = positions.get(symbol);
                if (position != null && position.getQuantity() > 0) {
                    return createSellOrder(symbol, position.getQuantity());
                }
            }
        }
        
        return null;
    }
    
    @Override
    public int getMinIndex() {
        int slowPeriod = parameters.containsKey("slowPeriod") ? 
                        (int) parameters.get("slowPeriod") : 30;
        return slowPeriod;
    }
    
    @Override
    public void reset() {
        super.reset();
        prices.clear();
        fastMA = 0.0;
        slowMA = 0.0;
        currentPrice = 0.0;
        fastAboveSlow = false;
        initialized = false;
    }
} 