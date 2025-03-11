package com.tradingsim.strategy;

import java.util.*;
import java.util.logging.Logger;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;

public class MovingAverageCrossoverStrategy extends BaseStrategy {
    private static final Logger LOGGER = Logger.getLogger(MovingAverageCrossoverStrategy.class.getName());
    private Queue<Double> fastPriceWindow;
    private Queue<Double> slowPriceWindow;
    private int fastPeriod;
    private int slowPeriod;
    private double lastFastMA;
    private double lastSlowMA;

    public MovingAverageCrossoverStrategy() {
        super("Moving Average Crossover", "A strategy that generates signals based on fast and slow moving average crossovers");
        this.fastPriceWindow = new LinkedList<>();
        this.slowPriceWindow = new LinkedList<>();
        this.lastFastMA = 0.0;
        this.lastSlowMA = 0.0;
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        fastPeriod = getParameter("fastPeriod", 5);
        slowPeriod = getParameter("slowPeriod", 20);
        fastPriceWindow.clear();
        slowPriceWindow.clear();
        lastFastMA = 0.0;
        lastSlowMA = 0.0;
        updateState("fastPeriod", fastPeriod);
        updateState("slowPeriod", slowPeriod);
        LOGGER.info("Strategy initialized with fast period: " + fastPeriod + ", slow period: " + slowPeriod);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        double currentPrice = marketData.getPrice();
        
        // Update price windows
        fastPriceWindow.offer(currentPrice);
        slowPriceWindow.offer(currentPrice);
        if (fastPriceWindow.size() > fastPeriod) {
            fastPriceWindow.poll();
        }
        if (slowPriceWindow.size() > slowPeriod) {
            slowPriceWindow.poll();
        }

        // Calculate moving averages
        double fastMA = fastPriceWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double slowMA = slowPriceWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Generate signals
        Order order = null;
        if (fastPriceWindow.size() == fastPeriod && slowPriceWindow.size() == slowPeriod) {
            Position currentPosition = positions.get(marketData.getSymbol());
            boolean hasPosition = currentPosition != null && currentPosition.getQuantity() != 0;

            // Check for crossovers
            boolean crossedAboveMA = fastMA > slowMA && lastFastMA <= lastSlowMA;
            boolean crossedBelowMA = fastMA < slowMA && lastFastMA >= lastSlowMA;

            if (crossedAboveMA && !hasPosition) {
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
                LOGGER.info("BUY signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            } else if (crossedBelowMA && hasPosition) {
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
                LOGGER.info("SELL signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            }
        }

        // Update state
        lastFastMA = fastMA;
        lastSlowMA = slowMA;
        updateState("fastMA", fastMA);
        updateState("slowMA", slowMA);
        updateState("currentPrice", currentPrice);
        
        return order;
    }

    @Override
    public void reset() {
        super.reset();
        // Preserve the parameters in state
        updateState("fastPeriod", fastPeriod);
        updateState("slowPeriod", slowPeriod);
        // Clear calculation state
        fastPriceWindow.clear();
        slowPriceWindow.clear();
        lastFastMA = 0.0;
        lastSlowMA = 0.0;
        LOGGER.info("Strategy reset");
    }
} 