package com.tradingsim.strategy;

import java.util.HashMap;
import java.util.Map;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;

/**
 * Base abstract class for trading strategies providing common functionality.
 */
public abstract class BaseStrategy implements TradingStrategy {
    protected String name;
    protected String description;
    protected Map<String, Object> parameters;
    protected Map<String, Object> state;

    public BaseStrategy(String name, String description) {
        this.name = name;
        this.description = description;
        this.parameters = new HashMap<>();
        this.state = new HashMap<>();
    }

    @Override
    public void initialize(Map<String, Object> parameters) {
        this.parameters = new HashMap<>(parameters);
        reset();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Object> getState() {
        return new HashMap<>(state);
    }

    @Override
    public void reset() {
        state.clear();
    }

    /**
     * Helper method to safely get a parameter value with a default
     */
    protected <T> T getParameter(String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        T value = (T) parameters.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Helper method to update the strategy state
     */
    protected void updateState(String key, Object value) {
        state.put(key, value);
    }
} 