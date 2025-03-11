package com.tradingsim.strategy;

import java.util.*;
import java.util.logging.Logger;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;

public class BollingerBandsStrategy extends BaseStrategy {
    private static final Logger LOGGER = Logger.getLogger(BollingerBandsStrategy.class.getName());
    private Queue<Double> priceWindow;
    private int period;
    private double stdDevMultiplier;

    public BollingerBandsStrategy() {
        super("Bollinger Bands", "A strategy that generates signals based on price movements relative to Bollinger Bands");
        this.priceWindow = new LinkedList<>();
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        period = getParameter("period", 20);
        stdDevMultiplier = getParameter("stdDevMultiplier", 2.0);
        priceWindow.clear();
        updateState("period", period);
        updateState("stdDevMultiplier", stdDevMultiplier);
        LOGGER.info("Strategy initialized with period: " + period + ", stdDev multiplier: " + stdDevMultiplier);
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
            // Calculate SMA and standard deviation
            double sma = priceWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = priceWindow.stream()
                .mapToDouble(price -> Math.pow(price - sma, 2))
                .average()
                .orElse(0.0);
            double stdDev = Math.sqrt(variance);

            // Calculate Bollinger Bands
            double upperBand = sma + (stdDev * stdDevMultiplier);
            double lowerBand = sma - (stdDev * stdDevMultiplier);

            Position currentPosition = positions.get(marketData.getSymbol());
            boolean hasPosition = currentPosition != null && currentPosition.getQuantity() != 0;

            if (currentPrice < lowerBand && !hasPosition) {
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
                LOGGER.info("BUY signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            } else if (currentPrice > upperBand && hasPosition) {
                order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
                LOGGER.info("SELL signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
            }

            // Update state
            updateState("sma", sma);
            updateState("upperBand", upperBand);
            updateState("lowerBand", lowerBand);
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