import React, { useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const StrategyDashboard = () => {
  // Sample data based on your provided information
  const data = [
    {
      Symbol: "AAPL",
      Parameters: "period=27.0;stdDevMultiplier=2.0",
      "Sharpe Ratio": 0.7755,
      "Max Drawdown": 0.2166,
      "Win Rate": 0.4242,
      "Total Trades": 33,
      "Profit Loss": 1.35
    },
    {
      Symbol: "ABBV",
      Parameters: "period=12.0;stdDevMultiplier=1.5",
      "Sharpe Ratio": 0.7825,
      "Max Drawdown": 0.1379,
      "Win Rate": 0.3810,
      "Total Trades": 84,
      "Profit Loss": 0.97
    },
    {
      Symbol: "ABNB",
      Parameters: "period=15.0;stdDevMultiplier=3.0",
      "Sharpe Ratio": 0.5276,
      "Max Drawdown": 0.2678,
      "Win Rate": 0.5000,
      "Total Trades": 6,
      "Profit Loss": 0.79
    }
  ];

  // State to track which metric to display
  const [selectedMetric, setSelectedMetric] = useState("Sharpe Ratio");
  
  // Available metrics for dropdown
  const metrics = ["Sharpe Ratio", "Max Drawdown", "Win Rate", "Total Trades", "Profit Loss"];
  
  // Colors for different symbols
  const colors = {
    "AAPL": "#ff6b6b",
    "ABBV": "#4ecdc4",
    "ABNB": "#ffd166"
  };

  const formatTooltip = (value) => {
    if (selectedMetric === "Win Rate") {
      return `${(value * 100).toFixed(2)}%`;
    }
    return value.toFixed(4);
  };

  return (
    <div className="p-6 bg-white rounded-lg shadow">
      <h2 className="text-2xl font-bold mb-4">Trading Strategy Comparison</h2>
      
      <div className="mb-6">
        <label className="block text-sm font-medium mb-2">Select Metric:</label>
        <select 
          className="p-2 border rounded w-64"
          value={selectedMetric}
          onChange={(e) => setSelectedMetric(e.target.value)}
        >
          {metrics.map((metric) => (
            <option key={metric} value={metric}>{metric}</option>
          ))}
        </select>
      </div>
      
      <div className="h-64 mb-8">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={data}
            margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="Symbol" />
            <YAxis 
              domain={selectedMetric === "Win Rate" ? [0, 1] : 'auto'}
              tickFormatter={selectedMetric === "Win Rate" ? (tick) => `${(tick * 100).toFixed(0)}%` : undefined}
            />
            <Tooltip formatter={formatTooltip} />
            <Legend />
            <Bar 
              dataKey={selectedMetric} 
              name={selectedMetric}
              fill="#8884d8"
              radius={[4, 4, 0, 0]}
              label={{ position: 'top', formatter: formatTooltip }}
            >
              {data.map((entry, index) => (
                <Bar key={`cell-${index}`} fill={colors[entry.Symbol]} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      
      <div className="overflow-x-auto mb-6">
        <table className="min-w-full border-collapse">
          <thead className="bg-gray-100">
            <tr>
              <th className="border px-4 py-2">Symbol</th>
              <th className="border px-4 py-2">Parameters</th>
              <th className="border px-4 py-2">Sharpe Ratio</th>
              <th className="border px-4 py-2">Max Drawdown</th>
              <th className="border px-4 py-2">Win Rate</th>
              <th className="border px-4 py-2">Total Trades</th>
              <th className="border px-4 py-2">Profit Loss</th>
            </tr>
          </thead>
          <tbody>
            {data.map((row, index) => (
              <tr key={index} className={index % 2 === 0 ? 'bg-gray-50' : ''}>
                <td className="border px-4 py-2">{row.Symbol}</td>
                <td className="border px-4 py-2">{row.Parameters}</td>
                <td className="border px-4 py-2">{row["Sharpe Ratio"].toFixed(4)}</td>
                <td className="border px-4 py-2">{row["Max Drawdown"].toFixed(4)}</td>
                <td className="border px-4 py-2">{(row["Win Rate"] * 100).toFixed(2)}%</td>
                <td className="border px-4 py-2">{row["Total Trades"]}</td>
                <td className="border px-4 py-2">{row["Profit Loss"].toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      
      <div className="p-4 bg-gray-50 rounded">
        <h3 className="font-bold mb-2">Analysis Summary</h3>
        <p>
          AAPL shows the highest profit (1.35) but has moderate drawdown (21.7%).
          ABBV has the best Sharpe ratio (0.78) with lowest drawdown (13.8%), suggesting better risk-adjusted returns.
          ABNB has highest win rate (50%) but lowest Sharpe ratio (0.53) and profit (0.79).
        </p>
      </div>
    </div>
  );
};

export default StrategyDashboard;