package com.yujiyamamoto64.googlefinancescan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yujiyamamoto64.googlefinancescan.entity.StockSnapshot;

public interface StockSnapshotRepository extends JpaRepository<StockSnapshot, Long> {
	Optional<StockSnapshot> findByTickerIgnoreCase(String ticker);
	List<StockSnapshot> findByTickerContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(String ticker, String companyName);
}
