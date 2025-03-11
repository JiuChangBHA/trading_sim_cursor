package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
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
        
        // Create test data with a clear trend pattern
        // First 5 days increasing: [99,100,101,102,103]
        // Next 5 days decreasing: [102,101,100,99,98]
        double[] prices = {99, 100, 101, 102, 103, 102, 101, 100, 99, 98};
        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            testData.add(new MarketData(SYMBOL, price, 1000.0, LocalDateTime.now().plusDays(i), price - 0.5, price + 0.5));
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
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0));
        // Process first 5 days of data (uptrend)
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Day 6 should generate a SELL signal as fast MA crosses below slow MA
        Order order = strategy.processMarketData(testData.get(5), positions);
        assertNull(order, "Should generate a HOLD signal on day 5");
    }

    @Test
    void testSellSignalOnCrossover() {
        // Add a position to simulate selling
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0));
        
        // Process first 6 days
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Day 6 should generate a SELL signal
        LOGGER.info("Day 6: " + testData.get(5).getPrice() + " " + positions.get(SYMBOL));
        Order order = strategy.processMarketData(testData.get(6), positions);
        assertNotNull(order, "Should generate a signal on day 6");
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(SYMBOL, order.getSymbol());
    }

    @Test
    void testHoldSignalAfterCrossover() {
        // Add a position to simulate selling
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0));
        
        // Process first 6 days
        for (int i = 0; i < 7; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Day 6 should generate a SELL signal
        Order order = strategy.processMarketData(testData.get(7), positions);
        assertNull(order, "Should hold on day 7");
    }

    @Test
    void testNoSignalWithoutPosition() {
        LOGGER.info("initial positions: " + positions.toString());
        // Process data without having a position
        for (int i = 0; i < 6; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            if (i < 5) {
                assertNull(order, "Should not generate signal before crossover");
            } else {
                // On day 6, we should not generate a SELL signal without position
                assertNull(order, "Should not generate SELL signal on crossover");
            }
        }
    }

    @Test
    void testStateUpdates() {
        // Process a data point and check if state is updated
        strategy.processMarketData(testData.get(5), positions);
        Map<String, Object> state = strategy.getState();
        
        assertTrue(state.containsKey("fastMA"));
        assertTrue(state.containsKey("slowMA"));
        assertTrue(state.containsKey("currentPrice"));
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