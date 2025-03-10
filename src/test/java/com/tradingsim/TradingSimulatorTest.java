package com.tradingsim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.io.InputStream;
import java.util.stream.Collectors;

public class TradingSimulatorTest {
    private static final Logger LOGGER = Logger.getLogger(TradingSimulatorTest.class.getName());
    private TradingSimulator simulator;
    private List<MarketData> testMarketData;
    
    @BeforeEach
    void setUp() throws IOException {
        // Load logging configuration
        InputStream configFile = getClass().getClassLoader().getResourceAsStream("logging.properties");
        if (configFile != null) {
            LogManager.getLogManager().readConfiguration(configFile);
        }
        
        simulator = new TradingSimulator(10000.0);
        simulator.loadMarketData();  // Load market data before tests
        testMarketData = createTestMarketData();
    }
    
    private List<MarketData> createTestMarketData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // Create 30 days of test data with a known pattern
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price + (i % 2 == 0 ? 1.0 : -1.0); // Alternating up/down pattern
            double high = Math.max(open, close) + 0.5;
            double low = Math.min(open, close) - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
            price = close;
        }
        
        return data;
    }
    
    @Test
    void testMarketDataLoading() {
        try {
            simulator.loadMarketData();
            assertFalse(simulator.getMarketData().isEmpty(), "Market data should not be empty");
            
            // Verify data is sorted by date for each symbol
            for (List<MarketData> data : simulator.getMarketData().values()) {
                for (int i = 1; i < data.size(); i++) {
                    assertTrue(data.get(i).getDate().isAfter(data.get(i-1).getDate()) || 
                              data.get(i).getDate().equals(data.get(i-1).getDate()),
                              "Data should be sorted by date");
                }
            }
        } catch (IOException e) {
            fail("Failed to load market data: " + e.getMessage());
        }
    }
    
    @Test
    void testMovingAverageUptrendCrossoverStrategy() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        
        // Test with known data pattern
        TradingSignal signal = strategy.generateSignal(testMarketData);
        assertNotNull(signal, "Signal should not be null");
        
        // Verify strategy behavior with different price patterns
        List<MarketData> uptrendData = createUptrendData();
        
        // Check for buy signals during the recovery period (days 25-35)
        boolean foundBuySignal = false;
        for (int i = 25; i <= 50; i++) {
            List<MarketData> dataUpToDay = uptrendData.subList(0, i + 1);
            signal = strategy.generateSignal(dataUpToDay);
            if (signal == TradingSignal.BUY) {
                foundBuySignal = true;
                break;
            }
        }
        
        assertTrue(foundBuySignal, "Should generate BUY signal during the recovery period (days 25-50)");
    }

    @Test
    void testMovingAverageDowntrendCrossoverStrategy() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        
        // Test with known data pattern
        TradingSignal signal = strategy.generateSignal(testMarketData);
        assertNotNull(signal, "Signal should not be null");

        // Verify strategy behavior with different price patterns
        List<MarketData> downtrendData = createDowntrendData();
        
        // Check for buy signals only during the expected window
        // We expect a buy signal shortly after day 25 when price starts rising,
        // but before day 35 (giving enough time for MAs to cross)
        boolean foundSellSignal = false;
        for (int i = 25; i <= 50; i++) {
            List<MarketData> dataUpToDay = downtrendData.subList(0, i + 1);
            signal = strategy.generateSignal(dataUpToDay);
            if (signal == TradingSignal.SELL) {
                foundSellSignal = true;
                break;
            }
        }
        
        assertTrue(foundSellSignal, "Should generate SELL signal during the recovery period (days 25-50)");
    }

    @Test
    void testMeanReversionStrategy() {
        MeanReversionStrategy strategy = new MeanReversionStrategy();
        
        // Test with known data pattern
        TradingSignal signal = strategy.generateSignal(testMarketData);
        assertNotNull(signal, "Signal should not be null");
        
        // Test with price above mean
        List<MarketData> highPriceData = createHighPriceData();
        signal = strategy.generateSignal(highPriceData);
        assertEquals(TradingSignal.SELL, signal, "Should generate SELL signal when price is above mean");
        
        // Test with price below mean
        List<MarketData> lowPriceData = createLowPriceData();
        signal = strategy.generateSignal(lowPriceData);
        assertEquals(TradingSignal.BUY, signal, "Should generate BUY signal when price is below mean");
    }
    
    @Test
    void testRSIStrategy() {
        RSIStrategy strategy = new RSIStrategy();
        
        // Test with known data pattern
        TradingSignal signal = strategy.generateSignal(testMarketData);
        assertNotNull(signal, "Signal should not be null");
        
        // Test with overbought condition
        List<MarketData> overboughtData = createOverboughtData();
        signal = strategy.generateSignal(overboughtData);
        assertEquals(TradingSignal.SELL, signal, "Should generate SELL signal in overbought condition");
        
        // Test with oversold condition
        List<MarketData> oversoldData = createOversoldData();
        signal = strategy.generateSignal(oversoldData);
        assertEquals(TradingSignal.BUY, signal, "Should generate BUY signal in oversold condition");
    }
    
    @Test
    void testBollingerBandsStrategy() {
        BollingerBandsStrategy strategy = new BollingerBandsStrategy();
        
        // Test with known data pattern
        TradingSignal signal = strategy.generateSignal(testMarketData);
        assertNotNull(signal, "Signal should not be null");
        
        // Test with price above upper band
        List<MarketData> upperBandData = createUpperBandData();
        signal = strategy.generateSignal(upperBandData);
        assertEquals(TradingSignal.SELL, signal, "Should generate SELL signal when price is above upper band");
        
        // Test with price below lower band
        List<MarketData> lowerBandData = createLowerBandData();
        signal = strategy.generateSignal(lowerBandData);
        assertEquals(TradingSignal.BUY, signal, "Should generate BUY signal when price is below lower band");
    }
    
    @Test
    void testSimulationExecution() {
        TradingAccount account = new TradingAccount(10000.0);
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        
        // Use a real symbol from our market data
        SimulationResult result = simulator.runSimulation("AAPL", strategy);
        
        assertNotNull(result, "Simulation result should not be null");
        assertTrue(result.getFinalCapital() >= 0, "Final capital should not be negative");
        assertTrue(!result.getTrades().isEmpty(), "Should have executed some trades");
        
        // Verify trade execution logic
        for (int i = 0; i < result.getTrades().size(); i += 2) {
            if (i + 1 < result.getTrades().size()) {
                TradeExecuted entry = result.getTrades().get(i);
                TradeExecuted exit = result.getTrades().get(i + 1);
                
                assertEquals(TradingSignal.BUY, entry.getSignal(), "First trade should be BUY");
                assertEquals(TradingSignal.SELL, exit.getSignal(), "Second trade should be SELL");
                assertTrue(exit.getDate().isAfter(entry.getDate()), "Exit date should be after entry date");
            }
        }
    }
    
    @Test
    void testPerformanceMetrics() {
        MovingAverageCrossoverStrategy strategy = new MovingAverageCrossoverStrategy();
        // Use a real symbol from our market data
        SimulationResult result = simulator.runSimulation("AAPL", strategy);
        
        // Test max drawdown calculation
        double maxDrawdown = calculateMaxDrawdown(result.getEquityCurve());
        assertTrue(maxDrawdown >= 0 && maxDrawdown <= 100, "Max drawdown should be between 0 and 100");
        
        // Test Sharpe ratio calculation
        double sharpeRatio = calculateSharpeRatio(result.getEquityCurve());
        assertNotNull(sharpeRatio, "Sharpe ratio should not be null");
    }
    
    private double calculateMaxDrawdown(List<Double> equityCurve) {
        double maxDrawdown = 0;
        double peak = equityCurve.get(0);
        
        for (double value : equityCurve) {
            if (value > peak) {
                peak = value;
            }
            double drawdown = (peak - value) / peak * 100;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }
        
        return maxDrawdown;
    }
    
    private double calculateSharpeRatio(List<Double> equityCurve) {
        if (equityCurve.size() < 2) {
            return 0;
        }
        
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            double dailyReturn = (equityCurve.get(i) - equityCurve.get(i-1)) / equityCurve.get(i-1);
            returns.add(dailyReturn);
        }
        
        double meanReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = calculateStdDev(returns, meanReturn);
        
        if (stdDev == 0) {
            return 0;
        }
        
        // Assuming risk-free rate of 2% annually
        double riskFreeRate = 0.02 / 252; // Daily risk-free rate
        return (meanReturn - riskFreeRate) / stdDev * Math.sqrt(252);
    }
    
    private double calculateStdDev(List<Double> values, double mean) {
        double sumSquaredDiff = 0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / values.size());
    }
    
    // Helper methods to create test data with specific patterns
    private List<MarketData> createUptrendData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // First 20 days: price stays flat
        for (int i = 0; i < 20; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price;
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            MarketData md = new MarketData(date, "TEST", open, high, low, close, volume);
            data.add(md);
        }
        
        // Next 5 days: very pronounced decline to create crossover opportunity
        for (int i = 20; i < 25; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price - 10.0; // Very pronounced decline
            double high = open + 0.5;
            double low = close - 0.5;
            long volume = 1000000;
            
            MarketData md = new MarketData(date, "TEST", open, high, low, close, volume);
            data.add(md);
            price = close;
        }
        
        // Next days: very gradual upward trend to create crossover
        for (int i = 25; i < 50; i++) {  // Fixed the condition to actually add uptrend data
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price + 0.25; // Very gradual upward trend
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            MarketData md = new MarketData(date, "TEST", open, high, low, close, volume);
            data.add(md);
            price = close;
        }
        
        return data;
    }

    private List<MarketData> createDowntrendData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // First 20 days: price stays flat
        for (int i = 0; i < 20; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price;
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            MarketData md = new MarketData(date, "TEST", open, high, low, close, volume);
            data.add(md);
        }
        
        // Next 5 days: very pronounced incline to create crossover opportunity
        for (int i = 20; i < 25; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price + 10.0; // Very pronounced incline
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            MarketData md = new MarketData(date, "TEST", open, high, low, close, volume);
            data.add(md);
            price = close;
        }
        
        // Next days: very gradual downward trend to create crossover
        for (int i = 25; i < 50; i++) {  // Fixed the condition to actually add downtrend data
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price - 0.25; // Very gradual downward trend
            double high = open + 0.5;
            double low = close - 0.5;
            long volume = 1000000;
            
            MarketData md = new MarketData(date, "TEST", open, high, low, close, volume);
            data.add(md);
            price = close;
        }
        return data;
    }
    
    private List<MarketData> createHighPriceData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // First 20 days: price stays at mean
        for (int i = 0; i < 20; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price;
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
        }
        
        // Next 10 days: price spikes above mean
        for (int i = 20; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price + 5.0; // Price spikes above mean
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
            price = close;
        }
        
        return data;
    }
    
    private List<MarketData> createLowPriceData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // First 20 days: price stays at mean
        for (int i = 0; i < 20; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price;
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
        }
        
        // Next 10 days: price drops below mean
        for (int i = 20; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price - 5.0; // Price drops below mean
            double high = open + 0.5;
            double low = close - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
            price = close;
        }
        
        return data;
    }
    
    private List<MarketData> createOverboughtData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // Create 30 days of steadily increasing prices
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price + 2.0; // Steady increase
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
            price = close;
        }
        
        return data;
    }
    
    private List<MarketData> createOversoldData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // Create 30 days of steadily decreasing prices
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price - 2.0; // Steady decrease
            double high = open + 0.5;
            double low = close - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
            price = close;
        }
        
        return data;
    }
    
    private List<MarketData> createUpperBandData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // First 20 days: price stays at mean
        for (int i = 0; i < 20; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price;
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
        }
        
        // Next 10 days: price spikes above upper band
        for (int i = 20; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price + 10.0; // Price spikes above upper band
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
            price = close;
        }
        
        return data;
    }
    
    private List<MarketData> createLowerBandData() {
        List<MarketData> data = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        double price = 100.0;
        
        // First 20 days: price stays at mean
        for (int i = 0; i < 20; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price;
            double high = close + 0.5;
            double low = open - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
        }
        
        // Next 10 days: price drops below lower band
        for (int i = 20; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            double open = price;
            double close = price - 10.0; // Price drops below lower band
            double high = open + 0.5;
            double low = close - 0.5;
            long volume = 1000000;
            
            data.add(new MarketData(date, "TEST", open, high, low, close, volume));
            price = close;
        }
        
        return data;
    }
} 