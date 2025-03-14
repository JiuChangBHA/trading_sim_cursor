package com.tradingsim.strategy;

import java.util.*;
import com.tradingsim.model.*;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.io.IOException;

/**
 * A strategy that generates buy/sell signals based on Bollinger Bands
 */
public class BollingerBandsStrategy extends BaseStrategy {
    private static final Logger LOGGER = Logger.getLogger(BollingerBandsStrategy.class.getName());
    private List<Double> prices = new ArrayList<>();
    private double sma = 0.0;
    private double upperBand = 0.0;
    private double lowerBand = 0.0;
    private double currentPrice = 0.0;
    
    static {
        try {
            LogManager.getLogManager().readConfiguration(
                BollingerBandsStrategy.class.getClassLoader().getResourceAsStream("logging.properties")
            );
        } catch (IOException e) {
            LOGGER.severe("Could not load logging.properties file: " + e.getMessage());
        }
    }
    
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
                    (int) getIntegerParameter("period") : 20;
        double stdDevMultiplier = parameters.containsKey("stdDevMultiplier") ? 
                                 (double) getDoubleParameter("stdDevMultiplier") : 2.0;
        
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
                    (int) getIntegerParameter("period") : 20;
        return period;
    }

    @Override
    protected boolean extraValidations() {
        // For BollingerBandsStrategy, we require a stdMultiplier parameter.
        if (!parameters.containsKey("stdDevMultiplier")) {
            System.out.println("Missing required parameter: stdDevMultiplier (for BollingerBandsStrategy)");
            return false;
        }
        Object multiplierObj = parameters.get("stdDevMultiplier");
        if (!(multiplierObj instanceof Number)) {
            System.out.println("Parameter 'stdDevMultiplier' must be numeric.");
            return false;
        }
        double stdDevMultiplier = ((Number) multiplierObj).doubleValue();
        if (stdDevMultiplier <= 0) {
            System.out.println("Parameter 'stdDevMultiplier' must be greater than 0.");
            return false;
        }
        return true;
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