package com.tradingsim;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

public class TradingSimulator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String MARKET_DATA_DIR = "src/main/resources/market_data";
    private static final String RESULTS_DIR = "src/main/resources/simulation_results";
    
    private final List<TradingStrategy> strategies;
    private final Map<String, List<MarketData>> marketData;
    private final double initialCapital;
    private final List<TradeExecuted> trades;
    private final List<Double> equityCurve;
    private static final Logger LOGGER = Logger.getLogger(TradingSimulator.class.getName());
    
    public TradingSimulator(double initialCapital) {
        this.strategies = new ArrayList<>();
        this.marketData = new HashMap<>();
        this.initialCapital = initialCapital;
        this.trades = new ArrayList<>();
        this.equityCurve = new ArrayList<>();
        
        // Initialize strategies
        strategies.add(new MovingAverageCrossoverStrategy());
        strategies.add(new MeanReversionStrategy());
        strategies.add(new RSIStrategy());
        strategies.add(new BollingerBandsStrategy());
    }
    
    public void loadMarketData() throws IOException {
        Path marketDataPath = Paths.get(MARKET_DATA_DIR);
        if (!Files.exists(marketDataPath)) {
            throw new IOException("Market data directory not found: " + MARKET_DATA_DIR);
        }
        
        // Find the most recent market data directory
        Path latestDir = Files.list(marketDataPath)
            .filter(Files::isDirectory)
            .max(Comparator.comparing(p -> p.getFileName().toString()))
            .orElseThrow(() -> new IOException("No market data directories found"));
            
        System.out.println("Loading market data from: " + latestDir);
        
        // Load data for each symbol
        Files.list(latestDir)
            .filter(p -> p.toString().endsWith(".csv"))
            .forEach(this::loadSymbolData);
    }
    
    private void loadSymbolData(Path file) {
        try {
            String symbol = file.getFileName().toString().replace("_data.csv", "");
            List<MarketData> data = new ArrayList<>();
            
            List<String> lines = Files.readAllLines(file);
            // Skip header row
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i).split(",");
                MarketData marketData = new MarketData(
                    LocalDate.parse(fields[0], DATE_FORMATTER),
                    fields[1], // Use symbol from CSV
                    Double.parseDouble(fields[2]), // Open
                    Double.parseDouble(fields[3]), // High
                    Double.parseDouble(fields[4]), // Low
                    Double.parseDouble(fields[5]), // Close
                    (long) Double.parseDouble(fields[6]) // Volume - parse as Double first
                );
                data.add(marketData);
            }
            
            marketData.put(symbol, data);
        } catch (IOException e) {
            System.err.println("Error loading data for " + file.getFileName() + ": " + e.getMessage());
        }
    }
    
    private void logTrade(TradeExecuted trade) {
        LOGGER.info(String.format("Trade executed: %s - Signal: %s, Price: %.2f, P/L: %.2f",
            trade.getDate(), trade.getSignal(), trade.getPrice(), trade.getProfitLoss()));
    }

    private void updateEquityCurve(List<Double> equityCurve, double currentEquity) {
        equityCurve.add(currentEquity);
    }

    public SimulationResult runSimulation(TradingStrategy strategy, String symbol) {
        List<MarketData> symbolData = marketData.get(symbol);
        if (symbolData == null || symbolData.isEmpty()) {
            throw new IllegalArgumentException("No market data available for symbol: " + symbol);
        }

        List<TradeExecuted> trades = new ArrayList<>();
        List<Double> equityCurve = new ArrayList<>();
        double currentCapital = initialCapital;
        int currentShares = 0;

        // Initialize strategy with parameters
        strategy.initialize(new HashMap<>());

        for (int i = strategy.getPeriod(); i < symbolData.size(); i++) {
            Signal signal = strategy.generateSignal(symbolData, i);
            double currentPrice = symbolData.get(i).getClose();
            LocalDate currentDate = symbolData.get(i).getDate();

            if (signal == Signal.BUY && currentShares == 0) {
                int sharesToBuy = (int) (currentCapital / currentPrice);
                if (sharesToBuy > 0) {
                    currentShares = sharesToBuy;
                    double cost = currentShares * currentPrice;
                    currentCapital -= cost;
                    trades.add(new TradeExecuted(currentDate, signal, currentPrice, 0));
                    logTrade(trades.get(trades.size() - 1));
                }
            } else if (signal == Signal.SELL && currentShares > 0) {
                double saleProceeds = currentShares * currentPrice;
                double profitLoss = saleProceeds - (currentShares * trades.get(trades.size() - 1).getPrice());
                currentCapital += saleProceeds;
                currentShares = 0;
                trades.add(new TradeExecuted(currentDate, signal, currentPrice, profitLoss));
                logTrade(trades.get(trades.size() - 1));
            }

            // Update equity curve
            equityCurve.add(currentCapital + (currentShares * currentPrice));
        }

        return new SimulationResult(trades, equityCurve);
    }
    
    public void exportResults(SimulationResult result, String symbol) throws IOException {
        Path resultsPath = Paths.get(RESULTS_DIR);
        if (!Files.exists(resultsPath)) {
            Files.createDirectories(resultsPath);
        }
        
        String timestamp = LocalDate.now().format(DATE_FORMATTER);
        String filename = String.format("%s_simulation_%s.csv", symbol, timestamp);
        Path outputFile = resultsPath.resolve(filename);
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile))) {
            // Write header
            writer.println("Date,Signal,Price,ProfitLoss,Equity");
            
            // Write trades
            List<TradeExecuted> trades = result.getTrades();
            List<Double> equityCurve = result.getEquityCurve();
            for (int i = 0; i < trades.size(); i++) {
                TradeExecuted trade = trades.get(i);
                double equity = i < equityCurve.size() ? equityCurve.get(i) : equityCurve.get(equityCurve.size() - 1);
                
                writer.printf("%s,%s,%.2f,%.2f,%.2f%n",
                    trade.getDate(),
                    trade.getSignal(),
                    trade.getPrice(),
                    trade.getProfitLoss(),
                    equity
                );
            }
        }
        
        System.out.println("Results exported to: " + outputFile);
    }
    
    public Map<String, List<MarketData>> getMarketData() {
        return marketData;
    }
    
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            TradingSimulator simulator = new TradingSimulator(100000.0); // $100,000 initial capital
            
            // Load market data
            simulator.loadMarketData();
            
            // Select symbol
            System.out.print("\nEnter symbol to trade: ");
            String symbol = scanner.nextLine();
            
            // Select strategy
            System.out.println("\nAvailable strategies:");
            for (int i = 0; i < simulator.strategies.size(); i++) {
                System.out.println((i + 1) + ". " + simulator.strategies.get(i).getName());
            }
            System.out.print("\nSelect strategy (1-" + simulator.strategies.size() + "): ");
            int strategyIndex = Integer.parseInt(scanner.nextLine()) - 1;
            TradingStrategy strategy = simulator.strategies.get(strategyIndex);
            
            // Configure strategy
            strategy.configure(scanner);
            
            // Run simulation
            System.out.println("\nRunning simulation...");
            SimulationResult result = simulator.runSimulation(strategy, symbol);
            
            // Print results
            System.out.println("\nSimulation Results:");
            System.out.printf("Initial Capital: $%.2f%n", result.getInitialCapital());
            System.out.printf("Final Capital: $%.2f%n", result.getFinalCapital());
            System.out.printf("Total Return: %.2f%%%n", 
                ((result.getFinalCapital() - result.getInitialCapital()) / result.getInitialCapital()) * 100);
            System.out.println("Number of trades: " + result.getTrades().size());
            
            // Export results
            simulator.exportResults(result, symbol);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 