package com.tradingsim;

import java.util.*;
import java.util.logging.Logger;

public class BollingerBandsStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(BollingerBandsStrategy.class.getName());
    private int period = 20;
    private double stdDevMultiplier = 2.0;
    
    @Override
    public String getName() {
        return "Bollinger Bands";
    }
    
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter period (default " + period + "): ");
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            period = Integer.parseInt(input);
        }
        
        System.out.print("Enter standard deviation multiplier (default " + stdDevMultiplier + "): ");
        input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            stdDevMultiplier = Double.parseDouble(input);
        }
        
        System.out.println("Strategy configured: Period = " + period + ", StdDev Multiplier = " + stdDevMultiplier);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            period = (int) parameters.get("period");
        }
        if (parameters.containsKey("stdDevMultiplier")) {
            stdDevMultiplier = (double) parameters.get("stdDevMultiplier");
        }
    }
    
    @Override
    public Signal generateSignal(List<MarketData> data, int currentIndex) {
        if (currentIndex < period) {
            return Signal.HOLD;
        }

        double sma = calculateSMA(data, currentIndex);
        double stdDev = calculateStdDev(data, currentIndex, sma);
        double upperBand = sma + (stdDev * stdDevMultiplier);
        double lowerBand = sma - (stdDev * stdDevMultiplier);
        double currentPrice = data.get(currentIndex).getClose();

        if (currentPrice < lowerBand) {
            return Signal.BUY;
        } else if (currentPrice > upperBand) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }
    
    private double calculateSMA(List<MarketData> data, int currentIndex) {
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += data.get(currentIndex - i).getClose();
        }
        return sum / period;
    }
    
    private double calculateStdDev(List<MarketData> data, int currentIndex, double mean) {
        double sumSquaredDiff = 0;
        for (int i = 0; i < period; i++) {
            double diff = data.get(currentIndex - i).getClose() - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / period);
    }

    @Override
    public int getPeriod() {
        return period;
    }
} 