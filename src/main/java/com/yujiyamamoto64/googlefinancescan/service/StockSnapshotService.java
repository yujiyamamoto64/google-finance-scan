package com.yujiyamamoto64.googlefinancescan.service;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yujiyamamoto64.googlefinancescan.entity.StockSnapshot;
import com.yujiyamamoto64.googlefinancescan.model.StockIndicators;
import com.yujiyamamoto64.googlefinancescan.repository.StockSnapshotRepository;

@Service
public class StockSnapshotService {

	private static final Logger log = LoggerFactory.getLogger(StockSnapshotService.class);
	private final StockSnapshotRepository repository;

	public StockSnapshotService(StockSnapshotRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public StockSnapshot saveOrUpdate(StockIndicators indicators) {
		Optional<StockSnapshot> existing = repository.findByTickerIgnoreCase(indicators.getTicker());
		StockSnapshot snap = existing.orElseGet(StockSnapshot::new);
		snap.setTicker(indicators.getTicker());
		snap.setCompanyName(indicators.getCompanyName());
		snap.setExchange(indicators.getExchange());
		snap.setSector(indicators.getSector());
		snap.setCurrency(indicators.getCurrency());
		snap.setPrice(indicators.getPrice());
		snap.setChangePercent(indicators.getChangePercent());
		snap.setMarketCap(indicators.getMarketCap());
		snap.setPriceToBook(indicators.getPriceToBook());
		snap.setReturnOnAssets(indicators.getReturnOnAssets());
		snap.setReturnOnCapital(indicators.getReturnOnCapital());
		snap.setTotalAssets(indicators.getTotalAssets());
		snap.setTotalLiabilities(indicators.getTotalLiabilities());
		snap.setNetIncome(indicators.getNetIncome());
		snap.setSharesOutstanding(indicators.getSharesOutstanding());
		snap.setDividendYield(indicators.getDividendYield());
		snap.setUpdatedAt(OffsetDateTime.now());
		StockSnapshot saved = repository.save(snap);
		log.info("{} atualizado em {}", saved.getTicker(), saved.getUpdatedAt());
		return saved;
	}
}
