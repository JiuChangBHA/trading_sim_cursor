package com.tradingsim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.tradingsim.model.MarketData;

public class MarketDataLoader {
    private static final Logger LOGGER = Logger.getLogger(MarketDataLoader.class.getName());
    private static MarketDataLoader instance;
    private final Map<String, List<MarketData>> testData;
    private final Map<String, List<MarketData>> realData;
    private final List<String> symbols;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String MARKET_DATA_DIR = "src/main/resources/market_data";

    private MarketDataLoader() throws IOException {
        testData = new HashMap<>();
        realData = new HashMap<>();
        loadMarketData();
        symbols = new ArrayList<>(realData.keySet());
    }

    public static synchronized MarketDataLoader getInstance() throws IOException {
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
            try {
                // Try to load the data if not already loaded
                List<MarketData> data = loadSymbolData(symbol);
                loadRealData(symbol, data);
            } catch (IOException e) {
                throw new IllegalArgumentException("No data available for symbol: " + symbol);
            }
        }
        return realData.get(symbol);
    }

    public void loadRealData(String symbol, List<MarketData> data) {
        realData.put(symbol, new ArrayList<>(data));
    }

    /**
     * Find the most recent data directory
     * @return Path to the most recent data directory
     * @throws IOException If no directory can be found
     */
    private Path getLatestDataDirectory() throws IOException {
        Path dataDir = Paths.get(MARKET_DATA_DIR);
        if (!Files.exists(dataDir)) {
            throw new IOException("Market data directory not found: " + MARKET_DATA_DIR);
        }
        
        return Files.list(dataDir)
            .filter(Files::isDirectory)
            .max(Comparator.comparing(p -> p.getFileName().toString()))
            .orElseThrow(() -> new IOException("No market data directories found"));
    }

    /**
     * Load market data for a specific symbol from a CSV file.
     * @param symbol The stock symbol
     * @return List of market data points
     * @throws IOException If the file cannot be read
     */
    public List<MarketData> loadSymbolData(String symbol) throws IOException {
        Path latestDir = getLatestDataDirectory();
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
    public void loadMarketData(List<String> symbols) throws IOException {
        Map<String, List<MarketData>> result = new HashMap<>();
        for (String symbol : symbols) {
            result.put(symbol, loadSymbolData(symbol));
        }
        realData.putAll(result);
    }

    /**
     * Load all market data from the market data directory
     */
    public void loadMarketData() throws IOException {
        Path dataDir = getLatestDataDirectory();
        List<String> symbols = Files.list(dataDir)
            .filter(Files::isDirectory)
            .map(p -> p.getFileName().toString())
            .collect(Collectors.toList());
        System.out.println("Symbols: " + symbols);
        loadMarketData(symbols);
    }
    

    /**
     * Returns a list of symbols based on CSV file names in the given directory.
     * Files must be named in the format: <symbol>_data.csv
     *
     * @return list of symbols extracted from file names.
    */
    public List<String> getSymbols() throws IOException {
        Path dataDir = getLatestDataDirectory();
        List<String> symbols = new ArrayList<>();
        
        // Filter files that end with "_data.csv"
        File[] files = dataDir.toFile().listFiles((dir, name) -> name.endsWith("_data.csv"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                // Remove the "_data.csv" suffix to get the symbol
                String symbol = fileName.substring(0, fileName.length() - "_data.csv".length());
                symbols.add(symbol);
            }
        }
        return symbols;
    }
}