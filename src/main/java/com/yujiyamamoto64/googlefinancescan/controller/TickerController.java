package com.yujiyamamoto64.googlefinancescan.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yujiyamamoto64.googlefinancescan.model.TickerQuote;
import com.yujiyamamoto64.googlefinancescan.service.GoogleFinanceScraper;
import com.yujiyamamoto64.googlefinancescan.service.StockSnapshotService;

@RestController
@RequestMapping("/api")
public class TickerController {

	private static final List<String> DEFAULT_TICKERS = Arrays.asList(
		"PETR4", "VALE3", "ITUB4", "BBAS3", "ABEV3", "MGLU3", "KLBN11", "WEGE3", "BBDC4"
	);

	private final GoogleFinanceScraper scraper;
	private final StockSnapshotService snapshotService;

	public TickerController(GoogleFinanceScraper scraper, StockSnapshotService snapshotService) {
		this.scraper = scraper;
		this.snapshotService = snapshotService;
	}

	@GetMapping("/tickers")
	public List<TickerQuote> tickerTape() {
		return DEFAULT_TICKERS.stream()
			.map(ticker -> scraper.fetchIndicators(ticker, "BVMF"))
			.peek(snapshotService::saveOrUpdate) // mantém banco atualizado enquanto alimenta o tape
			.map(ind -> new TickerQuote(ind.getTicker(), ind.getPrice(), ind.getChangePercent()))
			.collect(Collectors.toList());
	}
}
