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

		double pvbPoints = scoreLowerIsBetter(indicators.getPriceToBook(), 1.0, 3.0, 25);
		total += pvbPoints;
		parts.add(new ScoreBreakdown("Preço/Valor Patrimonial", indicators.getPriceToBook(), 25, pvbPoints,
			"P/VPA <= 1,0 ganha todos os pontos; acima de 3,0 perde todos."));

		double ebitdaPoints = scoreHigherIsBetter(indicators.getEbitdaMargin(), 10.0, 30.0, 20);
		total += ebitdaPoints;
		parts.add(new ScoreBreakdown("EBITDA margin (%)", indicators.getEbitdaMargin(), 20, ebitdaPoints,
			"EBITDA margin acima de 30% leva nota máxima; abaixo de 10% zera."));

		double roePoints = scoreHigherIsBetter(indicators.getRoe(), 8.0, 20.0, 20);
		total += roePoints;
		parts.add(new ScoreBreakdown("ROE (%)", indicators.getRoe(), 20, roePoints,
			"ROE acima de 20% maximiza a nota; abaixo de 8% perde pontos."));

		double debtEquityPoints = scoreLowerIsBetter(indicators.getDebtToEquity(), 0.5, 2.0, 15);
		total += debtEquityPoints;
		parts.add(new ScoreBreakdown("Alavancagem (Debt/Equity)", indicators.getDebtToEquity(), 15, debtEquityPoints,
			"<=0,5 é excelente; >=2,0 consome todos os pontos deste critério."));

		double epsPoints = (indicators.getEps() != null && indicators.getEps() > 0) ? 10 : 0;
		total += epsPoints;
		parts.add(new ScoreBreakdown("EPS positivo", indicators.getEps(), 10, epsPoints,
			epsPoints > 0 ? "EPS > 0 soma estabilidade." : "EPS negativo zera este critério."));

		double dividendPoints = scoreHigherIsBetter(indicators.getDividendYield(), 2.0, 8.0, 5);
		total += dividendPoints;
		parts.add(new ScoreBreakdown("Dividend Yield (%)", indicators.getDividendYield(), 5, dividendPoints,
			"Yield alto melhora o score, limitado a 8%."));

		// Pontos restantes para estabilidade de preço (proxy simples: ausência de dados -> neutro).
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
