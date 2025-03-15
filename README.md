# Trading Simulation Platform

A comprehensive Java-based trading simulation platform for backtesting and optimizing trading strategies with historical market data.

## Overview

This project provides a robust environment for simulating trading activities and optimizing strategy parameters, allowing users to:
- Backtest trading strategies using historical market data
- Optimize strategy parameters to maximize performance metrics
- Visualize trading performance and portfolio metrics
- Analyze market behavior and strategy effectiveness

## Project Structure

- `src/main/java/`: Core Java implementation of the trading simulator and strategies
- `src/main/python/`: Python scripts for data loading
- `src/test/java/`: Test classes including strategy optimization tests
- `resources/`: 
  - `lib/market_data/`: Directory for storing market data
  - `optimization_reports/`: Saved optimization results
  - `simulation_results/`: Saved simulation results
  - `visualization/`: Scripts for visualizing results

### Directory Tree
```
trading_sim/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/
│   │   │   │   └── tradingsim/
│   │   │   │       ├── model/
│   │   │   │       │   ├── MarketData.java
│   │   │   │       │   ├── Order.java
│   │   │   │       │   └── Position.java
│   │   │   │       ├── strategy/
│   │   │   │       │   ├── BaseStrategy.java
│   │   │   │       │   ├── MovingAverageStrategy.java
│   │   │   │       │   ├── RSIStrategy.java
│   │   │   │       │   └── MACDStrategy.java
│   │   │   │       └── model/
│   │   │   │           ├── MarketDataLoader.java
│   │   │   │           ├── OptimizationResult.java
│   │   │   │           ├── SimulationResult.java
│   │   │   │           ├── StrategyOptimizer.java
│   │   │   │           ├── TradingAccount.java
│   │   │   │           └── TradingSimulator.java
│   │   ├── python/
│   │   │   └── RealTimeMarketDataLoader.py
│   │   └── resources/
│   │       ├── lib
│   │       │   └── json-simple-1.1.1.jar
│   │       ├── market_data
│   │       ├── optimization_reports
│   │       ├── simulation_results
│   │       ├── visualization
│   │       │   └── trading_visualization.jar
│   │       └── logging.properties
│   ├── test/
│   │   └── java/
│   │       └── com/
│   │           └── tradingsim/
│   │               ├── TradingSimulatorTest.java
│   │               ├── StrategyOptimization.java
│   │               ├── StrategyOptimizationReportTest.java
│   │               └── strategy/
│   │                   ├── MeanReversionStrategyTest.java
│   │                   ├── RSIStrategyTest.java
│   │                   ├── MovingAverageCrossoverStrategyTest.java
│   │                   └── BollingerBandsStrategyTest.java
│   └── .env
├── pom.xml
├── README.md
└── LICENSE
```

## Getting Started

### Prerequisites

- Java JDK 11+
- Maven
- Python 3.8+ (for data loading and visualization)
- Required Python packages:
  - pandas
  - numpy
  - matplotlib
  - yfinance (for data fetching)
- API Keys (`src/.env`)

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/trading_sim.git
   cd trading_sim
   ```

2. Build the project with Maven:
   ```
   mvn clean install
   ```

3. Install Python dependencies:
   ```
   pip install pandas numpy matplotlib yfinance
   ```

## Usage

### Loading Market Data

Run the Python script to fetch and prepare market data:

```
python src/main/python/load_market_data.py
```

This will save the market data to `resources/lib/market_data/` for use in simulations.

### Running a Simulation

To run a trading simulation with the default parameters:

```
mvn exec:java
```

This command executes the `TradingSimulator.java` main class, which runs the simulation with predefined strategies and parameters. Simulation results will be saved to the `resources/simulation_results/` directory.

### Strategy Optimization

To optimize strategy parameters:

```
mvn test -Dtest=StrategyOptimizationReportTest
```

This runs the optimization test which will:
1. Test various combinations of strategy parameters defined in `StrategyOptimizationReportTest.java`
2. Evaluate each parameter set against historical data
3. Save optimization reports to `resources/optimization_reports/`

You can modify the parameter ranges in `StrategyOptimizationReportTest.java` to customize the optimization process.

### Visualization

To visualize simulation and optimization results:

1. Navigate to the visualization directory:
   ```
   cd resources/visualization
   ```

2. Run the visualization scripts:
   ```
   python visualize_simulation.py
   python visualize_optimization.py
   ```

These scripts read the saved files from `optimization_reports/` and `simulation_results/` directories and generate visual representations of the results.

## Customizing Strategies

To implement your own trading strategy:

1. Create a new class that extends the base Strategy class
2. Implement the required methods for signal generation
3. Register your strategy in the `TradingSimulator.java` file
4. Run the simulation with your custom strategy

Example:
```java
public class MyCustomStrategy extends BaseStrategy {
    private List<Double> prices = new ArrayList<>();
    private double currentPrice = 0.0;

    @Override
    public String getName() {
        return "My Custom";
    }
    
    @Override
    public String getDescription() {
        return "You can create your custom strategies like this";
    }
    
    public void initialize(Map<String, Object> parameters) {
        // Only set defaults if not already present
        if (!parameters.containsKey("period")) {
            parameters.put("period", 20);
        }
        super.initialize(parameters);
        // Rest of your initialization
        prices.clear();
        currentPrice = 0.0;
    }

    
    @Override
    public void configure(Scanner scanner) {
        System.out.println("\nConfiguring " + getName() + " Strategy");
        System.out.print("Enter period (default: " + parameters.get("period") + "): ");
        String input = scanner.nextLine();
        // ...
    }

    @Override
    public Order processMarketData(MarketData marketData, Map<String, Position> positions) {
        currentPrice = marketData.getClose();
        String symbol = marketData.getSymbol();
    }

    @Override
    public int getMinIndex() {
        int period = parameters.containsKey("period") ? 
                    (int) getIntegerParameter("period") : 20;
        return period;
    }

    @Override
    protected boolean extraValidations() {
        return true;
    }
    
    @Override
    public void reset() {
        super.reset();
        prices.clear();
        // ...
        currentPrice = 0.0;
    }
}
```

## Configuration

Key configuration files:
- `StrategyOptimizationReportTest.java`: Parameter ranges for optimization

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Thanks to all contributors who have helped shape this project
- Inspired by various open-source trading frameworks and financial analysis tools 