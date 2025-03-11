package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import com.tradingsim.model.*;
import com.tradingsim.model.Order.OrderSide;

class BollingerBandsStrategyTest {
    private BollingerBandsStrategy strategy;
    private List<MarketData> testData;
    private Map<String, Position> positions;
    private static final String SYMBOL = "TEST";

    @BeforeEach
    void setUp() {
        strategy = new BollingerBandsStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("stdDevMultiplier", 2.0);
        strategy.initialize(params);

        testData = new ArrayList<>();
        positions = new HashMap<>();
        
        // Create test data with a clear pattern for Bollinger Bands
        // Start with stable prices, then sharp move up, then sharp move down
        double[] prices = {100, 100, 100, 100, 100,  // Stable period
                         110, 120, 130, 140, 150,    // Sharp move up (more extreme)
                         140, 130, 120, 110, 100};   // Sharp move down
        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            testData.add(new MarketData(SYMBOL, price, 1000.0, LocalDateTime.now().plusDays(i), price - 0.5, price + 0.5));
        }
    }

    @Test
    void testInitialization() {
        Map<String, Object> state = strategy.getState();
        assertEquals(5, state.get("period"));
        assertEquals(2.0, state.get("stdDevMultiplier"));
    }

    @Test
    void testNoSignalBeforeSufficientData() {
        // Test first few data points before we have enough for Bollinger Bands
        for (int i = 0; i < 4; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            assertNull(order, "Should not generate signal before having sufficient data");
        }
    }

    @Test
    void testSellSignalOnUpperBand() {
        // Add a position to simulate holding
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0));
        
        // Process first stable period and move into uptrend
        for (int i = 0; i < 9; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            if (i < 8) {
                assertNull(order, "Should not generate signal before extreme move");
            } else {
                assertNotNull(order, "Should generate SELL signal on extreme upward move");
                assertEquals(OrderSide.SELL, order.getSide());
                assertEquals(SYMBOL, order.getSymbol());
            }
        }
    }

    @Test
    void testBuySignalOnLowerBand() {
        // Process data through uptrend and into downtrend
        for (int i = 5; i < 14; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            if (i < 13) {
                assertNull(order, "Should not generate signal before extreme move");
            } else {
                assertNotNull(order, "Should generate BUY signal on extreme downward move");
                assertEquals(OrderSide.BUY, order.getSide());
            }
        }
    }

    @Test
    void testStateUpdates() {
        // Process enough data points to generate bands
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        Map<String, Object> state = strategy.getState();
        assertTrue(state.containsKey("sma"));
        assertTrue(state.containsKey("upperBand"));
        assertTrue(state.containsKey("lowerBand"));
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
        assertEquals(5, state.get("period"));
        assertEquals(2.0, state.get("stdDevMultiplier"));
        
        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 