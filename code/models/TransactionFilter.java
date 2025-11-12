package models;

import java.time.LocalDate;

public class TransactionFilter {
	private LocalDate startDate;
	private LocalDate endDate;
	private Double minAmount;
	private Double maxAmount;
	private String category;
	private String type;

	public TransactionFilter() {}

	public boolean matches(Transaction t) {
		if (startDate != null && t.getDate().isBefore(startDate)) return false;
		if (endDate != null && t.getDate().isAfter(endDate)) return false;
		if (minAmount != null && t.getAmount() < minAmount) return false;
		if (maxAmount != null && t.getAmount() > maxAmount) return false;
		if (category != null && !category.isEmpty() && !category.equals(t.getCategory())) return false;
		if (type != null && !type.isEmpty() && !type.equals(t.getType())) return false;
		return true;
	}

	public void setStartDate(LocalDate d) { this.startDate = d; }
	public void setEndDate(LocalDate d) { this.endDate = d; }
	public void setMinAmount(Double v) { this.minAmount = v; }
	public void setMaxAmount(Double v) { this.maxAmount = v; }
	public void setCategory(String c) { this.category = c; }
	public void setType(String t) { this.type = t; }
}
