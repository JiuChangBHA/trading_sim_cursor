package com.tradingsim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.time.LocalDate;

public class StrategyOptimizerTest {
    private TradingSimulator simulator;
    private StrategyOptimizer optimizer;
    private static final String TEST_SYMBOL = "TEST";
    private List<MarketData> testData;

    @BeforeEach
    void setUp() {
        simulator = new TradingSimulator(10000.0);
        optimizer = new StrategyOptimizer(simulator, TEST_SYMBOL);
        testData = createTestMarketData();
        
        // Load test data into simulator
        MarketDataLoader.getInstance().addTestData(TEST_SYMBOL, testData);
    }

    private List<MarketData> createTestMarketData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        // Create 100 days of test data with a clear trend pattern
        double price = 100.0;
        
        // First 30 days: flat
        for (int i = 0; i < 30; i++) {
            data.add(createMarketData(startDate.plusDays(i), price));
        }
        
        // Next 30 days: uptrend
        for (int i = 30; i < 60; i++) {
            price += 1.0;
            data.add(createMarketData(startDate.plusDays(i), price));
        }
        
        // Next 40 days: downtrend
        for (int i = 60; i < 100; i++) {
            price -= 0.5;
            data.add(createMarketData(startDate.plusDays(i), price));
        }
        
        return data;
    }

    private MarketData createMarketData(LocalDate date, double price) {
        return new MarketData(date, TEST_SYMBOL, price, price + 0.5, price - 0.5, price, 1000000);
    }

    @Test
    void testParameterCombinationGeneration() {
        optimizer.addParameterRange("param1", Arrays.asList(1, 2));
        optimizer.addParameterRange("param2", Arrays.asList("A", "B"));
        
        List<OptimizationResult> results = optimizer.optimize(new MovingAverageCrossoverStrategy());
        
        assertEquals(4, results.size(), "Should generate all possible parameter combinations");
        
        Set<String> combinations = new HashSet<>();
        for (OptimizationResult result : results) {
            String combo = result.getParameters().get("param1") + "-" + result.getParameters().get("param2");
            combinations.add(combo);
        }
        
        assertTrue(combinations.contains("1-A"));
        assertTrue(combinations.contains("1-B"));
        assertTrue(combinations.contains("2-A"));
        assertTrue(combinations.contains("2-B"));
    }

    @Test
    void testMovingAverageCrossoverOptimization() {
        // Test with known optimal parameters for our test data
        optimizer.addParameterRange("fastPeriod", Arrays.asList(5, 10, 15));
        optimizer.addParameterRange("slowPeriod", Arrays.asList(20, 25, 30));

        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        List<OptimizationResult> results = optimizer.optimize(strategy);

        assertFalse(results.isEmpty(), "Should have optimization results");
        
        // Verify results are sorted by Sharpe ratio
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i-1).getSharpeRatio() >= results.get(i).getSharpeRatio(),
                      "Results should be sorted by Sharpe ratio in descending order");
        }

        // Verify metrics for best result
        OptimizationResult best = results.get(0);
        assertNotNull(best.getParameters().get("fastPeriod"));
        assertNotNull(best.getParameters().get("slowPeriod"));
        
        // Verify parameter constraints
        for (OptimizationResult result : results) {
            int fastPeriod = (int) result.getParameters().get("fastPeriod");
            int slowPeriod = (int) result.getParameters().get("slowPeriod");
            assertTrue(fastPeriod < slowPeriod, 
                      "Fast period should be less than slow period");
        }
    }

    @Test
    void testMetricsCalculation() {
        optimizer.addParameterRange("fastPeriod", Arrays.asList(5));
        optimizer.addParameterRange("slowPeriod", Arrays.asList(20));

        List<OptimizationResult> results = optimizer.optimize(new MovingAverageCrossoverStrategy());
        assertFalse(results.isEmpty());

        OptimizationResult result = results.get(0);
        
        // Verify metric bounds
        assertTrue(result.getSharpeRatio() >= -10 && result.getSharpeRatio() <= 10,
                  "Sharpe ratio should be within reasonable bounds");
        assertTrue(result.getMaxDrawdown() >= 0 && result.getMaxDrawdown() <= 1,
                  "Max drawdown should be between 0 and 1");
        assertTrue(result.getWinRate() >= 0 && result.getWinRate() <= 1,
                  "Win rate should be between 0 and 1");
        assertTrue(result.getTotalTrades() >= 0,
                  "Total trades should be non-negative");
    }

    @Test
    void testEdgeCases() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        
        // Test with empty parameter ranges
        List<OptimizationResult> results = optimizer.optimize(strategy);
        assertTrue(results.isEmpty(), "Should handle empty parameter ranges");
        
        // Test with single parameter value
        optimizer.addParameterRange("fastPeriod", Arrays.asList(5));
        results = optimizer.optimize(strategy);
        assertEquals(1, results.size(), "Should handle single parameter value");
        
        // Test with invalid parameter combinations
        optimizer.addParameterRange("fastPeriod", Arrays.asList(30));
        optimizer.addParameterRange("slowPeriod", Arrays.asList(10));
        results = optimizer.optimize(strategy);
        assertFalse(results.isEmpty(), "Should handle invalid parameter combinations");
        
        // Verify metrics are still valid even with bad parameters
        OptimizationResult result = results.get(0);
        assertTrue(result.getSharpeRatio() >= -10 && result.getSharpeRatio() <= 10);
        assertTrue(result.getMaxDrawdown() >= 0 && result.getMaxDrawdown() <= 1);
        assertTrue(result.getWinRate() >= 0 && result.getWinRate() <= 1);
    }

    @Test
    void testParallelExecution() {
        // Add many parameter combinations to force parallel execution
        List<Integer> fastPeriods = new ArrayList<>();
        List<Integer> slowPeriods = new ArrayList<>();
        for (int i = 5; i <= 20; i++) {
            fastPeriods.add(i);
        }
        for (int i = 21; i <= 50; i++) {
            slowPeriods.add(i);
        }
        
        optimizer.addParameterRange("fastPeriod", new ArrayList<Object>(fastPeriods));
        optimizer.addParameterRange("slowPeriod", new ArrayList<Object>(slowPeriods));

        long startTime = System.currentTimeMillis();
        List<OptimizationResult> results = optimizer.optimize(new MovingAverageCrossoverStrategy());
        long endTime = System.currentTimeMillis();

        assertFalse(results.isEmpty());
        assertEquals(fastPeriods.size() * slowPeriods.size(), results.size(),
                    "Should process all parameter combinations");
        
        // Verify results are consistent
        Set<String> uniqueCombinations = new HashSet<>();
        for (OptimizationResult result : results) {
            String combo = result.getParameters().get("fastPeriod") + "-" + 
                          result.getParameters().get("slowPeriod");
            assertTrue(uniqueCombinations.add(combo),
                      "Should not have duplicate parameter combinations");
        }
    }

    @Test
    void testResultComparison() {
        OptimizationResult result1 = new OptimizationResult(
            new HashMap<>(), 1.5, 0.2, 0.1, 10, 0.6);
        OptimizationResult result2 = new OptimizationResult(
            new HashMap<>(), 1.0, 0.3, 0.15, 15, 0.7);
        
        assertTrue(result1.compareTo(result2) < 0,
                  "Higher Sharpe ratio should be preferred");
        
        // Test equality
        OptimizationResult result3 = new OptimizationResult(
            new HashMap<>(), 1.5, 0.4, 0.2, 20, 0.8);
        assertEquals(0, result1.compareTo(result3),
                    "Equal Sharpe ratios should be considered equal");
    }
} 