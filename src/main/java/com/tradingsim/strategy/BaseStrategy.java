package com.tradingsim.strategy;

import java.util.HashMap;
import java.util.Map;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;

/**
 * Base implementation of the TradingStrategy interface with common functionality
 */
public abstract class BaseStrategy implements TradingStrategy {
    protected Map<String, Object> parameters = new HashMap<>();
    protected Map<String, Object> state = new HashMap<>();
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        this.parameters.clear();
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
        reset();
    }
    
    @Override
    public Map<String, Object> getState() {
        return new HashMap<>(state);
    }
    
    @Override
    public void reset() {
        state.clear();
        // Copy parameters to state for easy access
        state.putAll(parameters);
    }
    
    @Override
    public int getMinIndex() {
        // Default implementation - override in specific strategies if needed
        return 0;
    }
    
    /**
     * Helper method to create a buy order
     * @param symbol The symbol to buy
     * @param quantity The quantity to buy
     * @return A new buy order
     */
    protected Order createBuyOrder(String symbol, double quantity) {
        return new Order(symbol, Order.OrderType.MARKET, Order.OrderSide.BUY, quantity);
    }
    
    /**
     * Helper method to create a sell order
     * @param symbol The symbol to sell
     * @param quantity The quantity to sell
     * @return A new sell order
     */
    protected Order createSellOrder(String symbol, double quantity) {
        return new Order(symbol, Order.OrderType.MARKET, Order.OrderSide.SELL, quantity);
    }

    @Override
    public TradingStrategy duplicate() {
        try {
            // Create a new instance of the same class
            BaseStrategy newStrategy = getClass().getDeclaredConstructor().newInstance();
            // Copy the parameters
            newStrategy.initialize(new HashMap<>(parameters));
            return newStrategy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to duplicate strategy: " + e.getMessage(), e);
        }
    }
} 