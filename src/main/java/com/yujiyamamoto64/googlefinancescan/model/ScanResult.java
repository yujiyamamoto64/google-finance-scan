package com.yujiyamamoto64.googlefinancescan.model;

public class ScanResult {
	private final StockIndicators indicators;
	private final ScoreResult score;

	public ScanResult(StockIndicators indicators, ScoreResult score) {
		this.indicators = indicators;
		this.score = score;
	}

	public StockIndicators getIndicators() {
		return indicators;
	}

	public ScoreResult getScore() {
		return score;
	}
}
