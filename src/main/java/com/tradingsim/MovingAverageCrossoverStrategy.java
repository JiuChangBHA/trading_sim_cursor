package com.tradingsim;

import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class MovingAverageCrossoverStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(MovingAverageCrossoverStrategy.class.getName());
    private int fastPeriod = 5;  // Default short MA period
    private int slowPeriod = 20; // Default long MA period
    
    @Override
    public String getName() {
        return "Moving Average Crossover";
    }
    
    @Override
    public TradingSignal generateSignal(List<MarketData> marketData) {
        if (marketData == null || marketData.size() < slowPeriod + 1) {
            return TradingSignal.HOLD; // Not enough data
        }
        
        // Calculate current and previous moving averages
        int currentIndex = marketData.size() - 1;
        int previousIndex = currentIndex - 1;
        
        // Make sure we have enough data to calculate previous values
        if (previousIndex < slowPeriod) {
            return TradingSignal.HOLD;
        }
        
        double currentFastMA = calculateSMA(marketData, currentIndex, fastPeriod);
        double currentSlowMA = calculateSMA(marketData, currentIndex, slowPeriod);
        double previousFastMA = calculateSMA(marketData, previousIndex, fastPeriod);
        double previousSlowMA = calculateSMA(marketData, previousIndex, slowPeriod);

        
        // Generate buy signal when fast MA crosses above slow MA
        if (previousFastMA <= previousSlowMA && currentFastMA > currentSlowMA) {
            return TradingSignal.BUY;
        }
        
        // Generate sell signal when fast MA crosses below slow MA
        if (previousFastMA >= previousSlowMA && currentFastMA < currentSlowMA) {
            return TradingSignal.SELL;
        }
        
        return TradingSignal.HOLD;
    }

    private double calculateSMA(List<MarketData> data, int endIndex, int period) {
        if (endIndex - period + 1 < 0) {
            throw new IllegalArgumentException("Not enough data to calculate SMA");
        }
        double sum = 0.0;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum += data.get(i).getClose();
        }
        return sum / period;
    }
    
    
    @Override
    public void configure(Scanner scanner) {
        System.out.print("Enter fast period (default " + fastPeriod + "): ");
        String input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            fastPeriod = Integer.parseInt(input);
        }
        
        System.out.print("Enter slow period (default " + slowPeriod + "): ");
        input = scanner.nextLine().trim();
        if (!input.isEmpty()) {
            slowPeriod = Integer.parseInt(input);
        }
        
        System.out.println("Strategy configured: Fast MA = " + fastPeriod + ", Slow MA = " + slowPeriod);
    }
}