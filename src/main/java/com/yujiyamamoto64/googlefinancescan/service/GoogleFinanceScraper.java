package com.yujiyamamoto64.googlefinancescan.service;

import java.io.IOException;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

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

			Double marketCap = parseStat(doc, "Market cap", "Valor de mercado");

			Double priceToBook = parseStat(doc,
				"Price to book",
				"P/VP",
				"P/VPA",
				"Preco/Valor Patrimonial",
				"Preço/Valor Patrimonial",
				"Preco/Valor Patrimonio",
				"Preço/Valor Patrimonio"
			);
			Double equity = parseStat(doc,
				"Shareholders' equity",
				"Patrimonio liquido",
				"Patrimonio líquido",
				"Capital proprio",
				"Capital próprio"
			);

			Double peRatio = parseStat(doc, "P/E ratio", "P/L", "Price to earnings");
			Double ebitdaMargin = parseStat(doc, "EBITDA margin", "Margem EBITDA");

			Double roe = parseStat(doc,
				"ROE",
				"Return on equity",
				"Retorno sobre patrimonio",
				"Retorno sobre patrim\u00f4nio",
				"Retorno sobre capital proprio",
				"Retorno sobre capital pr\u00f3prio",
				"Retorno sobre PL",
				"Retorno sobre capital"
			);

			Double debtToEquity = parseStat(doc, "Debt / equity", "Debt to equity");

			Double eps = parseStat(doc, "EPS", "Earnings per share", "EPS (TTM)", "LPA");

			Double sharesOutstanding = parseStat(doc,
				"Shares outstanding",
				"Acoes em circulacao",
				"A\u00e7\u00f5es em circula\u00e7\u00e3o",
				"Total shares",
				"Total de acoes",
				"Total de a\u00e7\u00f5es",
				"Acoes emitidas",
				"A\u00e7\u00f5es emitidas",
				"Shares"
			);

			Double dividendYield = parseStat(doc, "Dividend yield", "Dividendos", "Dividend Yield");

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
			throw new IllegalStateException("Nao foi possivel localizar o preco atual da acao.");
		}
		return parseNumeric(priceEl.text());
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
				if (prefix.contains("R$")) return "BRL";
				if (prefix.contains("$")) return "USD";
				if (prefix.contains("\u20ac")) return "EUR";
			}
		}
		return "BVMF".equalsIgnoreCase(exchange) ? "BRL" : "USD";
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

	private Double parseStat(Document doc, String... labels) {
		if (labels == null) {
			return null;
		}
		for (String label : labels) {
			Double val = parseStatSingle(doc, label);
			if (val != null) {
				return val;
			}
		}
		return null;
	}

	private Double parseStatSingle(Document doc, String label) {
		String normalizedLabel = normalizeLabel(label);

		for (Element row : doc.select("div.P6K39c")) {
			Element labelEl = row.selectFirst("div.mfs7Fc");
			if (labelMatches(labelEl, normalizedLabel)) {
				Element value = row.selectFirst("div[jsname=U8sYAd]");
				if (value != null && !value.text().isBlank()) {
					return parseOptionalNumeric(value.text());
				}
			}
		}

		for (Element genericLabel : doc.getAllElements()) {
			if (labelMatches(genericLabel, normalizedLabel)) {
				Element sibling = genericLabel.parent() != null ? genericLabel.parent().selectFirst("div[jsname=U8sYAd]") : null;
				if (sibling != null) {
					return parseOptionalNumeric(sibling.text());
				}
			}
		}

		return null;
	}

	private boolean labelMatches(Element element, String normalizedLabel) {
		if (element == null) {
			return false;
		}
		String text = element.ownText();
		if (text == null || text.isBlank()) {
			text = element.text();
		}
		if (text == null || text.isBlank()) {
			return false;
		}
		return normalizeLabel(text).equals(normalizedLabel);
	}

	private String normalizeLabel(String raw) {
		if (raw == null) {
			return "";
		}
		String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
			.replaceAll("\\p{M}+", "")
			.replaceAll("[^\\p{Alnum}]+", "")
			.toLowerCase(Locale.ROOT);
		return normalized;
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
			return null;
		}
	}

	private double parseNumeric(String raw) throws ParseException {
		String cleaned = raw.replace("\u00a0", "").trim();
		boolean isPercent = cleaned.endsWith("%");
		if (isPercent) {
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}

		cleaned = cleaned.toLowerCase()
			.replace(" bi", "B").replace("b", "B")
			.replace(" mi", "M").replace("m", "M")
			.replace(" mil", "K");

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

		cleaned = cleaned.replaceAll("[^\\d,\\.\\-]", "");
		if (cleaned.contains(",") && cleaned.contains(".")) {
			cleaned = cleaned.replace(",", "");
		} else {
			cleaned = cleaned.replace(",", ".");
		}

		if (cleaned.isBlank()) {
			throw new ParseException("Valor vazio apos limpeza: " + raw, 0);
		}

		Number number = NumberFormat.getNumberInstance(Locale.US).parse(cleaned);
		double value = number.doubleValue() * multiplier;
		return isPercent ? value : value;
	}

}
