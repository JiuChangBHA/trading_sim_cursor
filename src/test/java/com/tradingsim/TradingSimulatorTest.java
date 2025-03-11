package com.tradingsim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.io.InputStream;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

public class TradingSimulatorTest {
    private static final Logger LOGGER = Logger.getLogger(TradingSimulatorTest.class.getName());
    private TradingSimulator simulator;
    private List<MarketData> testData;
    private static final double DELTA = 0.001;
    
    @BeforeEach
    void setUp() throws IOException {
        // Load logging configuration
        InputStream configFile = getClass().getClassLoader().getResourceAsStream("logging.properties");
        if (configFile != null) {
            LogManager.getLogManager().readConfiguration(configFile);
        }
        
        simulator = new TradingSimulator(10000.0);
        simulator.loadMarketData();  // Load market data before tests
        testData = new ArrayList<>();
        
        // Add test market data with clear trends
        // First 5 days with increasing prices
        testData.add(new MarketData(LocalDate.of(2024, 1, 1), "TEST", 100.0, 102.0, 99.0, 101.0, 1000));
        testData.add(new MarketData(LocalDate.of(2024, 1, 2), "TEST", 101.0, 103.0, 100.0, 102.0, 1100));
        testData.add(new MarketData(LocalDate.of(2024, 1, 3), "TEST", 102.0, 104.0, 101.0, 103.0, 1200));
        testData.add(new MarketData(LocalDate.of(2024, 1, 4), "TEST", 103.0, 105.0, 102.0, 104.0, 1300));
        testData.add(new MarketData(LocalDate.of(2024, 1, 5), "TEST", 104.0, 106.0, 103.0, 105.0, 1400));
        
        // Next 5 days with decreasing prices
        testData.add(new MarketData(LocalDate.of(2024, 1, 6), "TEST", 103.0, 105.0, 102.0, 103.0, 1500));
        testData.add(new MarketData(LocalDate.of(2024, 1, 7), "TEST", 102.0, 104.0, 101.0, 102.0, 1600));
        testData.add(new MarketData(LocalDate.of(2024, 1, 8), "TEST", 101.0, 103.0, 100.0, 101.0, 1700));
        testData.add(new MarketData(LocalDate.of(2024, 1, 9), "TEST", 100.0, 102.0, 99.0, 100.0, 1800));
        testData.add(new MarketData(LocalDate.of(2024, 1, 10), "TEST", 99.0, 101.0, 98.0, 99.0, 1900));
    }
    
    @Test
    void testMarketDataLoading() {
        try {
            simulator.loadMarketData();
            assertFalse(simulator.getMarketData().isEmpty(), "Market data should not be empty");
            
            // Verify data is sorted by date for each symbol
            for (List<MarketData> data : simulator.getMarketData().values()) {
                for (int i = 1; i < data.size(); i++) {
                    assertTrue(data.get(i).getDate().isAfter(data.get(i-1).getDate()) || 
                              data.get(i).getDate().equals(data.get(i-1).getDate()),
                              "Data should be sorted by date");
                }
            }
        } catch (IOException e) {
            fail("Failed to load market data: " + e.getMessage());
        }
    }
    
    @Test
    void testMovingAverageUptrendCrossoverStrategy() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("fastPeriod", 2);
        params.put("slowPeriod", 5);
        strategy.initialize(params);

        // Test uptrend scenario (first 5 days)
        assertEquals(Signal.HOLD, strategy.generateSignal(testData, 4));
        assertEquals(Signal.HOLD, strategy.generateSignal(testData, 5));
        assertEquals(Signal.SELL, strategy.generateSignal(testData, 6));
    }
    
    @Test
    void testMovingAverageDowntrendCrossoverStrategy() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("fastPeriod", 2);
        params.put("slowPeriod", 5);
        strategy.initialize(params);

        // Test downtrend scenario (last 5 days)
        assertEquals(Signal.HOLD, strategy.generateSignal(testData, 7));
        assertEquals(Signal.HOLD, strategy.generateSignal(testData, 8));
    }

    @Test
    void testMeanReversionStrategy() {
        MeanReversionStrategy strategy = new MeanReversionStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("threshold", 2.0);
        strategy.initialize(params);

        // Test mean reversion signals
        assertEquals(Signal.HOLD, strategy.generateSignal(testData, 4));
        assertEquals(Signal.SELL, strategy.generateSignal(testData, 5));
        assertEquals(Signal.BUY, strategy.generateSignal(testData, 9));
    }
    
    @Test
    void testRSIStrategy() {
        RSIStrategy strategy = new RSIStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("overboughtThreshold", 70.0);
        params.put("oversoldThreshold", 30.0);
        strategy.initialize(params);

        // Test RSI signals
        assertEquals(Signal.HOLD, strategy.generateSignal(testData, 4));
        assertEquals(Signal.SELL, strategy.generateSignal(testData, 5));
        assertEquals(Signal.BUY, strategy.generateSignal(testData, 9));
    }
    
    @Test
    void testBollingerBandsStrategy() {
        BollingerBandsStrategy strategy = new BollingerBandsStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("stdDevMultiplier", 2.0);
        strategy.initialize(params);

        // Test Bollinger Bands signals
        assertEquals(Signal.HOLD, strategy.generateSignal(testData, 4));
        assertEquals(Signal.SELL, strategy.generateSignal(testData, 5));
        assertEquals(Signal.BUY, strategy.generateSignal(testData, 9));
    }
    
    @Test
    void testSimulationExecution() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("shortPeriod", 2);
        params.put("longPeriod", 5);
        strategy.initialize(params);

        SimulationResult result = simulator.runSimulation(strategy, "AAPL");
        assertNotNull(result);
        assertFalse(result.getTrades().isEmpty());
        assertFalse(result.getEquityCurve().isEmpty());
    }
    
    @Test
    void testPerformanceMetrics() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("shortPeriod", 2);
        params.put("longPeriod", 5);
        strategy.initialize(params);

        SimulationResult result = simulator.runSimulation(strategy, "AAPL");
        assertNotNull(result);
        assertTrue(result.getFinalCapital() > 0);
    }
} 