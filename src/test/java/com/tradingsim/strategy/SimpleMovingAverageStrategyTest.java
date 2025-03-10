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
        MarketData marketData = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        
        // Process first two data points (window size is 3)
        strategy.processMarketData(marketData, positions);
        strategy.processMarketData(marketData, positions);
        
        // Should not generate signal yet
        Order order = strategy.processMarketData(marketData, positions);
        assertNull(order);
    }

    @Test
    void testBuySignalOnCrossover() {
        // Create market data with increasing prices
        MarketData marketData1 = new MarketData("AAPL", 101.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        MarketData marketData2 = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 100.9, 101.1);
        MarketData marketData3 = new MarketData("AAPL", 102.0, 1000, LocalDateTime.now(), 101.9, 102.1);
        MarketData marketData4 = new MarketData("AAPL", 103.0, 1000, LocalDateTime.now(), 102.9, 103.1);
        // Process data points
        strategy.processMarketData(marketData1, positions);
        strategy.processMarketData(marketData2, positions);
        
        // Should generate buy signal on crossover
        Order order = strategy.processMarketData(marketData3, positions);
        assertNotNull(order);
        assertEquals(OrderSide.BUY, order.getSide());
        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(1.0, order.getQuantity());
        assertEquals("AAPL", order.getSymbol());
    }

    @Test
    void testSellSignalOnCrossover() {
        // First create a position
        Position position = new Position("AAPL", 1.0, 100.0);
        positions.put("AAPL", position);
        
        // Create market data with decreasing prices
        MarketData marketData1 = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 101.9, 102.1);
        MarketData marketData2 = new MarketData("AAPL", 101.0, 1000, LocalDateTime.now(), 100.9, 101.1);
        MarketData marketData3 = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        MarketData marketData4 = new MarketData("AAPL", 99.0, 1000, LocalDateTime.now(), 98.9, 99.1);

        // Process data points
        strategy.processMarketData(marketData1, positions);
        strategy.processMarketData(marketData2, positions);
        
        // Should generate sell signal on crossover
        Order order = strategy.processMarketData(marketData3, positions);
        assertNotNull(order);
        assertEquals(OrderSide.SELL, order.getSide());
        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(1.0, order.getQuantity());
        assertEquals("AAPL", order.getSymbol());
    }

    @Test
    void testNoSignalWhenNoCrossover() {
        // Create market data with stable prices
        MarketData marketData1 = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        MarketData marketData2 = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        MarketData marketData3 = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        
        // Process data points
        strategy.processMarketData(marketData1, positions);
        strategy.processMarketData(marketData2, positions);
        
        // Should not generate signal
        Order order = strategy.processMarketData(marketData3, positions);
        assertNull(order);
    }

    @Test
    void testReset() {
        // Create and process some market data
        MarketData marketData = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        strategy.processMarketData(marketData, positions);
        strategy.processMarketData(marketData, positions);
        strategy.processMarketData(marketData, positions);
        
        // Reset the strategy
        strategy.reset();
        
        // Verify state is cleared
        Map<String, Object> state = strategy.getState();
        assertTrue(state.isEmpty());
        
        // Process new data after reset
        Order order = strategy.processMarketData(marketData, positions);
        assertNull(order); // Should not generate signal until window is filled again
    }

    @Test
    void testStateUpdates() {
        MarketData marketData = new MarketData("AAPL", 100.0, 1000, LocalDateTime.now(), 99.9, 100.1);
        strategy.processMarketData(marketData, positions);
        strategy.processMarketData(marketData, positions);
        strategy.processMarketData(marketData, positions);
        
        Map<String, Object> state = strategy.getState();
        assertEquals(100.0, state.get("currentPrice"));
        assertEquals(100.0, state.get("movingAverage"));
    }
} 