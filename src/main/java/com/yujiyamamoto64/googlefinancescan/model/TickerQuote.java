package com.yujiyamamoto64.googlefinancescan.model;

public class TickerQuote {
	private final String symbol;
	private final double price;
	private final Double changePercent;

	public TickerQuote(String symbol, double price, Double changePercent) {
		this.symbol = symbol;
		this.price = price;
		this.changePercent = changePercent;
	}

	public String getSymbol() {
		return symbol;
	}

	public double getPrice() {
		return price;
	}

	public Double getChangePercent() {
		return changePercent;
	}
}
