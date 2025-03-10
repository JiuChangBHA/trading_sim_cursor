import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.time.temporal.ChronoUnit;
import java.nio.file.Files;

/**
 * RealTimeMarketDataLoader
 * 
 * A class to fetch real-time market data from financial APIs
 * and integrate with the TradingSimulator application.
 */
public class RealTimeMarketDataLoader {
    
    // API configuration
    private String ALPACA_API_KEY;
    private String ALPACA_API_SECRET;
    private String ALPACA_BASE_URL;
    
    // Alternative free API (backup)
    private static final String FINNHUB_API_KEY = "YOUR_FINNHUB_API_KEY"; // Replace with your API key
    private static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";
    
    // Financial Modeling Prep API for S&P 500 list
    private static final String FMP_API_KEY = "3X3BETqy2TPrB9kJy1ZBOic9zTi1V6uG"; // Replace with your API key
    private static final String FMP_BASE_URL = "https://financialmodelingprep.com/api/v3";
    
    /**
     * Constructor that loads Alpaca credentials from file
     */
    public RealTimeMarketDataLoader() {
        loadAlpacaCredentials();
    }
    
    /**
     * Loads Alpaca API credentials from the alpaca_auth file
     */
    private void loadAlpacaCredentials() {
        try {
            // Use the current working directory
            File authFile = new File("alpaca.auth");
            
            if (!authFile.exists()) {
                throw new FileNotFoundException("alpaca.auth file not found in " + authFile.getAbsolutePath());
            }

            List<String> lines = Files.readAllLines(authFile.toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("key=")) {
                    ALPACA_API_KEY = line.substring(4);
                } else if (line.startsWith("secret=")) {
                    ALPACA_API_SECRET = line.substring(7);
                } else if (line.startsWith("endpoint=")) {
                    ALPACA_BASE_URL = line.substring(9);
                }
            }

            if (ALPACA_API_KEY == null || ALPACA_API_SECRET == null || ALPACA_BASE_URL == null) {
                throw new IllegalStateException("Missing required credentials in alpaca.auth file");
            }

            System.out.println("Successfully loaded Alpaca credentials:");
            System.out.println("API Key: " + ALPACA_API_KEY);
            System.out.println("API Secret: " + ALPACA_API_SECRET);
            System.out.println("Base URL: " + ALPACA_BASE_URL);
        } catch (Exception e) {
            System.err.println("Error loading Alpaca credentials: " + e.getMessage());
            throw new RuntimeException("Failed to load Alpaca credentials", e);
        }
    }
    
    public static void main(String[] args) {
        RealTimeMarketDataLoader loader = new RealTimeMarketDataLoader();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("===== Real-Time Market Data Loader =====");
        System.out.println("1. Load current data for a symbol");
        System.out.println("2. Load historical data with updates");
        System.out.println("3. Live market data monitor");
        System.out.println("4. Export data to TradingSimulator format");
        System.out.println("5. Export market data for all US companies");
        System.out.println("6. Exit");
        
        System.out.print("Select an option: ");
        int option = scanner.nextInt();
        scanner.nextLine(); // consume newline
        
        switch (option) {
            case 1:
                System.out.print("Enter symbol (e.g., AAPL): ");
                String symbol = scanner.nextLine();
                MarketData data = loader.getCurrentMarketData(symbol);
                if (data != null) {
                    System.out.println("\nCurrent Market Data for " + symbol + ":");
                    System.out.println("Date: " + data.getDate());
                    System.out.println("Open: $" + data.getOpen());
                    System.out.println("High: $" + data.getHigh());
                    System.out.println("Low: $" + data.getLow());
                    System.out.println("Close: $" + data.getClose());
                    System.out.println("Volume: " + data.getVolume());
                }
                break;
                
            case 2:
                System.out.print("Enter symbol (e.g., AAPL): ");
                symbol = scanner.nextLine();
                System.out.print("Enter number of days of history: ");
                int days = scanner.nextInt();
                scanner.nextLine(); // consume newline
                
                List<MarketData> historicalData = loader.getHistoricalDataWithUpdates(symbol, days);
                if (historicalData != null && !historicalData.isEmpty()) {
                    System.out.println("\nHistorical Data for " + symbol + " (Last " + 
                                      Math.min(5, historicalData.size()) + " records):");
                    
                    int startIdx = Math.max(0, historicalData.size() - 5);
                    for (int i = startIdx; i < historicalData.size(); i++) {
                        MarketData md = historicalData.get(i);
                        System.out.printf("%s: Open $%.2f, High $%.2f, Low $%.2f, Close $%.2f, Volume %d%n",
                            md.getDate(), md.getOpen(), md.getHigh(), md.getLow(), md.getClose(), md.getVolume());
                    }
                    
                    System.out.println("\nTotal records: " + historicalData.size());
                }
                break;
                
            case 3:
                System.out.print("Enter symbols separated by comma (e.g., AAPL,MSFT,GOOG): ");
                String symbolsInput = scanner.nextLine();
                String[] symbols = symbolsInput.split(",");
                
                System.out.print("Enter refresh interval in seconds (5-60): ");
                int interval = scanner.nextInt();
                interval = Math.max(5, Math.min(60, interval)); // Ensure interval is between 5-60 seconds
                
                System.out.println("\nStarting live market monitor. Press Ctrl+C to stop.");
                loader.startLiveMarketMonitor(symbols, interval);
                break;
                
            case 4:
                System.out.print("Enter symbol (e.g., AAPL): ");
                symbol = scanner.nextLine();
                System.out.print("Enter number of days of history: ");
                days = scanner.nextInt();
                scanner.nextLine(); // consume newline
                
                System.out.print("Enter output file name (e.g., aapl_data.csv): ");
                String fileName = scanner.nextLine();
                
                boolean success = loader.exportDataForTradingSimulator(symbol, days, fileName);
                if (success) {
                    System.out.println("Data successfully exported to " + fileName);
                    System.out.println("You can now use this file with TradingSimulator.");
                }
                break;
                
            case 5:
                System.out.print("Enter start date (YYYY-MM-DD): ");
                String startDate = scanner.nextLine();
                System.out.print("Enter end date (YYYY-MM-DD): ");
                String endDate = scanner.nextLine();
                
                loader.exportAllUSMarketData(startDate, endDate);
                break;
                
            case 6:
                System.out.println("Exiting...");
                break;
                
            default:
                System.out.println("Invalid option.");
                break;
        }
        
        scanner.close();
    }
    
    /**
     * Fetches the current market data for a given symbol
     */
    public MarketData getCurrentMarketData(String symbol) {
        try {
            JSONObject json = fetchDataFromAlpaca("/v2/latest/trades/" + symbol, null);
            
            if (json != null && json.containsKey("trade")) {
                JSONObject trade = (JSONObject) json.get("trade");
                
                if (!trade.isEmpty()) {
                    return parseAlpacaTrade(trade, symbol);
                } else {
                    System.out.println("No data returned for symbol: " + symbol);
                    return null;
                }
            } else {
                // Try backup API
                return getCurrentMarketDataFromFinnhub(symbol);
            }
        } catch (Exception e) {
            System.out.println("Error fetching current market data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Backup method to get current market data from Finnhub
     */
    private MarketData getCurrentMarketDataFromFinnhub(String symbol) {
        try {
            String quoteUrl = FINNHUB_BASE_URL + "/quote?symbol=" + symbol + "&token=" + FINNHUB_API_KEY;
            String response = fetchDataFromUrl(quoteUrl);
            
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            
            if (json != null && !json.isEmpty()) {
                double currentPrice = ((Number) json.get("c")).doubleValue();
                double openPrice = ((Number) json.get("o")).doubleValue();
                double highPrice = ((Number) json.get("h")).doubleValue();
                double lowPrice = ((Number) json.get("l")).doubleValue();
                double previousClose = ((Number) json.get("pc")).doubleValue();
                
                // Finnhub doesn't provide volume in the quote endpoint
                // You would need a separate call to get volume data
                long volume = 0;
                
                return new MarketData(
                    LocalDate.now(),
                    symbol,
                    openPrice,
                    highPrice,
                    lowPrice,
                    currentPrice,
                    volume
                );
            } else {
                System.out.println("No data returned from backup API for symbol: " + symbol);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error with backup API: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Fetches historical data with the most recent updates for a symbol
     */
    public List<MarketData> getHistoricalDataWithUpdates(String symbol, int days) {
        try {
            // Calculate start and end dates
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);
            
            // Format dates for Alpaca API
            String start = startDate.format(DateTimeFormatter.ISO_DATE);
            String end = endDate.format(DateTimeFormatter.ISO_DATE);
            
            // Build URL with query parameters
            String endpoint = String.format("/v2/aggs/ticker/%s/range/1/day/%s/%s", 
                symbol, start, end);
            
            JSONObject json = fetchDataFromAlpaca(endpoint, null);
            
            if (json != null && json.containsKey("results")) {
                JSONArray results = (JSONArray) json.get("results");
                
                if (!results.isEmpty()) {
                    return parseAlpacaBars(results, symbol);
                } else {
                    System.out.println("No historical data returned for symbol: " + symbol);
                    return null;
                }
            } else {
                // Try backup method
                System.out.println("Primary API failed, trying backup method...");
                return getHistoricalDataFromFinnhub(symbol, days);
            }
        } catch (Exception e) {
            System.out.println("Error fetching historical data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Backup method to get historical data from Finnhub
     */
    private List<MarketData> getHistoricalDataFromFinnhub(String symbol, int days) {
        try {
            // Calculate from and to timestamps (Unix timestamps in seconds)
            long toTimestamp = System.currentTimeMillis() / 1000;
            long fromTimestamp = toTimestamp - (days * 24 * 60 * 60);
            
            String candlesUrl = FINNHUB_BASE_URL + "/stock/candle?symbol=" + symbol + 
                              "&resolution=D&from=" + fromTimestamp + "&to=" + toTimestamp + 
                              "&token=" + FINNHUB_API_KEY;
            
            String response = fetchDataFromUrl(candlesUrl);
            
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            
            if (json != null && "ok".equals(json.get("s"))) {
                List<MarketData> data = new ArrayList<>();
                
                JSONArray timestamps = (JSONArray) json.get("t");
                JSONArray opens = (JSONArray) json.get("o");
                JSONArray highs = (JSONArray) json.get("h");
                JSONArray lows = (JSONArray) json.get("l");
                JSONArray closes = (JSONArray) json.get("c");
                JSONArray volumes = (JSONArray) json.get("v");
                
                for (int i = 0; i < timestamps.size(); i++) {
                    long timestamp = ((Number) timestamps.get(i)).longValue();
                    LocalDate date = LocalDate.ofEpochDay(timestamp / (24 * 60 * 60));
                    
                    double open = ((Number) opens.get(i)).doubleValue();
                    double high = ((Number) highs.get(i)).doubleValue();
                    double low = ((Number) lows.get(i)).doubleValue();
                    double close = ((Number) closes.get(i)).doubleValue();
                    long volume = ((Number) volumes.get(i)).longValue();
                    
                    data.add(new MarketData(date, symbol, open, high, low, close, volume));
                }
                
                return data;
            } else {
                System.out.println("No data returned from backup API for symbol: " + symbol);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error with backup API: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Starts a live market monitor that refreshes data at specified intervals
     */
    public void startLiveMarketMonitor(String[] symbols, int intervalInSeconds) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("\n" + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("--------------------------------------------------");
                
                for (String symbol : symbols) {
                    MarketData data = getCurrentMarketData(symbol);
                    if (data != null) {
                        System.out.printf("%-6s: $%-8.2f | Open: $%-8.2f | High: $%-8.2f | Low: $%-8.2f | Vol: %d%n",
                            symbol, data.getClose(), data.getOpen(), data.getHigh(), data.getLow(), data.getVolume());
                    } else {
                        System.out.printf("%-6s: Data unavailable%n", symbol);
                    }
                }
            }
        }, 0, intervalInSeconds * 1000);
    }
    
    /**
     * Exports data in a format compatible with the TradingSimulator
     */
    public boolean exportDataForTradingSimulator(String symbol, int days, String fileName) {
        List<MarketData> data = getHistoricalDataWithUpdates(symbol, days);
        
        if (data == null || data.isEmpty()) {
            System.out.println("No data available to export.");
            return false;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            // Write header
            writer.println("Date,Symbol,Open,High,Low,Close,Volume");
            
            // Write data
            for (MarketData md : data) {
                writer.printf("%s,%s,%.2f,%.2f,%.2f,%.2f,%d%n",
                    md.getDate().format(DateTimeFormatter.ISO_DATE),
                    md.getSymbol(),
                    md.getOpen(),
                    md.getHigh(),
                    md.getLow(),
                    md.getClose(),
                    md.getVolume());
            }
            
            return true;
        } catch (IOException e) {
            System.out.println("Error exporting data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Exports market data for all US companies within specified date range
     */
    public void exportAllUSMarketData(String startDate, String endDate) {
        try {
            // Fetch current S&P 500 constituents
            List<String> symbols = fetchSP500Symbols();
            
            if (symbols.isEmpty()) {
                System.out.println("Error: Failed to fetch company list. Please try again later.");
                return;
            }
            
            // Calculate estimates
            int totalCompanies = symbols.size();
            long days = ChronoUnit.DAYS.between(LocalDate.parse(startDate), LocalDate.parse(endDate));
            
            // More accurate size estimation:
            // Average CSV line: "YYYY-MM-DD,SYMBOL,123.45,123.45,123.45,123.45,1234567\n" ≈ 60 bytes
            // Header line: "Date,Symbol,Open,High,Low,Close,Volume\n" ≈ 40 bytes
            // Add 10% buffer for potential larger numbers and symbols
            long avgDailyDataSize = 66; // 60 bytes * 1.1 for buffer
            long estimatedFileSize = (totalCompanies * (40 + (days * avgDailyDataSize))); // bytes
            
            // More accurate runtime estimation:
            // Alpha Vantage free tier: 5 API calls per minute = 12 seconds between calls
            // Each company needs 1 API call
            // Add 20% buffer for network latency and processing
            long estimatedRuntimeSeconds = (totalCompanies * 12 * 120) / 100;
            
            // Show estimates and ask for confirmation
            System.out.printf("\nEstimated Statistics:\n");
            System.out.printf("Total companies: %d\n", totalCompanies);
            System.out.printf("Date range: %s to %s (%d days)\n", startDate, endDate, days);
            
            // File size in appropriate units
            String fileSizeStr;
            if (estimatedFileSize > 1_000_000_000) { // > 1GB
                fileSizeStr = String.format("%.2f GB", estimatedFileSize / 1_000_000_000.0);
            } else if (estimatedFileSize > 1_000_000) { // > 1MB
                fileSizeStr = String.format("%.2f MB", estimatedFileSize / 1_000_000.0);
            } else if (estimatedFileSize > 1_000) { // > 1KB
                fileSizeStr = String.format("%.2f KB", estimatedFileSize / 1_000.0);
            } else {
                fileSizeStr = estimatedFileSize + " bytes";
            }
            System.out.printf("Estimated file size: %s\n", fileSizeStr);
            
            // Runtime in appropriate units
            if (estimatedRuntimeSeconds < 60) {
                System.out.printf("Estimated runtime: %d seconds\n", estimatedRuntimeSeconds);
            } else if (estimatedRuntimeSeconds < 3600) {
                System.out.printf("Estimated runtime: %.1f minutes\n", estimatedRuntimeSeconds / 60.0);
            } else {
                System.out.printf("Estimated runtime: %.1f hours\n", estimatedRuntimeSeconds / 3600.0);
            }
            
            System.out.printf("Estimated completion time: %s\n",
                LocalDateTime.now().plusSeconds(estimatedRuntimeSeconds)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.print("\nProceed with export? (yes/no): ");
            
            Scanner scanner = new Scanner(System.in);
            String confirmation = scanner.nextLine();
            
            if (!confirmation.equalsIgnoreCase("yes")) {
                System.out.println("Export cancelled.");
                return;
            }
            
            // Create output directory
            String outputDir = "market_data_export_" + startDate + "_to_" + endDate;
            new File(outputDir).mkdirs();
            
            // Export data for each symbol
            int processed = 0;
            for (String symbol : symbols) {
                try {
                    List<MarketData> data = getHistoricalDataWithUpdates(symbol, 
                        (int) ChronoUnit.DAYS.between(LocalDate.parse(startDate), LocalDate.parse(endDate)));
                    
                    if (data != null && !data.isEmpty()) {
                        String fileName = outputDir + "/" + symbol + "_data.csv";
                        exportDataForTradingSimulator(symbol, data, fileName);
                        processed++;
                        
                        // Show progress
                        if (processed % 10 == 0) {
                            System.out.printf("Processed %d/%d companies (%.1f%%)\n", 
                                processed, totalCompanies, (processed * 100.0 / totalCompanies));
                        }
                    }
                    
                    // Rate limiting to avoid API restrictions
                    Thread.sleep(12000); // 5 API calls per minute = 12 seconds between calls
                    
                } catch (Exception e) {
                    System.out.println("Error processing " + symbol + ": " + e.getMessage());
                }
            }
            
            System.out.println("\nExport completed!");
            System.out.println("Successfully processed " + processed + " companies");
            System.out.println("Data exported to: " + outputDir);
            
        } catch (Exception e) {
            System.out.println("Error during export: " + e.getMessage());
        }
    }
    
    /**
     * Modified version of exportDataForTradingSimulator that accepts MarketData list
     */
    private boolean exportDataForTradingSimulator(String symbol, List<MarketData> data, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            // Write header
            writer.println("Date,Symbol,Open,High,Low,Close,Volume");
            
            // Write data
            for (MarketData md : data) {
                writer.printf("%s,%s,%.2f,%.2f,%.2f,%.2f,%d%n",
                    md.getDate().format(DateTimeFormatter.ISO_DATE),
                    md.getSymbol(),
                    md.getOpen(),
                    md.getHigh(),
                    md.getLow(),
                    md.getClose(),
                    md.getVolume());
            }
            
            return true;
        } catch (IOException e) {
            System.out.println("Error exporting data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Fetches data from Alpaca API
     */
    private JSONObject fetchDataFromAlpaca(String endpoint, String queryParams) {
        try {
            String urlString = ALPACA_BASE_URL + endpoint;
            if (queryParams != null) {
                urlString += "?" + queryParams;
            }
            
        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
            
            // Add Alpaca API authentication headers
            connection.setRequestProperty("APCA-API-KEY-ID", ALPACA_API_KEY);
            connection.setRequestProperty("APCA-API-SECRET-KEY", ALPACA_API_SECRET);
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
                JSONParser parser = new JSONParser();
                return (JSONObject) parser.parse(response.toString());
        } else {
            throw new IOException("HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Error fetching data from Alpaca: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses a trade response from Alpaca
     */
    private MarketData parseAlpacaTrade(JSONObject trade, String symbol) {
        LocalDateTime timestamp = LocalDateTime.parse(
            (String) trade.get("t"), 
            DateTimeFormatter.ISO_DATE_TIME
        );
        
        double price = ((Number) trade.get("p")).doubleValue();
        long size = ((Number) trade.get("s")).longValue();
        
        // For trade data, we'll use the same price for open/high/low/close
        return new MarketData(
            timestamp.toLocalDate(),
            symbol,
            price,
            price,
            price,
            price,
            size
        );
    }
    
    /**
     * Parses bar data from Alpaca
     */
    private List<MarketData> parseAlpacaBars(JSONArray bars, String symbol) {
        List<MarketData> dataList = new ArrayList<>();
        
        for (Object barObj : bars) {
            JSONObject bar = (JSONObject) barObj;
            
            LocalDateTime timestamp = LocalDateTime.parse(
                (String) bar.get("t"), 
                DateTimeFormatter.ISO_DATE_TIME
            );
            
            double open = ((Number) bar.get("o")).doubleValue();
            double high = ((Number) bar.get("h")).doubleValue();
            double low = ((Number) bar.get("l")).doubleValue();
            double close = ((Number) bar.get("c")).doubleValue();
            long volume = ((Number) bar.get("v")).longValue();
            
            dataList.add(new MarketData(
                timestamp.toLocalDate(),
                symbol,
                open,
                high,
                low,
                close,
                volume
            ));
        }
        
        return dataList;
    }
    
    /**
     * Fetches current S&P 500 constituents list from Wikipedia
     */
    private List<String> fetchSP500Symbols() {
        try {
            String url = "https://en.wikipedia.org/wiki/List_of_S%26P_500_companies";
            System.out.println("Fetching S&P 500 list from Wikipedia...");
            
            String response = fetchDataFromUrl(url);
            
            // Extract table content between first occurrence of <tbody> and </tbody>
            int startIndex = response.indexOf("<tbody>");
            int endIndex = response.indexOf("</tbody>");
            
            if (startIndex == -1 || endIndex == -1) {
                throw new IOException("Could not find table data in Wikipedia page");
            }
            
            String tableContent = response.substring(startIndex, endIndex);
            List<String> symbols = new ArrayList<>();
            
            // Extract symbols from table rows
            // Look for cells containing ticker symbols
            String[] rows = tableContent.split("<tr>");
            for (String row : rows) {
                // Look for cells that contain ticker symbols
                if (row.contains("<td>")) {
                    // Extract text between <td> and </td>
                    int tdStart = row.indexOf("<td>") + 4;
                    int tdEnd = row.indexOf("</td>", tdStart);
                    if (tdStart >= 4 && tdEnd > tdStart) {
                        String cellContent = row.substring(tdStart, tdEnd);
                        // Clean up the content
                        cellContent = cellContent.replaceAll("<[^>]*>", "").trim();
                        
                        // Check if this looks like a ticker symbol (1-5 capital letters)
                        if (cellContent.matches("[A-Z]{1,5}")) {
                            symbols.add(cellContent);
                        }
                    }
                }
            }
            
            if (!symbols.isEmpty()) {
                System.out.println("Successfully fetched " + symbols.size() + " symbols from Wikipedia");
                return symbols;
            }
            
            throw new IOException("No symbols found in Wikipedia page");
            
        } catch (Exception e) {
            System.out.println("Error fetching S&P 500 list from Wikipedia: " + e.getMessage());
            System.out.println("Using backup list...");
            
            // Updated backup list as of March 2024 with sector weights
            List<String> backupList = Arrays.asList(
                // Information Technology (29.7%)
                "AAPL", "MSFT", "NVDA", "AVGO", "AMD", "CRM", "ADBE", "CSCO", "ACN", "ORCL",
                // Financials (12.9%)
                "BRK.B", "JPM", "V", "MA", "BAC", "BLK", "GS", "MS", "SPGI", "AXP",
                // Healthcare (12.8%)
                "UNH", "LLY", "JNJ", "ABBV", "MRK", "TMO", "ABT", "DHR", "BMY", "AMGN",
                // Consumer Discretionary (10.3%)
                "AMZN", "TSLA", "HD", "MCD", "NKE", "SBUX", "TJX", "BKNG", "LOW", "MAR",
                // Communication Services (8.7%)
                "META", "GOOGL", "NFLX", "CMCSA", "T", "VZ", "DIS", "TMUS", "CHTR", "WBD",
                // Industrials (8.4%)
                "CAT", "GE", "HON", "UPS", "BA", "RTX", "LMT", "UNP", "DE", "MMM",
                // Consumer Staples (6.1%)
                "PG", "KO", "PEP", "COST", "WMT", "PM", "MDLZ", "MO", "CL", "EL",
                // Energy (3.4%)
                "XOM", "CVX", "COP", "EOG", "SLB", "PSX", "VLO", "PXD", "MPC", "OXY",
                // Recently Added
                "UBER", "CRWD", "SMCI", "VST", "KKR", "GDDY"
            );
            
            System.out.println("Using " + backupList.size() + " companies from backup list");
            return backupList;
        }
    }
    
    /**
     * Helper method to fetch data from any URL
     */
    private String fetchDataFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            return response.toString();
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }
    
    /**
     * Market Data class compatible with the TradingSimulator
     */
    static class MarketData {
        private LocalDate date;
        private String symbol;
        private double open;
        private double high;
        private double low;
        private double close;
        private long volume;
        
        public MarketData(LocalDate date, String symbol, double open, double high, 
                         double low, double close, long volume) {
            this.date = date;
            this.symbol = symbol;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
        
        public LocalDate getDate() { return date; }
        public String getSymbol() { return symbol; }
        public double getOpen() { return open; }
        public double getHigh() { return high; }
        public double getLow() { return low; }
        public double getClose() { return close; }
        public long getVolume() { return volume; }
    }
}
