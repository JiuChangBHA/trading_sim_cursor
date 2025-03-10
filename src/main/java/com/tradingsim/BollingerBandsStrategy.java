package com.tradingsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BollingerBandsStrategy implements TradingStrategy {
    private int period = 20;
    private double stdDevMultiplier = 2.0;
    
    @Override
    public String getName() {
        return "Bollinger Bands";
    }
    
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter period (default: " + period + "): ");
        String input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            period = Integer.parseInt(input);
        }
        
        System.out.print("Enter standard deviation multiplier (default: " + stdDevMultiplier + "): ");
        input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            stdDevMultiplier = Double.parseDouble(input);
        }
        
        System.out.println("Strategy configured: Period = " + period + 
                          ", StdDev Multiplier = " + stdDevMultiplier);
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
        
        double sma = calculateSMA(prices);
        double stdDev = calculateStdDev(prices, sma);
        
        double upperBand = sma + (stdDevMultiplier * stdDev);
        double lowerBand = sma - (stdDevMultiplier * stdDev);
        
        double currentPrice = marketData.get(marketData.size() - 1).getClose();
        
        if (currentPrice > upperBand) {
            return TradingSignal.SELL; // Price above upper band
        } else if (currentPrice < lowerBand) {
            return TradingSignal.BUY; // Price below lower band
        }
        
        return TradingSignal.HOLD;
    }
    
    private double calculateSMA(List<Double> prices) {
        double sum = 0;
        for (double price : prices) {
            sum += price;
        }
        return sum / prices.size();
    }
    
    private double calculateStdDev(List<Double> prices, double mean) {
        double sumSquaredDiff = 0;
        for (double price : prices) {
            double diff = price - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / prices.size());
    }
} 