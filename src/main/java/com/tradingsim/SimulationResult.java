package com.tradingsim;

import java.util.List;

public class SimulationResult {
    private double initialCapital;
    private double finalCapital;
    private List<TradeExecuted> trades;
    private List<Double> equityCurve;
    private String strategyName;
    private List<MarketData> marketData;
    
    public SimulationResult(double initialCapital, double finalCapital,
                          List<TradeExecuted> trades, List<Double> equityCurve,
                          String strategyName, List<MarketData> marketData) {
        this.initialCapital = initialCapital;
        this.finalCapital = finalCapital;
        this.trades = trades;
        this.equityCurve = equityCurve;
        this.strategyName = strategyName;
        this.marketData = marketData;
    }
    
    public double getInitialCapital() { return initialCapital; }
    public double getFinalCapital() { return finalCapital; }
    public List<TradeExecuted> getTrades() { return trades; }
    public List<Double> getEquityCurve() { return equityCurve; }
    public String getStrategyName() { return strategyName; }
    public List<MarketData> getMarketData() { return marketData; }
} 