package com.tradingsim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.tradingsim.model.MarketData;
import com.tradingsim.strategy.*;
import java.util.logging.Logger;

public class StrategyOptimizationReportTest {
    private static final Logger LOGGER = Logger.getLogger(StrategyOptimizationReportTest.class.getName());
    private static final double INITIAL_CAPITAL = 100000.0;
    private MarketDataLoader marketDataLoader;
    private static List<String> SYMBOLS = new ArrayList<>();
    private static final String REPORTS_DIR = "src/main/resources/optimization_reports";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private TradingSimulator simulator;
    private Map<String, StrategyOptimizer> optimizers;

    @BeforeEach
    void setUp() throws IOException {
        simulator = new TradingSimulator(INITIAL_CAPITAL);
        optimizers = new HashMap<>();
        
        // Load market data for all symbols
        try {
            marketDataLoader = MarketDataLoader.getInstance();
        } catch (IOException e) {
            fail("Failed to load market data: " + e.getMessage());
        }
        SYMBOLS = marketDataLoader.getSymbols();

        // Create optimizers for each symbol
        for (String symbol : SYMBOLS) {
            optimizers.put(symbol, new StrategyOptimizer(simulator, symbol));
        }
        
        // Create reports directory if it doesn't exist
        Path reportsPath = Paths.get(REPORTS_DIR);
        if (!Files.exists(reportsPath)) {
            Files.createDirectories(reportsPath);
        }
    }

    @Test
    void generateOptimizationReports() throws IOException {
        // Define strategies to optimize
        List<TradingStrategy> strategies = Arrays.asList(
            new MovingAverageCrossoverStrategy(),
            new BollingerBandsStrategy(),
            new RSIStrategy(),
            new MeanReversionStrategy()
        );

        for (TradingStrategy strategy : strategies) {
            String strategyName = strategy.getClass().getSimpleName();
            System.out.println("\nOptimizing " + strategyName + "...");

            // Create strategy-specific report file
            String timestamp = LocalDate.now().format(DATE_FORMATTER);
            Path reportFile = Paths.get(REPORTS_DIR, strategyName + "_optimization_" + timestamp + ".csv");

            try (var writer = Files.newBufferedWriter(reportFile)) {
                // Write header
                writer.write("Symbol,Parameters,Sharpe Ratio,Max Drawdown,Win Rate,Total Trades,Profit Loss\n");

                for (String symbol : marketDataLoader.getSymbols()) {
                    // System.out.println("Processing " + symbol + "...");
                    StrategyOptimizer optimizer = optimizers.get(symbol);

                    // Set parameter ranges based on strategy type
                    setStrategyParameters(optimizer, strategy);
                    
                    // Run optimization
                    List<OptimizationResult> results = optimizer.optimize(strategy);
                    // LOGGER.info("results size: " + results.size());                
                    if (!results.isEmpty()) {
                        // Get best result
                        OptimizationResult best = results.get(0);
                        
                        // Write result to file
                        writer.write(String.format("%s,%s,%.4f,%.4f,%.4f,%d,%.2f\n",
                            symbol,
                            formatParameters(best.getParameters()),
                            best.getSharpeRatio(),
                            best.getMaxDrawdown(),
                            best.getWinRate(),
                            best.getTotalTrades(),
                            best.getProfitLoss()
                        ));
                    }
                }
            }
            
            System.out.println("Report generated: " + reportFile);
        }
    }

    private List<Object> generateRange(double start, double end, double step) {
        List<Object> range = new ArrayList<>();
        for (double i = start; i <= end; i += step) {
            range.add(i);
        }
        return range;
    }

    private void setStrategyParameters(StrategyOptimizer optimizer, TradingStrategy strategy) {
        optimizer.reset();
        
        if (strategy instanceof MovingAverageCrossoverStrategy) {
            optimizer.addParameterRange("fastPeriod", generateRange(5, 20, 1));
            optimizer.addParameterRange("slowPeriod", generateRange(20, 50, 1));
        }
        else if (strategy instanceof BollingerBandsStrategy) {
            optimizer.addParameterRange("period", generateRange(5, 30, 1));
            optimizer.addParameterRange("stdDevMultiplier", generateRange(1.0, 5, 0.25));
        }
        else if (strategy instanceof RSIStrategy) {
            optimizer.addParameterRange("period", generateRange(5, 25, 2));
            optimizer.addParameterRange("overboughtThreshold", generateRange(65, 85, 5));
            optimizer.addParameterRange("oversoldThreshold", generateRange(15, 35, 5));
        }
        else if (strategy instanceof MeanReversionStrategy) {
            optimizer.addParameterRange("period", generateRange(5, 25, 5));
            optimizer.addParameterRange("threshold", generateRange(0.5, 2.5, 0.25));
        }
    }

    private String formatParameters(Map<String, Object> params) {
        return params.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + ";" + b)
            .orElse("");
    }
} 