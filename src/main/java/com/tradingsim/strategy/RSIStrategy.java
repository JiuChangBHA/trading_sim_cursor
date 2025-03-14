package com.tradingsim.strategy;

import java.util.*;
import com.tradingsim.model.*;

/**
 * A strategy that generates buy/sell signals based on RSI (Relative Strength Index)
 */
public class RSIStrategy extends BaseStrategy {
    private List<Double> prices = new ArrayList<>();
    private double rsi = 50.0; // Default neutral RSI
    private double currentPrice = 0.0;
    
    @Override
    public String getName() {
        return "RSI Strategy";
    }
    
    @Override
    public String getDescription() {
        return "Generates signals based on overbought/oversold conditions using RSI";
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        // Only set defaults if not already present
        if (!parameters.containsKey("period")) {
            parameters.put("period", 14);
        }
        if (!parameters.containsKey("overboughtThreshold")) {
            parameters.put("overboughtThreshold", 70.0);
        }
        if (!parameters.containsKey("oversoldThreshold")) {
            parameters.put("oversoldThreshold", 30.0); // Default values
        }
        super.initialize(parameters);
        prices.clear();
        rsi = 50.0;
        currentPrice = 0.0;
    }

    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter RSI period (default: " + parameters.get("period") + "): ");
        String input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            parameters.put("period", Integer.parseInt(input));
        }
        
        System.out.print("Enter oversold threshold (default: " + parameters.get("oversoldThreshold") + "): ");
        input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            parameters.put("oversoldThreshold", Double.parseDouble(input));
        }
        
        System.out.print("Enter overbought threshold (default: " + parameters.get("overboughtThreshold") + "): ");
        input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            parameters.put("overboughtThreshold", Double.parseDouble(input));
        }
        
        System.out.println("Strategy configured: Period = " + parameters.get("period") + 
                        ", Oversold = " + parameters.get("oversoldThreshold") + 
                        ", Overbought = " + parameters.get("overboughtThreshold"));
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        currentPrice = marketData.getClose();
        String symbol = marketData.getSymbol();
        
        // Add price to list
        prices.add(currentPrice);
        
        // Get parameters (default values if not specified)
        int period = parameters.containsKey("period") ? 
                    (int) getIntegerParameter("period") : 14;
        double overboughtThreshold = parameters.containsKey("overboughtThreshold") ? 
                                    (double) getDoubleParameter("overboughtThreshold") : 70.0;
        double oversoldThreshold = parameters.containsKey("oversoldThreshold") ? 
                                  (double) getDoubleParameter("oversoldThreshold") : 30.0;
        
        // Need at least period+1 prices to calculate RSI
        if (prices.size() <= period) {
            return null;
        }
        
        // Keep only necessary prices
        while (prices.size() > period + 1) {
            prices.remove(0);
        }
        
        // Calculate RSI
        double sumGain = 0.0;
        double sumLoss = 0.0;
        
        for (int i = 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i-1);
            if (change > 0) {
                sumGain += change;
            } else {
                sumLoss -= change; // Convert to positive
            }
        }
        
        double avgGain = sumGain / period;
        double avgLoss = sumLoss / period;
        
        if (avgLoss == 0) {
            rsi = 100.0; // Prevent division by zero
        } else {
            double rs = avgGain / avgLoss;
            rsi = 100.0 - (100.0 / (1.0 + rs));
        }
        
        // Generate signals based on RSI thresholds
        if (rsi >= overboughtThreshold) {
            // Overbought condition - SELL
            Position position = positions.get(symbol);
            if (position != null && position.getQuantity() > 0) {
                return createSellOrder(symbol, position.getQuantity());
            }
        } else if (rsi <= oversoldThreshold) {
            // Oversold condition - BUY
            return createBuyOrder(symbol, 1.0);
        }
        
        return null;
    }
    
    @Override
    public int getMinIndex() {
        int period = parameters.containsKey("period") ? 
                    (int) getIntegerParameter("period") : 14;
        return period + 1;
    }

    @Override
    protected boolean extraValidations() {
        // Check for both overboughtThreshold and oversoldThreshold.
        Object overboughtObj = parameters.get("overboughtThreshold");
        Object oversoldObj = parameters.get("oversoldThreshold");
        if (!(overboughtObj instanceof Number) || !(oversoldObj instanceof Number)) {
            System.out.println("RSI parameters must be numeric.");
            return false;
        }
        double overbought = ((Number) overboughtObj).doubleValue();
        double oversold = ((Number) oversoldObj).doubleValue();
        if (overbought <= oversold) {
            System.out.println("Overbought threshold must be greater than oversold threshold.");
            return false;
        }
        return true;
    }
    
    @Override
    public void reset() {
        super.reset();
        prices.clear();
        rsi = 50.0;
        currentPrice = 0.0;
    }
} 