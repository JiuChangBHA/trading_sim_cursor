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
 * A strategy that generates buy/sell signals based on the Relative Strength Index (RSI).
 */
public class RSIStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(RSIStrategy.class.getName());
    private int period = 14;
    private double overboughtThreshold = 70.0;
    private double oversoldThreshold = 30.0;
    private List<Double> prices = new ArrayList<>();
    private boolean inPosition = false;
    private Double lastPrice;
    private Double lastRSI;

    public RSIStrategy() {
        this.lastPrice = null;
        this.lastRSI = null;
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            this.period = (int) parameters.get("period");
        }
        if (parameters.containsKey("overboughtThreshold")) {
            this.overboughtThreshold = (double) parameters.get("overboughtThreshold");
        }
        if (parameters.containsKey("oversoldThreshold")) {
            this.oversoldThreshold = (double) parameters.get("oversoldThreshold");
        }
        this.prices.clear();
        this.inPosition = false;
        this.lastPrice = null;
        this.lastRSI = null;
        LOGGER.info("Strategy initialized with period: " + period + 
                   ", overbought: " + overboughtThreshold + 
                   ", oversold: " + oversoldThreshold);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        // Add the latest price to our list
        prices.add(marketData.getClose());
        this.lastPrice = marketData.getClose();
        
        // We need at least period+1 prices to calculate RSI
        if (prices.size() <= period + 1) {
            return null;
        }
        
        // Calculate RSI
        double rsi = calculateRSI();
        this.lastRSI = rsi;
        
        // Check if we have a position in this symbol
        boolean hasPosition = positions.containsKey(marketData.getSymbol()) && 
                             positions.get(marketData.getSymbol()).getQuantity() > 0;
        
        // Generate signals based on RSI
        if (rsi < oversoldThreshold && !hasPosition) {
            // RSI is oversold - buy signal
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
        } else if (rsi > overboughtThreshold && hasPosition) {
            // RSI is overbought - sell signal
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
        }
        
        return null;
    }
    
    private double calculateRSI() {
        double sumGain = 0;
        double sumLoss = 0;
        
        // Calculate initial average gain and loss
        for (int i = 1; i <= period; i++) {
            double change = prices.get(prices.size() - i) - prices.get(prices.size() - i - 1);
            if (change >= 0) {
                sumGain += change;
            } else {
                sumLoss -= change; // Make loss positive
            }
        }
        
        double avgGain = sumGain / period;
        double avgLoss = sumLoss / period;
        
        // Calculate RSI
        if (avgLoss == 0) {
            return 100;
        }
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    @Override
    public String getName() {
        return "RSI Strategy";
    }

    @Override
    public String getDescription() {
        return "Generates signals based on the Relative Strength Index (RSI) indicator.";
    }

    @Override
    public Map<String, Object> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("period", period);
        state.put("overboughtThreshold", overboughtThreshold);
        state.put("oversoldThreshold", oversoldThreshold);
        state.put("inPosition", inPosition);
        state.put("lastRSI", lastRSI);
        return state;
    }

    @Override
    public int getMinIndex() {
        return period + 1;
    }

    @Override
    public void reset() {
        prices.clear();
        inPosition = false;
        lastPrice = null;
        lastRSI = null;
        LOGGER.info("Strategy reset");
    }
} 