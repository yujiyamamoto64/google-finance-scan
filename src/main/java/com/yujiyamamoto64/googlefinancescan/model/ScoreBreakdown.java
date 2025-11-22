package com.yujiyamamoto64.googlefinancescan.model;

public class ScoreBreakdown {
	private final String metric;
	private final Double value;
	private final double weight;
	private final double contribution;
	private final String explanation;

	public ScoreBreakdown(String metric, Double value, double weight, double contribution, String explanation) {
		this.metric = metric;
		this.value = value;
		this.weight = weight;
		this.contribution = contribution;
		this.explanation = explanation;
	}

	public String getMetric() {
		return metric;
	}

	public Double getValue() {
		return value;
	}

	public double getWeight() {
		return weight;
	}

	public double getContribution() {
		return contribution;
	}

	public String getExplanation() {
		return explanation;
	}
}
