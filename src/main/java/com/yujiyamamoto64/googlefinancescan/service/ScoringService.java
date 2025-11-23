package com.yujiyamamoto64.googlefinancescan.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yujiyamamoto64.googlefinancescan.model.ScoreBreakdown;
import com.yujiyamamoto64.googlefinancescan.model.ScoreResult;
import com.yujiyamamoto64.googlefinancescan.model.StockIndicators;

@Service
public class ScoringService {

	public ScoreResult score(StockIndicators indicators) {
		List<ScoreBreakdown> parts = new ArrayList<>();
		double total = 0.0;

		double pvbPoints = scoreLowerIsBetter(indicators.getPriceToBook(), 1.0, 3.0, 20);
		total += pvbPoints;
		parts.add(new ScoreBreakdown("Preço/Valor Patrimonial", indicators.getPriceToBook(), 20, pvbPoints,
			"P/VPA <= 1,0 ganha todos os pontos; acima de 3,0 perde todos."));

		double pePoints = scoreLowerIsBetter(indicators.getPeRatio(), 6.0, 25.0, 25);
		total += pePoints;
		parts.add(new ScoreBreakdown("P/L (TTM)", indicators.getPeRatio(), 25, pePoints,
			"P/L entre 6 e 15 é saudável; acima de 25 perde todos os pontos."));

		double roePoints = scoreHigherIsBetter(indicators.getRoe(), 8.0, 20.0, 20);
		total += roePoints;
		parts.add(new ScoreBreakdown("ROE (%)", indicators.getRoe(), 20, roePoints,
			"ROE acima de 20% maximiza a nota; abaixo de 8% perde pontos."));

		double epsPoints = (indicators.getEps() != null && indicators.getEps() > 0) ? 10 : 0;
		total += epsPoints;
		parts.add(new ScoreBreakdown("EPS positivo", indicators.getEps(), 10, epsPoints,
			epsPoints > 0 ? "EPS > 0 soma estabilidade." : "EPS negativo zera este critério."));

		double dividendPoints = scoreHigherIsBetter(indicators.getDividendYield(), 2.0, 8.0, 15);
		total += dividendPoints;
		parts.add(new ScoreBreakdown("Dividend Yield (%)", indicators.getDividendYield(), 15, dividendPoints,
			"Yield alto melhora o score, limitado a 8%."));

		double sizePoints = scoreHigherIsBetter(indicators.getMarketCap(), 2_000_000_000D, 10_000_000_000D, 10);
		total += sizePoints;
		parts.add(new ScoreBreakdown("Porte (Market Cap)", indicators.getMarketCap(), 10, sizePoints,
			"Acima de US$10B recebe nota máxima; abaixo de US$2B zera."));

		double resiliencePoints = 5; // bônus fixo, evita score muito baixo se demais dados faltarem.
		total += resiliencePoints;
		parts.add(new ScoreBreakdown("Bônus de estabilidade", null, 5, resiliencePoints,
			"Pequeno bônus para compensar eventuais dados ausentes."));

		double capped = Math.min(100.0, Math.round(total * 10) / 10.0);
		String verdict = verdict(capped);

		return new ScoreResult(capped, verdict, parts);
	}

	private double scoreLowerIsBetter(Double value, double bestThreshold, double worstThreshold, double maxPoints) {
		if (value == null || Double.isNaN(value)) {
			return 0;
		}
		if (value <= bestThreshold) {
			return maxPoints;
		}
		if (value >= worstThreshold) {
			return 0;
		}
		double slope = (value - bestThreshold) / (worstThreshold - bestThreshold);
		return (1 - slope) * maxPoints;
	}

	private double scoreHigherIsBetter(Double value, double minThreshold, double targetThreshold, double maxPoints) {
		if (value == null || Double.isNaN(value)) {
			return 0;
		}
		if (value <= minThreshold) {
			return 0;
		}
		if (value >= targetThreshold) {
			return maxPoints;
		}
		double slope = (value - minThreshold) / (targetThreshold - minThreshold);
		return slope * maxPoints;
	}

	private String verdict(double score) {
		if (score >= 70) {
			return "Compra potencial";
		}
		if (score >= 55) {
			return "Neutro / observar";
		}
		return "Evitar por enquanto";
	}
}
