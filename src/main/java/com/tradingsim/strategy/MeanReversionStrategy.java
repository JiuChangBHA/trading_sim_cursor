package com.tradingsim.strategy;

import java.util.*;
import com.tradingsim.model.*;

/**
 * A strategy that generates buy/sell signals based on mean reversion
 */
public class MeanReversionStrategy extends BaseStrategy {
    private List<Double> prices = new ArrayList<>();
    private double mean = 0.0;
    private double stdDev = 0.0;
    private double zScore = 0.0;
    private double currentPrice = 0.0;
    
    @Override
    public String getName() {
        return "Mean Reversion";
    }
    
    @Override
    public String getDescription() {
        return "Generates signals when price deviates significantly from its mean";
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        // Only set defaults if not already present
        if (!parameters.containsKey("period")) {
            parameters.put("period", 20);
        }
        if (!parameters.containsKey("threshold")) {
            parameters.put("threshold", 2.0); // Default values
        }
        super.initialize(parameters);
        prices.clear();
        mean = 0.0;
        stdDev = 0.0;
        zScore = 0.0;
        currentPrice = 0.0;
    }
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter lookback period (default: " + parameters.get("period") + "): ");
        String input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            parameters.put("period", Integer.parseInt(input));
        }
        
        System.out.print("Enter z-score threshold (default: " + parameters.get("threshold") + "): ");
        input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            parameters.put("threshold", Double.parseDouble(input));
        }
        
        System.out.println("Strategy configured: Period = " + parameters.get("period") + 
                        ", Threshold = " + parameters.get("threshold"));
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
        double threshold = parameters.containsKey("threshold") ? 
                          (double) getDoubleParameter("threshold") : 2.0;
        
        // Need at least period prices
        if (prices.size() <= period) {
            return null;
        }
        
        // Keep only necessary prices
        while (prices.size() >= period + 2) {
            prices.remove(0);
        }
        
        // Calculate mean
        double sum = 0.0;
        for (int i = 0; i < period; i++) {
            sum += prices.get(i);
        }
        mean = sum / period;
        
        // Calculate standard deviation
        double sumSquaredDiff = 0.0;
        for (int i = 0; i < period; i++) {
            double price = prices.get(i);
            double diff = price - mean;
            sumSquaredDiff += diff * diff;
        }
        stdDev = Math.sqrt(sumSquaredDiff / period);
        
        // Calculate z-score
        if (stdDev > 0) {
            zScore = (currentPrice - mean) / stdDev;
        } else {
            zScore = 0.0;
        }
        // Generate signals based on z-score
        if (zScore > threshold) {
            // Price significantly above mean - SELL
            Position position = positions.get(symbol);
            if (position != null && position.getQuantity() > 0) {
                return createSellOrder(symbol, position.getQuantity());
            }
        } else if (zScore < -threshold) {
            // Price significantly below mean - BUY
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
        // Check for 'threshold' parameter specific to MeanReversionStrategy.
        Object thresholdObj = parameters.get("threshold");
        if (!(thresholdObj instanceof Number)) {
            System.out.println("Parameter 'threshold' must be a number.");
            return false;
        }
        double threshold = ((Number) thresholdObj).doubleValue();
        if (threshold <= 0) {
            System.out.println("Parameter 'threshold' must be greater than 0.");
            return false;
        }
        return true;
    }
    
    @Override
    public void reset() {
        super.reset();
        prices.clear();
        mean = 0.0;
        stdDev = 0.0;
        zScore = 0.0;
        currentPrice = 0.0;
    }
} 