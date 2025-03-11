package com.tradingsim;
import com.tradingsim.strategy.TradingStrategy;
import com.tradingsim.model.Order;
import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.*;

public class StrategyOptimizer {
    private static final Logger LOGGER = Logger.getLogger(StrategyOptimizer.class.getName());
    private final TradingSimulator simulator;
    private final String symbol;
    private final Map<String, List<Object>> parameterRanges;
    private final List<OptimizationResult> results;
    private final int threads;

    public StrategyOptimizer(TradingSimulator simulator, String symbol) {
        this.simulator = simulator;
        this.symbol = symbol;
        this.parameterRanges = new HashMap<>();
        this.results = new ArrayList<>();
        this.threads = Runtime.getRuntime().availableProcessors();
    }

    public void addParameterRange(String paramName, List<Object> values) {
        parameterRanges.put(paramName, values);
    }

    public List<OptimizationResult> optimize(TradingStrategy strategy) {
        List<Map<String, Object>> parameterCombinations = generateParameterCombinations();
        LOGGER.info("Starting optimization with " + parameterCombinations.size() + " parameter combinations");

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<OptimizationResult>> futures = new ArrayList<>();

        // Submit tasks for parallel execution
        for (Map<String, Object> params : parameterCombinations) {
            futures.add(executor.submit(() -> evaluateParameters(strategy, params)));
        }

        // Collect results
        for (Future<OptimizationResult> future : futures) {
            try {
                OptimizationResult result = future.get();
                results.add(result);
            } catch (Exception e) {
                LOGGER.warning("Error evaluating parameters: " + e.getMessage());
            }
        }

        executor.shutdown();
        Collections.sort(results); // Sort by Sharpe ratio
        return results;
    }

    private List<Map<String, Object>> generateParameterCombinations() {
        List<Map<String, Object>> combinations = new ArrayList<>();
        if (parameterRanges.isEmpty()) {
            return combinations;
        }
        generateCombinationsRecursive(new HashMap<>(), new ArrayList<>(parameterRanges.keySet()), 0, combinations);
        return combinations;
    }

    private void generateCombinationsRecursive(Map<String, Object> current, List<String> paramNames, 
                                             int index, List<Map<String, Object>> combinations) {
        if (index == paramNames.size()) {
            combinations.add(new HashMap<>(current));
            return;
        }

        String paramName = paramNames.get(index);
        List<Object> values = parameterRanges.get(paramName);
        if (values == null || values.isEmpty()) {
            generateCombinationsRecursive(current, paramNames, index + 1, combinations);
            return;
        }

        for (Object value : values) {
            current.put(paramName, value);
            generateCombinationsRecursive(current, paramNames, index + 1, combinations);
        }
        current.remove(paramName); // Clean up after recursion
    }

    private OptimizationResult evaluateParameters(TradingStrategy strategy, Map<String, Object> params) {
        // Configure strategy with parameters
        strategy.initialize(params);
        
        // Run simulation
        SimulationResult simResult = simulator.runSimulation(strategy, symbol);
        
        // Calculate metrics
        double sharpeRatio = calculateSharpeRatio(simResult.getEquityCurve());
        double profitLoss = calculateProfitLoss(simResult.getEquityCurve());
        double maxDrawdown = calculateMaxDrawdown(simResult.getEquityCurve());
        int totalTrades = simResult.getExecutedOrders().size();
        double profitFactor = calculateProfitFactor(simResult.getExecutedOrders());
        double winRate = calculateWinRate(simResult.getExecutedOrders());

        return new OptimizationResult(params, sharpeRatio, profitLoss, maxDrawdown, totalTrades, profitFactor);
    }

    private double calculateSharpeRatio(List<Double> equityCurve) {
        if (equityCurve.size() < 2) return 0;

        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            returns.add((equityCurve.get(i) - equityCurve.get(i-1)) / equityCurve.get(i-1));
        }

        double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = calculateStdDev(returns);
        
        if (stdDev == 0) return 0;
        double riskFreeRate = 0.02 / 252; // 2% annual risk-free rate converted to daily
        return (meanReturn - riskFreeRate) / stdDev * Math.sqrt(252);
    }

    private double calculateProfitLoss(List<Double> equityCurve) {
        if (equityCurve.isEmpty()) return 0;
        return (equityCurve.get(equityCurve.size() - 1) - equityCurve.get(0)) / equityCurve.get(0);
    }

    private double calculateMaxDrawdown(List<Double> equityCurve) {
        double maxDrawdown = 0;
        double peak = equityCurve.get(0);
        
        for (double value : equityCurve) {
            if (value > peak) {
                peak = value;
            }
            double drawdown = (peak - value) / peak;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }
        
        return maxDrawdown;
    }

    private double calculateProfitFactor(List<Order> orders) {
        if (orders.isEmpty()) return 0;
        
        double totalProfit = 0;
        double totalLoss = 0;
        
        for (Order order : orders) {
            if (order.getProfitLoss() > 0) {
                totalProfit += order.getProfitLoss();
            } else {
                totalLoss += Math.abs(order.getProfitLoss());
            }
        }
        
        if (totalLoss == 0) return Double.POSITIVE_INFINITY;
        return totalProfit / totalLoss;
    }

    private double calculateWinRate(List<Order> orders) {
        if (orders.isEmpty()) return 0;
        
        long profitableTrades = orders.stream()
            .filter(order -> order.getProfitLoss() > 0)
            .count();
            
        return (double) profitableTrades / orders.size();
    }

    private double calculateStdDev(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sumSquaredDiff = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .sum();
        return Math.sqrt(sumSquaredDiff / values.size());
    }
} 