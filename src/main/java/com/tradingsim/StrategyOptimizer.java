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

    public void reset() {
        this.parameterRanges.clear();
        this.results.clear();
    }

    public void addParameterRange(String paramName, List<Object> values) {
        parameterRanges.put(paramName, values);
    }

    public List<OptimizationResult> optimize(TradingStrategy strategy) {
        // Clear previous results
        results.clear();
        List<Map<String, Object>> parameterCombinations = generateParameterCombinations(strategy);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<OptimizationResult>> futures = new ArrayList<>();

        // Submit tasks for parallel execution
        for (Map<String, Object> params : parameterCombinations) {
            // Create a deep copy of the parameters to avoid sharing objects between threads
            Map<String, Object> paramsCopy = new HashMap<>(params);
            futures.add(executor.submit(() -> evaluateParameters(strategy.duplicate(), paramsCopy)));
        }

        // Create a local list to collect results
        List<OptimizationResult> localResults = Collections.synchronizedList(new ArrayList<>());
        
        // Collect results
        for (Future<OptimizationResult> future : futures) {
            try {
                OptimizationResult result = future.get();
                if (result != null) {
                    localResults.add(result);
                }
            } catch (Exception e) {
                LOGGER.warning("Error evaluating parameters: " + e.getMessage());
                // Print the full stack trace for better debugging
                e.printStackTrace();
            }
        }

        executor.shutdown();
        try {
            // Wait for all tasks to complete with a timeout
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.warning("Executor interrupted: " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Add to results after all processing is complete
        results.addAll(localResults);
        
        Collections.sort(results); // Sort by Sharpe ratio
        return results;
    }

    private List<Map<String, Object>> generateParameterCombinations(TradingStrategy strategy) {
        List<Map<String, Object>> combinations = new ArrayList<>();
        if (parameterRanges.isEmpty()) {
            return combinations;
        }
        generateCombinationsRecursive(new HashMap<>(), new ArrayList<>(parameterRanges.keySet()), 0, combinations, strategy);
        return combinations;
    }

    private void generateCombinationsRecursive(Map<String, Object> current, List<String> paramNames, 
                                             int index, List<Map<String, Object>> combinations, TradingStrategy strategy) {

        if (index == paramNames.size()) {
            strategy.initialize(current);
            if (strategy.isValidParameters()) {
                combinations.add(new HashMap<>(current));
            }
            return;
        }

        String paramName = paramNames.get(index);
        List<Object> values = parameterRanges.get(paramName);
        if (values == null || values.isEmpty()) {
            generateCombinationsRecursive(current, paramNames, index + 1, combinations, strategy);
            return;
        }

        for (Object value : values) {
            current.put(paramName, value);
            generateCombinationsRecursive(current, paramNames, index + 1, combinations, strategy);
        }
        current.remove(paramName); // Clean up after recursion
    }

    private OptimizationResult evaluateParameters(TradingStrategy strategy, Map<String, Object> params) {
        try {
            // Make a defensive copy of the parameters
            Map<String, Object> paramsCopy = new HashMap<>(params);
            
            // Configure strategy with parameters
            strategy.initialize(paramsCopy);
            
            // Run simulation
            SimulationResult simResult = simulator.runSimulation(strategy, symbol);
            
            // Create defensive copies of collections from the simulation result
            List<Double> equityCurveCopy = new ArrayList<>(simResult.getEquityCurve());
            List<Order> ordersCopy = new ArrayList<>(simResult.getExecutedOrders());
            // List<LocalDate> datesCopy = new ArrayList<>(simResult.getDates());
            // Calculate metrics using the copies
            double sharpeRatio = calculateSharpeRatio(equityCurveCopy);
            double profitLoss = calculateProfitLoss(equityCurveCopy);
            double maxDrawdown = calculateMaxDrawdown(equityCurveCopy);
            int totalTrades = ordersCopy.size();
            double profitFactor = calculateProfitFactor(ordersCopy);
            double winRate = calculateWinRate(ordersCopy);
            
            return new OptimizationResult(paramsCopy, sharpeRatio, profitLoss, maxDrawdown, totalTrades, profitFactor, winRate);
        } catch (Exception e) {
            LOGGER.warning("Error in thread " + Thread.currentThread().getId() + " evaluating parameters " + params + ": " + e.getClass().getName() + ": " + e.getMessage());
            // Print the stack trace for debugging
            e.printStackTrace();
            return null;
        }
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