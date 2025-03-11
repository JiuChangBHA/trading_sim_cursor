package com.tradingsim.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;

/**
 * A strategy that generates buy/sell signals based on moving average crossovers.
 */
public class MovingAverageCrossoverStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(MovingAverageCrossoverStrategy.class.getName());
    private int shortPeriod = 10;
    private int longPeriod = 30;
    private List<Double> prices = new ArrayList<>();
    private boolean inPosition = false;
    private double lastFastMA;
    private double lastSlowMA;

    public MovingAverageCrossoverStrategy() {
        this.lastFastMA = 0.0;
        this.lastSlowMA = 0.0;
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("shortPeriod")) {
            this.shortPeriod = (int) parameters.get("shortPeriod");
        }
        if (parameters.containsKey("longPeriod")) {
            this.longPeriod = (int) parameters.get("longPeriod");
        }
        this.prices.clear();
        this.inPosition = false;
        this.lastFastMA = 0.0;
        this.lastSlowMA = 0.0;
        LOGGER.info("Strategy initialized with short period: " + shortPeriod + ", long period: " + longPeriod);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        // Add the latest price to our list
        prices.add(marketData.getClose());
        
        // We need at least longPeriod prices to calculate moving averages
        if (prices.size() < longPeriod) {
            return null;
        }
        
        // Calculate moving averages
        double shortMA = calculateMA(shortPeriod);
        double longMA = calculateMA(longPeriod);
        
        // Store the latest MAs for state tracking
        this.lastFastMA = shortMA;
        this.lastSlowMA = longMA;
        
        // Check if we have a position in this symbol
        boolean hasPosition = positions.containsKey(marketData.getSymbol()) && 
                             positions.get(marketData.getSymbol()).getQuantity() > 0;
        
        // Generate signals based on crossovers
        if (shortMA > longMA && !hasPosition) {
            // Bullish crossover - buy signal
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
        } else if (shortMA < longMA && hasPosition) {
            // Bearish crossover - sell signal
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
        }
        
        return null;
    }
    
    private double calculateMA(int period) {
        int startIdx = prices.size() - period;
        double sum = 0;
        for (int i = startIdx; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }
    
    @Override
    public String getName() {
        return "Moving Average Crossover";
    }
    
    @Override
    public String getDescription() {
        return "Generates signals based on the crossover of short-term and long-term moving averages.";
    }
    
    @Override
    public Map<String, Object> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("shortPeriod", shortPeriod);
        state.put("longPeriod", longPeriod);
        state.put("inPosition", inPosition);
        state.put("fastMA", lastFastMA);
        state.put("slowMA", lastSlowMA);
        return state;
    }
    
    @Override
    public int getMinIndex() {
        return longPeriod;
    }
    
    @Override
    public void reset() {
        prices.clear();
        inPosition = false;
        this.lastFastMA = 0.0;
        this.lastSlowMA = 0.0;
        LOGGER.info("Strategy reset");
    }
} 