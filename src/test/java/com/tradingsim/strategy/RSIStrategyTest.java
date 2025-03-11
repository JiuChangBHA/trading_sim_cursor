package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import com.tradingsim.model.*;
import com.tradingsim.model.Order.OrderSide;

class RSIStrategyTest {
    private RSIStrategy strategy;
    private List<MarketData> testData;
    private Map<String, Position> positions;
    private static final String SYMBOL = "TEST";

    @BeforeEach
    void setUp() {
        strategy = new RSIStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("period", 5);
        params.put("overboughtThreshold", 70.0);
        params.put("oversoldThreshold", 30.0);
        strategy.initialize(params);

        testData = new ArrayList<>();
        positions = new HashMap<>();
        
        // Create test data with a clear pattern for RSI
        // Start with stable prices, then continuous up moves, then continuous down moves
        double[] prices = {100, 100, 100, 100, 100,  // Stable period
                         105, 110, 115, 120, 125,     // Strong up moves
                         120, 115, 110, 105, 100};    // Strong down moves
        for (int i = 0; i < prices.length; i++) {
            double price = prices[i];
            testData.add(new MarketData(SYMBOL, price, 1000.0, LocalDateTime.now().plusDays(i), price - 0.5, price + 0.5));
        }
    }

    @Test
    void testInitialization() {
        Map<String, Object> state = strategy.getState();
        assertEquals(5, state.get("period"));
        assertEquals(70.0, state.get("overboughtThreshold"));
        assertEquals(30.0, state.get("oversoldThreshold"));
    }

    @Test
    void testNoSignalBeforeSufficientData() {
        // Test first few data points before we have enough for RSI
        for (int i = 0; i < 5; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            assertNull(order, "Should not generate signal before having sufficient data");
        }
    }

    @Test
    void testSellSignalOnOverbought() {
        // Add a position to simulate holding
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0));
        
        // Process data through uptrend
        for (int i = 0; i < 9; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            if (i < 8) {
                assertNull(order, "Should not generate signal before overbought condition");
            } else {
                assertNotNull(order, "Should generate SELL signal on overbought condition");
                assertEquals(OrderSide.SELL, order.getSide());
                assertEquals(SYMBOL, order.getSymbol());
            }
        }
    }

    @Test
    void testBuySignalOnOversold() {
        // Process data through downtrend
        for (int i = 5; i < 14; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            if (i < 13) {
                assertNull(order, "Should not generate signal before oversold condition");
            } else {
                assertNotNull(order, "Should generate BUY signal on oversold condition");
                assertEquals(OrderSide.BUY, order.getSide());
            }
        }
    }

    @Test
    void testStateUpdates() {
        // Process enough data points to calculate RSI
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        Map<String, Object> state = strategy.getState();
        assertTrue(state.containsKey("rsi"));
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
        assertEquals(70.0, state.get("overboughtThreshold"));
        assertEquals(30.0, state.get("oversoldThreshold"));
        
        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 