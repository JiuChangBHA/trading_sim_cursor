package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import com.tradingsim.model.*;
import com.tradingsim.model.Order.OrderSide;

class MeanReversionStrategyTest {
    private MeanReversionStrategy strategy;
    private List<MarketData> testData;
    private Map<String, Position> positions;
    private static final String SYMBOL = "TEST";

    @BeforeEach
    void setUp() {
        strategy = new MeanReversionStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("threshold", 2.0);
        strategy.initialize(params);

        testData = new ArrayList<>();
        positions = new HashMap<>();
        
        // Create test data with a clear mean reversion pattern
        // Start with stable prices, then extreme deviation up, then reversion to mean
        double[] prices = {100, 100, 100, 100, 100,  // Stable period (mean = 100)
                         120, 140, 160, 140, 120,     // Extreme deviation up and reversion
                         80, 60, 40, 60, 80};         // Extreme deviation down and reversion
        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            testData.add(new MarketData(SYMBOL, price, 1000.0, LocalDateTime.now().plusDays(i), price - 0.5, price + 0.5));
        }
    }

    @Test
    void testInitialization() {
        Map<String, Object> state = strategy.getState();
        assertEquals(5, state.get("period"));
        assertEquals(2.0, state.get("threshold"));
    }

    @Test
    void testNoSignalBeforeSufficientData() {
        // Test first few data points before we have enough for mean calculation
        for (int i = 0; i < 4; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            assertNull(order, "Should not generate signal before having sufficient data");
        }
    }

    @Test
    void testSellSignalOnHighDeviation() {
        // Add a position to simulate holding
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0));
        
        // Process data through upward deviation
        for (int i = 0; i < 8; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            if (i < 7) {
                assertNull(order, "Should not generate signal before extreme deviation");
            } else {
                assertNotNull(order, "Should generate SELL signal on high deviation");
                assertEquals(OrderSide.SELL, order.getSide());
                assertEquals(SYMBOL, order.getSymbol());
            }
        }
    }

    @Test
    void testBuySignalOnLowDeviation() {
        // Process data through downward deviation
        for (int i = 5; i < 12; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            if (i < 11) {
                assertNull(order, "Should not generate signal before extreme deviation");
            } else {
                assertNotNull(order, "Should generate BUY signal on low deviation");
                assertEquals(OrderSide.BUY, order.getSide());
            }
        }
    }

    @Test
    void testStateUpdates() {
        // Process enough data points to calculate mean and z-score
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        Map<String, Object> state = strategy.getState();
        assertTrue(state.containsKey("mean"));
        assertTrue(state.containsKey("zScore"));
        assertTrue(state.containsKey("stdDev"));
        assertTrue(state.containsKey("currentPrice"));
    }

    @Test
    void testReset() {
        // Process some data
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Reset the strategy
        strategy.reset();
        
        // Verify state is reset
        Map<String, Object> state = strategy.getState();
        assertEquals(5, state.get("period"));
        assertEquals(2.0, state.get("threshold"));
        
        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 