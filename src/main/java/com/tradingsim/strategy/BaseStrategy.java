package com.tradingsim.strategy;

import java.util.HashMap;
import java.util.Map;
import com.tradingsim.model.Order;

/**
 * Base implementation of the TradingStrategy interface with common functionality
 */
public abstract class BaseStrategy implements TradingStrategy {
    protected Map<String, Object> parameters = new HashMap<>();
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        this.parameters.clear();
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
        reset();
    }
    
    @Override
    public void reset() {
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

    /**
     * Check if the parameters are valid
     * @return true if the parameters are valid, false otherwise
     */

     @Override
     public boolean isValidParameters() {
        // Common validations:
        if (parameters == null || parameters.isEmpty()) {
            System.out.println("Parameters map is null or empty.");
            return false;
        }
        if (parameters.containsKey("period")) {
            Object periodObj = getIntegerParameter("period");
            int period = (Integer) periodObj;
                if (period <= 0) {
                    System.out.println("Parameter 'period' must be greater than 0.");
                    return false;
                }
            }
        
        // Call subclass-specific validations.
        return extraValidations();
    }

    /**
     * Subclasses can override this method to add their own parameter validations.
     */
    protected boolean extraValidations() {
        return true;
    }

    protected int getIntegerParameter(String key) {
        Object value = parameters.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return (int) Math.floor(((Number) value).doubleValue());
        }
        throw new IllegalArgumentException("Parameter '" + key + "' must be an Integer or a Number.");
    }

    protected double getDoubleParameter(String key) {
        Object value = parameters.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("Parameter '" + key + "' must be a Double or a Number.");
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