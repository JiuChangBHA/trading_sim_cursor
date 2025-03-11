package com.tradingsim.strategy;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    @BeforeEach
    void setUp() {
        strategy = new SimpleMovingAverageStrategy();
        parameters = new HashMap<>();
        parameters.put("windowSize", 3); // Small window size for testing
        strategy.initialize(parameters);
        
        positions = new HashMap<>();
    }

    @Test
    void testInitialization() {
        assertEquals("Simple Moving Average", strategy.getName());
        assertEquals("A basic moving average crossover strategy", strategy.getDescription());
        
        Map<String, Object> state = strategy.getState();
        assertEquals(3, state.get("windowSize"));
    }

    @Test
    void testNoSignalBeforeWindowFilled() {
        // Create market data with increasing prices
        MarketData data = new MarketData(
            LocalDate.now().minusDays(4),
            "AAPL",
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
        // Create market data with increasing prices
        MarketData newData1 = new MarketData(
            LocalDate.now().minusDays(4),
            "AAPL",
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        MarketData newData2 = new MarketData(
            LocalDate.now().minusDays(3),
            "AAPL",
            102.0, // open
            107.0, // high
            97.0,  // low
            104.0, // close
            1000L  // volume
        );
        MarketData newData3 = new MarketData(
            LocalDate.now().minusDays(2),
            "AAPL",
            104.0, // open
            109.0, // high
            99.0,  // low
            106.0, // close
            1000L  // volume
        );
        MarketData newData4 = new MarketData(
            LocalDate.now().minusDays(1),
            "AAPL",
            106.0, // open
            111.0, // high
            101.0, // low
            108.0, // close
            1000L  // volume
        );
        // Process data points
        strategy.processMarketData(newData1, positions);
        strategy.processMarketData(newData2, positions);
        
        // Should generate buy signal on crossover
        Order order = strategy.processMarketData(newData3, positions);
        assertNotNull(order);
        assertEquals(OrderSide.BUY, order.getSide());
        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(1.0, order.getQuantity());
        assertEquals("AAPL", order.getSymbol());
    }

    @Test
    void testSellSignalOnCrossover() {
        // First create a position
        Position position = new Position("AAPL", 10, 100.0, LocalDate.now());
        positions.put("AAPL", position);
        
        // Create market data with decreasing prices
        MarketData data1 = new MarketData(
            LocalDate.now().minusDays(4),
            "AAPL",
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        MarketData data2 = new MarketData(
            LocalDate.now().minusDays(3),
            "AAPL",
            101.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        MarketData data3 = new MarketData(
            LocalDate.now().minusDays(2),
            "AAPL",
            100.0, // open
            105.0, // high
            95.0,  // low
            99.0,  // close
            1000L  // volume
        );
        MarketData data4 = new MarketData(
            LocalDate.now().minusDays(1),
            "AAPL",
            99.0,  // open
            105.0, // high
            95.0,  // low
            98.0,  // close
            1000L  // volume
        );

        // Process data points
        strategy.processMarketData(data1, positions);
        strategy.processMarketData(data2, positions);
        
        // Should generate sell signal on crossover
        Order order = strategy.processMarketData(data3, positions);
        assertNotNull(order);
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(1.0, order.getQuantity());
        assertEquals("AAPL", order.getSymbol());
    }

    @Test
    void testNoSignalWhenNoCrossover() {
        // Create market data with stable prices
        MarketData data1 = new MarketData(
            LocalDate.now().minusDays(4),
            "AAPL",
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        MarketData data2 = new MarketData(
            LocalDate.now().minusDays(3),
            "AAPL",
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        MarketData data3 = new MarketData(
            LocalDate.now().minusDays(2),
            "AAPL",
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
            "AAPL",
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
        
        // Verify state is cleared
        Map<String, Object> state = strategy.getState();
        assertTrue(state.isEmpty());
        
        // Process new data after reset
        Order order = strategy.processMarketData(data, positions);
        assertNull(order); // Should not generate signal until window is filled again
    }

    @Test
    void testStateUpdates() {
        MarketData data = new MarketData(
            LocalDate.now().minusDays(4),
            "AAPL",
            100.0, // open
            105.0, // high
            95.0,  // low
            102.0, // close
            1000L  // volume
        );
        strategy.processMarketData(data, positions);
        strategy.processMarketData(data, positions);
        strategy.processMarketData(data, positions);
        
        Map<String, Object> state = strategy.getState();
        assertEquals(102.0, state.get("currentPrice"));
        assertEquals(100.0, state.get("movingAverage"));
    }
} 