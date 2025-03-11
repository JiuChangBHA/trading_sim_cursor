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
 * A strategy that generates buy/sell signals based on Bollinger Bands.
 */
public class BollingerBandsStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(BollingerBandsStrategy.class.getName());
    private int period = 20;
    private double multiplier = 2.0;
    private List<Double> prices = new ArrayList<>();
    private double lastSMA;
    private double lastUpperBand;
    private double lastLowerBand;
    private double lastStdDev;

    public BollingerBandsStrategy() {
        this.lastSMA = 0.0;
        this.lastUpperBand = 0.0;
        this.lastLowerBand = 0.0;
        this.lastStdDev = 0.0;
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            this.period = (int) parameters.get("period");
        }
        if (parameters.containsKey("multiplier")) {
            this.multiplier = (double) parameters.get("multiplier");
        }
        this.prices.clear();
        this.lastSMA = 0.0;
        this.lastUpperBand = 0.0;
        this.lastLowerBand = 0.0;
        this.lastStdDev = 0.0;
        LOGGER.info("Strategy initialized with period: " + period + ", multiplier: " + multiplier);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        // Add the latest price to our list
        prices.add(marketData.getClose());
        
        // We need at least period prices to calculate Bollinger Bands
        if (prices.size() < period) {
            return null;
        }
        
        // Calculate Bollinger Bands
        double sma = calculateSMA();
        double stdDev = calculateStdDev(sma);
        double upperBand = sma + (multiplier * stdDev);
        double lowerBand = sma - (multiplier * stdDev);
        
        // Store the latest values for state tracking
        this.lastSMA = sma;
        this.lastUpperBand = upperBand;
        this.lastLowerBand = lowerBand;
        this.lastStdDev = stdDev;
        
        double currentPrice = prices.get(prices.size() - 1);
        
        // Check if we have a position in this symbol
        boolean hasPosition = positions.containsKey(marketData.getSymbol()) && 
                             positions.get(marketData.getSymbol()).getQuantity() > 0;
        
        // Generate signals based on Bollinger Bands
        if (currentPrice < lowerBand && !hasPosition) {
            // Price is below lower band - buy signal
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
        } else if (currentPrice > upperBand && hasPosition) {
            // Price is above upper band - sell signal
            return new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
        }
        
        return null;
    }
    
    private double calculateSMA() {
        int startIdx = prices.size() - period;
        double sum = 0;
        for (int i = startIdx; i < prices.size(); i++) {
            sum += prices.get(i);
        }
        return sum / period;
    }
    
    private double calculateStdDev(double mean) {
        int startIdx = prices.size() - period;
        double sumSquaredDiff = 0;
        for (int i = startIdx; i < prices.size(); i++) {
            double diff = prices.get(i) - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / period);
    }
    
    @Override
    public String getName() {
        return "Bollinger Bands Strategy";
    }
    
    @Override
    public String getDescription() {
        return "Generates signals based on Bollinger Bands, buying when price touches the lower band and selling when it touches the upper band.";
    }
    
    @Override
    public Map<String, Object> getState() {
        Map<String, Object> state = new HashMap<>();
        state.put("period", period);
        state.put("multiplier", multiplier);
        state.put("sma", lastSMA);
        state.put("upperBand", lastUpperBand);
        state.put("lowerBand", lastLowerBand);
        state.put("stdDev", lastStdDev);
        return state;
    }
    
    @Override
    public int getMinIndex() {
        return period;
    }
    
    @Override
    public void reset() {
        prices.clear();
        lastSMA = 0.0;
        lastUpperBand = 0.0;
        lastLowerBand = 0.0;
        lastStdDev = 0.0;
        LOGGER.info("Strategy reset");
    }
} 