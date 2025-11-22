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

		double roaPoints = scoreHigherIsBetter(indicators.getReturnOnAssets(), 8.0, 20.0, 20);
		total += roaPoints;
		parts.add(new ScoreBreakdown("Return on Assets (%)", indicators.getReturnOnAssets(), 20, roaPoints,
			"ROA acima de 20% maximiza a nota; abaixo de 8% perde pontos."));

		double rocPoints = scoreHigherIsBetter(indicators.getReturnOnCapital(), 10.0, 25.0, 20);
		total += rocPoints;
		parts.add(new ScoreBreakdown("Return on Capital (%)", indicators.getReturnOnCapital(), 20, rocPoints,
			"ROC acima de 25% leva nota máxima; abaixo de 10% zera."));

		Double liabilities = indicators.getTotalLiabilities();
		Double assets = indicators.getTotalAssets();
		Double leverage = (assets != null && assets > 0 && liabilities != null) ? liabilities / assets : null;
		double leveragePoints = scoreLowerIsBetter(leverage, 0.5, 1.0, 15);
		total += leveragePoints;
		parts.add(new ScoreBreakdown("Alavancagem (Passivo/Ativo)", leverage, 15, leveragePoints,
			"<=0,5 é excelente; >=1,0 consome todos os pontos deste critério."));

		double incomePoints = (indicators.getNetIncome() != null && indicators.getNetIncome() > 0) ? 10 : 0;
		total += incomePoints;
		parts.add(new ScoreBreakdown("Lucro líquido positivo", indicators.getNetIncome(), 10, incomePoints,
			incomePoints > 0 ? "Lucro positivo soma estabilidade." : "Lucro negativo retira pontos."));

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
