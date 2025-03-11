package com.tradingsim;

import java.util.List;

public class SimulationResult {
    private final List<TradeExecuted> trades;
    private final List<Double> equityCurve;

    public SimulationResult(List<TradeExecuted> trades, List<Double> equityCurve) {
        this.trades = trades;
        this.equityCurve = equityCurve;
    }

    public List<TradeExecuted> getTrades() {
        return trades;
    }

    public List<Double> getEquityCurve() {
        return equityCurve;
    }

    public double getInitialCapital() {
        return equityCurve.isEmpty() ? 0.0 : equityCurve.get(0);
    }

    public double getFinalCapital() {
        return equityCurve.isEmpty() ? 0.0 : equityCurve.get(equityCurve.size() - 1);
    }
} 