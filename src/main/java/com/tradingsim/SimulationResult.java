package com.tradingsim;

import java.util.List;
import com.tradingsim.model.Order;

/**
 * Stores the results of a trading simulation.
 */
public class SimulationResult {
    private final List<Order> executedOrders;
    private final List<Double> equityCurve;
    private final double initialCapital;

    public SimulationResult(List<Order> executedOrders, List<Double> equityCurve, double initialCapital) {
        this.executedOrders = executedOrders;
        this.equityCurve = equityCurve;
        this.initialCapital = initialCapital;
    }

    public List<Order> getExecutedOrders() {
        return executedOrders;
    }

    public List<Double> getEquityCurve() {
        return equityCurve;
    }

    public double getInitialCapital() {
        return initialCapital;
    }

    public double getFinalCapital() {
        return equityCurve.isEmpty() ? initialCapital : equityCurve.get(equityCurve.size() - 1);
    }

    public int getNumberOfTrades() {
        return executedOrders.size();
    }

    public double getTotalReturn() {
        return ((getFinalCapital() - initialCapital) / initialCapital) * 100;
    }
} 