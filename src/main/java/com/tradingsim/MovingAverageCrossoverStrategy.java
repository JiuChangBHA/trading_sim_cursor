package com.tradingsim;

import java.util.*;
import java.util.logging.Logger;

public class MovingAverageCrossoverStrategy implements TradingStrategy {
    private static final Logger LOGGER = Logger.getLogger(MovingAverageCrossoverStrategy.class.getName());
    private int fastPeriod = 5;
    private int slowPeriod = 20;

    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("fastPeriod")) {
            fastPeriod = (int) parameters.get("fastPeriod");
        }
        if (parameters.containsKey("slowPeriod")) {
            slowPeriod = (int) parameters.get("slowPeriod");
        }
    }

    @Override
    public Signal generateSignal(List<MarketData> data, int currentIndex) {
        if (currentIndex < slowPeriod) {
            return Signal.HOLD;
        }

        double fastMA = calculateSMA(data, currentIndex, fastPeriod);
        double slowMA = calculateSMA(data, currentIndex, slowPeriod);
        double prevFastMA = calculateSMA(data, currentIndex - 1, fastPeriod);
        double prevSlowMA = calculateSMA(data, currentIndex - 1, slowPeriod);

        LOGGER.info("Fast MA: " + fastMA + ", Slow MA: " + slowMA);

        if (fastMA > slowMA && prevFastMA <= prevSlowMA) {
            LOGGER.info("BUY signal generated at index " + currentIndex);
            return Signal.BUY;
        } else if (fastMA < slowMA && prevFastMA >= prevSlowMA) {
            LOGGER.info("SELL signal generated at index " + currentIndex);
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    private double calculateSMA(List<MarketData> data, int currentIndex, int period) {
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += data.get(currentIndex - i).getClose();
        }
        return sum / period;
    }

    @Override
    public String getName() {
        return "Moving Average Crossover";
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

    @Override
    public int getPeriod() {
        return slowPeriod;
    }
}