package com.tradingsim.strategy;

import java.util.Map;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;

/**
 * Core interface for all trading strategies.
 * Defines the standard contract that all strategy implementations must follow.
 */
public interface TradingStrategy {
    
    /**
     * Initialize the strategy with configuration parameters
     * @param parameters Strategy-specific configuration parameters
     */
    void initialize(Map<String, Object> parameters);
    
    /**
     * Process new market data and generate trading signals
     * @param marketData Current market data
     * @param positions Current portfolio positions
     * @return Order to be executed, or null if no action should be taken
     */
    Order processMarketData(MarketData marketData, Map<String, Position> positions);
    
    /**
     * Get the name of the strategy
     * @return Strategy name
     */
    String getName();
    
    /**
     * Get the description of the strategy
     * @return Strategy description
     */
    String getDescription();
    
    /**
     * Get the current state of the strategy
     * @return Map containing strategy state variables
     */
    Map<String, Object> getState();
    
    /**
     * Get the minimum index of the market data that the strategy can process
     * This is useful for strategies that require historical data
     * @return index of the market data
     */
    int getMinIndex();
    
    /**
     * Reset the strategy to its initial state
     */
    void reset();
} 