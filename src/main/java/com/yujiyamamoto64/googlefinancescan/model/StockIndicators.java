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
	private final Double ebitdaMargin;
	private final Double roe;
	private final Double debtToEquity;
	private final Double eps;
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
		Double ebitdaMargin,
		Double roe,
		Double debtToEquity,
		Double eps,
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
		this.ebitdaMargin = ebitdaMargin;
		this.roe = roe;
		this.debtToEquity = debtToEquity;
		this.eps = eps;
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

	public Double getEbitdaMargin() {
		return ebitdaMargin;
	}

	public Double getRoe() {
		return roe;
	}

	public Double getDebtToEquity() {
		return debtToEquity;
	}

	public Double getEps() {
		return eps;
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
			", ebitdaMargin=" + ebitdaMargin +
			", roe=" + roe +
			", debtToEquity=" + debtToEquity +
			", eps=" + eps +
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
			&& Objects.equals(ebitdaMargin, that.ebitdaMargin) && Objects.equals(roe, that.roe)
			&& Objects.equals(debtToEquity, that.debtToEquity) && Objects.equals(eps, that.eps)
			&& Objects.equals(sharesOutstanding, that.sharesOutstanding) && Objects.equals(dividendYield, that.dividendYield);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ticker, exchange, currency, price, changePercent, companyName, sector, marketCap, priceToBook, ebitdaMargin, roe,
			debtToEquity, eps, sharesOutstanding, dividendYield);
	}
}
