package com.tradingsim;

import java.util.*;
import java.util.logging.Logger;

public class MarketDataLoader {
    private static final Logger LOGGER = Logger.getLogger(MarketDataLoader.class.getName());
    private static MarketDataLoader instance;
    private final Map<String, List<MarketData>> testData;
    private final Map<String, List<MarketData>> realData;

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
} 