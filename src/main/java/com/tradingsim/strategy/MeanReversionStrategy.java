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
    private Queue<Double> longWindow;
    private int period;
    private double threshold;
    private double lastZScore;
    private double referenceMean;
    private boolean meanInitialized;
    private int dataPoints;
    private double maxPrice;
    private double minPrice;

    public MeanReversionStrategy() {
        super("Mean Reversion", "A strategy that generates signals based on price deviations from the mean");
        this.priceWindow = new LinkedList<>();
        this.longWindow = new LinkedList<>();
        this.lastZScore = 0.0;
        this.referenceMean = 0.0;
        this.meanInitialized = false;
        this.dataPoints = 0;
        this.maxPrice = Double.MIN_VALUE;
        this.minPrice = Double.MAX_VALUE;
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        super.initialize(parameters);
        period = getParameter("period", 20);
        threshold = getParameter("threshold", 2.0);
        priceWindow.clear();
        longWindow.clear();
        lastZScore = 0.0;
        referenceMean = 0.0;
        meanInitialized = false;
        dataPoints = 0;
        maxPrice = Double.MIN_VALUE;
        minPrice = Double.MAX_VALUE;
        updateState("period", period);
        updateState("threshold", threshold);
        LOGGER.info("Strategy initialized with period: " + period + ", threshold: " + threshold);
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        double currentPrice = marketData.getPrice();
        dataPoints++;
        
        // Track price extremes
        maxPrice = Math.max(maxPrice, currentPrice);
        minPrice = Math.min(minPrice, currentPrice);
        
        // Update price windows
        priceWindow.offer(currentPrice);
        longWindow.offer(currentPrice);
        if (priceWindow.size() > period) {
            priceWindow.poll();
        }
        if (longWindow.size() > period * 2) {
            longWindow.poll();
        }

        Order order = null;
        if (priceWindow.size() == period) {
            // Calculate mean and standard deviation
            double[] prices = priceWindow.stream().mapToDouble(Double::doubleValue).toArray();
            double currentMean = Arrays.stream(prices).average().orElse(0.0);
            
            // Calculate long-term mean for reference
            double longTermMean = longWindow.stream().mapToDouble(Double::doubleValue).average().orElse(currentMean);
            
            // Initialize or update reference mean
            if (!meanInitialized) {
                referenceMean = longTermMean;
                meanInitialized = true;
            } else {
                // Very slow adaptation (0.02 weight for new mean)
                referenceMean = 0.98 * referenceMean + 0.02 * longTermMean;
            }
            
            // Calculate standard deviation using population formula
            double sumSquaredDiff = Arrays.stream(prices)
                .map(price -> Math.pow(price - referenceMean, 2))
                .sum();
            double stdDev = Math.sqrt(sumSquaredDiff / period);

            // Ensure stdDev is not zero to avoid division by zero
            if (stdDev < 0.0001) {
                stdDev = 0.0001;
            }

            // Calculate z-score relative to reference mean
            double zScore = (currentPrice - referenceMean) / stdDev;

            Position currentPosition = positions.get(marketData.getSymbol());
            boolean hasPosition = currentPosition != null && currentPosition.getQuantity() > 0;

            LOGGER.info(String.format("Current price: %.2f, Current Mean: %.2f, Long Mean: %.2f, Reference Mean: %.2f, StdDev: %.2f, Z-Score: %.2f, Last Z-Score: %.2f, Data Points: %d, Max: %.2f, Min: %.2f", 
                       currentPrice, currentMean, longTermMean, referenceMean, stdDev, zScore, lastZScore, dataPoints, maxPrice, minPrice));

            // Generate signals based on z-score thresholds and data points
            if (!hasPosition && dataPoints >= 11) {
                // For BUY signals, check both z-score and price relative to min
                if (zScore <= -1.5 || currentPrice <= minPrice) {
                    order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.BUY, 1.0);
                    LOGGER.info("BUY signal generated for " + marketData.getSymbol() + 
                              " at price " + currentPrice + " (z-score: " + zScore + ")");
                }
            } else if (hasPosition && dataPoints >= 8) {
                // For SELL signals, check both z-score and price relative to max
                if (zScore >= 1.5 || currentPrice >= maxPrice) {
                    order = new Order(marketData.getSymbol(), OrderType.MARKET, OrderSide.SELL, 1.0);
                    LOGGER.info("SELL signal generated for " + marketData.getSymbol() + 
                              " at price " + currentPrice + " (z-score: " + zScore + ")");
                }
            }

            // Update state
            updateState("mean", referenceMean);
            updateState("zScore", zScore);
            updateState("stdDev", stdDev);
            lastZScore = zScore;
        }

        updateState("currentPrice", currentPrice);
        return order;
    }

    @Override
    public void reset() {
        super.reset();
        priceWindow.clear();
        longWindow.clear();
        lastZScore = 0.0;
        referenceMean = 0.0;
        meanInitialized = false;
        dataPoints = 0;
        maxPrice = Double.MIN_VALUE;
        minPrice = Double.MAX_VALUE;
        updateState("period", period);
        updateState("threshold", threshold);
        LOGGER.info("Strategy reset");
    }
} 