package com.yujiyamamoto64.googlefinancescan.model;

import java.util.List;

public class ScoreResult {
	private final double score;
	private final String verdict;
	private final List<ScoreBreakdown> breakdown;

	public ScoreResult(double score, String verdict, List<ScoreBreakdown> breakdown) {
		this.score = score;
		this.verdict = verdict;
		this.breakdown = breakdown;
	}

	public double getScore() {
		return score;
	}

	public String getVerdict() {
		return verdict;
	}

	public List<ScoreBreakdown> getBreakdown() {
		return breakdown;
	}
}
