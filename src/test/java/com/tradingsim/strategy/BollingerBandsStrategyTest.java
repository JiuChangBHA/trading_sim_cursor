package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
        params.put("stdDevMultiplier", 1.5); // Lower multiplier for testing
        strategy.initialize(params);

        testData = new ArrayList<>();
        positions = new HashMap<>();
        
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        // Create test data with a clear pattern for Bollinger Bands
        // Start with stable prices to establish the bands
        for (int i = 0; i < 5; i++) {
            testData.add(new MarketData(
                startDate.plusDays(i),
                SYMBOL,
                100.0, // open
                102.0, // high
                98.0,  // low
                100.0, // close - stable price
                1000L  // volume
            ));
        }
        
        // Add extreme upward movement to break upper band
        for (int i = 0; i < 3; i++) {
            double price = 100.0 + ((i + 1) * 10.0); // 110, 120, 130
            testData.add(new MarketData(
                startDate.plusDays(i + 5),
                SYMBOL,
                price, // open
                price + 2.0, // high
                price - 2.0, // low
                price, // close
                1000L  // volume
            ));
        }
        
        // Add extreme downward movement to break lower band
        for (int i = 0; i < 3; i++) {
            double price = 130.0 - ((i + 1) * 15.0); // 115, 100, 85
            testData.add(new MarketData(
                startDate.plusDays(i + 8),
                SYMBOL,
                price, // open
                price + 2.0, // high
                price - 2.0, // low
                price, // close
                1000L  // volume
            ));
        }
    }

    @Test
    void testInitialization() {
        Map<String, Object> state = strategy.getState();
        assertEquals(5, state.get("period"));
        assertEquals(1.5, state.get("stdDevMultiplier"));
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
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process first stable period
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process extreme upward movement
        Order order1 = strategy.processMarketData(testData.get(5), positions);
        assertNull(order1, "Should not generate signal on first upward move");
        
        Order order2 = strategy.processMarketData(testData.get(6), positions);
        assertNotNull(order2, "Should generate SELL signal on extreme upward move");
        assertEquals(OrderSide.SELL, order2.getSide());
        assertEquals(SYMBOL, order2.getSymbol());
    }

    @Test
    void testBuySignalOnLowerBand() {
        // Process all data to establish bands
        for (int i = 0; i < 10; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process extreme downward movement
        Order order = strategy.processMarketData(testData.get(10), positions);
        assertNotNull(order, "Should generate BUY signal on extreme downward move");
        assertEquals(OrderSide.BUY, order.getSide());
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
        assertEquals(1.5, state.get("stdDevMultiplier"));
        
        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 