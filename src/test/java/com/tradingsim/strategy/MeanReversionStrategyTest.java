package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
        params.put("threshold", 1.5); // Lower threshold for testing
        strategy.initialize(params);

        testData = new ArrayList<>();
        positions = new HashMap<>();
        
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        
        // Create test data with a clear mean reversion pattern
        // Start with stable prices to establish mean
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
        
        // Add extreme upward deviation from mean
        for (int i = 0; i < 3; i++) {
            double price = 100.0 + ((i + 1) * 10.0); // 110, 120, 130
            testData.add(new MarketData(
                startDate.plusDays(i + 5),
                SYMBOL,
                price, // open
                price + 2.0, // high
                price - 1.0, // low
                price, // close
                1000L  // volume
            ));
        }
        
        // Add reversion back to mean
        for (int i = 0; i < 3; i++) {
            double price = 130.0 - ((i + 1) * 10.0); // 120, 110, 100
            testData.add(new MarketData(
                startDate.plusDays(i + 8),
                SYMBOL,
                price, // open
                price + 1.0, // high
                price - 2.0, // low
                price, // close
                1000L  // volume
            ));
        }
        
        // Add extreme downward deviation from mean
        for (int i = 0; i < 3; i++) {
            double price = 100.0 - ((i + 1) * 10.0); // 90, 80, 70
            testData.add(new MarketData(
                startDate.plusDays(i + 11),
                SYMBOL,
                price, // open
                price + 1.0, // high
                price - 2.0, // low
                price, // close
                1000L  // volume
            ));
        }
    }

    @Test
    void testInitialization() {
        assertEquals(5, strategy.getMinIndex());
    }

    @Test
    void testNoSignalBeforeSufficientData() {
        // Test first few data points before we have enough for mean calculation
        for (int i = 0; i < 5; i++) {
            Order order = strategy.processMarketData(testData.get(i), positions);
            assertNull(order, "Should not generate signal before having sufficient data");
        }
    }

    @Test
    void testSellSignalOnHighDeviation() {
        // Add a position to simulate holding
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process stable period to establish mean
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        Order order2 = strategy.processMarketData(testData.get(6), positions);
        assertNotNull(order2, "Should generate SELL signal on high deviation");
        assertEquals(OrderSide.SELL, order2.getSide());
        System.out.println(SYMBOL + ", order symbol: " + order2.getSymbol());
        assertEquals(SYMBOL, order2.getSymbol());
    }

    @Test
    void testBuySignalOnLowDeviation() {
        // Process data through stable period and upward deviation
        for (int i = 0; i < 9; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process downward deviation
        Order order1 = strategy.processMarketData(testData.get(9), positions);
        assertNull(order1, "Should not generate signal on first downward deviation");
        
        Order order2 = strategy.processMarketData(testData.get(10), positions);
        assertNotNull(order2, "Should generate BUY signal on low deviation");
        assertEquals(OrderSide.BUY, order2.getSide());

        Order order3 = strategy.processMarketData(testData.get(11), positions);
        assertNotNull(order3, "Should generate signal BUY on a second downward deviation");
        assertEquals(OrderSide.BUY, order3.getSide());

        Order order4 = strategy.processMarketData(testData.get(12), positions);
        assertNotNull(order4, "Should generate signal BUY on a third downward deviation");
        assertEquals(OrderSide.BUY, order4.getSide());
    }

    @Test
    void testReset() {
        // Process some data
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Reset the strategy
        strategy.reset();
        
        // Test that the strategy is reset
        assertEquals(5, strategy.getMinIndex());

        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 