package com.tradingsim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
        List<MarketData> testData = new ArrayList<>();
        this.positions = new HashMap<>();

        LocalDate startDate = LocalDate.of(2020, 1, 1);
        String SYMBOL = "TEST";
        
        // 1. Stable period (baseline)
        for (int i = 0; i < 5; i++) {
            testData.add(new MarketData(
                startDate.plusDays(i),
                SYMBOL,
                100.0, // open
                102.0, // high
                98.0,  // low
                100.0, // close
                1000L  // volume
            ));
        }
        
        // 2. Uptrend to push RSI into overbought territory
        for (int i = 0; i < 5; i++) {
            double price = 100.0 + ((i + 1) * 5.0); // 105, 110, 115, 120, 125
            testData.add(new MarketData(
                startDate.plusDays(i + 5),
                SYMBOL,
                price,           // open
                price + 2.0,     // high
                price - 2.0,     // low
                price,           // close
                1000L            // volume
            ));
        }
        
        // 3. Correction phase (small pullback)
        for (int i = 0; i < 3; i++) {
            double price = 125.0 - ((i + 1) * 3.0); // 122, 119, 116
            testData.add(new MarketData(
                startDate.plusDays(i + 10),
                SYMBOL,
                price,           // open
                price + 2.0,     // high
                price - 2.0,     // low
                price,           // close
                1000L            // volume
            ));
        }
        
        // 4. Downtrend to push RSI into oversold territory
        for (int i = 0; i < 5; i++) {
            double price = 116.0 - ((i + 1) * 5.0); // 111, 106, 101, 96, 91
            testData.add(new MarketData(
                startDate.plusDays(i + 13),
                SYMBOL,
                price,           // open
                price + 2.0,     // high
                price - 2.0,     // low
                price,           // close
                1000L            // volume
            ));
        }
        
        // 5. Recovery / bounce back (RSI should start rising from oversold)
        for (int i = 0; i < 3; i++) {
            double price = 91.0 + ((i + 1) * 4.0); // 95, 99, 103
            testData.add(new MarketData(
                startDate.plusDays(i + 18),
                SYMBOL,
                price,           // open
                price + 2.0,     // high
                price - 2.0,     // low
                price,           // close
                1000L            // volume
            ));
        }
        this.testData = testData;
    }

    @Test
    void testInitialization() {
        assertEquals(6, strategy.getMinIndex());
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
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process stable period and first few up moves
        for (int i = 0; i < 4; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process more up moves to reach overbought
        Order order1 = strategy.processMarketData(testData.get(4), positions);
        assertNull(order1, "Should not generate signal before reaching overbought");
        
        Order order2 = strategy.processMarketData(testData.get(5), positions);
        assertNotNull(order2, "Should generate SELL signal on overbought condition");
        assertEquals(OrderSide.SELL, order2.getSide());
        assertEquals(SYMBOL, order2.getSymbol());
    }

    @Test
    void testBuySignalOnOversold() {
        // Process all data up to down moves
        for (int i = 0; i < 13; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process more down moves to reach oversold
        Order order1 = strategy.processMarketData(testData.get(11), positions);
        assertNull(order1, "Should not generate signal before reaching oversold");
        
        Order order2 = strategy.processMarketData(testData.get(12), positions);
        assertNotNull(order2, "Should generate BUY signal on oversold condition");
        assertEquals(OrderSide.BUY, order2.getSide());

        Order order3 = strategy.processMarketData(testData.get(13), positions);
        assertNotNull(order3, "Should generate BUY signal on oversold condition");
        assertEquals(OrderSide.BUY, order3.getSide());
    }

    @Test
    void testReset() {
        // Process some data
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Reset the strategy
        strategy.reset();
        
        // verify that the strategy is reset
        assertEquals(6, strategy.getMinIndex());

        // Process first data point after reset
        Order order = strategy.processMarketData(testData.get(0), positions);
        assertNull(order, "Should not generate signal immediately after reset");
    }
} 