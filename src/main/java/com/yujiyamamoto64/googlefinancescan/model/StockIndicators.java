package com.yujiyamamoto64.googlefinancescan.model;

import java.util.Objects;

public class StockIndicators {
	private final String ticker;
	private final String exchange;
	private final String currency;
	private final double price;
	private final Double changePercent;
	private final String companyName;
	private final String sector;
	private final Double marketCap;
	private final Double priceToBook;
	private final Double returnOnAssets;
	private final Double returnOnCapital;
	private final Double totalAssets;
	private final Double totalLiabilities;
	private final Double netIncome;
	private final Double sharesOutstanding;
	private final Double dividendYield;

	public StockIndicators(
		String ticker,
		String exchange,
		String currency,
		double price,
		Double changePercent,
		String companyName,
		String sector,
		Double marketCap,
		Double priceToBook,
		Double returnOnAssets,
		Double returnOnCapital,
		Double totalAssets,
		Double totalLiabilities,
		Double netIncome,
		Double sharesOutstanding,
		Double dividendYield
	) {
		this.ticker = ticker;
		this.exchange = exchange;
		this.currency = currency;
		this.price = price;
		this.changePercent = changePercent;
		this.companyName = companyName;
		this.sector = sector;
		this.marketCap = marketCap;
		this.priceToBook = priceToBook;
		this.returnOnAssets = returnOnAssets;
		this.returnOnCapital = returnOnCapital;
		this.totalAssets = totalAssets;
		this.totalLiabilities = totalLiabilities;
		this.netIncome = netIncome;
		this.sharesOutstanding = sharesOutstanding;
		this.dividendYield = dividendYield;
	}

	public String getTicker() {
		return ticker;
	}

	public String getExchange() {
		return exchange;
	}

	public String getCurrency() {
		return currency;
	}

	public double getPrice() {
		return price;
	}

	public Double getChangePercent() {
		return changePercent;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getSector() {
		return sector;
	}

	public Double getMarketCap() {
		return marketCap;
	}

	public Double getPriceToBook() {
		return priceToBook;
	}

	public Double getReturnOnAssets() {
		return returnOnAssets;
	}

	public Double getReturnOnCapital() {
		return returnOnCapital;
	}

	public Double getTotalAssets() {
		return totalAssets;
	}

	public Double getTotalLiabilities() {
		return totalLiabilities;
	}

	public Double getNetIncome() {
		return netIncome;
	}

	public Double getSharesOutstanding() {
		return sharesOutstanding;
	}

	public Double getDividendYield() {
		return dividendYield;
	}

	@Override
	public String toString() {
		return "StockIndicators{" +
			"ticker='" + ticker + '\'' +
			", exchange='" + exchange + '\'' +
			", currency='" + currency + '\'' +
			", price=" + price +
			", changePercent=" + changePercent +
			", companyName='" + companyName + '\'' +
			", sector='" + sector + '\'' +
			", marketCap=" + marketCap +
			", priceToBook=" + priceToBook +
			", returnOnAssets=" + returnOnAssets +
			", returnOnCapital=" + returnOnCapital +
			", totalAssets=" + totalAssets +
			", totalLiabilities=" + totalLiabilities +
			", netIncome=" + netIncome +
			", sharesOutstanding=" + sharesOutstanding +
			", dividendYield=" + dividendYield +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof StockIndicators that)) {
			return false;
		}
		return Double.compare(that.price, price) == 0 && Objects.equals(ticker, that.ticker) && Objects.equals(exchange, that.exchange)
			&& Objects.equals(currency, that.currency) && Objects.equals(changePercent, that.changePercent)
			&& Objects.equals(companyName, that.companyName) && Objects.equals(sector, that.sector)
			&& Objects.equals(marketCap, that.marketCap) && Objects.equals(priceToBook, that.priceToBook)
			&& Objects.equals(returnOnAssets, that.returnOnAssets) && Objects.equals(returnOnCapital, that.returnOnCapital)
			&& Objects.equals(totalAssets, that.totalAssets) && Objects.equals(totalLiabilities, that.totalLiabilities)
			&& Objects.equals(netIncome, that.netIncome) && Objects.equals(sharesOutstanding, that.sharesOutstanding)
			&& Objects.equals(dividendYield, that.dividendYield);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ticker, exchange, currency, price, changePercent, companyName, sector, marketCap, priceToBook, returnOnAssets, returnOnCapital,
			totalAssets, totalLiabilities, netIncome, sharesOutstanding, dividendYield);
	}
}
