package com.tradingsim.strategy;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.tradingsim.model.MarketData;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.Order.OrderSide;
import com.tradingsim.model.Order.OrderType;
import java.time.LocalDate;

class SimpleMovingAverageStrategyTest {
    private SimpleMovingAverageStrategy strategy;
    private Map<String, Object> parameters;
    private Map<String, Position> positions;
    private List<MarketData> testData;
    private static final String SYMBOL = "TEST";

    @BeforeEach
    void setUp() {
        strategy = new SimpleMovingAverageStrategy();
        parameters = new HashMap<>();
        parameters.put("windowSize", 3); // Small window size for testing
        strategy.initialize(parameters);
        
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
        assertEquals("Simple Moving Average", strategy.getName());
        assertEquals("A basic moving average crossover strategy", strategy.getDescription());
        
        assertEquals(3, strategy.getMinIndex());
    }

    @Test
    void testNoSignalBeforeWindowFilled() {
        // Create market data with increasing prices
        MarketData data = new MarketData(
            LocalDate.now().minusDays(4),
            SYMBOL,
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        
        // Process first two data points (window size is 3)
        strategy.processMarketData(data, positions);
        strategy.processMarketData(data, positions);
        
        // Should not generate signal yet
        Order order = strategy.processMarketData(data, positions);
        assertNull(order);
    }

    @Test
    void testBuySignalOnCrossover() {        
        // Define prices for each day
        double[] prices = {
            110, 108, 106, 104, 102,  // Days 1-5
            100, 98, 96, 100, 104  // Days 6-10
        };

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        List<MarketData> testBuyData = new ArrayList<>();
        // Create test data with the defined prices
        for (int i = 0; i < prices.length; i++) {
            testBuyData.add(new MarketData(
                startDate.plusDays(i),
                SYMBOL,
                prices[i],      // open
                prices[i] + 2.0, // high
                prices[i] - 2.0, // low
                prices[i],      // close
                1000L          // volume
            ));
        }
        
        // Process first 8 days to establish downtrend
        for (int i = 0; i < 8; i++) {
            strategy.processMarketData(testBuyData.get(i), positions);
        }
        
        // Process the new uptrend data
        Order order = strategy.processMarketData(testBuyData.get(8), positions);
        
        assertNotNull(order, "Should generate a BUY signal on upward crossover");
        assertEquals(OrderSide.BUY, order.getSide());
    }

    @Test
    void testSellSignalOnCrossover() {        // Add a position to simulate selling
        positions.put(SYMBOL, new Position(SYMBOL, 1.0, 100.0, LocalDate.now()));
        
        // Process first 6 days to establish trend
        for (int i = 0; i < 6; i++) {
            strategy.processMarketData(testData.get(i), positions);
        }
        
        // Process day 7 which should trigger crossover
        Order order = strategy.processMarketData(testData.get(6), positions);
        assertNotNull(order, "Should generate a SELL signal on crossover");
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(SYMBOL, order.getSymbol());
    }

    @Test
    void testNoSignalWhenNoCrossover() {
        // Create market data with stable prices
        MarketData data1 = new MarketData(
            LocalDate.now().minusDays(4),
            SYMBOL,
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        MarketData data2 = new MarketData(
            LocalDate.now().minusDays(3),
            SYMBOL,
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        MarketData data3 = new MarketData(
            LocalDate.now().minusDays(2),
            SYMBOL,
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        
        // Process data points
        strategy.processMarketData(data1, positions);
        strategy.processMarketData(data2, positions);
        
        // Should not generate signal
        Order order = strategy.processMarketData(data3, positions);
        assertNull(order);
    }

    @Test
    void testReset() {
        // Create and process some market data
        MarketData data = new MarketData(
            LocalDate.now().minusDays(4),
            SYMBOL,
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        strategy.processMarketData(data, positions);
        strategy.processMarketData(data, positions);
        strategy.processMarketData(data, positions);
        
        // Reset the strategy
        strategy.reset();
        
        // Verify strategy is cleared
        assertEquals(3, strategy.getMinIndex());
        
        // Process new data after reset
        Order order = strategy.processMarketData(data, positions);
        assertNull(order); // Should not generate signal until window is filled again
    }

} 