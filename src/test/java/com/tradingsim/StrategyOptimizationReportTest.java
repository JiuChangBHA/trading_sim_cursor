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

        // Map to store aggregated results for summary report
        Map<String, Map<LocalDate, List<Double>>> strategyTimeSeriesData = new HashMap<>();
        
        for (TradingStrategy strategy : strategies) {
            String strategyName = strategy.getClass().getSimpleName();
            System.out.println("\nOptimizing " + strategyName + "...");

            // Create strategy-specific report file
            String timestamp = LocalDate.now().format(DATE_FORMATTER);
            Path reportFile = Paths.get(REPORTS_DIR, strategyName + "_optimization_" + timestamp + ".csv");
            
            // Create a map to store time series data for this strategy
            Map<LocalDate, List<Double>> timeSeriesMetrics = new HashMap<>();
            strategyTimeSeriesData.put(strategyName, timeSeriesMetrics);
            
            try (var writer = Files.newBufferedWriter(reportFile)) {
                // Write header
                writer.write("Symbol,Parameters,Sharpe Ratio,Max Drawdown,Win Rate,Total Trades,Profit Loss\n");
                
                for (String symbol : marketDataLoader.getSymbols()) {
                    System.out.println("Processing " + symbol + "...");
                    StrategyOptimizer optimizer = optimizers.get(symbol);
                    
                    // Set parameter ranges based on strategy type
                    setStrategyParameters(optimizer, strategy);
                    
                    // Run optimization
                    List<OptimizationResult> results = optimizer.optimize(strategy);
                    
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
                        
                        // Run a simulation with the best parameters to get detailed equity curve
                        TradingStrategy optimizedStrategy = createStrategyInstance(strategy.getClass().getName());
                        optimizedStrategy.initialize(best.getParameters());
                        SimulationResult simResult = simulator.runSimulation(optimizedStrategy, symbol);
                        
                        // Export detailed simulation results
                        simulator.exportResults(simResult, symbol);
                        
                        // Store time series data for summary report
                        List<MarketData> marketData = marketDataLoader.getMarketData(symbol);
                        List<Double> equityCurve = simResult.getEquityCurve();
                        
                        for (int i = 0; i < marketData.size() && i < equityCurve.size(); i++) {
                            LocalDate date = marketData.get(i).getDate();
                            
                            // Initialize the list for this date if it doesn't exist
                            if (!timeSeriesMetrics.containsKey(date)) {
                                // [Sharpe, MaxDrawdown, WinRate, ProfitLoss]
                                timeSeriesMetrics.put(date, Arrays.asList(0.0, 0.0, 0.0, 0.0));
                            }
                            
                            // Update metrics for this date (running average)
                            List<Double> currentMetrics = timeSeriesMetrics.get(date);
                            List<Double> newMetrics = new ArrayList<>(currentMetrics);
                            
                            // Calculate metrics up to this point in the simulation
                            double sharpeRatio = calculateSharpeRatio(equityCurve.subList(0, i+1));
                            double maxDrawdown = calculateMaxDrawdown(equityCurve.subList(0, i+1));
                            double winRate = i > 0 ? best.getWinRate() : 0.0; // Use overall win rate
                            double profitLoss = i > 0 ? equityCurve.get(i) - equityCurve.get(0) : 0.0;
                            
                            // Update running averages
                            int symbolCount = timeSeriesMetrics.get(date).size() > 0 ? 
                                marketDataLoader.getSymbols().indexOf(symbol) + 1 : 1;
                            
                            newMetrics.set(0, (currentMetrics.get(0) * (symbolCount-1) + sharpeRatio) / symbolCount);
                            newMetrics.set(1, (currentMetrics.get(1) * (symbolCount-1) + maxDrawdown) / symbolCount);
                            newMetrics.set(2, (currentMetrics.get(2) * (symbolCount-1) + winRate) / symbolCount);
                            newMetrics.set(3, (currentMetrics.get(3) * (symbolCount-1) + profitLoss) / symbolCount);
                            
                            timeSeriesMetrics.put(date, newMetrics);
                        }
                    }
                }
            }
            
            System.out.println("Report generated: " + reportFile);
            
            // Generate time series summary report for this strategy
            generateTimeSeriesSummary(strategyName, strategyTimeSeriesData.get(strategyName));
        }
    }

    private void generateTimeSeriesSummary(String strategyName, Map<LocalDate, List<Double>> timeSeriesData) throws IOException {
        String timestamp = LocalDate.now().format(DATE_FORMATTER);
        Path summaryFile = Paths.get(REPORTS_DIR, strategyName + "_timeseries_" + timestamp + ".csv");
        
        try (var writer = Files.newBufferedWriter(summaryFile)) {
            // Write header
            writer.write("Date,Avg Sharpe Ratio,Avg Max Drawdown,Avg Win Rate,Avg Profit Loss\n");
            
            // Sort dates
            List<LocalDate> sortedDates = new ArrayList<>(timeSeriesData.keySet());
            Collections.sort(sortedDates);
            
            // Write data for each date
            for (LocalDate date : sortedDates) {
                List<Double> metrics = timeSeriesData.get(date);
                writer.write(String.format("%s,%.4f,%.4f,%.4f,%.2f\n",
                    date,
                    metrics.get(0), // Sharpe Ratio
                    metrics.get(1), // Max Drawdown
                    metrics.get(2), // Win Rate
                    metrics.get(3)  // Profit Loss
                ));
            }
        }
        
        System.out.println("Time series summary generated: " + summaryFile);
    }

    private double calculateSharpeRatio(List<Double> equityCurve) {
        if (equityCurve.size() < 2) return 0.0;
        
        // Calculate daily returns
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            returns.add((equityCurve.get(i) - equityCurve.get(i-1)) / equityCurve.get(i-1));
        }
        
        // Calculate mean and standard deviation
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream()
            .mapToDouble(r -> Math.pow(r - mean, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        // Calculate annualized Sharpe ratio (assuming 252 trading days)
        return stdDev > 0 ? (mean / stdDev) * Math.sqrt(252) : 0.0;
    }

    private double calculateMaxDrawdown(List<Double> equityCurve) {
        if (equityCurve.size() < 2) return 0.0;
        
        double maxDrawdown = 0.0;
        double peak = equityCurve.get(0);
        
        for (int i = 1; i < equityCurve.size(); i++) {
            double current = equityCurve.get(i);
            peak = Math.max(peak, current);
            double drawdown = (peak - current) / peak;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }
        
        return maxDrawdown;
    }

    private TradingStrategy createStrategyInstance(String className) {
        try {
            Class<?> strategyClass = Class.forName(className);
            return (TradingStrategy) strategyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create strategy instance: " + e.getMessage(), e);
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