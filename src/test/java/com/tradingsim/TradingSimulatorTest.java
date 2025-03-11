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
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.strategy.MovingAverageCrossoverStrategy;
import com.tradingsim.strategy.MeanReversionStrategy;
import com.tradingsim.strategy.RSIStrategy;
import com.tradingsim.strategy.BollingerBandsStrategy;

public class TradingSimulatorTest {
    private static final Logger LOGGER = Logger.getLogger(TradingSimulatorTest.class.getName());
    private TradingSimulator simulator;
    private List<MarketData> testData;
    private static final double DELTA = 0.001;
    private static final String TEST_SYMBOL = "TEST";
    
    @BeforeEach
    void setUp() throws IOException {
        // Load logging configuration
        InputStream configFile = getClass().getClassLoader().getResourceAsStream("logging.properties");
        if (configFile != null) {
            LogManager.getLogManager().readConfiguration(configFile);
        }
        
        simulator = new TradingSimulator(10000.0);
        
        // Create test data
        testData = createTestData();
        
        // Add test data to MarketDataLoader
        MarketDataLoader.getInstance().addTestData(TEST_SYMBOL, testData);
    }
    
    private List<MarketData> createTestData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        // First 5 days with increasing prices
        for (int i = 0; i < 5; i++) {
            double price = 100.0 + i;
            data.add(new MarketData(
                startDate.plusDays(i),
                TEST_SYMBOL,
                price,
                price + 2.0,
                price - 1.0,
                price,
                1000 + (i * 100)
            ));
        }
        
        // Next 5 days with decreasing prices
        for (int i = 0; i < 5; i++) {
            double price = 104.0 - i;
            data.add(new MarketData(
                startDate.plusDays(i + 5),
                TEST_SYMBOL,
                price,
                price + 2.0,
                price - 1.0,
                price,
                1500 + (i * 100)
            ));
        }
        
        return data;
    }
    
    @Test
    void testMarketDataLoading() {
        // Verify test data is loaded
        Map<String, List<MarketData>> marketData = MarketDataLoader.getInstance().getAllData();
        assertFalse(marketData.isEmpty(), "Market data should not be empty");
        assertTrue(marketData.containsKey(TEST_SYMBOL), "Market data should contain test symbol");
        
        // Verify data is sorted by date
        List<MarketData> symbolData = marketData.get(TEST_SYMBOL);
        for (int i = 1; i < symbolData.size(); i++) {
            assertTrue(symbolData.get(i).getDate().isAfter(symbolData.get(i-1).getDate()) || 
                      symbolData.get(i).getDate().equals(symbolData.get(i-1).getDate()),
                      "Data should be sorted by date");
        }
    }
    
    @Test
    void testMovingAverageUptrendCrossoverStrategy() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("fastPeriod", 2);
        params.put("slowPeriod", 5);
        strategy.initialize(params);
        Map<String, Position> positions = new HashMap<>();

        // Process first 5 days
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Add a position to simulate holding
        positions.put(TEST_SYMBOL, new Position(TEST_SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process day 6 (price starts decreasing)
        Order order = strategy.processMarketData(testData.get(5), positions);
        assertNotNull(order, "Should generate signal when trend changes");
        assertEquals(OrderSide.SELL, order.getSide());
    }
    
    @Test
    void testMeanReversionStrategy() {
        MeanReversionStrategy strategy = new MeanReversionStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("threshold", 1.0); // Lower threshold for test
        strategy.initialize(params);
        Map<String, Position> positions = new HashMap<>();

        // Process first 5 days to establish mean
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Add a position to simulate holding
        positions.put(TEST_SYMBOL, new Position(TEST_SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process day 6 (price starts deviating from mean)
        Order order = strategy.processMarketData(testData.get(5), positions);
        assertNotNull(order, "Should generate signal when price deviates from mean");
        assertEquals(OrderSide.SELL, order.getSide());
    }
    
    @Test
    void testRSIStrategy() {
        RSIStrategy strategy = new RSIStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("overboughtThreshold", 60.0); // Lower threshold for test
        params.put("oversoldThreshold", 40.0); // Higher threshold for test
        strategy.initialize(params);
        Map<String, Position> positions = new HashMap<>();

        // Process first 5 days
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Add a position to simulate holding
        positions.put(TEST_SYMBOL, new Position(TEST_SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process day 6 (price starts decreasing)
        Order order = strategy.processMarketData(testData.get(5), positions);
        assertNotNull(order, "Should generate signal when RSI changes");
        assertEquals(OrderSide.SELL, order.getSide());
    }
    
    @Test
    void testBollingerBandsStrategy() {
        BollingerBandsStrategy strategy = new BollingerBandsStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("stdDevMultiplier", 1.0); // Lower multiplier for test
        strategy.initialize(params);
        Map<String, Position> positions = new HashMap<>();

        // Process first 5 days
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Add a position to simulate holding
        positions.put(TEST_SYMBOL, new Position(TEST_SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process day 6 (price starts decreasing)
        Order order = strategy.processMarketData(testData.get(5), positions);
        assertNotNull(order, "Should generate signal when price touches Bollinger Band");
        assertEquals(OrderSide.SELL, order.getSide());
    }
    
    @Test
    void testSimulationExecution() {
        // Add test data to simulator
        simulator.getMarketData().put(TEST_SYMBOL, testData);
        
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("fastPeriod", 2);
        params.put("slowPeriod", 5);
        strategy.initialize(params);

        SimulationResult result = simulator.runSimulation(strategy, TEST_SYMBOL);
        assertNotNull(result);
        assertFalse(result.getEquityCurve().isEmpty());
    }
    
    @Test
    void testPerformanceMetrics() {
        // Add test data to simulator
        simulator.getMarketData().put(TEST_SYMBOL, testData);
        
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("fastPeriod", 2);
        params.put("slowPeriod", 5);
        strategy.initialize(params);

        SimulationResult result = simulator.runSimulation(strategy, TEST_SYMBOL);
        assertNotNull(result);
        assertTrue(result.getFinalCapital() >= 0);
    }
} 