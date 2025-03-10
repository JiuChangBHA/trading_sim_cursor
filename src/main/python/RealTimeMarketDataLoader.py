import os
import time
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
import pandas as pd
from dotenv import load_dotenv
from alpaca.data.historical.stock import StockHistoricalDataClient
from alpaca.data.requests import StockBarsRequest
from alpaca.data.timeframe import TimeFrame, TimeFrameUnit

# Load environment variables from .env file
load_dotenv('src/.env')

# get api key and secret key from .env file
api_key = os.getenv('ALPACA_API_KEY')
secret_key = os.getenv('ALPACA_SECRET_KEY')

if api_key is None or secret_key is None:
    raise ValueError("ALPACA_API_KEY and ALPACA_SECRET_KEY must be set in src/.env file")

# Initialize the client
stock_historical_data_client = StockHistoricalDataClient(api_key, secret_key)

def fetch_sp500_symbols():
    """Fetch S&P 500 symbols from Wikipedia"""
    import requests
    from bs4 import BeautifulSoup
    
    url = "https://en.wikipedia.org/wiki/List_of_S%26P_500_companies"
    response = requests.get(url)
    soup = BeautifulSoup(response.text, 'html.parser')
    
    # Find the first table
    table = soup.find('table', {'class': 'wikitable'})
    symbols = []
    
    for row in table.find_all('tr')[1:]:  # Skip header row
        cols = row.find_all('td')
        if cols:
            symbol = cols[0].text.strip()
            symbols.append(symbol)
    
    return symbols

def fetch_historical_data_chunk(symbol, start_date, end_date):
    """Fetch a chunk of historical data for a single symbol"""
    try:
        req = StockBarsRequest(
            symbol_or_symbols=symbol,
            timeframe=TimeFrame(amount=1, unit=TimeFrameUnit.Day),
            start=start_date,
            end_date=end_date,
            limit=1000  # Maximum allowed by free plan
        )
        df = stock_historical_data_client.get_stock_bars(req).df
        
        if df is None or df.empty:
            return None
            
        # Format the data as requested
        df = df.reset_index()
        df['Date'] = pd.to_datetime(df['timestamp']).dt.date
        df = df.rename(columns={
            'symbol': 'Symbol',
            'open': 'Open',
            'high': 'High',
            'low': 'Low',
            'close': 'Close',
            'volume': 'Volume'
        })
        
        # Select only the required columns
        df = df[['Date', 'Symbol', 'Open', 'High', 'Low', 'Close', 'Volume']]
        
        return df
    except Exception as e:
        print(f"Error fetching data chunk for {symbol}: {str(e)}")
        return None

def fetch_historical_data(symbol, start_date, end_date):
    """Fetch all historical data for a single symbol by breaking it into chunks"""
    all_data = []
    current_start = start_date
    
    while current_start < end_date:
        # Calculate chunk end date (1000 days from current start)
        chunk_end = min(current_start + timedelta(days=1000), end_date)
        
        # Fetch chunk
        chunk_df = fetch_historical_data_chunk(symbol, current_start, chunk_end)
        
        if chunk_df is not None and not chunk_df.empty:
            all_data.append(chunk_df)
        
        # Move to next chunk
        current_start = chunk_end + timedelta(days=1)
        
        # Rate limiting between chunks
        time.sleep(1)
    
    if all_data:
        # Combine all chunks
        return pd.concat(all_data, ignore_index=True)
    return None

def process_symbols_in_batches(symbols, batch_size=50):
    """Process symbols in batches to stay within rate limits"""
    for i in range(0, len(symbols), batch_size):
        batch = symbols[i:i + batch_size]
        print(f"Processing batch {i//batch_size + 1} of {(len(symbols) + batch_size - 1)//batch_size}")
        
        for symbol in batch:
            # Calculate date ranges (5 years ago to now)
            end_date = datetime.now(ZoneInfo("America/New_York"))
            start_date = end_date - timedelta(days=5*365)  # 5 years ago
            
            # Create output directory if it doesn't exist
            output_dir = f"market_data_export_{start_date.strftime('%Y-%m-%d')}_to_{end_date.strftime('%Y-%m-%d')}"
            os.makedirs(output_dir, exist_ok=True)
            
            # Fetch data
            df = fetch_historical_data(symbol, start_date, end_date)
            
            if df is not None and not df.empty:
                # Sort by date
                df = df.sort_values('Date')
                
                # Save to CSV
                output_file = f"{output_dir}/{symbol}_data.csv"
                df.to_csv(output_file, index=False)
                print(f"Saved data for {symbol} from {df['Date'].min()} to {df['Date'].max()}")
            
            # Rate limiting: 1 second delay between symbols
            time.sleep(1)

def main():
    # Fetch S&P 500 symbols
    print("Fetching S&P 500 symbols...")
    symbols = fetch_sp500_symbols()
    print(f"Found {len(symbols)} symbols")
    
    # Process symbols in batches
    process_symbols_in_batches(symbols)
    print("Data export completed!")

if __name__ == "__main__":
    main()
