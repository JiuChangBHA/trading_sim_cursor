package com.tradingsim;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import com.tradingsim.strategy.TradingStrategy;
import com.tradingsim.strategy.MovingAverageCrossoverStrategy;
import com.tradingsim.strategy.MeanReversionStrategy;
import com.tradingsim.strategy.RSIStrategy;
import com.tradingsim.model.Order;
import com.tradingsim.model.Position;
import com.tradingsim.model.MarketData;
import com.tradingsim.strategy.BollingerBandsStrategy;

public class TradingSimulator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String RESULTS_DIR = "src/main/resources/simulation_results";
    
    private final List<TradingStrategy> strategies;
    private final MarketDataLoader marketDataLoader;
    private final double initialCapital;
    private final List<Order> executedOrders;
    private final List<Double> equityCurve;
    private static final Logger LOGGER = Logger.getLogger(TradingSimulator.class.getName());
    
    public TradingSimulator(double initialCapital) throws IOException {
        this.strategies = new ArrayList<>();
        this.marketDataLoader = MarketDataLoader.getInstance();
        this.initialCapital = initialCapital;
        this.executedOrders = new ArrayList<>();
        this.equityCurve = new ArrayList<>();
        
        // Initialize strategies
        strategies.add(new MovingAverageCrossoverStrategy());
        strategies.add(new MeanReversionStrategy());
        strategies.add(new RSIStrategy());
        strategies.add(new BollingerBandsStrategy());
    }

    public List<MarketData> getMarketData(String symbol) {
        return marketDataLoader.getMarketData(symbol);
    }
    
    private void logTrade(Order order) {
        LOGGER.info(String.format("Trade executed: %s - Order: %s, Price: %.2f, Quantity: %.4f, P/L: %.2f",
            order.getExecutionDate(), order.getSide(), order.getExecutionPrice(), order.getQuantity(), order.getProfitLoss()));
    }

    private void updateEquityCurve(List<Double> equityCurve, double currentEquity) {
        equityCurve.add(currentEquity);
    }

    public SimulationResult runSimulation(TradingStrategy strategy, String symbol) {
        List<MarketData> symbolData = marketDataLoader.getMarketData(symbol);
        if (symbolData == null || symbolData.isEmpty()) {
            throw new IllegalArgumentException("No market data available for symbol: " + symbol);
        }
        Map<String, Position> positions = new HashMap<>();

        List<Order> executedOrders = new ArrayList<>();
        List<Double> equityCurve = new ArrayList<>();
        double currentCapital = initialCapital;
        Position currentPosition = null;

        // Initialize strategy with parameters
        for (int i = strategy.getMinIndex(); i < symbolData.size(); i++) {
            MarketData marketData = symbolData.get(i);
            double currentPrice = marketData.getClose();
            LocalDate currentDate = marketData.getDate();
            // Update position with current price if it exists
            if (currentPosition != null) {
                currentPosition.setCurrentPrice(currentPrice);
            }

            // Process market data with strategy
            Order order = strategy.processMarketData(marketData, positions);
            if (order != null) {
                if (order.getSide() == Order.OrderSide.BUY) {
                    // Calculate how many shares we can buy with current capital
                    // Allow fractional shares
                    double sharesToBuy = currentCapital / currentPrice;
                    
                    // Apply a minimum purchase amount (e.g., $10 worth)
                    if (sharesToBuy * currentPrice >= 10.0) {
                        double cost = sharesToBuy * currentPrice;
                        currentCapital -= cost;
                        
                        // Create or update position
                        if (currentPosition == null) {
                            currentPosition = new Position(symbol, sharesToBuy, currentPrice, currentDate);
                            positions.put(symbol, currentPosition);
                        } else {
                            currentPosition.updatePosition(sharesToBuy, currentPrice, currentDate);
                        }
                        
                        // Update order quantity to actual shares bought
                        order.setQuantity(sharesToBuy);
                        
                        // Execute the order
                        order.execute(currentDate, currentPrice, 0);
                        executedOrders.add(order);
                        logTrade(order);
                    }
                } else if (order.getSide() == Order.OrderSide.SELL && currentPosition != null && currentPosition.getQuantity() > 0) {
                    double sharesToSell = currentPosition.getQuantity();
                    
                    // Calculate realized profit/loss
                    double realizedPnL = currentPosition.updatePosition(-sharesToSell, currentPrice, currentDate);
                    
                    // Update capital
                    double saleProceeds = sharesToSell * currentPrice;
                    currentCapital += saleProceeds;
                    
                    // Update order quantity to actual shares sold
                    order.setQuantity(sharesToSell);
                    
                    // Execute the order
                    order.execute(currentDate, currentPrice, realizedPnL);
                    executedOrders.add(order);
                    logTrade(order);
                    
                    // Reset position
                    currentPosition = null;
                }
            }
            // Calculate the current equity (capital + position value)
            double positionValue = (currentPosition != null) ? currentPosition.getMarketValue() : 0;
            double currentEquity = currentCapital + positionValue;
            // Use the updateEquityCurve method to record the equity value
            updateEquityCurve(equityCurve, currentEquity);
        }

        return new SimulationResult(executedOrders, equityCurve, initialCapital);
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
            writer.println("Date,Order,Price,ProfitLoss,Equity");
            
            // Write trades
            List<Order> orders = result.getExecutedOrders();
            List<Double> equityCurve = result.getEquityCurve();
            for (int i = 0; i < orders.size(); i++) {
                Order order = orders.get(i);
                double equity = i < equityCurve.size() ? equityCurve.get(i) : equityCurve.get(equityCurve.size() - 1);
                
                writer.printf("%s,%s,%.2f,%.2f,%.2f%n",
                    order.getExecutionDate(),
                    order.getSide(),
                    order.getExecutionPrice(),
                    order.getProfitLoss(),
                    equity
                );
            }
        }
        
        System.out.println("Results exported to: " + outputFile);
    }
    
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            TradingSimulator simulator = new TradingSimulator(100000.0); // $100,000 initial capital
            
            // Load all available symbols
            List<String> availableSymbols = new ArrayList<>();
            
            // Load market data for all available symbols
            availableSymbols = simulator.marketDataLoader.getSymbols();
            System.out.println("Available symbols: " + String.join(", ", availableSymbols));
         
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
            
            // Configure strategy parametersdisplay
            Map<String, Object> parameters = new HashMap<>();
            System.out.println("\nEnter strategy parameters:");
            // Add parameter configuration based on strategy type
            strategy.initialize(parameters);
            
            // Run simulation
            System.out.println("\nRunning simulation...");
            SimulationResult result = simulator.runSimulation(strategy, symbol);
            
            // Print results
            System.out.println("\nSimulation Results:");
            System.out.printf("Initial Capital: $%.2f%n", result.getInitialCapital());
            System.out.printf("Final Capital: $%.2f%n", result.getFinalCapital());
            System.out.printf("Total Return: %.2f%%%n", 
                ((result.getFinalCapital() - result.getInitialCapital()) / result.getInitialCapital()) * 100);
            System.out.println("Number of trades: " + result.getExecutedOrders().size());
            
            // Export results
            simulator.exportResults(result, symbol);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}