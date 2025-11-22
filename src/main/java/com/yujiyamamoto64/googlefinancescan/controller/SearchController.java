package com.yujiyamamoto64.googlefinancescan.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yujiyamamoto64.googlefinancescan.entity.StockSnapshot;
import com.yujiyamamoto64.googlefinancescan.repository.StockSnapshotRepository;

@RestController
public class SearchController {

	private final StockSnapshotRepository repository;

	public SearchController(StockSnapshotRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/api/search")
	public List<Suggestion> search(@RequestParam("q") String q) {
		String term = q == null ? "" : q.trim();
		if (term.length() < 1) {
			return List.of();
		}
		List<StockSnapshot> matches = repository.findByTickerContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(term, term);
		return matches.stream()
			.map(s -> new Suggestion(s.getTicker(), s.getCompanyName(), s.getScore()))
			.collect(Collectors.toList());
	}

	public record Suggestion(String ticker, String name, Double score) {}
}
