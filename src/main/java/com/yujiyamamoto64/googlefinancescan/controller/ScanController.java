package com.yujiyamamoto64.googlefinancescan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yujiyamamoto64.googlefinancescan.model.ScanResult;
import com.yujiyamamoto64.googlefinancescan.model.ScoreResult;
import com.yujiyamamoto64.googlefinancescan.model.StockIndicators;
import com.yujiyamamoto64.googlefinancescan.service.GoogleFinanceScraper;
import com.yujiyamamoto64.googlefinancescan.service.ScoringService;
import com.yujiyamamoto64.googlefinancescan.service.StockSnapshotService;

@RestController
@RequestMapping("/api/scan")
public class ScanController {

	private final GoogleFinanceScraper scraper;
	private final ScoringService scoringService;
	private final StockSnapshotService snapshotService;

	public ScanController(GoogleFinanceScraper scraper, ScoringService scoringService, StockSnapshotService snapshotService) {
		this.scraper = scraper;
		this.scoringService = scoringService;
		this.snapshotService = snapshotService;
	}

	@GetMapping("/{ticker}")
	public ScanResult scan(
		@PathVariable String ticker,
		@RequestParam(defaultValue = "BVMF") String exchange
	) {
		StockIndicators indicators = scraper.fetchIndicators(ticker, exchange);
		ScoreResult score = scoringService.score(indicators);
		snapshotService.saveOrUpdate(indicators, score.getScore()); // atualiza o banco a cada busca
		return new ScanResult(indicators, score);
	}
}
