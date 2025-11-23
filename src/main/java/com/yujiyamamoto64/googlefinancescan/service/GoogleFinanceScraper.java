package com.yujiyamamoto64.googlefinancescan.service;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
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
			Double changePercent = parseChangePercent(doc);
			String currency = guessCurrency(doc, normalizedExchange);
			String companyName = parseCompanyName(doc, ticker);
			String sector = parseSector(doc);
			Double marketCap = parseStat(doc, "Market cap");

			Double priceToBook = parseStat(doc, "Price to book");
			Double equity = firstNonNull(
				parseStat(doc, "Capital próprio"),
				parseStat(doc, "Patrimônio líquido"),
				parseStat(doc, "Total equity"),
				parseStat(doc, "Shareholders' equity")
			);
			Double peRatio = parseStat(doc, "P/E ratio");
			Double ebitdaMargin = parseStat(doc, "EBITDA margin");
			Double roe = parseStat(doc, "Return on equity");
			Double debtToEquity = parseStat(doc, "Debt / equity");
			Double eps = firstNonNull(
				parseStat(doc, "EPS"),
				parseStat(doc, "Earnings per share"),
				parseStat(doc, "EPS (TTM)")
			);
			Double sharesOutstanding = parseStat(doc, "Shares outstanding");
			Double dividendYield = parseStat(doc, "Dividend yield");
			Double derivedPriceToBook = derivePriceToBook(priceToBook, price, equity, sharesOutstanding);

			return new StockIndicators(
				ticker,
				normalizedExchange,
				currency,
				price,
				changePercent,
				companyName,
				sector,
				marketCap,
				derivedPriceToBook,
				peRatio,
				ebitdaMargin,
				roe,
				debtToEquity,
				eps,
				sharesOutstanding,
				dividendYield
			);
		} catch (HttpStatusException ex) {
			throw new IllegalStateException("Google Finance retornou status " + ex.getStatusCode() + " para " + ticker, ex);
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

	private Double parseChangePercent(Document doc) {
		Element changeEl = doc.select("div.zWwE1 .JwB6zf, span.JwB6zf").first();
		if (changeEl != null) {
			return parseOptionalNumeric(changeEl.text());
		}
		return null;
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

	private String parseCompanyName(Document doc, String fallback) {
		Element nameEl = doc.selectFirst("div.zzDege, div.e1AOyf h1, div.e1AOyf .zzDege, div.e1AOyf div.eYanAe");
		if (nameEl != null && !nameEl.text().isBlank()) {
			return nameEl.text();
		}
		return fallback;
	}

	private String parseSector(Document doc) {
		Element sectorLabel = doc.select("*:matchesOwn((?i)^Sector$)").first();
		if (sectorLabel != null) {
			Element sibling = sectorLabel.nextElementSibling();
			if (sibling != null && !sibling.text().isBlank()) {
				return sibling.text();
			}
		}
		Element meta = doc.selectFirst("meta[name=description]");
		if (meta != null) {
			String content = meta.attr("content");
			if (content != null && !content.isBlank()) {
				return content;
			}
		}
		return null;
	}

	private Double parseStat(Document doc, String label) {
		String escapedLabel = escapeLabel(label);
		// 1. Padrão principal do Google Finance atual (2024/2025)
		Element row = doc.selectFirst("div.P6K39c:has(div.mfs7Fc:matchesOwn(" + escapedLabel + "))");
		if (row != null) {
			Element value = row.selectFirst("div.P6K39c[jsname=U8sYAd]");
			if (value != null && !value.text().isBlank()) {
				return parseOptionalNumeric(value.text());
			}
		}

		// 2. fallback genérico
		Element genericLabel = doc.selectFirst("*:matchesOwn(" + escapedLabel + ")");
		if (genericLabel != null) {
			Element sibling = genericLabel.parent().selectFirst("div[jsname=U8sYAd]");
			if (sibling != null) {
				return parseOptionalNumeric(sibling.text());
			}

			// 3. fallback adicional: caminha por irmãos próximos buscando o primeiro valor numérico
			Element next = genericLabel.nextElementSibling();
			int hops = 0;
			while (next != null && hops++ < 4) {
				Double val = parseOptionalNumeric(next.text());
				if (val != null) {
					return val;
				}
				next = next.nextElementSibling();
			}
			Element parentNext = genericLabel.parent() != null ? genericLabel.parent().nextElementSibling() : null;
			hops = 0;
			while (parentNext != null && hops++ < 4) {
				Double val = parseOptionalNumeric(parentNext.text());
				if (val != null) {
					return val;
				}
				parentNext = parentNext.nextElementSibling();
			}
		}

		return null;
	}

	private String escapeLabel(String label) {
		// Escapa regex e caracteres que quebram o seletor (ex: apóstrofo).
		return Pattern.quote(label).replace("'", "\\\\'");
	}

	private Double derivePriceToBook(Double parsedPriceToBook, double price, Double equity, Double sharesOutstanding) {
		if (parsedPriceToBook != null) {
			return parsedPriceToBook;
		}
		if (equity == null || sharesOutstanding == null || sharesOutstanding <= 0) {
			return null;
		}
		double bookValuePerShare = equity / sharesOutstanding;
		if (bookValuePerShare <= 0) {
			return null;
		}
		return price / bookValuePerShare;
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

	@SafeVarargs
	private final <T> T firstNonNull(T... values) {
		for (T v : values) {
			if (v != null) {
				return v;
			}
		}
		return null;
	}
}
