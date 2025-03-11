package com.tradingsim.strategy;

import java.util.List;
import java.util.Map;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;

/**
 * Interface for all trading strategies
 */
public interface TradingStrategy {
    /**
     * Get the name of the strategy
     * @return The strategy name
     */
    String getName();
    
    /**
     * Get a description of the strategy
     * @return The strategy description
     */
    String getDescription();
    
    /**
     * Initialize the strategy with parameters
     * @param parameters Map of parameter names to values
     */
    void initialize(Map<String, Object> parameters);
    
    /**
     * Process new market data and generate trading signals
     * @param marketData The latest market data
     * @param positions Current positions held
     * @return An order to execute, or null if no action should be taken
     */
    Order processMarketData(MarketData marketData, Map<String, Position> positions);
    
    /**
     * Get the current state of the strategy
     * @return Map of state variables
     */
    Map<String, Object> getState();
    
    /**
     * Reset the strategy state
     */
    void reset();
    
    /**
     * Get the minimum number of data points needed before the strategy can generate signals
     * @return The minimum index
     */
    int getMinIndex();
} 