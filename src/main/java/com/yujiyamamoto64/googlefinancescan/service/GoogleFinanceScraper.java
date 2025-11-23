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

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0 Safari/537.36";

	public StockIndicators fetchIndicators(String ticker, String exchange) {
		String normalizedExchange = Optional.ofNullable(exchange).filter(s -> !s.isBlank()).orElse("BVMF");
		String url = "https://www.google.com/finance/quote/" + ticker + ":" + normalizedExchange;

		try {
			Document doc = Jsoup.connect(url)
					.userAgent(USER_AGENT)
					.timeout(20_000)
					.followRedirects(true)
					.get();

			double price = parsePrice(doc);
			Double changePercent = parseChangePercent(doc);
			String currency = guessCurrency(doc, normalizedExchange);
			String companyName = parseCompanyName(doc, ticker);
			String sector = parseSector(doc);

			Double marketCap = parseStat(doc, "Market cap", "Valor de mercado");

			Double priceToBook = parseStat(doc, "Price to book", "P/VP", "P/VPA");
			Double equity = parseStat(doc, "Shareholders' equity", "Patrimônio líquido", "Capital próprio");

			Double pe = parseStat(doc, "P/E ratio", "P/L");
			Double ebitdaMargin = parseStat(doc, "EBITDA margin", "Margem EBITDA");

			Double roe = parseStat(doc,
					"ROE",
					"Return on equity",
					"Retorno sobre patrimônio",
					"Retorno sobre capital próprio",
					"Retorno sobre PL");

			Double debtToEquity = parseStat(doc, "Debt / equity", "Debt to equity");

			Double eps = parseStat(doc, "EPS", "Earnings per share", "EPS (TTM)");

			Double sharesOutstanding = parseSharesOutstanding(doc);

			Double dividendYield = parseStat(doc, "Dividend yield", "Dividendos");

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
					pe,
					ebitdaMargin,
					roe,
					debtToEquity,
					eps,
					sharesOutstanding,
					dividendYield);

		} catch (HttpStatusException e) {
			throw new IllegalStateException("Google Finance retornou status " + e.getStatusCode() + " para " + ticker,
					e);
		} catch (IOException | ParseException e) {
			throw new IllegalStateException("Erro ao acessar Google Finance: " + e.getMessage(), e);
		}
	}

	// ============================
	// PARSERS PRINCIPAIS
	// ============================

	private double parsePrice(Document doc) throws ParseException {
		Element priceEl = Optional.ofNullable(doc.selectFirst("div.YMlKec.fxKbKc"))
				.orElse(doc.selectFirst(".YMlKec"));
		if (priceEl == null)
			throw new IllegalStateException("Não encontrei o preço.");
		return parseNumeric(priceEl.text());
	}

	private Double parseChangePercent(Document doc) {
		Element el = doc.select("div.zWwE1 .JwB6zf, span.JwB6zf").first();
		return el != null ? parseOptionalNumeric(el.text()) : null;
	}

	private String guessCurrency(Document doc, String exchange) {
		Element priceEl = doc.selectFirst(".YMlKec");
		if (priceEl != null) {
			String text = priceEl.text().trim();
			if (text.startsWith("R$"))
				return "BRL";
			if (text.startsWith("$"))
				return "USD";
			if (text.startsWith("€"))
				return "EUR";
		}
		return exchange.equalsIgnoreCase("BVMF") ? "BRL" : "USD";
	}

	private String parseCompanyName(Document doc, String fallback) {
		Element el = doc.selectFirst("div.zzDege, div.e1AOyf h1");
		return el != null ? el.text() : fallback;
	}

	private String parseSector(Document doc) {
		Element el = doc.selectFirst("*:matchesOwn(^Sector$)");
		if (el != null && el.nextElementSibling() != null)
			return el.nextElementSibling().text();

		Element meta = doc.selectFirst("meta[name=description]");
		return meta != null ? meta.attr("content") : null;
	}

	// ====================================================
	// FUNÇÃO MAIS IMPORTANTE — PARSE SEGURO DE INDICADOR
	// ====================================================
	private Double parseStat(Document doc, String... labels) {
		for (String label : labels) {
			Double val = parseStatSingle(doc, label);
			if (val != null)
				return val;
		}
		return null;
	}

	private Double parseStatSingle(Document doc, String label) {
		String esc = Pattern.quote(label);

		// 1) Novo padrão do Google Finance
		Element row = doc.selectFirst("div.P6K39c:has(div.mfs7Fc:matchesOwn(" + esc + "))");
		if (row != null) {
			Element value = row.selectFirst("div[jsname=U8sYAd]");
			if (value != null && !value.text().isBlank())
				return parseOptionalNumeric(value.text());
		}

		// 2) fallback direto — sem "andar pelos irmãos"
		Element labelEl = doc.selectFirst("*:matchesOwn(" + esc + ")");
		if (labelEl != null) {
			Element sibling = labelEl.parent().selectFirst("div[jsname=U8sYAd]");
			if (sibling != null)
				return parseOptionalNumeric(sibling.text());
		}

		return null;
	}

	// ============================
	// SHARES OUTSTANDING — FIX REAL
	// ============================

	private Double parseSharesOutstanding(Document doc) {
		Double raw = parseStat(doc,
				"Shares outstanding",
				"Ações em circulação",
				"Acoes em circulacao",
				"Total shares",
				"Total de ações",
				"Ações emitidas",
				"Shares");

		if (raw == null)
			return null;

		// ⚠ Ações não podem ter valor negativo ou muito pequeno
		if (raw < 1_000_000)
			return null;

		return raw;
	}

	// ============================
	// P/VP por derivação
	// ============================

	private Double derivePriceToBook(Double pvb, double price, Double equity, Double shares) {
		if (pvb != null)
			return pvb;
		if (equity == null || shares == null || shares <= 0)
			return null;

		double bookValue = equity / shares;
		if (bookValue <= 0)
			return null;

		return price / bookValue;
	}

	// ============================
	// PARSER NUMÉRICO ROBUSTO
	// ============================

	private Double parseOptionalNumeric(String raw) {
		try {
			return parseNumeric(raw);
		} catch (Exception e) {
			return null;
		}
	}

	private double parseNumeric(String raw) throws ParseException {
		String cleaned = raw
				.replace("\u00a0", "")
				.replace("%", "")
				.trim();

		// Suporte a "2,3 bi", "400 milhões"
		cleaned = cleaned.toLowerCase()
				.replace("bi", "B")
				.replace("b", "B")
				.replace("milhões", "M")
				.replace("mi", "M")
				.replace("m", "M");

		boolean isPercent = raw.endsWith("%");

		double mult = 1;
		if (cleaned.endsWith("T")) {
			mult = 1_000_000_000_000D;
			cleaned = cleaned.replace("T", "");
		}
		if (cleaned.endsWith("B")) {
			mult = 1_000_000_000D;
			cleaned = cleaned.replace("B", "");
		}
		if (cleaned.endsWith("M")) {
			mult = 1_000_000D;
			cleaned = cleaned.replace("M", "");
		}
		if (cleaned.endsWith("K")) {
			mult = 1_000D;
			cleaned = cleaned.replace("K", "");
		}

		cleaned = cleaned.replaceAll("[^\\d,\\.\\-]", "");
		if (cleaned.contains(",") && cleaned.contains("."))
			cleaned = cleaned.replace(",", "");
		else
			cleaned = cleaned.replace(",", ".");

		Number n = NumberFormat.getNumberInstance(Locale.US).parse(cleaned);
		return n.doubleValue() * mult;
	}
}
