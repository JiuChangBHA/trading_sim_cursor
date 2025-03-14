"""
Trading Simulator Visualization

This script visualizes the results from the trading simulator and strategy optimization.
It loads simulation and optimization CSV files, plots equity curves, compares strategies,
and displays time series metrics.
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import glob
import os
from datetime import datetime

# Set plot style
plt.style.use('ggplot')
sns.set_theme(style="darkgrid")
plt.rcParams['figure.figsize'] = [12, 8]
plt.rcParams['figure.dpi'] = 100


# =============================================================================
# 1. Simulation Results Visualization
# =============================================================================
def list_simulation_files(sim_results_path):
    """
    Get the latest simulation files from the given directory.
    """
    sim_files = glob.glob(os.path.join(sim_results_path, '*_simulation_*.csv'))
    sim_files.sort(key=os.path.getmtime, reverse=True)
    print("Available simulation files:")
    for i, file in enumerate(sim_files[:10]):  # show top 10 most recent
        print(f"{i+1}. {os.path.basename(file)}")
    return sim_files


def plot_equity_curve(file_path):
    """
    Plot equity curve from a simulation result file.
    """
    # Read the CSV file
    df = pd.read_csv(file_path)
    
    # Convert Date to datetime
    df['Date'] = pd.to_datetime(df['Date'])
    
    # Extract symbol and strategy from filename
    filename = os.path.basename(file_path)
    parts = filename.split('_')
    symbol = parts[0]
    strategy = parts[1] if len(parts) > 1 else 'Unknown'
    
    # Plot equity curve
    plt.figure(figsize=(14, 8))
    plt.plot(df['Date'], df['Equity'], label='Equity Curve', linewidth=2)
    
    # Mark buy and sell points
    buys = df[df['Order'] == 'BUY']
    sells = df[df['Order'] == 'SELL']
    
    if not buys.empty:
        plt.scatter(buys['Date'], buys['Equity'], color='green', s=100, marker='^', label='Buy')
    if not sells.empty:
        plt.scatter(sells['Date'], sells['Equity'], color='red', s=100, marker='v', label='Sell')
    
    # Calculate performance metrics
    initial_equity = df['Equity'].iloc[0]
    final_equity = df['Equity'].iloc[-1]
    total_return = ((final_equity / initial_equity) - 1) * 100
    
    # Add title and labels
    plt.title(f'{symbol} - {strategy} Strategy\nTotal Return: {total_return:.2f}%', fontsize=16)
    plt.xlabel('Date', fontsize=14)
    plt.ylabel('Equity ($)', fontsize=14)
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=12)
    
    # Format y-axis as currency
    plt.gca().yaxis.set_major_formatter(plt.FuncFormatter(lambda x, _: f'${x:,.0f}'))
    
    plt.tight_layout()
    plt.show()
    
    return df


# =============================================================================
# 2. Compare Multiple Strategies
# =============================================================================
def compare_strategies(symbol, strategy_files):
    """
    Compare multiple strategies for the same symbol by plotting their equity curves.
    """
    plt.figure(figsize=(14, 8))
    
    for file_path in strategy_files:
        # Read the CSV file
        df = pd.read_csv(file_path)
        df['Date'] = pd.to_datetime(df['Date'])
        filename = os.path.basename(file_path)
        parts = filename.split('_')
        strategy = parts[1] if len(parts) > 1 else 'Unknown'
        plt.plot(df['Date'], df['Equity'], label=f'{strategy}', linewidth=2)
    
    plt.title(f'Strategy Comparison for {symbol}', fontsize=16)
    plt.xlabel('Date', fontsize=14)
    plt.ylabel('Equity ($)', fontsize=14)
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=12)
    plt.gca().yaxis.set_major_formatter(plt.FuncFormatter(lambda x, _: f'${x:,.0f}'))
    plt.tight_layout()
    plt.show()


# =============================================================================
# 3. Optimization Results Visualization
# =============================================================================
def list_optimization_files(opt_results_path):
    """
    Get optimization and time series files from the optimization directory.
    """
    opt_files = glob.glob(os.path.join(opt_results_path, '*_optimization_*.csv'))
    opt_files.sort(key=os.path.getmtime, reverse=True)
    ts_files = glob.glob(os.path.join(opt_results_path, '*_timeseries_*.csv'))
    ts_files.sort(key=os.path.getmtime, reverse=True)
    
    print("Available optimization files:")
    for i, file in enumerate(opt_files[:5]):
        print(f"{i+1}. {os.path.basename(file)}")
    
    print("\nAvailable time series files:")
    for i, file in enumerate(ts_files[:5]):
        print(f"{i+1}. {os.path.basename(file)}")
    
    return opt_files, ts_files


def plot_optimization_results(file_path):
    """
    Plot optimization results from a file.
    """
    df = pd.read_csv(file_path)
    filename = os.path.basename(file_path)
    strategy = filename.split('_')[0]
    
    fig, axes = plt.subplots(2, 2, figsize=(16, 12))
    
    sns.barplot(x='Symbol', y='Sharpe Ratio', data=df, ax=axes[0, 0])
    axes[0, 0].set_title('Sharpe Ratio by Symbol', fontsize=14)
    axes[0, 0].set_xticklabels(axes[0, 0].get_xticklabels(), rotation=45)
    
    sns.barplot(x='Symbol', y='Max Drawdown', data=df, ax=axes[0, 1])
    axes[0, 1].set_title('Max Drawdown by Symbol', fontsize=14)
    axes[0, 1].set_xticklabels(axes[0, 1].get_xticklabels(), rotation=45)
    
    sns.barplot(x='Symbol', y='Win Rate', data=df, ax=axes[1, 0])
    axes[1, 0].set_title('Win Rate by Symbol', fontsize=14)
    axes[1, 0].set_xticklabels(axes[1, 0].get_xticklabels(), rotation=45)
    
    sns.barplot(x='Symbol', y='Profit Loss', data=df, ax=axes[1, 1])
    axes[1, 1].set_title('Profit/Loss by Symbol', fontsize=14)
    axes[1, 1].set_xticklabels(axes[1, 1].get_xticklabels(), rotation=45)
    
    plt.suptitle(f'Optimization Results for {strategy} Strategy', fontsize=18)
    plt.tight_layout(rect=[0, 0, 1, 0.96])
    plt.show()
    
    print(f"\nParameter Statistics for {strategy} Strategy:")
    param_data = {}
    for _, row in df.iterrows():
        params = row['Parameters'].split(';')
        for param in params:
            key, value = param.split('=')
            if key not in param_data:
                param_data[key] = []
            try:
                param_data[key].append(float(value))
            except ValueError:
                param_data[key].append(value)
    
    for param, values in param_data.items():
        if all(isinstance(v, (int, float)) for v in values):
            print(f"{param}: Mean = {np.mean(values):.2f}, Min = {min(values):.2f}, Max = {max(values):.2f}")
        else:
            print(f"{param}: Values = {values}")
    
    return df


# =============================================================================
# 4. Time Series Metrics Visualization
# =============================================================================
def plot_time_series_metrics(file_path):
    """
    Plot time series metrics from a file.
    """
    df = pd.read_csv(file_path)
    df['Date'] = pd.to_datetime(df['Date'])
    filename = os.path.basename(file_path)
    strategy = filename.split('_')[0]
    
    fig, axes = plt.subplots(2, 2, figsize=(16, 12))
    
    axes[0, 0].plot(df['Date'], df['Avg Sharpe Ratio'], linewidth=2)
    axes[0, 0].set_title('Average Sharpe Ratio Over Time', fontsize=14)
    axes[0, 0].set_xlabel('Date')
    axes[0, 0].set_ylabel('Sharpe Ratio')
    axes[0, 0].grid(True, alpha=0.3)
    
    axes[0, 1].plot(df['Date'], df['Avg Max Drawdown'], linewidth=2)
    axes[0, 1].set_title('Average Max Drawdown Over Time', fontsize=14)
    axes[0, 1].set_xlabel('Date')
    axes[0, 1].set_ylabel('Max Drawdown')
    axes[0, 1].grid(True, alpha=0.3)
    
    axes[1, 0].plot(df['Date'], df['Avg Win Rate'], linewidth=2)
    axes[1, 0].set_title('Average Win Rate Over Time', fontsize=14)
    axes[1, 0].set_xlabel('Date')
    axes[1, 0].set_ylabel('Win Rate')
    axes[1, 0].grid(True, alpha=0.3)
    
    axes[1, 1].plot(df['Date'], df['Avg Profit Loss'], linewidth=2)
    axes[1, 1].set_title('Average Profit/Loss Over Time', fontsize=14)
    axes[1, 1].set_xlabel('Date')
    axes[1, 1].set_ylabel('Profit/Loss ($)')
    axes[1, 1].grid(True, alpha=0.3)
    
    plt.suptitle(f'Time Series Metrics for {strategy} Strategy', fontsize=18)
    plt.tight_layout(rect=[0, 0, 1, 0.96])
    plt.show()
    
    return df


# =============================================================================
# 5. Compare Strategies Performance
# =============================================================================
def compare_strategy_performance(opt_files):
    """
    Compare performance metrics across all strategies.
    """
    all_data = []
    for file_path in opt_files:
        filename = os.path.basename(file_path)
        strategy = filename.split('_')[0]
        df = pd.read_csv(file_path)
        df['Strategy'] = strategy
        all_data.append(df)
    
    if not all_data:
        print("No optimization files found.")
        return
    
    combined_df = pd.concat(all_data, ignore_index=True)
    
    fig, axes = plt.subplots(2, 2, figsize=(16, 12))
    
    sns.boxplot(x='Strategy', y='Sharpe Ratio', data=combined_df, ax=axes[0, 0])
    axes[0, 0].set_title('Sharpe Ratio by Strategy', fontsize=14)
    axes[0, 0].set_xticklabels(axes[0, 0].get_xticklabels(), rotation=45)
    
    sns.boxplot(x='Strategy', y='Max Drawdown', data=combined_df, ax=axes[0, 1])
    axes[0, 1].set_title('Max Drawdown by Strategy', fontsize=14)
    axes[0, 1].set_xticklabels(axes[0, 1].get_xticklabels(), rotation=45)
    
    sns.boxplot(x='Strategy', y='Win Rate', data=combined_df, ax=axes[1, 0])
    axes[1, 0].set_title('Win Rate by Strategy', fontsize=14)
    axes[1, 0].set_xticklabels(axes[1, 0].get_xticklabels(), rotation=45)
    
    sns.boxplot(x='Strategy', y='Profit Loss', data=combined_df, ax=axes[1, 1])
    axes[1, 1].set_title('Profit/Loss by Strategy', fontsize=14)
    axes[1, 1].set_xticklabels(axes[1, 1].get_xticklabels(), rotation=45)
    
    plt.suptitle('Strategy Performance Comparison', fontsize=18)
    plt.tight_layout(rect=[0, 0, 1, 0.96])
    plt.show()
    
    print("\nSummary Statistics by Strategy:")
    summary = combined_df.groupby('Strategy').agg({
        'Sharpe Ratio': ['mean', 'std', 'min', 'max'],
        'Max Drawdown': ['mean', 'std', 'min', 'max'],
        'Win Rate': ['mean', 'std', 'min', 'max'],
        'Profit Loss': ['mean', 'std', 'min', 'max']
    })
    print(summary)
    return summary


# =============================================================================
# Main Execution Block
# =============================================================================
if __name__ == '__main__':
    # 1. Simulation Results Visualization
    sim_results_path = os.path.join('..', '..', 'src', 'main', 'resources', 'simulation_results')
    sim_files = list_simulation_files(sim_results_path)
    
    if sim_files:
        selected_file_index = 0  # Change this to select a different file
        selected_file = sim_files[selected_file_index]
        df_sim = plot_equity_curve(selected_file)
        
        # Display trade statistics if available
        trades = df_sim[df_sim['Order'] != 'NONE']
        if not trades.empty:
            print("\nTrade Statistics:")
            print(f"Number of trades: {len(trades)}")
            print(f"Profitable trades: {len(trades[trades['ProfitLoss'] > 0])}")
            print(f"Loss-making trades: {len(trades[trades['ProfitLoss'] < 0])}")
            win_rate = len(trades[trades['ProfitLoss'] > 0]) / len(trades) * 100
            print(f"Win rate: {win_rate:.2f}%")
            avg_profit = trades[trades['ProfitLoss'] > 0]['ProfitLoss'].mean()
            avg_loss = trades[trades['ProfitLoss'] < 0]['ProfitLoss'].mean()
            print(f"Average profit per winning trade: ${avg_profit:.2f}")
            print(f"Average loss per losing trade: ${avg_loss:.2f}")
    else:
        print("No simulation files found.")
    
    # 2. Compare Multiple Strategies for a given symbol
    symbol_to_compare = 'AAPL'  # Change this to the symbol you want to compare
    symbol_files = [f for f in sim_files if os.path.basename(f).startswith(symbol_to_compare)]
    if symbol_files:
        compare_strategies(symbol_to_compare, symbol_files)
    else:
        print(f"No simulation files found for {symbol_to_compare}.")
    
    # 3. Optimization Results Visualization
    opt_results_path = os.path.join('..', '..', 'src', 'main', 'resources', 'optimization_reports')
    opt_files, ts_files = list_optimization_files(opt_results_path)
    
    if opt_files:
        selected_file_index = 0  # Change this to select a different file
        selected_opt_file = opt_files[selected_file_index]
        plot_optimization_results(selected_opt_file)
    else:
        print("No optimization files found.")
    
    # 4. Time Series Metrics Visualization
    if ts_files:
        selected_ts_index = 0  # Change this to select a different file
        selected_ts_file = ts_files[selected_ts_index]
        plot_time_series_metrics(selected_ts_file)
    else:
        print("No time series files found.")
    
    # 5. Compare Strategies Performance
    summary_stats = compare_strategy_performance(opt_files)
