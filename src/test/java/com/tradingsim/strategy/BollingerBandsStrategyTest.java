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
        params.put("stdDevMultiplier", 1.5);
        strategy.initialize(params);

        testData = new ArrayList<>();
        positions = new HashMap<>();
        
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        // Define prices for each day
        double[] prices = {
            100, 104, 102, 106, 104,  // Days 1-5
            109, 120, 130, 140, 150,  // Days 6-10
            135, 110, 90              // Days 11-13
        };
        
        // Create test data with the defined prices
        for (int i = 0; i < prices.length; i++) {
            testData.add(new MarketData(
                startDate.plusDays(i),
                SYMBOL,
                prices[i],      // open
                prices[i] + 2.0, // high
                prices[i] - 2.0, // low
                prices[i],      // close
                1000L          // volume
            ));
        }
    }

    @Test
    void testInitialization() {
        assertEquals(5, strategy.getMinIndex());
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
        for (int i = 0; i < 4; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process extreme upward movement
        Order order1 = strategy.processMarketData(testData.get(4), positions);
        assertNull(order1, "Should not generate signal on first upward move");
        
        Order order2 = strategy.processMarketData(testData.get(5), positions);
        assertNotNull(order2, "Should generate SELL signal on extreme upward move");
        assertEquals(OrderSide.SELL, order2.getSide());
        assertEquals(SYMBOL, order2.getSymbol());

        // Order order3 = strategy.processMarketData(testData.get(6), positions);
        // assertNull(order3, "Should not generate signal with no position");
    }

    @Test
    void testBuySignalOnLowerBand() {
        // Process all data to establish bands
        for (int i = 0; i < 10; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process extreme downward movement
        Order order1 = strategy.processMarketData(testData.get(10), positions);
        assertNull(order1, "Should not generate signal on first downward move");
        
        Order order2 = strategy.processMarketData(testData.get(11), positions);
        assertNotNull(order2, "Should generate BUY signal on extreme downward move");
        assertEquals(OrderSide.BUY, order2.getSide());

        Order order3 = strategy.processMarketData(testData.get(12), positions);
        assertNotNull(order3, "Should generate signal BUY on a secondextreme downward move");
        assertEquals(OrderSide.BUY, order3.getSide());
    }

    @Test
    void testReset() {
        // Process some data
        for (int i = 0; i < 5; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Reset the strategy
        strategy.reset();
        
        // Verify parameters are preserved but strategy is reset
        assertEquals(5, strategy.getMinIndex());

        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 