# Trading Simulation

A comprehensive trading simulation platform for backtesting trading strategies with historical market data.

## Overview

This project provides a flexible environment for simulating trading activities, allowing users to:
- Backtest trading strategies using historical market data
- Visualize trading performance and portfolio metrics
- Implement and compare different trading algorithms
- Analyze market behavior and strategy effectiveness

## Features

- **Historical Data Integration**: Import and use real market data for accurate simulations
- **Strategy Implementation**: Create and test custom trading strategies
- **Portfolio Management**: Track portfolio performance, including profits, losses, and risk metrics
- **Visualization Tools**: Generate charts and reports to analyze trading results
- **Customizable Parameters**: Adjust simulation settings to test different market scenarios

## Getting Started

### Prerequisites

- Python 3.8+
- Required Python packages (install via `pip install -r requirements.txt`):
  - pandas
  - numpy
  - matplotlib
  - yfinance (for data fetching)
  - other dependencies as specified in requirements.txt

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/trading_sim.git
   cd trading_sim
   ```

2. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

3. Run the simulation:
   ```
   python main.py
   ```

## Usage

### Basic Simulation

```python
from trading_sim import TradingSimulator, Strategy

# Initialize a simple strategy
strategy = Strategy.SimpleMovingAverage(short_period=10, long_period=30)

# Create simulator with historical data
simulator = TradingSimulator(data_source='AAPL', start_date='2020-01-01', end_date='2021-01-01')

# Run simulation with the strategy
results = simulator.run(strategy, initial_capital=10000)

# Display results
results.summary()
results.plot_performance()
```

### Creating Custom Strategies

Implement your own trading strategies by extending the base Strategy class:

```python
from trading_sim import Strategy

class MyCustomStrategy(Strategy):
    def __init__(self, parameter1, parameter2):
        self.param1 = parameter1
        self.param2 = parameter2
        
    def generate_signals(self, market_data):
        # Implement your strategy logic here
        # Return buy/sell signals based on market_data
        return signals
```

## Project Structure

- `data/`: Contains historical market data and data handling utilities
- `strategies/`: Implementation of various trading strategies
- `simulator/`: Core simulation engine
- `visualization/`: Tools for generating charts and visual reports
- `utils/`: Helper functions and utilities
- `examples/`: Example scripts demonstrating different use cases

## Configuration

Adjust simulation parameters in the `config.py` file:

```python
# Example configuration
CONFIG = {
    'default_capital': 10000,
    'commission_rate': 0.001,
    'risk_free_rate': 0.02,
    'data_source': 'yahoo',
    'log_level': 'INFO'
}
```

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