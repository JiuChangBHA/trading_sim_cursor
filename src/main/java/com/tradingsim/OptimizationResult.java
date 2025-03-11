package com.tradingsim;

import java.util.Map;

public class OptimizationResult implements Comparable<OptimizationResult> {
    private final Map<String, Object> parameters;
    private final double sharpeRatio;
    private final double profitLoss;
    private final double maxDrawdown;
    private final int totalTrades;
    private final double winRate;

    public OptimizationResult(Map<String, Object> parameters, double sharpeRatio, 
                            double profitLoss, double maxDrawdown, 
                            int totalTrades, double winRate) {
        this.parameters = parameters;
        this.sharpeRatio = sharpeRatio;
        this.profitLoss = profitLoss;
        this.maxDrawdown = maxDrawdown;
        this.totalTrades = totalTrades;
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

    public double getWinRate() {
        return winRate;
    }

    @Override
    public int compareTo(OptimizationResult other) {
        // Primary sort by Sharpe ratio
        return Double.compare(other.sharpeRatio, this.sharpeRatio);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Parameters: ").append(parameters).append("\n");
        sb.append(String.format("Sharpe Ratio: %.2f\n", sharpeRatio));
        sb.append(String.format("Profit/Loss: %.2f%%\n", profitLoss * 100));
        sb.append(String.format("Max Drawdown: %.2f%%\n", maxDrawdown));
        sb.append(String.format("Total Trades: %d\n", totalTrades));
        sb.append(String.format("Win Rate: %.2f%%\n", winRate * 100));
        return sb.toString();
    }
} 