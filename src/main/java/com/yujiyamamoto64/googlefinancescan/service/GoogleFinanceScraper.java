package com.yujiyamamoto64.googlefinancescan.service;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.yujiyamamoto64.googlefinancescan.model.StockIndicators;

@Service
public class GoogleFinanceScraper {

	private static final String USER_AGENT =
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0 Safari/537.36";

	public StockIndicators fetchIndicators(String ticker, String exchange) {
		String normalizedExchange = Optional.ofNullable(exchange).filter(s -> !s.isBlank()).orElse("BVMF");
		String url = "https://www.google.com/finance/quote/" + ticker + ":" + normalizedExchange;

		try {
			Connection connection = Jsoup.connect(url)
				.userAgent(USER_AGENT)
				.timeout(20_000)
				.followRedirects(true);

			Document doc = connection.get();

			double price = parsePrice(doc);
			String currency = guessCurrency(doc, normalizedExchange);

			Double priceToBook = parseStat(doc, "Price to book");
			Double returnOnAssets = parseStat(doc, "Return on assets");
			Double returnOnCapital = parseStat(doc, "Return on capital");
			Double totalAssets = parseStat(doc, "Total assets");
			Double totalLiabilities = parseStat(doc, "Total liabilities");
			Double netIncome = parseStat(doc, "Net income");
			Double sharesOutstanding = parseStat(doc, "Shares outstanding");
			Double dividendYield = parseStat(doc, "Dividend yield");

			return new StockIndicators(
				ticker,
				normalizedExchange,
				currency,
				price,
				priceToBook,
				returnOnAssets,
				returnOnCapital,
				totalAssets,
				totalLiabilities,
				netIncome,
				sharesOutstanding,
				dividendYield
			);
		} catch (IOException | ParseException ex) {
			throw new IllegalStateException("Falha ao acessar Google Finance: " + ex.getMessage(), ex);
		}
	}

	private double parsePrice(Document doc) throws ParseException {
		Element priceEl = Optional.ofNullable(doc.selectFirst("div.YMlKec.fxKbKc"))
			.orElse(doc.selectFirst(".YMlKec"));
		if (priceEl == null) {
			throw new IllegalStateException("Não foi possível localizar o preço atual da ação.");
		}
		String raw = priceEl.text();
		return parseNumeric(raw);
	}

	private String guessCurrency(Document doc, String exchange) {
		Element priceEl = Optional.ofNullable(doc.selectFirst("div.YMlKec.fxKbKc"))
			.orElse(doc.selectFirst(".YMlKec"));
		if (priceEl != null) {
			String text = priceEl.text().trim();
			String prefix = text.replaceFirst("[\\d].*$", "").trim();
			if (!prefix.isEmpty()) {
				return currencyFromSymbol(prefix);
			}
		}
		// fallback: Brazilian exchange defaults to BRL, otherwise USD.
		return "BVMF".equalsIgnoreCase(exchange) ? "BRL" : "USD";
	}

	private String currencyFromSymbol(String symbol) {
		if (symbol.contains("R$")) {
			return "BRL";
		}
		if (symbol.contains("$")) {
			return "USD";
		}
		if (symbol.contains("€")) {
			return "EUR";
		}
		return symbol;
	}

	private Double parseStat(Document doc, String label) {
		String regex = "(?i)^" + Pattern.quote(label) + "$";
		// Main table pattern (label cell + td.QXDnM with the value).
		Element labelElement = doc.select("div.rsPbEe:matchesOwn(" + regex + ")").first();
		if (labelElement != null) {
			Element row = labelElement.closest("tr");
			if (row != null) {
				Element valueCell = row.selectFirst("td.QXDnM");
				if (valueCell != null) {
					return parseOptionalNumeric(valueCell.text());
				}
			}
		}

		// Generic fallback: find any element that matches the label text and read the next sibling.
		Element generic = doc.select("*:matchesOwn(" + regex + ")").first();
		if (generic != null) {
			Element sibling = generic.nextElementSibling();
			if (sibling != null && !sibling.text().isBlank()) {
				return parseOptionalNumeric(sibling.text());
			}
		}

		return null;
	}

	private Double parseOptionalNumeric(String raw) {
		if (raw == null || raw.isBlank() || "?".equals(raw.trim())) {
			return null;
		}
		try {
			return parseNumeric(raw);
		} catch (IllegalStateException | ParseException ex) {
			// Se não for possível converter (ex: valor ausente), ignoramos o campo.
			return null;
		}
	}

	private double parseNumeric(String raw) throws ParseException {
		String cleaned = raw.replace("\u00a0", "").trim(); // non-breaking space
		boolean isPercent = cleaned.endsWith("%");
		if (isPercent) {
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}

		double multiplier = 1.0;
		if (cleaned.endsWith("T")) {
			multiplier = 1_000_000_000_000D;
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		} else if (cleaned.endsWith("B")) {
			multiplier = 1_000_000_000D;
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		} else if (cleaned.endsWith("M")) {
			multiplier = 1_000_000D;
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		} else if (cleaned.endsWith("K")) {
			multiplier = 1_000D;
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}

		// Remove currency symbols and thousand separators.
		cleaned = cleaned.replaceAll("[^\\d,\\.\\-]", "");
		// If both comma and dot exist, assume comma is thousand separator.
		if (cleaned.contains(",") && cleaned.contains(".")) {
			cleaned = cleaned.replace(",", "");
		} else {
			// Replace comma decimal separator with dot.
			cleaned = cleaned.replace(",", ".");
		}

		if (cleaned.isBlank()) {
			throw new ParseException("Valor vazio após limpeza: " + raw, 0);
		}

		Number number = NumberFormat.getNumberInstance(Locale.US).parse(cleaned);
		double value = number.doubleValue() * multiplier;
		return isPercent ? value : value;
	}
}
