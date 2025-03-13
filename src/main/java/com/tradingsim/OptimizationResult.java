package com.tradingsim;

import java.util.Map;

/**
 * Stores the result of a strategy optimization run.
 */
public class OptimizationResult implements Comparable<OptimizationResult> {
    private final Map<String, Object> parameters;
    private final double sharpeRatio;
    private final double profitLoss;
    private final double maxDrawdown;
    private final int totalTrades;
    private final double profitFactor;
    private double winRate; // Optional, calculated separately if needed

    public OptimizationResult(Map<String, Object> parameters, double sharpeRatio, double profitLoss, 
                             double maxDrawdown, int totalTrades, double profitFactor, double winRate) {
        this.parameters = parameters;
        this.sharpeRatio = sharpeRatio;
        this.profitLoss = profitLoss;
        this.maxDrawdown = maxDrawdown;
        this.totalTrades = totalTrades;
        this.profitFactor = profitFactor;
        this.winRate = winRate;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public double getSharpeRatio() {
        return sharpeRatio;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public int getTotalTrades() {
        return totalTrades;
    }

    public double getProfitFactor() {
        return profitFactor;
    }
    
    public double getWinRate() {
        return winRate;
    }
    
    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
    

    @Override
    public int compareTo(OptimizationResult other) {
        // Sort by Sharpe ratio in descending order
        return Double.compare(other.sharpeRatio, this.sharpeRatio);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parameters: ");
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
        }
        sb.append("Sharpe: ").append(String.format("%.2f", sharpeRatio));
        sb.append(", P/L: ").append(String.format("%.2f%%", profitLoss * 100));
        sb.append(", MaxDD: ").append(String.format("%.2f%%", maxDrawdown * 100));
        sb.append(", Trades: ").append(totalTrades);
        sb.append(", ProfitFactor: ").append(String.format("%.2f", profitFactor));
        if (winRate > 0) {
            sb.append(", WinRate: ").append(String.format("%.2f%%", winRate * 100));
        }
        return sb.toString();
    }
} 