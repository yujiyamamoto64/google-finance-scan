package com.yujiyamamoto64.googlefinancescan.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yujiyamamoto64.googlefinancescan.entity.StockSnapshot;
import com.yujiyamamoto64.googlefinancescan.repository.StockSnapshotRepository;

/**
 * Bootstrap inicial: garante que todos os tickers da B3 estejam no banco para aparecerem na busca.
 * Usa lista em classpath data/b3-tickers.txt (um ticker por linha, uppercase).
 */
@Component
public class TickerBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(TickerBootstrap.class);

	private final StockSnapshotRepository repository;

	@Value("classpath:data/b3-tickers.txt")
	private Resource tickersResource;

	public TickerBootstrap(StockSnapshotRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		List<TickerRow> tickers = loadTickers();
		int inserted = 0;
		for (String ticker : tickers) {
			boolean exists = repository.findByTickerIgnoreCase(ticker.code()).isPresent();
			if (!exists) {
				StockSnapshot snap = new StockSnapshot();
				snap.setTicker(ticker.code());
				snap.setCompanyName(ticker.name());
				snap.setExchange("BVMF");
				snap.setUpdatedAt(OffsetDateTime.now());
				repository.save(snap);
				inserted++;
			} else {
				// Atualiza nome se vier vazio no banco.
				repository.findByTickerIgnoreCase(ticker.code()).ifPresent(snap -> {
					if (snap.getCompanyName() == null || snap.getCompanyName().isBlank() || snap.getCompanyName().equalsIgnoreCase(snap.getTicker())) {
						snap.setCompanyName(ticker.name());
						repository.save(snap);
					}
				});
			}
		}
		if (inserted > 0) {
			log.info("Seed de tickers B3 concluido: {} novos registros inseridos", inserted);
		}
	}

	private List<TickerRow> loadTickers() throws IOException {
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(tickersResource.getInputStream(), StandardCharsets.UTF_8))) {
			return reader.lines()
				.map(String::trim)
				.filter(line -> !line.isEmpty() && !line.startsWith("#"))
				.map(this::parseRow)
				.collect(Collectors.toList());
		}
	}

	private TickerRow parseRow(String line) {
		String[] parts = line.split(";", 2);
		String code = parts[0].trim().toUpperCase();
		String name = parts.length > 1 ? parts[1].trim() : code;
		return new TickerRow(code, name);
	}

	private record TickerRow(String code, String name) {}
}
