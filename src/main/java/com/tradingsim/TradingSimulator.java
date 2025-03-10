package com.tradingsim;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TradingSimulator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String MARKET_DATA_DIR = "src/main/resources/market_data";
    private static final String RESULTS_DIR = "src/main/resources/simulation_results";
    
    private final List<TradingStrategy> strategies;
    private final Map<String, List<MarketData>> marketData;
    private final TradingAccount account;
    private final List<TradeExecuted> trades;
    private final List<Double> equityCurve;
    
    public TradingSimulator(double initialCapital) {
        this.strategies = new ArrayList<>();
        this.marketData = new HashMap<>();
        this.account = new TradingAccount(initialCapital);
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
    
    public SimulationResult runSimulation(String symbol, TradingStrategy strategy) {
        // Convert symbol to uppercase for case-insensitive lookup
        String upperSymbol = symbol.toUpperCase();
        List<MarketData> symbolData = marketData.get(upperSymbol);
        
        if (symbolData == null || symbolData.isEmpty()) {
            throw new IllegalArgumentException("No market data found for symbol: " + symbol);
        }
        
        // Reset account and trades
        account.setBalance(account.getInitialBalance());
        account.setShares(0);
        trades.clear();
        equityCurve.clear();
        
        // Run simulation
        for (int i = 0; i < symbolData.size(); i++) {
            List<MarketData> historicalData = symbolData.subList(0, i + 1);
            TradingSignal signal = strategy.generateSignal(historicalData);
            
            if (signal != TradingSignal.HOLD) {
                MarketData currentData = historicalData.get(i);
                executeTrade(signal, currentData);
            }
            
            // Record equity
            double currentEquity = account.getBalance() + 
                (account.getShares() * symbolData.get(i).getClose());
            equityCurve.add(currentEquity);
        }
        
        return new SimulationResult(
            account.getInitialBalance(),
            account.getBalance() + (account.getShares() * symbolData.get(symbolData.size() - 1).getClose()),
            new ArrayList<>(trades),
            new ArrayList<>(equityCurve),
            strategy.getName(),
            symbolData
        );
    }
    
    private void executeTrade(TradingSignal signal, MarketData data) {
        double price = data.getClose();
        double shares = 0;
        
        if (signal == TradingSignal.BUY) {
            shares = account.getBalance() / price;
            account.setShares(account.getShares() + shares);
            account.setBalance(0);
        } else if (signal == TradingSignal.SELL) {
            shares = account.getShares();
            account.setBalance(account.getBalance() + (shares * price));
            account.setShares(0);
        }
        
        if (shares > 0) {
            trades.add(new TradeExecuted(
                data.getDate(),
                signal,
                shares,
                price,
                shares * price
            ));
        }
    }
    
    public void exportResults(SimulationResult result, String symbol) throws IOException {
        Path resultsPath = Paths.get(RESULTS_DIR);
        if (!Files.exists(resultsPath)) {
            Files.createDirectories(resultsPath);
        }
        
        String timestamp = LocalDate.now().format(DATE_FORMATTER);
        String filename = String.format("%s_%s_%s.csv", 
            symbol, result.getStrategyName().replaceAll("\\s+", "_"), timestamp);
        
        Path outputFile = resultsPath.resolve(filename);
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile))) {
            // Write header
            writer.println("Date,Signal,Shares,Price,Value,Equity");
            
            // Write trades
            for (TradeExecuted trade : result.getTrades()) {
                // Find the corresponding equity value for this trade's date
                double equity = 0;
                for (int i = 0; i < result.getMarketData().size(); i++) {
                    if (result.getMarketData().get(i).getDate().equals(trade.getDate())) {
                        equity = result.getEquityCurve().get(i);
                        break;
                    }
                }
                
                writer.printf("%s,%s,%.2f,%.2f,%.2f,%.2f%n",
                    trade.getDate(),
                    trade.getSignal(),
                    trade.getShares(),
                    trade.getPrice(),
                    trade.getValue(),
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
            SimulationResult result = simulator.runSimulation(symbol, strategy);
            
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