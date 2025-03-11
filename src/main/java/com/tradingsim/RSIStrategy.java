package com.tradingsim;

import java.util.*;
import java.util.logging.Logger;

public class RSIStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(RSIStrategy.class.getName());
    private int period = 14;
    private double overboughtThreshold = 70;
    private double oversoldThreshold = 30;
    
    @Override
    public String getName() {
        return "RSI";
    }
    
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter period (default " + period + "): ");
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            period = Integer.parseInt(input);
        }
        
        System.out.print("Enter overbought threshold (default " + overboughtThreshold + "): ");
        input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            overboughtThreshold = Double.parseDouble(input);
        }
        
        System.out.print("Enter oversold threshold (default " + oversoldThreshold + "): ");
        input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            oversoldThreshold = Double.parseDouble(input);
        }
        
        System.out.println("Strategy configured: Period = " + period + 
                          ", Overbought = " + overboughtThreshold + 
                          ", Oversold = " + oversoldThreshold);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            period = (int) parameters.get("period");
        }
        if (parameters.containsKey("overboughtThreshold")) {
            overboughtThreshold = (double) parameters.get("overboughtThreshold");
        }
        if (parameters.containsKey("oversoldThreshold")) {
            oversoldThreshold = (double) parameters.get("oversoldThreshold");
        }
    }
    
    @Override
    public Signal generateSignal(List<MarketData> data, int currentIndex) {
        if (currentIndex < period + 1) {
            return Signal.HOLD;
        }

        double rsi = calculateRSI(data, currentIndex);
        
        if (rsi > overboughtThreshold) {
            return Signal.SELL;
        } else if (rsi < oversoldThreshold) {
            return Signal.BUY;
        }

        return Signal.HOLD;
    }
    
    private double calculateRSI(List<MarketData> data, int currentIndex) {
        double avgGain = 0;
        double avgLoss = 0;

        // Calculate first average gain and loss
        for (int i = currentIndex - period; i < currentIndex; i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change >= 0) {
                avgGain += change;
            } else {
                avgLoss += -change;
            }
        }

        avgGain /= period;
        avgLoss /= period;

        // Calculate RSI
        if (avgLoss == 0) {
            return 100;
        }
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    @Override
    public int getPeriod() {
        return period;
    }
} 