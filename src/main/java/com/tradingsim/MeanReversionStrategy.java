package com.tradingsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MeanReversionStrategy implements TradingStrategy {
    private int period = 20;
    private double threshold = 1.5;
    
    @Override
    public String getName() {
        return "Mean Reversion";
    }
    
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter lookback period (default: " + period + "): ");
        String input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            period = Integer.parseInt(input);
        }
        
        System.out.print("Enter z-score threshold (default: " + threshold + "): ");
        input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            threshold = Double.parseDouble(input);
        }
        
        System.out.println("Strategy configured: Period = " + period + 
                          ", Threshold = " + threshold);
    }
    
    @Override
    public TradingSignal generateSignal(List<MarketData> marketData) {
        if (marketData.size() <= period) {
            return TradingSignal.HOLD; // Not enough data
        }
        
        List<Double> prices = new ArrayList<>();
        for (int i = marketData.size() - period; i < marketData.size(); i++) {
            prices.add(marketData.get(i).getClose());
        }
        
        double mean = calculateMean(prices);
        double stdDev = calculateStdDev(prices, mean);
        double currentPrice = marketData.get(marketData.size() - 1).getClose();
        
        // Calculate z-score (how many standard deviations from mean)
        double zScore = (currentPrice - mean) / stdDev;
        
        if (zScore > threshold) {
            return TradingSignal.SELL; // Price too high, expect reversion
        } else if (zScore < -threshold) {
            return TradingSignal.BUY; // Price too low, expect reversion
        }
        
        return TradingSignal.HOLD;
    }
    
    private double calculateMean(List<Double> values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }
    
    private double calculateStdDev(List<Double> values, double mean) {
        double sumSquaredDiff = 0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / values.size());
    }
} 