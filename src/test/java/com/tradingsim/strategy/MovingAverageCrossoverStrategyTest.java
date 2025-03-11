package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.*;
import java.io.IOException;
import com.tradingsim.model.*;
import com.tradingsim.model.Order.OrderSide;

class MovingAverageCrossoverStrategyTest {
    private static final Logger LOGGER = Logger.getLogger(MovingAverageCrossoverStrategyTest.class.getName());
    private MovingAverageCrossoverStrategy strategy;
    private List<MarketData> testData;
    private Map<String, Position> positions;
    private static final String SYMBOL = "TEST";

    @BeforeAll
    static void setupLogger() {
        try {
            LogManager.getLogManager().readConfiguration(
                MovingAverageCrossoverStrategyTest.class.getClassLoader().getResourceAsStream("logging.properties")
            );
            LOGGER.info("Logging configured successfully");
        } catch (IOException e) {
            LOGGER.severe("Could not load logging.properties file: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        LOGGER.info("Setting up test case");
        strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("fastPeriod", 2);
        params.put("slowPeriod", 5);
        strategy.initialize(params);

        testData = new ArrayList<>();
        positions = new HashMap<>();
        
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        // Create test data with a clear trend pattern
        // First 5 days with steady uptrend
        for (int i = 0; i < 5; i++) {
            double price = 100.0 + (i * 2.0); // 100, 102, 104, 106, 108
            testData.add(new MarketData(
                startDate.plusDays(i),
                SYMBOL,
                price,
                price + 1.0,
                price - 1.0,
                price,
                1000L
            ));
        }
        
        // Next 5 days with sharp downtrend to create crossover
        for (int i = 0; i < 5; i++) {
            double price = 108.0 - (i * 4.0); // 108, 104, 100, 96, 92
            testData.add(new MarketData(
                startDate.plusDays(i + 5),
                SYMBOL,
                price,
                price + 1.0,
                price - 1.0,
                price,
                1000L
            ));
        }
    }

    @Test
    void testInitialization() {
        Map<String, Object> state = strategy.getState();
        assertEquals(2, state.get("fastPeriod"));
        assertEquals(5, state.get("slowPeriod"));
    }

    @Test
    void testNoSignalBeforeSufficientData() {
        // Test first few data points before we have enough for moving averages
        for (int i = 0; i < 4; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            assertNull(order, "Should not generate signal before having sufficient data");
        }
    }

    @Test
    void testHoldSignalOnCrossover() {
        // Add a position to simulate holding
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process first 5 days of data (uptrend)
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Day 5 should not generate a signal yet
        Order order = strategy.processMarketData(testData.get(5), positions);
        assertNull(order, "Should not generate a signal on day 5 yet");
    }

    @Test
    void testSellSignalOnCrossover() {
        // Add a position to simulate selling
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process first 6 days to establish trend
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process day 7 which should trigger crossover
        Order order = strategy.processMarketData(testData.get(7), positions);
        assertNotNull(order, "Should generate a SELL signal on crossover");
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(SYMBOL, order.getSymbol());
    }

    @Test
    void testNoSignalWithoutPosition() {
        // Process data without having a position
        for (int i = 0; i < 8; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            assertNull(order, "Should not generate SELL signal without position");
        }
    }

    @Test
    void testBuySignalOnCrossover() {
        // Process first 8 days to establish downtrend
        for (int i = 0; i < 8; i++) {
            LOGGER.info("Processing data point: " + testData.get(i));
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Create a new uptrend to trigger buy signal
        for (int i = 0; i < 3; i++) {
            double price = 104.0 + (i * 4.0); // 104, 108, 112 - starting from day 8's price
            testData.add(new MarketData(
                LocalDate.of(2024, 1, 14).plusDays(i),
                SYMBOL,
                price,
                price + 1.0,
                price - 1.0,
                price,
                1000L
            ));
        }
        
        // Process the new uptrend data
        strategy.processMarketData(testData.get(10), positions);
        Order order = strategy.processMarketData(testData.get(11), positions);
        
        assertNotNull(order, "Should generate a BUY signal on upward crossover");
        assertEquals(OrderSide.BUY, order.getSide());
    }

    @Test
    void testReset() {
        // Process some data
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Reset the strategy
        strategy.reset();
        
        // Verify state is reset
        Map<String, Object> state = strategy.getState();
        assertEquals(2, state.get("fastPeriod"));
        assertEquals(5, state.get("slowPeriod"));
        
        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 