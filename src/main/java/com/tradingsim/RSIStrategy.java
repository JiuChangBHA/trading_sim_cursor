package com.tradingsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RSIStrategy implements TradingStrategy {
    private int period = 14;
    private int oversoldThreshold = 30;
    private int overboughtThreshold = 70;
    
    @Override
    public String getName() {
        return "Relative Strength Index (RSI)";
    }
    
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter RSI period (default: " + period + "): ");
        String input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            period = Integer.parseInt(input);
        }
        
        System.out.print("Enter oversold threshold (default: " + oversoldThreshold + "): ");
        input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            oversoldThreshold = Integer.parseInt(input);
        }
        
        System.out.print("Enter overbought threshold (default: " + overboughtThreshold + "): ");
        input = scanner.nextLine();
        if (!input.trim().isEmpty()) {
            overboughtThreshold = Integer.parseInt(input);
        }
        
        System.out.println("Strategy configured: Period = " + period + 
                          ", Oversold = " + oversoldThreshold + 
                          ", Overbought = " + overboughtThreshold);
    }
    
    @Override
    public TradingSignal generateSignal(List<MarketData> marketData) {
        if (marketData.size() <= period + 1) {
            return TradingSignal.HOLD; // Not enough data
        }
        
        double rsi = calculateRSI(marketData);
        
        if (rsi < oversoldThreshold) {
            return TradingSignal.BUY; // Oversold condition
        } else if (rsi > overboughtThreshold) {
            return TradingSignal.SELL; // Overbought condition
        }
        
        return TradingSignal.HOLD;
    }
    
    private double calculateRSI(List<MarketData> data) {
        // Extract close prices
        List<Double> prices = new ArrayList<>();
        for (int i = data.size() - period - 1; i < data.size(); i++) {
            prices.add(data.get(i).getClose());
        }
        
        // Calculate price changes
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();
        
        for (int i = 1; i < prices.size(); i++) {
            double change = prices.get(i) - prices.get(i - 1);
            gains.add(Math.max(0, change));
            losses.add(Math.max(0, -change));
        }
        
        // Calculate average gains and losses
        double avgGain = 0;
        double avgLoss = 0;
        
        for (int i = 0; i < period; i++) {
            avgGain += gains.get(i);
            avgLoss += losses.get(i);
        }
        
        avgGain /= period;
        avgLoss /= period;
        
        // Calculate remaining averages using smoothing
        for (int i = period; i < gains.size(); i++) {
            avgGain = ((period - 1) * avgGain + gains.get(i)) / period;
            avgLoss = ((period - 1) * avgLoss + losses.get(i)) / period;
        }
        
        // Calculate RSI
        if (avgLoss == 0) {
            return 100;
        }
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
} 