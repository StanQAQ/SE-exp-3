package repository;

import models.PieData;
import models.TimeSeriesData;
import models.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {
	private final TransactionRepository repo;

	public AnalyticsService(TransactionRepository repo) {
		this.repo = repo;
	}

	public List<PieData> pieByCategory() {
		List<Transaction> all = repo.listTransactions();
		Map<String, Double> sums = new HashMap<>();
		for (Transaction t : all) {
			String c = t.getCategory();
			double amt = t.getAmount();
			if ("expense".equalsIgnoreCase(t.getType())) amt = Math.abs(amt);
			sums.put(c, sums.getOrDefault(c, 0.0) + amt);
		}
		return sums.entrySet().stream()
				.map(e -> new PieData(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}

	public List<TimeSeriesData> timeSeriesMonthly() {
		List<Transaction> all = repo.listTransactions();
		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM");
		Map<String, Double> sums = new TreeMap<>();
		for (Transaction t : all) {
			String period = t.getDate().format(f);
			double amt = t.getAmount();
			if ("expense".equalsIgnoreCase(t.getType())) amt = Math.abs(amt);
			sums.put(period, sums.getOrDefault(period, 0.0) + amt);
		}
		List<TimeSeriesData> out = new ArrayList<>();
		for (Map.Entry<String, Double> e : sums.entrySet()) out.add(new TimeSeriesData(e.getKey(), e.getValue()));
		return out;
	}
}
