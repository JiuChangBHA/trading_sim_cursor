package com.tradingsim;

public class TradingAccount {
    private double initialBalance;
    private double balance;
    private double shares;
    
    public TradingAccount(double initialBalance) {
        this.initialBalance = initialBalance;
        this.balance = initialBalance;
        this.shares = 0;
    }
    
    public double getInitialBalance() { return initialBalance; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getShares() { return shares; }
    public void setShares(double shares) { this.shares = shares; }
} 