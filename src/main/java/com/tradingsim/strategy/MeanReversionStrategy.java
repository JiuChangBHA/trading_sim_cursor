package com.tradingsim.strategy;

import java.util.*;
import java.util.logging.Logger;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;

public class MeanReversionStrategy extends BaseStrategy {
    private static final Logger LOGGER = Logger.getLogger(MeanReversionStrategy.class.getName());
    private Queue<Double> priceWindow;
    private int period;
    private double threshold;

    public MeanReversionStrategy() {
        super("Mean Reversion", "A strategy that generates signals based on price deviations from the mean");
        this.priceWindow = new LinkedList<>();
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        period = getParameter("period", 20);
        threshold = getParameter("threshold", 2.0);
        priceWindow.clear();
        updateState("period", period);
        updateState("threshold", threshold);
        LOGGER.info("Strategy initialized with period: " + period + ", threshold: " + threshold);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        double currentPrice = marketData.getPrice();
        
        // Update price window
        priceWindow.offer(currentPrice);
        if (priceWindow.size() > period) {
            priceWindow.poll();
        }

        Order order = null;
        if (priceWindow.size() == period) {
            // Calculate mean and standard deviation
            double mean = priceWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = priceWindow.stream()
                .mapToDouble(price -> Math.pow(price - mean, 2))
                .average()
                .orElse(0.0);
            double stdDev = Math.sqrt(variance);

            // Calculate z-score
            double zScore = (currentPrice - mean) / stdDev;

            Position currentPosition = positions.get(marketData.getSymbol());
            boolean hasPosition = currentPosition != null && currentPosition.getQuantity() != 0;

            if (zScore < -threshold && !hasPosition) {
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
                LOGGER.info("BUY signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            } else if (zScore > threshold && hasPosition) {
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
                LOGGER.info("SELL signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            }

            // Update state
            updateState("mean", mean);
            updateState("zScore", zScore);
            updateState("stdDev", stdDev);
        }

        updateState("currentPrice", currentPrice);
        return order;
    }

    @Override
    public void reset() {
        super.reset();
        priceWindow.clear();
        LOGGER.info("Strategy reset");
    }
} 