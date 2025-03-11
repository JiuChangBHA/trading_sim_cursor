package com.tradingsim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

import com.tradingsim.model.MarketData;

public class MarketDataLoader {
    private static final Logger LOGGER = Logger.getLogger(MarketDataLoader.class.getName());
    private static MarketDataLoader instance;
    private final Map<String, List<MarketData>> testData;
    private final Map<String, List<MarketData>> realData;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String MARKET_DATA_DIR = "src/main/resources/market_data";

    private MarketDataLoader() {
        testData = new HashMap<>();
        realData = new HashMap<>();
    }

    public static synchronized MarketDataLoader getInstance() {
        if (instance == null) {
            instance = new MarketDataLoader();
        }
        return instance;
    }

    public void addTestData(String symbol, List<MarketData> data) {
        testData.put(symbol, new ArrayList<>(data));
    }

    public void clearTestData() {
        testData.clear();
    }

    public List<MarketData> getMarketData(String symbol) {
        // Return test data if available, otherwise return real data
        if (testData.containsKey(symbol)) {
            return testData.get(symbol);
        }
        
        if (!realData.containsKey(symbol)) {
            throw new IllegalArgumentException("No data available for symbol: " + symbol);
        }
        return realData.get(symbol);
    }

    public void loadRealData(String symbol, List<MarketData> data) {
        realData.put(symbol, new ArrayList<>(data));
    }

    /**
     * Load market data for a specific symbol from a CSV file.
     * @param symbol The stock symbol
     * @return List of market data points
     * @throws IOException If the file cannot be read
     */
    public List<MarketData> loadMarketData(String symbol) throws IOException {
        Path dataDir = Paths.get(MARKET_DATA_DIR);
        if (!Files.exists(dataDir)) {
            throw new IOException("Market data directory not found: " + MARKET_DATA_DIR);
        }
        
        // Find the most recent data directory
        Path latestDir = Files.list(dataDir)
            .filter(Files::isDirectory)
            .max(Comparator.comparing(p -> p.getFileName().toString()))
            .orElseThrow(() -> new IOException("No market data directories found"));
            
        Path dataFile = latestDir.resolve(symbol + "_data.csv");
        if (!Files.exists(dataFile)) {
            throw new IOException("Market data file not found for symbol: " + symbol);
        }
        
        List<MarketData> data = new ArrayList<>();
        List<String> lines = Files.readAllLines(dataFile);
        
        // Skip header row
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = lines.get(i).split(",");
            MarketData marketData = new MarketData(
                LocalDate.parse(fields[0], DATE_FORMATTER),
                symbol,
                Double.parseDouble(fields[2]), // Open
                Double.parseDouble(fields[3]), // High
                Double.parseDouble(fields[4]), // Low
                Double.parseDouble(fields[5]), // Close
                (long) Double.parseDouble(fields[6]) // Volume
            );
            data.add(marketData);
        }
        
        return data;
    }
    
    /**
     * Load market data for multiple symbols.
     * @param symbols List of stock symbols
     * @return Map of symbol to market data
     * @throws IOException If any file cannot be read
     */
    public Map<String, List<MarketData>> loadMarketData(List<String> symbols) throws IOException {
        Map<String, List<MarketData>> result = new HashMap<>();
        for (String symbol : symbols) {
            result.put(symbol, loadMarketData(symbol));
        }
        return result;
    }

    /**
     * Get all market data
     * @return Map of symbol to market data
     */
    public Map<String, List<MarketData>> getAllData() {
        return new HashMap<>(testData);
    }
} 