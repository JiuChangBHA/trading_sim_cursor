package com.tradingsim.strategy;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;
import java.util.logging.*;

/**
 * A simple moving average crossover strategy.
 * Generates buy signals when price crosses above MA and sell signals when price crosses below MA.
 */
public class SimpleMovingAverageStrategy extends BaseStrategy {
    private static final Logger LOGGER = Logger.getLogger(SimpleMovingAverageStrategy.class.getName());
    private Queue<Double> priceWindow;
    private int windowSize;
    private double lastMA;
    private double lastPrice;

    public SimpleMovingAverageStrategy() {
        super("Simple Moving Average", "A basic moving average crossover strategy");
        this.priceWindow = new LinkedList<>();
        this.lastMA = 0.0;
        this.lastPrice = 0.0;
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        windowSize = getParameter("windowSize", 20);
        priceWindow.clear();
        lastMA = 0.0;
        lastPrice = 0.0;
        updateState("windowSize", windowSize);
        LOGGER.info("Strategy initialized with window size: " + windowSize);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        double currentPrice = marketData.getPrice();
        
        // Update price window
        priceWindow.offer(currentPrice);
        if (priceWindow.size() > windowSize) {
            priceWindow.poll();
        }

        // Calculate moving average
        double ma = priceWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Generate signals
        Order order = null;
        if (priceWindow.size() == windowSize) {
            Position currentPosition = positions.get(marketData.getSymbol());
            boolean hasPosition = currentPosition != null && currentPosition.getQuantity() != 0;

            // Check for crossovers
            boolean crossedAboveMA = currentPrice > ma && lastPrice <= lastMA;
            boolean crossedBelowMA = currentPrice < ma && lastPrice >= lastMA;

            if (crossedAboveMA && !hasPosition) {
                // Bullish crossover - Buy signal
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
                LOGGER.info("BUY signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            } else if (crossedBelowMA && hasPosition) {
                // Bearish crossover - Sell signal
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
                LOGGER.info("SELL signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            }
        }

        // Update state
        lastMA = ma;
        lastPrice = currentPrice;
        updateState("movingAverage", ma);
        updateState("currentPrice", currentPrice);
        
        return order;
    }

    @Override
    public void reset() {
        super.reset();
        priceWindow.clear();
        lastMA = 0.0;
        lastPrice = 0.0;
        LOGGER.info("Strategy reset");
    }
} 