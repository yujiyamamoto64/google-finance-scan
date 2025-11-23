package com.yujiyamamoto64.googlefinancescan.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory IP rate limiter with a simple sliding window.
 * Goal: mitigate abusive traffic (DoS) for the current tier.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

	private static final int REQUESTS_PER_MINUTE = 120; // conservative default
	private static final long WINDOW_MS = 60_000L;
	private static final long STALE_ENTRY_MS = WINDOW_MS * 5; // drop inactive IPs to avoid memory leaks
	private static final int MAX_TRACKED_IPS = 5_000; // avoid map explosion with spoofed IPs
	private static final int HOUSEKEEPING_EVERY = 50;

	private static class Counter {
		final AtomicInteger count = new AtomicInteger(0);
		volatile long windowStart;

		Counter(long now) {
			this.windowStart = now;
		}
	}

	private final Map<String, Counter> counters = new ConcurrentHashMap<>();
	private final AtomicInteger housekeepingCursor = new AtomicInteger(0);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String clientIp = extractClientIp(request);

		if (!allowRequest(clientIp)) {
			log.warn("Rate limit exceeded for IP {}", clientIp);
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setHeader(HttpHeaders.RETRY_AFTER, "60");
			response.getWriter().write("Too many requests. Please slow down.");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean allowRequest(String ip) {
		long now = Instant.now().toEpochMilli();

		Counter counter = counters.get(ip);
		if (counter == null) {
			if (counters.size() >= MAX_TRACKED_IPS) {
				return false;
			}
			counter = new Counter(now);
			Counter existing = counters.putIfAbsent(ip, counter);
			if (existing != null) {
				counter = existing;
			}
		}

		synchronized (counter) {
			long elapsed = now - counter.windowStart;
			if (elapsed > WINDOW_MS) {
				counter.windowStart = now;
				counter.count.set(0);
			}
			int current = counter.count.incrementAndGet();
			if (current <= REQUESTS_PER_MINUTE) {
				housekeeping(now);
				return true;
			}
			return false;
		}
	}

	private String extractClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private void housekeeping(long now) {
		if (housekeepingCursor.incrementAndGet() % HOUSEKEEPING_EVERY != 0) {
			return;
		}
		counters.entrySet().removeIf(entry -> now - entry.getValue().windowStart > STALE_ENTRY_MS);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// Only throttle API routes; static assets are left alone.
		String path = request.getRequestURI();
		return !path.startsWith("/api/");
	}
}
