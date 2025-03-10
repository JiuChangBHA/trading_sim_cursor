package com.tradingsim;

import java.util.List;
import java.util.Scanner;

public interface TradingStrategy {
    String getName();
    TradingSignal generateSignal(List<MarketData> marketData);
    void configure(Scanner scanner);
} 