package com.tradingsim;

import java.util.*;
import java.util.logging.Logger;

public class MeanReversionStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(MeanReversionStrategy.class.getName());
    private int period = 20;
    private double threshold = 2.0;
    
    @Override
    public String getName() {
        return "Mean Reversion";
    }
    
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter period (default " + period + "): ");
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            period = Integer.parseInt(input);
        }
        
        System.out.print("Enter threshold (default " + threshold + "): ");
        input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            threshold = Double.parseDouble(input);
        }
        
        System.out.println("Strategy configured: Period = " + period + ", Threshold = " + threshold);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            period = (int) parameters.get("period");
        }
        if (parameters.containsKey("threshold")) {
            threshold = (double) parameters.get("threshold");
        }
    }
    
    @Override
    public Signal generateSignal(List<MarketData> data, int currentIndex) {
        if (currentIndex < period) {
            return Signal.HOLD;
        }

        double sma = calculateSMA(data, currentIndex);
        double currentPrice = data.get(currentIndex).getClose();
        double zScore = (currentPrice - sma) / calculateStdDev(data, currentIndex, sma);

        if (zScore > threshold) {
            return Signal.SELL;
        } else if (zScore < -threshold) {
            return Signal.BUY;
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