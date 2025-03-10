# Trading Simulator

A Java-based trading simulator that allows you to backtest various trading strategies using historical market data. The simulator supports multiple technical analysis strategies including Moving Average Crossover, Mean Reversion, RSI, and Bollinger Bands.

## Features

- Multiple trading strategies implementation
- Historical market data support
- Performance metrics calculation (Sharpe ratio, max drawdown)
- Interactive command-line interface
- CSV export of simulation results
- Comprehensive unit tests

## Trading Strategies

1. **Moving Average Crossover**
   - Generates buy signals when short-term MA crosses above long-term MA
   - Generates sell signals when short-term MA crosses below long-term MA

2. **Mean Reversion**
   - Identifies overbought and oversold conditions
   - Generates buy signals when price is below mean
   - Generates sell signals when price is above mean

3. **RSI (Relative Strength Index)**
   - Uses RSI to identify overbought and oversold conditions
   - Generates buy signals in oversold conditions
   - Generates sell signals in overbought conditions

4. **Bollinger Bands**
   - Uses price position relative to Bollinger Bands
   - Generates buy signals when price is below lower band
   - Generates sell signals when price is above upper band

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

## Installation

1. Clone the repository:
```bash
git clone https://github.com/jiuchang07/trading_sim_cursor.git
cd trading_sim_cursor
```

2. Build the project:
```bash
mvn clean install
```

## Usage

1. Run the simulator:
```bash
mvn exec:java -Dexec.mainClass="com.tradingsim.TradingSimulator"
```

2. Follow the interactive prompts to:
   - Select a trading symbol
   - Choose a trading strategy
   - Configure strategy parameters
   - View simulation results

3. Simulation results will be exported to CSV files in the `src/main/resources/simulation_results` directory.

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── tradingsim/
│   │           ├── TradingSimulator.java
│   │           ├── TradingStrategy.java
│   │           ├── MovingAverageCrossoverStrategy.java
│   │           ├── MeanReversionStrategy.java
│   │           ├── RSIStrategy.java
│   │           ├── BollingerBandsStrategy.java
│   │           ├── MarketData.java
│   │           ├── TradingSignal.java
│   │           ├── TradeExecuted.java
│   │           ├── TradingAccount.java
│   │           └── SimulationResult.java
│   └── resources/
│       ├── market_data/
│       └── simulation_results/
└── test/
    └── java/
        └── com/
            └── tradingsim/
                └── TradingSimulatorTest.java
```

## Testing

Run the test suite:
```bash
mvn test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

- Jiuchang07

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. 