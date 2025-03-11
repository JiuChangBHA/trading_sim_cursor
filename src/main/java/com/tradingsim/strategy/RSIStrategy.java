package com.tradingsim.strategy;

import java.util.*;
import java.util.logging.Logger;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;

public class RSIStrategy extends BaseStrategy {
    private static final Logger LOGGER = Logger.getLogger(RSIStrategy.class.getName());
    private Queue<Double> priceWindow;
    private int period;
    private double overboughtThreshold;
    private double oversoldThreshold;
    private Double lastPrice;

    public RSIStrategy() {
        super("RSI", "A strategy that generates signals based on Relative Strength Index");
        this.priceWindow = new LinkedList<>();
        this.lastPrice = null;
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        period = getParameter("period", 14);
        overboughtThreshold = getParameter("overboughtThreshold", 70.0);
        oversoldThreshold = getParameter("oversoldThreshold", 30.0);
        priceWindow.clear();
        lastPrice = null;
        updateState("period", period);
        updateState("overboughtThreshold", overboughtThreshold);
        updateState("oversoldThreshold", oversoldThreshold);
        LOGGER.info("Strategy initialized with period: " + period + 
                   ", overbought: " + overboughtThreshold + 
                   ", oversold: " + oversoldThreshold);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        double currentPrice = marketData.getPrice();
        Order order = null;

        if (lastPrice != null) {
            double change = currentPrice - lastPrice;
            priceWindow.offer(change);
            if (priceWindow.size() > period) {
                priceWindow.poll();
            }

            if (priceWindow.size() == period) {
                // Calculate RSI
                double avgGain = 0;
                double avgLoss = 0;
                for (double priceChange : priceWindow) {
                    if (priceChange >= 0) {
                        avgGain += priceChange;
                    } else {
                        avgLoss -= priceChange;
                    }
                }
                avgGain /= period;
                avgLoss /= period;

                double rsi = 100.0;
                if (avgLoss > 0) {
                    double rs = avgGain / avgLoss;
                    rsi = 100.0 - (100.0 / (1.0 + rs));
                }

                Position currentPosition = positions.get(marketData.getSymbol());
                boolean hasPosition = currentPosition != null && currentPosition.getQuantity() != 0;

                if (rsi < oversoldThreshold && !hasPosition) {
                    order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
                    LOGGER.info("BUY signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
                } else if (rsi > overboughtThreshold && hasPosition) {
                    order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
                    LOGGER.info("SELL signal generated for " + marketData.getSymbol() + " at price " + currentPrice);
                }

                updateState("rsi", rsi);
            }
        }

        lastPrice = currentPrice;
        updateState("currentPrice", currentPrice);
        return order;
    }

    @Override
    public void reset() {
        super.reset();
        priceWindow.clear();
        lastPrice = null;
        LOGGER.info("Strategy reset");
    }
} 