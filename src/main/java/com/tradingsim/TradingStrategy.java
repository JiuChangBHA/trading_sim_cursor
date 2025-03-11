package com.tradingsim;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public interface TradingStrategy {
    String getName();
    Signal generateSignal(List<MarketData> data, int currentIndex);
    void configure(Scanner scanner);
    void initialize(Map<String, Object> parameters);
    int getPeriod();
} 